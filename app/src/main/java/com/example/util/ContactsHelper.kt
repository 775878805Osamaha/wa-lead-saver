package com.example.util

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object ContactsHelper {
    const val DEFAULT_CONTACT_NAME = "زبون متجر أومكس"

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

        // 1. Check using PhoneLookup
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.NUMBER,
                ContactsContract.PhoneLookup.DISPLAY_NAME
            )
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return true
                }
            }
        } catch (_: Exception) {
        }

        // 2. Fallback check across phone table
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
                    val cand = normNum ?: rawNum ?: ""
                    val candDigits = cand.filter { it.isDigit() }

                    if (candDigits == digitsOnly) {
                        return true
                    }
                    if (candDigits.length >= 8 && digitsOnly.length >= 8) {
                        val minLen = minOf(8, candDigits.length, digitsOnly.length)
                        if (candDigits.takeLast(minLen) == digitsOnly.takeLast(minLen)) {
                            return true
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return false
    }

    fun saveContact(context: Context, contactName: String = DEFAULT_CONTACT_NAME, phoneNumber: String): Result<Boolean> {
        if (!hasWritePermission(context)) {
            return Result.failure(SecurityException("WRITE_CONTACTS permission not granted"))
        }
        val normalized = PhoneNumberHelper.normalize(phoneNumber)
        if (contactExists(context, normalized)) {
            return Result.success(false) // Already exists
        }

        val finalName = DEFAULT_CONTACT_NAME

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
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, finalName)
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, normalized)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
