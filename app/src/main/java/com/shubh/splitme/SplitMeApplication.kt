package com.shubh.splitme

import android.app.Application
import androidx.room.Room
import com.shubh.splitme.data.SplitMeDatabase
import com.shubh.splitme.data.repository.BillRepository
import com.shubh.splitme.data.repository.GroupRepository
import com.shubh.splitme.data.repository.MemberRepository

class SplitMeApplication : Application() {
    val database: SplitMeDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            SplitMeDatabase::class.java,
            "splitme_database"
        ).fallbackToDestructiveMigration().build()
    }

    val memberRepository: MemberRepository by lazy {
        MemberRepository(database.memberDao())
    }

    val groupRepository: GroupRepository by lazy {
        GroupRepository(database.groupDao())
    }

    val billRepository: BillRepository by lazy {
        BillRepository(database.billDao())
    }
}
