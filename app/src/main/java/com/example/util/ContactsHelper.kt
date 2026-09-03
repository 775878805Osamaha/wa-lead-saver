package com.example.util

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object ContactsHelper {

    /**
     * Checks if READ_CONTACTS permission is granted.
     */
    fun hasReadPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if WRITE_CONTACTS permission is granted.
     */
    fun hasWritePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if both READ and WRITE contacts permissions are granted.
     */
    fun hasContactsPermissions(context: Context): Boolean {
        return hasReadPermission(context) && hasWritePermission(context)
    }

    /**
     * Checks if a phone number already exists in device contacts (Android/Samsung Contacts).
     * Compares normalized numbers as well as matching the last 8-9 digits.
     */
    fun contactExists(context: Context, phoneNumber: String): Boolean {
        if (!hasReadPermission(context)) return false

        val normalizedInput = PhoneNumberHelper.normalize(phoneNumber)
        val digitsOnlyInput = normalizedInput.filter { it.isDigit() }
        if (digitsOnlyInput.isEmpty()) return false

        val contentResolver = context.contentResolver

        // 1. Try PhoneLookup URI filter
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
            // Fallback to manual query on CommonDataKinds.Phone
        }

        // 2. Direct query on CommonDataKinds.Phone for exact and suffix match
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
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val normalizedIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                while (cursor.moveToNext()) {
                    val existingNumber = if (numberIndex >= 0) cursor.getString(numberIndex) else null
                    val existingNorm = if (normalizedIndex >= 0) cursor.getString(normalizedIndex) else null

                    val cleanExisting = (existingNorm ?: existingNumber ?: "").filter { it.isDigit() }

                    if (cleanExisting == digitsOnlyInput) {
                        return true
                    }

                    // Check suffix match (last 8 digits) to catch local vs international equivalence
                    if (cleanExisting.length >= 8 && digitsOnlyInput.length >= 8) {
                        val suffixLen = minOf(8, cleanExisting.length, digitsOnlyInput.length)
                        val existingSuffix = cleanExisting.takeLast(suffixLen)
                        val inputSuffix = digitsOnlyInput.takeLast(suffixLen)
                        if (existingSuffix == inputSuffix) {
                            return true
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore and return false
        }

        return false
    }

    /**
     * Creates a new contact in Android/Samsung Contacts using ContentResolver and ContentProviderOperation.
     * Never creates duplicate contacts.
     */
    fun saveContact(context: Context, contactName: String, phoneNumber: String): Result<Boolean> {
        if (!hasWritePermission(context)) {
            return Result.failure(SecurityException("WRITE_CONTACTS permission not granted"))
        }

        val normalized = PhoneNumberHelper.normalize(phoneNumber)

        // Strict duplicate check before inserting
        if (contactExists(context, normalized)) {
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

            // Contact Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contactName)
                    .build()
            )

            // Phone Number
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, normalized)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
