package com.shubh.splitme.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.shubh.splitme.domain.model.Bill
import com.shubh.splitme.domain.model.BillWithShares
import com.shubh.splitme.domain.model.ExpenseShare
import com.shubh.splitme.domain.repository.BillRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreBillRepository : BillRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val billsCollection = firestore.collection("bills")
    private val sharesCollection = firestore.collection("shares")

    override fun getBillsByGroup(groupId: String): Flow<List<BillWithShares>> = callbackFlow {
        val listener = billsCollection.whereEqualTo("groupId", groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val bills = snapshot.toObjects(Bill::class.java)
                launch {
                    val list = bills.map { bill ->
                        val shares = sharesCollection.whereEqualTo("billId", bill.id).get().await().toObjects(ExpenseShare::class.java)
                        BillWithShares(bill, shares)
                    }
                    trySend(list)
                }
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getAllBillsWithShares(): Flow<List<BillWithShares>> = callbackFlow {
        val listener = billsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val bills = snapshot.toObjects(Bill::class.java)
                launch {
                    val list = bills.map { bill ->
                        val shares = sharesCollection.whereEqualTo("billId", bill.id).get().await().toObjects(ExpenseShare::class.java)
                        BillWithShares(bill, shares)
                    }
                    trySend(list)
                }
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun createBillWithShares(bill: Bill, shares: List<ExpenseShare>) {
        val billRef = billsCollection.document()
        val billWithId = bill.copy(id = billRef.id)
        
        firestore.runTransaction { transaction ->
            transaction.set(billRef, billWithId)
            shares.forEach { share ->
                val shareRef = sharesCollection.document()
                transaction.set(shareRef, share.copy(id = shareRef.id, billId = billRef.id))
            }
        }.await()
    }

    override fun getBillWithSharesById(billId: String): Flow<BillWithShares?> = callbackFlow {
        val listener = billsCollection.document(billId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val bill = snapshot.toObject(Bill::class.java)
                if (bill != null) {
                    launch {
                        val shares = sharesCollection.whereEqualTo("billId", bill.id).get().await().toObjects(ExpenseShare::class.java)
                        trySend(BillWithShares(bill, shares))
                    }
                } else {
                    trySend(null)
                }
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun deleteBill(billId: String) {
        // In a real app, delete shares too
        billsCollection.document(billId).delete().await()
        // More robust: use a Cloud Function or batch delete for shares
    }
}
