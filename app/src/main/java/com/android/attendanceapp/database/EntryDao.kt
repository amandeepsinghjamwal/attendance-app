package com.android.attendanceapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.android.attendanceapp.database.EntriesEntity


@Dao
interface EntryDao {
    @Insert
    suspend fun insertEntry(entriesEntity: EntriesEntity)

    @Query(
        """
        SELECT * FROM entries 
        WHERE userId = :userId 
        ORDER BY entryId DESC 
        LIMIT 1
    """
    )
    suspend fun getLastEntryOfUser(userId: Long): EntriesEntity?

    @Query(
        """
        SELECT e.entryId, e.time, e.isEntry, u.name, u.imageData
        FROM entries e
        INNER JOIN user_entity u ON e.userId = u.userId
        WHERE e.time > :startDate and e.time < :endDate
        """
    )
    suspend fun getTodaysEntries(startDate: Long, endDate: Long): List<TodaysEntryWithUser>
}

data class TodaysEntryWithUser(
    val entryId: Long,
    val time: Long,
    val isEntry: Boolean,
    val Name: String,
    val imageData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TodaysEntryWithUser

        if (entryId != other.entryId) return false
        if (time != other.time) return false
        if (isEntry != other.isEntry) return false
        if (Name != other.Name) return false
        if (!imageData.contentEquals(other.imageData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entryId.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + isEntry.hashCode()
        result = 31 * result + Name.hashCode()
        result = 31 * result + imageData.contentHashCode()
        return result
    }
}

