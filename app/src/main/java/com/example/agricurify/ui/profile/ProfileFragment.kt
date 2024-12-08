package com.example.agricurify.ui.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.agricurify.R
import com.example.agricurify.SplashScreenActivity
import com.example.agricurify.data.preference.Preference
import com.example.agricurify.data.preference.dataStore
import com.example.agricurify.data.remote.ApiConfig
import com.example.agricurify.databinding.FragmentProfileBinding
import com.example.agricurify.ui.changepassword.ChangePasswordActivity
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var croppedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.apply {
            setShowHideAnimationEnabled(false)
            hide()
        }

        loadProfileData()
        setupLogoutButton()
        setupUpdateButton()
        setupImagePickerButton()
        setupFabEditButton()
        setupChangePasswordAction()
    }

    private fun loadProfileData() {
        val context = requireContext()
        val preference = Preference.getInstance(context.dataStore)

        // Show the progress bar when loading data
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val token = preference.getToken().first()

                if (token.isNotEmpty()) {
                    val response = ApiConfig.authentication().getProfile("Bearer $token")

                    response.data?.let { user ->
                        binding.edRegisterName.setText(user.name)
                        binding.edRegisterEmail.setText(user.email)
                        Glide.with(context)
                            .load(user.image)
                            .into(binding.imageView4)
                    }
                } else {
                    Toast.makeText(context, "Token not found, please log in.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load profile data: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // Hide the progress bar once the loading process is complete (success or failure)
                binding.progressBar.visibility = View.GONE
            }
        }
    }


    private fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {
            val context = requireContext()
            val preference = Preference.getInstance(context.dataStore)

            lifecycleScope.launch {
                try {
                    val token = preference.getToken().first()
                    ApiConfig.authentication().logout("Bearer $token")
                    preference.logout()

                    val intent = Intent(context, SplashScreenActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to log out: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupUpdateButton() {
        binding.updateButton.setOnClickListener {
            val name = binding.edRegisterName.text.toString()
            val email = binding.edRegisterEmail.text.toString()

            if (name == binding.edRegisterName.hint && email == binding.edRegisterEmail.hint && croppedImageUri == null) {
                Toast.makeText(requireContext(), "No changes to update", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty() && email.isEmpty() && croppedImageUri == null) {
                Toast.makeText(requireContext(), "Please enter at least one field (name, email, or image).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val context = requireContext()
            val preference = Preference.getInstance(context.dataStore)

            lifecycleScope.launch {
                try {
                    val token = preference.getToken().first()

                    if (token.isNotEmpty()) {
                        val updateRequest = mutableMapOf<String, String>()
                        if (name.isNotEmpty() && name != binding.edRegisterName.hint) {
                            updateRequest["name"] = name
                        }
                        if (email.isNotEmpty() && email != binding.edRegisterEmail.hint) {
                            updateRequest["email"] = email
                        }

                        val response = ApiConfig.authentication().updateUserData("Bearer $token", updateRequest)

                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "User data updated successfully", Toast.LENGTH_SHORT).show()
                            if (croppedImageUri != null) {
                                uploadProfileImageWithToken()
                            } else {
                                loadProfileData()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Failed to update user data", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun compressImage(uri: Uri): File {
        val context = requireContext()
        val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

        val maxSize = 800
        val ratio = maxSize.toFloat() / originalBitmap.width.coerceAtLeast(originalBitmap.height)
        val newWidth = (originalBitmap.width * ratio).toInt()
        val newHeight = (originalBitmap.height * ratio).toInt()

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false)

        val timestamp = System.currentTimeMillis()
        val file = File(context.cacheDir, "image_$timestamp.jpg")
        file.createNewFile()

        val outputStream = FileOutputStream(file)
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.flush()
        outputStream.close()

        return file
    }

    private fun uploadProfileImageWithToken() {
        val context = requireContext()
        val preference = Preference.getInstance(context.dataStore)

        lifecycleScope.launch {
            try {
                val token = preference.getToken().first()

                if (token.isNotEmpty() && croppedImageUri != null) {
                    val compressedImageFile = compressImage(croppedImageUri!!)

                    val fileExtension = getFileExtension(compressedImageFile)
                    if (fileExtension != "jpg" && fileExtension != "jpeg" && fileExtension != "png") {
                        Toast.makeText(requireContext(), "Invalid file type. Only JPG, JPEG, and PNG are allowed.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val requestBody = compressedImageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("profileImage", compressedImageFile.name, requestBody)

                    val response = ApiConfig.authentication().updateProfileImage("Bearer $token", imagePart)

                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Profile image updated successfully", Toast.LENGTH_SHORT).show()
                        loadProfileData()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update profile image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Token or cropped image is missing", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error uploading profile image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileExtension(file: File): String {
        val fileName = file.name
        return fileName.substring(fileName.lastIndexOf(".") + 1).lowercase(Locale.getDefault())
    }

    private fun setupImagePickerButton() {
        binding.imageView4.setOnClickListener {
            openGallery()
        }
    }

    private fun setupFabEditButton() {
        binding.fabEdit.setOnClickListener {
            openGallery()
        }
    }

    private fun setupChangePasswordAction() {
        binding.changePasswordTextView.setOnClickListener {
            val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
            startActivity(intent)
        }
    }

    @Suppress("DEPRECATION")
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    val imageUri = data?.data
                    imageUri?.let {
                        openUCrop(it)
                    }
                }

                UCrop.REQUEST_CROP -> {
                    croppedImageUri = UCrop.getOutput(data!!)

                    if (croppedImageUri != null) {
                        Log.d("ProfileFragment", "Cropped Image URI: $croppedImageUri")

                        Glide.with(requireContext())
                            .load(croppedImageUri)
                            .into(binding.imageView4)
                    }
                }
            }
        }
    }

    private fun openUCrop(uri: Uri) {
        val timestamp = System.currentTimeMillis()
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "image_$timestamp.png"))

        val options = UCrop.Options().apply {
            setCompressionQuality(80)
            setHideBottomControls(true)
            setFreeStyleCropEnabled(true)
            setStatusBarColor(resources.getColor(R.color.primary, null))
            setToolbarColor(resources.getColor(R.color.primary, null))
            setToolbarTitle(getString(R.string.crop_image))
            setToolbarWidgetColor(resources.getColor(R.color.white, null))
        }

        UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .withOptions(options)
            .start(requireContext(), this)
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
