package com.shubh.splitme.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shubh.splitme.data.dao.BillDao
import com.shubh.splitme.data.dao.GroupDao
import com.shubh.splitme.data.dao.MemberDao
import com.shubh.splitme.data.entity.*

@Database(
    entities = [
        Member::class,
        Group::class,
        GroupMemberCrossRef::class,
        Bill::class,
        ExpenseShare::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SplitMeDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun groupDao(): GroupDao
    abstract fun billDao(): BillDao
}
