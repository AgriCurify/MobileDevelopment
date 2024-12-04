package com.example.agricurify.data.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelResponse(

	@field:SerializedName("disease_info")
	val diseaseInfo: DiseaseInfo,

	@field:SerializedName("confidence")
	val confidence: Float,

	@field:SerializedName("label")
	val label: String
): Parcelable


@Parcelize
data class DiseaseInfo(

	@field:SerializedName("treatment")
	val treatment: List<String>,

	@field:SerializedName("name")
	val name: String,

	@field:SerializedName("description")
	val description: String
): Parcelable
