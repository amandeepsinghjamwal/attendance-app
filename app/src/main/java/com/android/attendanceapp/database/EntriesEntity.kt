package com.android.attendanceapp.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ])
data class EntriesEntity(
    @PrimaryKey(autoGenerate = true)
    val entryId: Long = 0,
    val time: Long,
    val userId:Long,
    val isEntry:Boolean,
)