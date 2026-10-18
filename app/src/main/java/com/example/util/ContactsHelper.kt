package com.example.util

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object ContactsHelper {
    fun hasReadPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWritePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasContactsPermissions(context: Context): Boolean {
        return hasReadPermission(context) && hasWritePermission(context)
    }

    /**
     * Checks if a contact already exists with the given phone number.
     * Uses indexed PhoneLookup first, then targeted indexed SQL queries, and finally safe fallback.
     */
    fun contactExists(context: Context, phoneNumber: String): Boolean {
        if (!hasReadPermission(context)) {
            return false
        }
        val normalizedInput = PhoneNumberHelper.normalize(phoneNumber)
        val digitsOnly = normalizedInput.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) {
            return false
        }

        val contentResolver = context.contentResolver

        // 1. Fast indexed check using PhoneLookup
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return true
                }
            }
        } catch (_: Exception) {
        }

        // 2. Targeted SQL selection check against CommonDataKinds.Phone
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            )
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                arrayOf(normalizedInput),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return true
                }
            }

            if (phoneNumber != normalizedInput) {
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                    arrayOf(phoneNumber),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return true
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 3. Fallback across phone table for format variations and Robolectric compatibility
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            )
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val normIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                while (cursor.moveToNext()) {
                    val rawNum = if (numberIdx >= 0) cursor.getString(numberIdx) else null
                    val normNum = if (normIdx >= 0) cursor.getString(normIdx) else null
                    val candidates = listOfNotNull(rawNum, normNum).filter { it.isNotBlank() }
                    for (cand in candidates) {
                        val candDigits = cand.filter { it.isDigit() }
                        if (candDigits.isNotEmpty() && candDigits == digitsOnly) {
                            return true
                        }

                        val normalizedCand = PhoneNumberHelper.normalize(cand)
                        if (normalizedCand.isNotEmpty() && (normalizedCand == normalizedInput || normalizedCand == phoneNumber)) {
                            return true
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return false
    }

    /**
     * Efficiently checks a batch of phone numbers against existing contacts.
     * Returns a Set of normalized phone numbers that already exist.
     */
    fun findExistingNumbers(context: Context, phoneNumbers: Collection<String>): Set<String> {
        if (!hasReadPermission(context) || phoneNumbers.isEmpty()) {
            return emptySet()
        }

        val existing = mutableSetOf<String>()
        val normalizedMap = mutableMapOf<String, String>()
        for (num in phoneNumbers) {
            val norm = PhoneNumberHelper.normalize(num)
            if (norm.isNotBlank()) {
                normalizedMap[norm] = num
            }
        }

        for ((norm, original) in normalizedMap) {
            if (contactExists(context, original) || (norm != original && contactExists(context, norm))) {
                existing.add(norm)
            }
        }

        return existing
    }

    /**
     * Saves a contact with duplicate check and validation.
     * [skipDuplicateCheck] can be set to true if caller already performed duplicate check.
     */
    fun saveContact(
        context: Context,
        contactName: String,
        phoneNumber: String,
        skipDuplicateCheck: Boolean = false
    ): Result<Boolean> {
        if (!hasWritePermission(context)) {
            return Result.failure(SecurityException("WRITE_CONTACTS permission not granted"))
        }
        val trimmedName = contactName.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Contact name cannot be empty"))
        }
        val normalized = PhoneNumberHelper.normalize(phoneNumber)
        if (!skipDuplicateCheck && contactExists(context, normalized)) {
            return Result.success(false) // Already exists
        }

        return try {
            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = ops.size
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, trimmedName)
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, normalized)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER, normalized)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves a list of contacts in batches using ContentProviderOperation.applyBatch.
     * Minimizes IPC round trips and overhead during bulk save.
     */
    fun saveContactsBatch(
        context: Context,
        contacts: List<Pair<String, String>> // List of (contactName, normalizedNumber)
    ): Result<Int> {
        if (!hasWritePermission(context)) {
            return Result.failure(SecurityException("WRITE_CONTACTS permission not granted"))
        }
        if (contacts.isEmpty()) {
            return Result.success(0)
        }

        var savedCount = 0
        val chunkSize = 50
        for (chunk in contacts.chunked(chunkSize)) {
            val ops = ArrayList<ContentProviderOperation>()
            for ((contactName, normalized) in chunk) {
                val trimmedName = contactName.trim()
                if (trimmedName.isBlank() || normalized.isBlank()) continue

                val rawContactInsertIndex = ops.size
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build()
                )
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, trimmedName)
                        .build()
                )
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, normalized)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER, normalized)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build()
                )
            }

            if (ops.isNotEmpty()) {
                try {
                    context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                    savedCount += chunk.size
                } catch (e: Exception) {
                    for ((cName, cNum) in chunk) {
                        val singleRes = saveContact(context, cName, cNum, skipDuplicateCheck = true)
                        if (singleRes.isSuccess && singleRes.getOrNull() == true) {
                            savedCount++
                        }
                    }
                }
            }
        }

        return Result.success(savedCount)
    }
}
