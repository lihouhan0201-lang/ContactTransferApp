package com.example.calllogimporter;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ContactBasedCallGenerator {
    private static final String TAG = "ContactCallGenerator";

    private final Random random;
    private final List<Contact> contacts;

    // 中国手机号段
    private final String[] mobilePrefixes = {"130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
            "150", "151", "152", "153", "155", "156", "157", "158", "159",
            "180", "181", "182", "183", "184", "185", "186", "187", "188", "189"};

    // 五位数运营商服务号码
    private final String[] operatorNumbers = {
            "10086", "10010", "10000", "10001", "10011", "10085", "10089",
            "12580", "12581", "12582", "12583", "12530", "12590", "12593",
            "10655", "10657", "10658", "10659", "10690", "10698", "10699"
    };

    // 常见区号
    private final String[] areaCodes = {
            "010", "021", "022", "023", "024", "025", "027", "028", "029",
            "0310", "0311", "0312", "0313", "0314", "0315", "0316", "0317", "0318", "0319",
            "0335", "0349", "0350", "0351", "0352", "0353", "0354", "0355", "0356", "0357", "0358", "0359",
            "0370", "0371", "0372", "0373", "0374", "0375", "0376", "0377", "0378", "0379",
            "0391", "0392", "0393", "0394", "0395", "0396", "0398",
            "0410", "0411", "0412", "0413", "0414", "0415", "0416", "0417", "0418", "0419",
            "0421", "0427", "0429", "0431", "0432", "0433", "0434", "0435", "0436", "0437", "0438", "0439",
            "0451", "0452", "0453", "0454", "0455", "0456", "0457", "0458", "0459",
            "0464", "0467", "0468", "0469",
            "0470", "0471", "0472", "0473", "0474", "0475", "0476", "0477", "0478", "0479",
            "0482", "0483", "0510", "0511", "0512", "0513", "0514", "0515", "0516", "0517", "0518", "0519",
            "0520", "0523", "0527", "0530", "0531", "0532", "0533", "0534", "0535", "0536", "0537", "0538", "0539",
            "0543", "0546", "0550", "0551", "0552", "0553", "0554", "0555", "0556", "0557", "0558", "0559",
            "0561", "0562", "0563", "0564", "0565", "0566",
            "0570", "0571", "0572", "0573", "0574", "0575", "0576", "0577", "0578", "0579",
            "0580", "0591", "0592", "0593", "0594", "0595", "0596", "0597", "0598", "0599",
            "0631", "0632", "0633", "0634", "0635",
            "0660", "0662", "0663", "0668",
            "0691", "0692",
            "0701", "0710", "0711", "0712", "0713", "0714", "0715", "0716", "0717", "0718", "0719",
            "0722", "0724", "0728",
            "0730", "0731", "0732", "0733", "0734", "0735", "0736", "0737", "0738", "0739",
            "0743", "0744", "0745", "0746",
            "0750", "0751", "0752", "0753", "0754", "0755", "0756", "0757", "0758", "0759",
            "0760", "0762", "0763", "0766", "0768", "0769",
            "0770", "0771", "0772", "0773", "0774", "0775", "0776", "0777", "0778", "0779",
            "0790", "0791", "0792", "0793", "0794", "0795", "0796", "0797", "0798", "0799",
            "0825", "0826", "0827",
            "0830", "0831", "0832", "0833", "0834", "0835", "0836", "0837", "0838", "0839",
            "0851", "0852", "0853", "0854", "0855", "0856", "0857", "0858", "0859",
            "0870", "0871", "0872", "0873", "0874", "0875", "0876", "0877", "0878", "0879",
            "0880", "0881", "0883", "0886", "0887", "0888",
            "0890", "0891", "0892", "0893", "0894", "0895", "0896", "0897", "0898", "0899"
    };

    public ContactBasedCallGenerator(List<Contact> contacts) {
        this.random = new Random();
        this.contacts = contacts;
    }

    public List<CallRecord> generateCallRecords(int count, long startDate, long endDate) {
        List<CallRecord> records = new ArrayList<>();

        if (contacts == null || contacts.isEmpty()) {
            Log.w(TAG, "没有可用的联系人，将增加陌生号码的比例");
        }

        Log.i(TAG, "基于 " + (contacts != null ? contacts.size() : 0) + " 个联系人生成 " + count + " 条通话记录");

        for (int i = 0; i < count; i++) {
            // 按照用户要求的比例生成不同类型的号码：
            // 45%联系人号码，5%的运营商服务号码，5%的固定电话，40%的陌生手机号码，5%的400开头服务号码
            double typeChoice = random.nextDouble();

            String name;
            String phoneNumber;
            boolean isContact = false;

            if (typeChoice < 0.45 && contacts != null && !contacts.isEmpty()) {
                // 45% 来自联系人
                Contact contact = contacts.get(random.nextInt(contacts.size()));
                name = contact.getName();
                phoneNumber = contact.getPhoneNumber();
                isContact = true;
                Log.d(TAG, "使用联系人号码: " + name + " - " + phoneNumber);
            } else if (typeChoice < 0.50) {
                // 5% 运营商服务号码
                phoneNumber = operatorNumbers[random.nextInt(operatorNumbers.length)];
                name = getOperatorName(phoneNumber);
                Log.d(TAG, "生成运营商号码: " + phoneNumber + " - " + name);
            } else if (typeChoice < 0.55) {
                // 5% 带区号的固定电话
                String areaCode = areaCodes[random.nextInt(areaCodes.length)];
                String localNumber = generateLocalNumber(7); // 7位本地号码
                phoneNumber = areaCode + localNumber;
                name = null; // 固定电话不设置名称，显示号码
                Log.d(TAG, "生成本地号码: " + phoneNumber);
            } else if (typeChoice < 0.95) {
                // 40% 随机手机号码
                String prefix = mobilePrefixes[random.nextInt(mobilePrefixes.length)];
                String suffix = generateRandomNumber(8); // 8位随机数字
                phoneNumber = prefix + suffix;
                name = null; // 陌生号码不设置名称，显示号码
                Log.d(TAG, "生成手机号码: " + phoneNumber);
            } else {
                // 5% 400开头随机服务号码
                phoneNumber = generateRandom400Number();
                name = null; // 服务号码不设置名称，显示号码
                Log.d(TAG, "生成400服务号码: " + phoneNumber);
            }

            // 随机生成通话类型 - 确保分布更均匀
            String callType = generateCallType();

            // 随机生成时间戳
            long timestamp = generateRandomTimestamp(startDate, endDate);

            // 随机生成通话时长（未接来电时长为0）
            int duration = callType.equals("MISSED") ? 0 : 30 + random.nextInt(1800);

            // 创建通话记录
            CallRecord record = new CallRecord(name, phoneNumber, callType, timestamp, duration);
            records.add(record);

            Log.d(TAG, "生成记录 " + (i + 1) + ": " + record.toString());
        }

        Log.i(TAG, "生成完成: " + records.size() + " 条记录");
        Log.i(TAG, "包含: 45%联系人 + 5%运营商号 + 5%固定电话 + 40%手机号 + 5%400服务号");

        return records;
    }

    // 生成随机的400号码（400开头，后面7位随机）
    private String generateRandom400Number() {
        // 400号码格式：400-XXX-XXXX（总共10位）
        // 400后面第一位通常是1-9，不能是0
        int firstDigit = 1 + random.nextInt(9); // 1-9
        String middlePart = generateRandomNumber(2); // 2位随机
        String lastPart = generateRandomNumber(4); // 4位随机

        return "400" + firstDigit + middlePart + lastPart;
    }

    // 根据运营商号码获取对应的名称
    private String getOperatorName(String phoneNumber) {
        switch (phoneNumber) {
            case "10086":
            case "10085":
            case "10089":
                return "中国移动";
            case "10010":
            case "10011":
                return "中国联通";
            case "10000":
            case "10001":
                return "中国电信";
            case "12580":
            case "12581":
            case "12582":
            case "12583":
            case "12530":
            case "12590":
            case "12593":
                return "移动服务";
            case "10655":
            case "10657":
            case "10658":
            case "10659":
                return "联通服务";
            case "10690":
            case "10698":
            case "10699":
                return "电信服务";
            default:
                return "运营商";
        }
    }

    // 生成更合理的通话类型分布
    private String generateCallType() {
        double type = random.nextDouble();
        if (type < 0.4) {
            return "INCOMING";  // 40% 呼入
        } else if (type < 0.8) {
            return "OUTGOING";  // 40% 呼出
        } else {
            return "MISSED";    // 20% 未接
        }
    }

    private long generateRandomTimestamp(long startDate, long endDate) {
        return startDate + (long) (random.nextDouble() * (endDate - startDate));
    }

    // 生成本地号码（固定电话后几位）
    private String generateLocalNumber(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    // 生成随机数字字符串
    private String generateRandomNumber(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}