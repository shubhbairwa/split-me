package com.shubh.splitme.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    override suspend fun saveMember(member: Member): Member {
        val docRef = if (member.id.isBlank()) membersCollection.document() else membersCollection.document(member.id)
        val toSave = if (member.id.isBlank()) member.copy(id = docRef.id) else member
        docRef.set(toSave).await()
        return toSave
    }

    override fun observeMember(id: String): Flow<Member?> = callbackFlow {
        val listener = membersCollection.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Member::class.java))
        }
        awaitClose { listener.remove() }
    }

    override suspend fun updateProfile(id: String, name: String, dateOfBirth: String, location: String?) {
        val fields = mapOf(
            "id" to id,
            "name" to name,
            "dateOfBirth" to dateOfBirth,
            "location" to location
        )
        membersCollection.document(id).set(fields, SetOptions.merge()).await()
    }

    override suspend fun updatePhoto(id: String, photoUrl: String?, photoPath: String?) {
        membersCollection.document(id)
            .set(mapOf("id" to id, "photoUrl" to photoUrl, "photoPath" to photoPath), SetOptions.merge())
            .await()
    }
}
