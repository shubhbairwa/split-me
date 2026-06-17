package com.shubh.splitme.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.shubh.splitme.domain.model.Group
import com.shubh.splitme.domain.model.GroupWithMembers
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.domain.repository.GroupRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreGroupRepository : GroupRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val groupsCollection = firestore.collection("groups")
    private val membersCollection = firestore.collection("members")

    override fun getGroupsWithMembers(): Flow<List<GroupWithMembers>> = callbackFlow {
        val listener = groupsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val groups = snapshot.toObjects(Group::class.java)
                launch {
                    val list = groups.map { group ->
                        val members = if (group.memberIds.isNotEmpty()) {
                            membersCollection.whereIn("id", group.memberIds).get().await().toObjects(Member::class.java)
                        } else emptyList()
                        GroupWithMembers(group, members)
                    }
                    trySend(list)
                }
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getGroupWithMembersById(groupId: String): Flow<GroupWithMembers?> = callbackFlow {
        val listener = groupsCollection.document(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val group = snapshot.toObject(Group::class.java)
                if (group != null) {
                    launch {
                        val members = if (group.memberIds.isNotEmpty()) {
                            membersCollection.whereIn("id", group.memberIds).get().await().toObjects(Member::class.java)
                        } else emptyList()
                        trySend(GroupWithMembers(group, members))
                    }
                } else {
                    trySend(null)
                }
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun createGroup(group: Group): String {
        val docRef = groupsCollection.document()
        val groupWithId = group.copy(id = docRef.id)
        docRef.set(groupWithId).await()
        return docRef.id
    }

    override suspend fun addMemberToGroup(groupId: String, memberId: String) {
        groupsCollection.document(groupId).update("memberIds", FieldValue.arrayUnion(memberId)).await()
    }

    override suspend fun deleteGroup(groupId: String) {
        groupsCollection.document(groupId).delete().await()
    }

    override suspend fun getGroupsWithMembersOnce(): List<GroupWithMembers> {
        val snapshot = groupsCollection.get().await()
        val groups = snapshot.toObjects(Group::class.java)
        return groups.map { group ->
            val members = if (group.memberIds.isNotEmpty()) {
                membersCollection.whereIn("id", group.memberIds).get().await().toObjects(Member::class.java)
            } else emptyList()
            GroupWithMembers(group, members)
        }
    }
}
