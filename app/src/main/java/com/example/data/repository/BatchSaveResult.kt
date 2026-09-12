package com.example.data.repository

data class BatchSaveResult(
    val savedCount: Int,
    val duplicateCount: Int,
    val missingPermission: Boolean,
    val isStopped: Boolean = false,
    val blockedMessage: String? = null
)
