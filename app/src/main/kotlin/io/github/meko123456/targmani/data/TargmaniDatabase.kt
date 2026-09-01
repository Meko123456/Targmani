package io.github.meko123456.targmani.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TranslationRecord::class], version = 1, exportSchema = false)
abstract class TargmaniDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao

    companion object {
        @Volatile private var instance: TargmaniDatabase? = null

        fun get(context: Context): TargmaniDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TargmaniDatabase::class.java,
                "targmani.db",
            ).build().also { instance = it }
        }
    }
}
