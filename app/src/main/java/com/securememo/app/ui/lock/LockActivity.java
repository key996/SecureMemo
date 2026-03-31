package com.securememo.app.ui.lock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.securememo.app.R;
import com.securememo.app.data.VaultRepository;
import com.securememo.app.ui.main.MainActivity;

/**
 * 启动界面：图案密码锁屏
 *
 * 两种模式：
 *   1. 首次使用：引导设置图案密码（需绘制两次确认）
 *   2. 已设置：验证图案密码后进入主界面
 */
public class LockActivity extends AppCompatActivity {

    private PatternLockView patternLockView;
    private TextView tvTitle;
    private TextView tvHint;
    private Button btnForgot;

    private VaultRepository repo;

    // 首次设置时，记录第一次绘制的图案
    private String firstPattern = null;
    private boolean isSetupMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        repo = VaultRepository.getInstance(this);

        patternLockView = findViewById(R.id.patternLockView);
        tvTitle = findViewById(R.id.tvTitle);
        tvHint = findViewById(R.id.tvHint);
        btnForgot = findViewById(R.id.btnForgot);

        // 判断是首次设置还是验证模式
        if (!repo.isSetupDone()) {
            enterSetupMode();
        } else {
            enterVerifyMode();
        }

        // 图案绘制完成回调
        patternLockView.setOnPatternCompleteListener(pattern -> {
            if (isSetupMode) {
                handleSetup(pattern);
            } else {
                handleVerify(pattern);
            }
        });

        // 忘记密码按钮
        btnForgot.setOnClickListener(v -> showForgotDialog());
    }

    // ==================== 设置模式 ====================

    private void enterSetupMode() {
        isSetupMode = true;
        firstPattern = null;
        tvTitle.setText("设置图案密码");
        tvHint.setText("请绘制您的解锁图案（至少连接 4 个点）");
        btnForgot.setVisibility(View.GONE);
    }

    private void handleSetup(String pattern) {
        if (firstPattern == null) {
            // 第一次绘制
            firstPattern = pattern;
            tvHint.setText("请再次绘制以确认");
            patternLockView.reset();
        } else {
            // 第二次绘制，验证是否一致
            if (firstPattern.equals(pattern)) {
                // 设置成功
                try {
                    repo.setupPattern(pattern);
                    Toast.makeText(this, "密码设置成功", Toast.LENGTH_SHORT).show();
                    goToMain();
                } catch (Exception e) {
                    Toast.makeText(this, "设置失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    enterSetupMode();
                }
            } else {
                // 两次不一致
                tvHint.setText("两次图案不一致，请重新绘制");
                patternLockView.showError();
                firstPattern = null;
            }
        }
    }

    // ==================== 验证模式 ====================

    private void enterVerifyMode() {
        isSetupMode = false;
        tvTitle.setText("SecureMemo");
        tvHint.setText("请绘制解锁图案");
        btnForgot.setVisibility(View.VISIBLE);
    }

    private void handleVerify(String pattern) {
        try {
            if (repo.verifyPattern(pattern)) {
                goToMain();
            } else {
                tvHint.setText("图案错误，请重试");
                patternLockView.showError();
            }
        } catch (Exception e) {
            tvHint.setText("验证失败：" + e.getMessage());
            patternLockView.showError();
        }
    }

    // ==================== 忘记密码 ====================

    private void showForgotDialog() {
        new AlertDialog.Builder(this)
            .setTitle("忘记密码")
            .setMessage("重置密码将清除所有数据，且无法恢复。\n\n确定要继续吗？")
            .setPositiveButton("清除数据并重置", (dialog, which) -> {
                repo.resetAll();
                Toast.makeText(this, "数据已清除", Toast.LENGTH_SHORT).show();
                enterSetupMode();
                patternLockView.reset();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // ==================== 跳转主界面 ====================

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
