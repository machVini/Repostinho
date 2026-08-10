package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.Resident
import kotlinx.coroutines.flow.Flow

interface ResidentRepository {
    fun getResidents(): Flow<List<Resident>>
    suspend fun saveResident(resident: Resident)
    suspend fun removeResident(residentId: String)
}
