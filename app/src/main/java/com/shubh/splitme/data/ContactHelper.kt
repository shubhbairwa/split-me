package com.shubh.splitme.data

import android.content.Context
import android.provider.ContactsContract
import com.shubh.splitme.domain.model.Member

data class ContactInfo(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val photoUri: String?
)

/**
 * Reads the device address book. Needs READ_CONTACTS to be granted.
 * Uses three bulk queries (contacts, phones, emails) instead of two extra queries per contact.
 */
fun fetchContacts(context: Context): List<ContactInfo> {
    val resolver = context.contentResolver

    // First phone number and email per contact id.
    val phones = mutableMapOf<String, String>()
    resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null, null
    )?.use { c ->
        val idCol = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val numberCol = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (c.moveToNext()) {
            val id = c.getString(idCol) ?: continue
            val number = c.getString(numberCol)?.trim().orEmpty()
            if (number.isNotEmpty()) phones.putIfAbsent(id, number)
        }
    }

    val emails = mutableMapOf<String, String>()
    resolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS
        ),
        null, null, null
    )?.use { c ->
        val idCol = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        val addressCol = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
        while (c.moveToNext()) {
            val id = c.getString(idCol) ?: continue
            val address = c.getString(addressCol)?.trim().orEmpty()
            if (address.isNotEmpty()) emails.putIfAbsent(id, address)
        }
    }

    val contacts = mutableListOf<ContactInfo>()
    resolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
        ),
        null, null,
        ContactsContract.Contacts.DISPLAY_NAME + " COLLATE NOCASE ASC"
    )?.use { c ->
        val idCol = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
        val nameCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
        val photoCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
        while (c.moveToNext()) {
            val id = c.getString(idCol) ?: continue
            val phone = phones[id]
            val email = emails[id]
            val name = c.getString(nameCol)?.takeIf { it.isNotBlank() } ?: phone ?: email ?: continue
            contacts.add(ContactInfo(id, name, phone, email, c.getString(photoCol)))
        }
    }
    return contacts
}

/**
 * Digits only, keeping the last 10 so "+91 98765-43210", "098765 43210" and "9876543210"
 * all compare equal. Returns null when there are too few digits to be a usable number.
 */
fun normalizePhone(phone: String?): String? {
    val digits = phone?.filter { it.isDigit() } ?: return null
    if (digits.length < 6) return null
    return digits.takeLast(10)
}

/** True when this member is the same person as the given contact details. */
fun Member.matchesContact(name: String, email: String?, phone: String?): Boolean {
    val myPhone = normalizePhone(phoneNumber)
    val theirPhone = normalizePhone(phone)
    if (myPhone != null && myPhone == theirPhone) return true

    val myEmail = this.email?.trim()?.takeIf { it.isNotEmpty() }
    val theirEmail = email?.trim()?.takeIf { it.isNotEmpty() }
    if (myEmail != null && myEmail.equals(theirEmail, ignoreCase = true)) return true

    // No phone or email on either side to tell them apart: fall back to the name.
    return myPhone == null && theirPhone == null && myEmail == null && theirEmail == null &&
        this.name.trim().equals(name.trim(), ignoreCase = true)
}
