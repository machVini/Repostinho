package com.mach.apps.repostinho.data.model

data class Resident(
    val id: String,
    val name: String,
    val isModerator: Boolean = false // Para os dois que controlam o financeiro
)