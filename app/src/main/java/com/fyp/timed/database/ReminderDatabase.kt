package com.fyp.timed.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fyp.timed.model.AddNewReminderDao
import com.fyp.timed.model.AddNewReminderTable
import com.fyp.timed.util.DATABASE_NAME

@Database(entities = arrayOf(AddNewReminderTable::class),version = 5)
abstract class ReminderDatabase: RoomDatabase(){

    abstract fun addNewReminderDao():AddNewReminderDao

    companion object {
        @Volatile private var instance: ReminderDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context)= instance ?: synchronized(LOCK){
            instance ?: buildDatabase(context).also { instance = it}
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(context,
            ReminderDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

}