package com.example.viewmodel

data class ScannedNumberItem(
    val phoneNumber: String,
    val isSelected: Boolean = true,
    val alreadyInContacts: Boolean = false
)
