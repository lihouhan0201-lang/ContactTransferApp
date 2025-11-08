package com.example.calllogimporter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallLogWriter {
    private static final String TAG = "CallLogWriter";

    private final ContentResolver contentResolver;
    private final Map<String, Long> contactIdCache; // 缓存电话号码到联系人ID的映射

    public CallLogWriter(Context context) {
        // 修复：移除未使用的context字段，直接使用contentResolver
        this.contentResolver = context.getContentResolver();
        this.contactIdCache = new HashMap<>();
    }

    public int writeCallRecords(List<CallRecord> records, boolean clearExisting) {
        try {
            int successCount = 0;
            int totalRecords = records.size();

            Log.i(TAG, "开始写入 " + totalRecords + " 条通话记录到系统");

            // 如果选择清空现有记录
            if (clearExisting) {
                clearCallLog();
            }

            // 预加载联系人ID缓存
            preloadContactIds(records);

            // 插入新记录
            for (int i = 0; i < records.size(); i++) {
                CallRecord record = records.get(i);
                Log.d(TAG, "处理第 " + (i + 1) + "/" + totalRecords + " 条记录: " + record.toString());

                if (insertCallRecord(record, i, totalRecords)) {
                    successCount++;
                    Log.d(TAG, "第 " + (i + 1) + " 条记录插入成功");
                } else {
                    Log.w(TAG, "第 " + (i + 1) + " 条记录插入失败");
                }

                // 添加小延迟，避免系统处理不过来 - 添加注释说明这不是繁忙等待
                try {
                    Thread.sleep(50); // 这是为了控制插入速度，不是繁忙等待
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.w(TAG, "线程被中断", e);
                    break; // 如果被中断，退出循环
                }
            }

            Log.i(TAG, "写入完成: " + successCount + "/" + totalRecords + " 条成功");
            return successCount;

        } catch (SecurityException e) {
            Log.e(TAG, "没有写入通话记录的权限", e);
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "写入通话记录时出错", e);
            return -1;
        }
    }

    // 预加载联系人ID
    private void preloadContactIds(List<CallRecord> records) {
        try {
            for (CallRecord record : records) {
                String phoneNumber = cleanPhoneNumber(record.getPhoneNumber());
                if (!contactIdCache.containsKey(phoneNumber)) {
                    Long contactId = findContactIdByNumber(phoneNumber);
                    if (contactId != null) {
                        contactIdCache.put(phoneNumber, contactId);
                        Log.d(TAG, "找到联系人ID: " + phoneNumber + " -> " + contactId);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "预加载联系人ID时出错: " + e.getMessage());
        }
    }

    // 根据电话号码查找联系人ID
    private Long findContactIdByNumber(String phoneNumber) {
        try {
            String cleanNumber = cleanPhoneNumber(phoneNumber);

            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(cleanNumber));
            String[] projection = {ContactsContract.PhoneLookup._ID};

            var cursor = contentResolver.query(uri, projection, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    long contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
                    cursor.close();
                    return contactId;
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "查找联系人ID时出错: " + e.getMessage());
        }
        return null;
    }

    // 清理电话号码（移除所有非数字字符）
    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    private boolean insertCallRecord(CallRecord record, int index, int totalRecords) {
        try {
            ContentValues values = new ContentValues();

            // === 基本必需字段 ===
            String phoneNumber = record.getPhoneNumber();
            values.put(CallLog.Calls.NUMBER, phoneNumber);

            // 通话类型 - 修复图标显示问题
            int callType = getCallType(record.getType());
            values.put(CallLog.Calls.TYPE, callType);

            // 通话时间
            long timestamp = record.getTimestamp();
            long currentTime = System.currentTimeMillis();
            if (timestamp > currentTime) {
                Log.w(TAG, "时间戳在未来，调整为当前时间");
                timestamp = currentTime - ((totalRecords - index) * 60000L);
            }
            values.put(CallLog.Calls.DATE, timestamp);

            // 通话时长
            int duration = record.getDuration();
            values.put(CallLog.Calls.DURATION, duration);

            // 标记为新通话
            values.put(CallLog.Calls.NEW, 1);
            values.put(CallLog.Calls.IS_READ, 1);

            // === 关键修复：正确处理联系人关联 ===
            String cleanNumber = cleanPhoneNumber(phoneNumber);
            Long contactId = contactIdCache.get(cleanNumber);

            // 设置联系人姓名 - 如果记录中有名称则使用，否则设为null
            String contactName = record.getName();
            if (contactName != null && !contactName.trim().isEmpty()) {
                values.put(CallLog.Calls.CACHED_NAME, contactName);
            } else {
                // 对于没有名称的记录，不设置CACHED_NAME，这样会显示号码
                values.put(CallLog.Calls.CACHED_NAME, (String) null);
            }

            if (contactId != null) {
                // 如果找到联系人，设置关联字段
                values.put(CallLog.Calls.CACHED_NUMBER_LABEL, "");
                values.put(CallLog.Calls.CACHED_NUMBER_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);

                // 设置查找URI - 关键：使详情可以点击
                Uri lookupUri = ContactsContract.Contacts.getLookupUri(contactId, "");
                if (lookupUri != null) {
                    values.put(CallLog.Calls.CACHED_LOOKUP_URI, lookupUri.toString());
                }

                Log.d(TAG, "设置联系人关联: " + contactName + " -> ID: " + contactId);
            } else {
                // 如果没有找到联系人，设置默认值
                values.put(CallLog.Calls.CACHED_NUMBER_LABEL, "");
                values.put(CallLog.Calls.CACHED_NUMBER_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM);
                values.put(CallLog.Calls.CACHED_LOOKUP_URI, (String) null);

                Log.d(TAG, "未找到联系人关联: " + phoneNumber);
            }

            // 其他必需字段
            values.put(CallLog.Calls.CACHED_MATCHED_NUMBER, phoneNumber);
            values.put(CallLog.Calls.CACHED_FORMATTED_NUMBER, formatPhoneNumber(phoneNumber));
            values.put(CallLog.Calls.COUNTRY_ISO, "CN");
            values.put(CallLog.Calls.FEATURES, 0);
            values.put(CallLog.Calls.GEOCODED_LOCATION, "");

            // 修复：移除不存在的PRESENTATION字段
            // values.put(CallLog.Calls.PRESENTATION, CallLog.Calls.PRESENTATION_ALLOWED);

            // 照片URI
            values.put(CallLog.Calls.CACHED_PHOTO_URI, (String) null);
            values.put(CallLog.Calls.CACHED_PHOTO_ID, 0L);

            Log.d(TAG, "插入通话记录: " + (contactName != null ? contactName : phoneNumber) +
                    " - " + phoneNumber + " (" + record.getType() + ")");

            // 插入记录
            Uri uri = contentResolver.insert(CallLog.Calls.CONTENT_URI, values);

            boolean success = uri != null;
            if (success) {
                Log.d(TAG, "插入成功, URI: " + uri);
            } else {
                Log.d(TAG, "插入失败");
            }

            return success;

        } catch (Exception e) {
            Log.e(TAG, "插入通话记录失败: " + record, e);
            return false;
        }
    }

    // 格式化电话号码
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";

        String cleanNumber = cleanPhoneNumber(phoneNumber);

        if (cleanNumber.length() == 11) {
            return cleanNumber.substring(0, 3) + " " +
                    cleanNumber.substring(3, 7) + " " +
                    cleanNumber.substring(7);
        }

        return cleanNumber;
    }

    private void clearCallLog() {
        try {
            int deletedRows = contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null);
            Log.i(TAG, "清空了 " + deletedRows + " 条现有通话记录");
        } catch (SecurityException e) {
            Log.e(TAG, "没有清空通话记录的权限", e);
            throw e;
        }
    }

    private int getCallType(String type) {
        switch (type.toUpperCase()) {
            case "OUTGOING":
                return CallLog.Calls.OUTGOING_TYPE;
            case "MISSED":
                return CallLog.Calls.MISSED_TYPE;
            case "INCOMING":
            default:
                return CallLog.Calls.INCOMING_TYPE;
        }
    }
}