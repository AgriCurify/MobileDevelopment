package com.example.agricurify.data.database

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val diseaseName: String,
    val description: String,
    val confidence: Float,
    val treatments: String,
    val imageUri: String,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
