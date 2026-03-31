package com.securememo.app.ui.settings;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.securememo.app.R;
import com.securememo.app.data.VaultRepository;
import com.securememo.app.data.WebDavSync;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设置界面：WebDAV 配置 + 备份/还原操作
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText etServer, etUsername, etPassword, etRemotePath;
    private Button btnSaveConfig, btnBackup, btnRestore;

    private WebDavSync webDavSync;
    private VaultRepository repo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        repo = VaultRepository.getInstance(this);
        webDavSync = new WebDavSync(this);

        etServer = findViewById(R.id.etServer);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etRemotePath = findViewById(R.id.etRemotePath);
        btnSaveConfig = findViewById(R.id.btnSaveConfig);
        btnBackup = findViewById(R.id.btnBackup);
        btnRestore = findViewById(R.id.btnRestore);

        // 加载已保存的配置
        String[] config = webDavSync.loadConfig();
        etServer.setText(config[0]);
        etUsername.setText(config[1]);
        etPassword.setText(config[2]);
        etRemotePath.setText(config[3]);

        btnSaveConfig.setOnClickListener(v -> saveConfig());
        btnBackup.setOnClickListener(v -> doBackup());
        btnRestore.setOnClickListener(v -> doRestore());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设置 / WebDAV 同步");
        }
    }

    private void saveConfig() {
        String server = etServer.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        String remotePath = etRemotePath.getText().toString().trim();

        if (server.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "请填写服务器地址和用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        webDavSync.saveConfig(server, username, password, remotePath);
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
    }

    private void doBackup() {
        if (!webDavSync.isConfigured()) {
            Toast.makeText(this, "请先配置 WebDAV", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "正在备份...", Toast.LENGTH_SHORT).show();
        File vaultFile = repo.getVaultFile();

        executor.execute(() -> {
            try {
                webDavSync.upload(vaultFile);
                runOnUiThread(() ->
                    Toast.makeText(this, "✅ 备份成功", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "❌ 备份失败：" + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void doRestore() {
        if (!webDavSync.isConfigured()) {
            Toast.makeText(this, "请先配置 WebDAV", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("还原数据")
            .setMessage("从 WebDAV 下载并覆盖本地数据？\n还原后需要重新输入密码解锁。")
            .setPositiveButton("确定还原", (dialog, which) -> {
                Toast.makeText(this, "正在下载...", Toast.LENGTH_SHORT).show();
                executor.execute(() -> {
                    try {
                        byte[] data = webDavSync.download();
                        repo.replaceVaultFile(data);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "✅ 还原成功，请重新启动应用解锁",
                                Toast.LENGTH_LONG).show();
                            // 退出到锁屏界面
                            finishAffinity();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() ->
                            Toast.makeText(this, "❌ 还原失败：" + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                        );
                    }
                });
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
