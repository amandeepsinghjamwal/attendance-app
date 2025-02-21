package com.android.attendanceapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface UserDao {
    @Insert
    suspend fun addUser(userEntity: UserEntity)

    @Transaction
    @Query("SELECT * FROM user_entity")
    suspend fun getUsersWithEntries(): List<UserWithEntries>

    @Query("SELECT * FROM user_entity")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("DELETE FROM user_entity WHERE userId > 1")
    suspend fun deleteAll()
}