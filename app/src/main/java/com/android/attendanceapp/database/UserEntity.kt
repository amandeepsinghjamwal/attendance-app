package com.android.attendanceapp.database

import android.graphics.Bitmap
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "user_entity")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "userId")
    val userId: Long? = null,
    @ColumnInfo("Name")
    val name: String,
    @ColumnInfo(name = "imageData")
    val imageData: ByteArray,
    @ColumnInfo(name = "embeddings")
    val embeddings: String,
    @ColumnInfo(name = "dob")
    val dob: Long,
    @ColumnInfo(name = "phone")
    val phone: String,
    @ColumnInfo(name = "gender")
    val gender: String,
    @ColumnInfo(name = "address")
    val address: String

) {
    @Ignore
    var bitmap: Bitmap? = null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserEntity

        if (userId != other.userId) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (embeddings != other.embeddings) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId?.hashCode() ?: 0
        result = 31 * result + imageData.contentHashCode()
        result = 31 * result + embeddings.hashCode()
        return result
    }

    fun validateData(): Boolean {
        return name.isNotEmpty() && gender.isNotEmpty() && phone.isNotEmpty() && address.isNotEmpty() && dob > 0
    }
}


data class UserWithEntries(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val entries: List<EntriesEntity>
)
