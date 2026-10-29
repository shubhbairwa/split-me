package com.shubh.splitme.data.firebase

import com.google.firebase.firestore.Query
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
    //
    // Only the user's own groups are listened to: ones they belong to, plus ones they created
    // (even if they later left): two queries, merged by id.
    override fun getGroupsWithMembers(userId: String): Flow<List<GroupWithMembers>> {
        val asMember = groupsCollection.whereArrayContains("memberIds", userId).liveObjects(Group::class.java)
        val asCreator = groupsCollection.whereEqualTo("createdBy", userId).liveObjects(Group::class.java)
        return combine(asMember, asCreator, allMembersFlow()) { memberOf, created, members ->
            val byId = members.associateBy { it.id }
            (memberOf + created).distinctBy { it.id }
                .sortedBy { it.name.lowercase() }
                .map { group -> GroupWithMembers(group, group.memberIds.mapNotNull { byId[it] }) }
        }
    }

    override fun getGroupWithMembersById(groupId: String): Flow<GroupWithMembers?> =
        combine(groupDocFlow(groupId), allMembersFlow()) { group, members ->
            group?.let {
                val byId = members.associateBy { m -> m.id }
                GroupWithMembers(it, it.memberIds.mapNotNull { id -> byId[id] })
            }
        }

    override suspend fun createGroup(group: Group, creatorId: String): String {
        val docRef = groupsCollection.document()
        // Creator and first member are written together, so the group is never briefly ownerless.
        val groupWithId = group.copy(
            id = docRef.id,
            createdBy = creatorId,
            memberIds = (listOf(creatorId) + group.memberIds).distinct()
        )
        docRef.set(groupWithId).await()
        return docRef.id
    }

    override suspend fun addMemberToGroup(groupId: String, memberId: String) {
        groupsCollection.document(groupId).update("memberIds", FieldValue.arrayUnion(memberId)).await()
    }

    override suspend fun addContactToGroup(groupId: String, name: String, email: String?, phone: String?): Member {
        val groupRef = groupsCollection.document(groupId)
        val existing = membersCollection.get().await()
            .toObjects(Member::class.java)
            .filter { it.id.isNotBlank() && it.matchesContact(name, email, phone) }
            // A real account wins over a leftover contact entry with the same number.
            .maxByOrNull { it.hasAccount }
        if (existing != null) {
            val batch = firestore.batch().update(groupRef, "memberIds", FieldValue.arrayUnion(existing.id))
            invitePhone(existing)?.let { batch.update(groupRef, "invitedPhones", FieldValue.arrayUnion(it)) }
            batch.commit().await()
            return existing
        }

        // New member: create it and add it to the group in one atomic write.
        val memberRef = membersCollection.document()
        val member = Member(id = memberRef.id, name = name.trim(), email = email?.trim(), phoneNumber = phone?.trim())
        val batch = firestore.batch()
            .set(memberRef, member)
            .update(groupRef, "memberIds", FieldValue.arrayUnion(member.id))
        invitePhone(member)?.let { batch.update(groupRef, "invitedPhones", FieldValue.arrayUnion(it)) }
        batch.commit().await()
        return member
    }

    override suspend fun removeMemberFromGroup(groupId: String, memberId: String) {
        val groupRef = groupsCollection.document(groupId)
        val member = runCatching { membersCollection.document(memberId).get().await().toObject(Member::class.java) }.getOrNull()
        val batch = firestore.batch().update(groupRef, "memberIds", FieldValue.arrayRemove(memberId))
        // Someone removed before signing up must not be able to join through their invite.
        member?.let(::invitePhone)?.let { batch.update(groupRef, "invitedPhones", FieldValue.arrayRemove(it)) }
        batch.commit().await()
    }

    /** The E.164 number to invite with, for contacts who don't have an account yet. */
    private fun invitePhone(member: Member): String? =
        member.phoneNumber?.trim()?.takeIf { !member.hasAccount && it.startsWith("+") }

    override suspend fun deleteGroup(groupId: String) {
        groupsCollection.document(groupId).delete().await()
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

    private fun <T : Any> Query.liveObjects(type: Class<T>): Flow<List<T>> = callbackFlow {
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
