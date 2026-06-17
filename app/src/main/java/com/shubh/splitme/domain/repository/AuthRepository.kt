package com.shubh.splitme.domain.repository

import com.shubh.splitme.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<Member?>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signup(email: String, password: String, name: String): Result<Unit>
    suspend fun logout()
    fun isUserLoggedIn(): Boolean
}
