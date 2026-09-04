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
        return findExistingContactName(context, phoneNumber) != null
    }

    /**
     * Finds the existing contact name in device contacts for a given phone number.
     */
    fun findExistingContactName(context: Context, phoneNumber: String): String? {
        if (!hasReadPermission(context)) return null

        val normalizedInput = PhoneNumberHelper.normalize(phoneNumber)
        val digitsOnlyInput = normalizedInput.filter { it.isDigit() }
        if (digitsOnlyInput.isEmpty()) return null

        val contentResolver = context.contentResolver

        // 1. Try PhoneLookup URI filter
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME
            )
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to manual query
        }

        // 2. Direct query on CommonDataKinds.Phone
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
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
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val normalizedIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                while (cursor.moveToNext()) {
                    val existingNumber = if (numberIndex >= 0) cursor.getString(numberIndex) else null
                    val existingNorm = if (normalizedIndex >= 0) cursor.getString(normalizedIndex) else null
                    val cleanExisting = (existingNorm ?: existingNumber ?: "").filter { it.isDigit() }

                    if (cleanExisting == digitsOnlyInput) {
                        return if (nameIndex >= 0) cursor.getString(nameIndex) ?: "Contact" else "Contact"
                    }

                    if (cleanExisting.length >= 8 && digitsOnlyInput.length >= 8) {
                        val suffixLen = minOf(8, cleanExisting.length, digitsOnlyInput.length)
                        if (cleanExisting.takeLast(suffixLen) == digitsOnlyInput.takeLast(suffixLen)) {
                            return if (nameIndex >= 0) cursor.getString(nameIndex) ?: "Contact" else "Contact"
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore
        }

        return null
    }

    /**
     * Reads device contacts list for duplicate analysis.
     */
    fun getAllDeviceContacts(context: Context): List<DeviceContactItem> {
        if (!hasReadPermission(context)) return emptyList()

        val results = mutableListOf<DeviceContactItem>()
        val contentResolver = context.contentResolver

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            )
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val normIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                    val num = if (numIdx >= 0) cursor.getString(numIdx) ?: "" else ""
                    val norm = if (normIdx >= 0) cursor.getString(normIdx) ?: "" else ""
                    val cleanNorm = if (norm.isNotBlank()) norm else PhoneNumberHelper.normalize(num)

                    if (num.isNotBlank()) {
                        results.add(
                            DeviceContactItem(
                                contactId = id,
                                name = name.ifBlank { num },
                                phoneNumber = num,
                                normalizedNumber = cleanNorm
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore
        }

        return results
    }

    /**
     * Updates an existing contact's display name matching the normalized phone number.
     */
    fun updateContactName(context: Context, phoneNumber: String, newName: String): Boolean {
        if (!hasWritePermission(context) || !hasReadPermission(context)) return false
        val digits = phoneNumber.filter { it.isDigit() }
        if (digits.isEmpty()) return false

        return try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            var targetRawContactId: Long? = null

            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val rawIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val num = if (numIdx >= 0) cursor.getString(numIdx) ?: "" else ""
                    val numDigits = num.filter { it.isDigit() }
                    if (numDigits == digits || (numDigits.length >= 8 && digits.length >= 8 && numDigits.takeLast(8) == digits.takeLast(8))) {
                        targetRawContactId = if (rawIdIdx >= 0) cursor.getLong(rawIdIdx) else null
                        break
                    }
                }
            }

            if (targetRawContactId != null) {
                val ops = ArrayList<ContentProviderOperation>()
                val where = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
                val args = arrayOf(
                    targetRawContactId.toString(),
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )

                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(where, args)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
                        .build()
                )
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
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

data class DeviceContactItem(
    val contactId: Long,
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String
)
