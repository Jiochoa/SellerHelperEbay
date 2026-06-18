package com.example.sellerhelperebay.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ItemEntryEntity::class, PhotoEntity::class, FieldValueEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemEntryDao(): ItemEntryDao
    abstract fun photoDao(): PhotoDao
    abstract fun fieldValueDao(): FieldValueDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "sellerhelper.db")
                .build()
    }
}
