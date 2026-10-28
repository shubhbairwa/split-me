package com.shubh.splitme.data.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.shubh.splitme.data.matchesContact
import com.shubh.splitme.domain.model.Group
import com.shubh.splitme.domain.model.GroupWithMembers
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.domain.repository.GroupRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

class FirestoreGroupRepository : GroupRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val groupsCollection = firestore.collection("groups")
    private val membersCollection = firestore.collection("members")

    // Groups and members are both live listeners and joined in memory. Local writes (adding a
    // contact, removing a member) show up straight away, also offline, and there is no
    // per-change server query (the old whereIn lookup was slow, could apply out of order and
    // failed for groups with more than 30 members).
    override fun getGroupsWithMembers(): Flow<List<GroupWithMembers>> =
        combine(groupsCollection.liveObjects(Group::class.java), allMembersFlow()) { groups, members ->
            val byId = members.associateBy { it.id }
            groups.map { group -> GroupWithMembers(group, group.memberIds.mapNotNull { byId[it] }) }
        }

    override fun getGroupWithMembersById(groupId: String): Flow<GroupWithMembers?> =
        combine(groupDocFlow(groupId), allMembersFlow()) { group, members ->
            group?.let {
                val byId = members.associateBy { m -> m.id }
                GroupWithMembers(it, it.memberIds.mapNotNull { id -> byId[id] })
            }
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

    override suspend fun addContactToGroup(groupId: String, name: String, email: String?, phone: String?): Member {
        val existing = membersCollection.get().await()
            .toObjects(Member::class.java)
            .firstOrNull { it.id.isNotBlank() && it.matchesContact(name, email, phone) }
        if (existing != null) {
            addMemberToGroup(groupId, existing.id)
            return existing
        }

        // New member: create it and add it to the group in one atomic write.
        val memberRef = membersCollection.document()
        val member = Member(id = memberRef.id, name = name.trim(), email = email?.trim(), phoneNumber = phone?.trim())
        firestore.batch()
            .set(memberRef, member)
            .update(groupsCollection.document(groupId), "memberIds", FieldValue.arrayUnion(member.id))
            .commit()
            .await()
        return member
    }

    override suspend fun removeMemberFromGroup(groupId: String, memberId: String) {
        groupsCollection.document(groupId).update("memberIds", FieldValue.arrayRemove(memberId)).await()
    }

    override suspend fun deleteGroup(groupId: String) {
        groupsCollection.document(groupId).delete().await()
    }

    override suspend fun getGroupsWithMembersOnce(): List<GroupWithMembers> {
        val groups = groupsCollection.get().await().toObjects(Group::class.java)
        val byId = membersCollection.get().await().toObjects(Member::class.java).associateBy { it.id }
        return groups.map { group -> GroupWithMembers(group, group.memberIds.mapNotNull { byId[it] }) }
    }

    private fun allMembersFlow(): Flow<List<Member>> = membersCollection.liveObjects(Member::class.java)

    private fun groupDocFlow(groupId: String): Flow<Group?> = callbackFlow {
        val listener = groupsCollection.document(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Group::class.java))
        }
        awaitClose { listener.remove() }
    }

    private fun <T : Any> CollectionReference.liveObjects(type: Class<T>): Flow<List<T>> = callbackFlow {
        val listener = addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot.toObjects(type))
        }
        awaitClose { listener.remove() }
    }
}
