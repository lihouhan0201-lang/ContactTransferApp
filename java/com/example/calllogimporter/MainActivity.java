package com.example.calllogimporter;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CallLogImporter";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private TextView tvLog;
    private Button btnGenerateFromContacts;
    private EditText etGenerateCount;
    private Spinner spinnerTimeRange;

    private boolean hasStoragePermission = false;
    private boolean hasCallLogPermission = false;
    private boolean hasContactsPermission = false;
    private boolean hasCameraPermission = false;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private ContactReader contactReader;

    // 时间范围选项
    private final String[] timeRangeOptions = {
            "最近1个月",
            "最近2个月",
            "最近3个月",
            "最近6个月",
            "最近9个月",
            "最近12个月",
            "最近1年",
            "最近2年",
            "最近3年"
    };

    // 时间范围对应的毫秒数
    private final long[] timeRangeMillis = {
            30L * 24 * 60 * 60 * 1000,      // 1个月
            60L * 24 * 60 * 60 * 1000,      // 2个月
            90L * 24 * 60 * 60 * 1000,      // 3个月
            180L * 24 * 60 * 60 * 1000,     // 6个月
            270L * 24 * 60 * 60 * 1000,     // 9个月
            365L * 24 * 60 * 60 * 1000,     // 12个月
            365L * 24 * 60 * 60 * 1000,     // 1年
            2L * 365 * 24 * 60 * 60 * 1000, // 2年
            3L * 365 * 24 * 60 * 60 * 1000  // 3年
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupTimeRangeSpinner();
        setupClickListeners();

        contactReader = new ContactReader(this);
        addLog("应用已启动");
        addLog("正在自动申请所需权限...");

        // 自动检查并申请权限
        checkAndRequestPermissions();
    }

    private void initViews() {
        tvLog = findViewById(R.id.tvLog);
        btnGenerateFromContacts = findViewById(R.id.btnGenerateFromContacts);
        etGenerateCount = findViewById(R.id.etGenerateCount);
        spinnerTimeRange = findViewById(R.id.spinnerTimeRange);
    }

    private void setupClickListeners() {
        btnGenerateFromContacts.setOnClickListener(v -> generateFromContacts());
    }

    private void setupTimeRangeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                timeRangeOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeRange.setAdapter(adapter);
        // 默认选择"最近3个月"
        spinnerTimeRange.setSelection(2);
    }

    private void checkAndRequestPermissions() {
        hasStoragePermission = hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        hasCallLogPermission = hasPermission(Manifest.permission.WRITE_CALL_LOG);
        hasContactsPermission = hasPermission(Manifest.permission.READ_CONTACTS);
        hasCameraPermission = hasPermission(Manifest.permission.CAMERA);

        updateButtonStates();

        addLog("检查权限状态...");
        addLog("存储权限: " + (hasStoragePermission ? "已授权" : "未授权"));
        addLog("通话记录权限: " + (hasCallLogPermission ? "已授权" : "未授权"));
        addLog("联系人权限: " + (hasContactsPermission ? "已授权" : "未授权"));
        addLog("相机权限: " + (hasCameraPermission ? "已授权" : "未授权"));

        // 如果权限不全，自动申请
        if (!hasStoragePermission || !hasCallLogPermission || !hasContactsPermission || !hasCameraPermission) {
            addLog("正在自动申请所需权限...");
            requestPermissions();
        } else {
            addLog("所有权限已就绪，可以开始生成通话记录");
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void updateButtonStates() {
        runOnUiThread(() -> {
            boolean contactPermissionsGranted = hasContactsPermission && hasCallLogPermission;

            btnGenerateFromContacts.setEnabled(contactPermissionsGranted);

            if (!contactPermissionsGranted) {
                addLog("请等待权限申请完成");
            }
        });
    }

    private void requestPermissions() {
        addLog("正在请求权限...");

        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_MEDIA_LOCATION
        };

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private void generateFromContacts() {
        if (!hasContactsPermission || !hasCallLogPermission) {
            addLog("权限不足，无法生成通话记录");
            Toast.makeText(this, "请先授予所有必要权限", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int count = Integer.parseInt(etGenerateCount.getText().toString());
            if (count <= 0) {
                Toast.makeText(this, "请输入有效的生成数量", Toast.LENGTH_SHORT).show();
                return;
            }

            if (count > 1000) {
                Toast.makeText(this, "生成数量过多，建议不超过1000条", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取选择的时间范围
            int selectedPosition = spinnerTimeRange.getSelectedItemPosition();
            long timeRange = timeRangeMillis[selectedPosition];
            long endDate = System.currentTimeMillis();
            long startDate = endDate - timeRange;

            addLog("开始生成通话记录...");
            addLog("时间范围: " + timeRangeOptions[selectedPosition]);
            addLog("号码类型比例:");
            addLog("- 45% 联系人号码");
            addLog("- 5% 运营商服务号码(10086等)");
            addLog("- 5% 固定电话");
            addLog("- 40% 陌生手机号码");
            addLog("- 5% 400随机服务号码");

            executor.execute(() -> {
                runOnUiThread(() -> showProgressDialog("正在生成多样化通话记录..."));

                try {
                    // 读取联系人
                    List<Contact> contacts = contactReader.readContacts();

                    runOnUiThread(() -> {
                        addLog("成功读取 " + contacts.size() + " 个联系人");
                        addLog("开始按照精确比例生成通话记录...");
                    });

                    // 生成通话记录（按照精确比例）
                    ContactBasedCallGenerator generator = new ContactBasedCallGenerator(contacts);
                    List<CallRecord> records = generator.generateCallRecords(count, startDate, endDate);

                    runOnUiThread(() -> {
                        addLog("成功生成 " + records.size() + " 条多样化通话记录");
                        addLog("开始写入系统...");
                    });

                    // 写入通话记录
                    CallLogWriter writer = new CallLogWriter(MainActivity.this);
                    int result = writer.writeCallRecords(records, false);

                    runOnUiThread(() -> {
                        hideProgressDialog();
                        if (result > 0) {
                            addLog("导入完成！成功导入 " + result + " 条通话记录");
                            addLog("号码类型精确分布:");
                            addLog("- 45% 联系人号码");
                            addLog("- 5% 运营商服务号码");
                            addLog("- 5% 固定电话");
                            addLog("- 40% 陌生手机号码");
                            addLog("- 5% 400随机服务号码");
                            addLog("通话类型: 40%呼入 + 40%呼出 + 20%未接");
                            Toast.makeText(MainActivity.this, "成功导入 " + result + " 条多样化通话记录", Toast.LENGTH_LONG).show();
                        } else {
                            addLog("导入失败");
                            Toast.makeText(MainActivity.this, "导入失败", Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        addLog("生成过程中出错: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "生成失败", Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                addLog("权限结果: " + permissions[i] + " = " + (granted ? "允许" : "拒绝"));
                if (!granted) {
                    allGranted = false;
                }
            }

            if (allGranted) {
                addLog("所有权限已授予");
                Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                addLog("部分权限被拒绝，某些功能可能无法使用");
                Toast.makeText(this, "部分权限被拒绝", Toast.LENGTH_LONG).show();
            }

            checkAndRequestPermissions();
        }
    }

    private void addLog(String message) {
        Log.d(TAG, message);
        runOnUiThread(() -> {
            String currentText = tvLog.getText().toString();
            tvLog.setText(message + "\n" + currentText);
        });
    }

    private ProgressDialog progressDialog;

    private void showProgressDialog(String message) {
        runOnUiThread(() -> {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.show();
        });
    }

    private void hideProgressDialog() {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }
}