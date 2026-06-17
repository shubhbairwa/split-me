package com.shubh.splitme.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.domain.repository.MemberRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreMemberRepository : MemberRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val membersCollection = firestore.collection("members")

    override fun getAllMembers(): Flow<List<Member>> = callbackFlow {
        val listener = membersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val members = snapshot.toObjects(Member::class.java)
                trySend(members)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getMemberById(id: String): Member? {
        return membersCollection.document(id).get().await().toObject(Member::class.java)
    }

    override suspend fun saveMember(member: Member) {
        membersCollection.document(member.id).set(member).await()
    }
}
