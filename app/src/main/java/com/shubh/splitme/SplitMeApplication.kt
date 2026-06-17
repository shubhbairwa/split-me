package com.shubh.splitme

import android.app.Application
import com.shubh.splitme.data.firebase.FirebaseAuthRepository
import com.shubh.splitme.data.firebase.FirestoreBillRepository
import com.shubh.splitme.data.firebase.FirestoreGroupRepository
import com.shubh.splitme.data.firebase.FirestoreMemberRepository
import com.shubh.splitme.domain.repository.AuthRepository
import com.shubh.splitme.domain.repository.BillRepository
import com.shubh.splitme.domain.repository.GroupRepository
import com.shubh.splitme.domain.repository.MemberRepository

class SplitMeApplication : Application() {
    
    // To change backend, just swap these implementations
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    val memberRepository: MemberRepository by lazy { FirestoreMemberRepository() }
    val groupRepository: GroupRepository by lazy { FirestoreGroupRepository() }
    val billRepository: BillRepository by lazy { FirestoreBillRepository() }
}
