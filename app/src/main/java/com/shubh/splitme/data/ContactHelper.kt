package com.shubh.splitme.data

import android.content.Context
import android.provider.ContactsContract

data class ContactInfo(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val photoUri: String?
)

fun fetchContacts(context: Context): List<ContactInfo> {
    val contacts = mutableListOf<ContactInfo>()
    val contentResolver = context.contentResolver
    
    val cursor = contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        null, null, null,
        ContactsContract.Contacts.DISPLAY_NAME + " ASC"
    )

    cursor?.use {
        while (it.moveToNext()) {
            val id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID) ?: continue)
            val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME) ?: continue) ?: "Unknown"
            val photoUri = it.getString(it.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI) ?: -1)
            
            var phone: String? = null
            if (it.getInt(it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER) ?: 0) > 0) {
                val pCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    arrayOf(id),
                    null
                )
                pCursor?.use { pc ->
                    if (pc.moveToFirst()) {
                        phone = pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER) ?: -1)
                    }
                }
            }

            var email: String? = null
            val eCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                arrayOf(id),
                null
            )
            eCursor?.use { ec ->
                if (ec.moveToFirst()) {
                    email = ec.getString(ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS) ?: -1)
                }
            }

            contacts.add(ContactInfo(id, name, phone, email, photoUri))
        }
    }
    return contacts
}
