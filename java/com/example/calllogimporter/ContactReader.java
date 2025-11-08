package com.example.calllogimporter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ContactReader {
    private static final String TAG = "ContactReader";

    private final Context context;
    private final ContentResolver contentResolver;

    public ContactReader(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    public List<Contact> readContacts() {
        List<Contact> contacts = new ArrayList<>();

        try {
            // 查询联系人
            String[] projection = {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
            };

            Cursor contactCursor = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    projection,
                    null, null, null
            );

            if (contactCursor != null) {
                while (contactCursor.moveToNext()) {
                    long contactId = contactCursor.getLong(contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                    String name = contactCursor.getString(contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));

                    // 获取该联系人的电话号码
                    List<String> phoneNumbers = getPhoneNumbers(contactId);

                    for (String phoneNumber : phoneNumbers) {
                        if (isValidPhoneNumber(phoneNumber)) {
                            contacts.add(new Contact(name, phoneNumber, contactId));
                            Log.d(TAG, "找到联系人: " + name + " - " + phoneNumber);
                        }
                    }
                }
                contactCursor.close();
            }

            Log.i(TAG, "成功读取 " + contacts.size() + " 个联系人");

        } catch (SecurityException e) {
            Log.e(TAG, "没有读取联系人的权限", e);
        } catch (Exception e) {
            Log.e(TAG, "读取联系人时出错", e);
        }

        return contacts;
    }

    private List<String> getPhoneNumbers(long contactId) {
        List<String> phoneNumbers = new ArrayList<>();

        try {
            String[] phoneProjection = {
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };

            Cursor phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    phoneProjection,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{String.valueOf(contactId)},
                    null
            );

            if (phoneCursor != null) {
                while (phoneCursor.moveToNext()) {
                    String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phoneNumbers.add(phoneNumber);
                }
                phoneCursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取电话号码时出错", e);
        }

        return phoneNumbers;
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // 简单验证：至少包含7位数字
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        return cleanNumber.length() >= 7;
    }
}