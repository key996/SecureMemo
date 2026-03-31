package com.securememo.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.securememo.app.crypto.AesGcmCrypto;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

/**
 * 数据仓库：负责加密存储和解密读取 vault.dat 文件
 *
 * 存储路径：/data/data/com.securememo.app/files/vault.dat
 * 内存缓存：解密后的 Vault 对象，避免每次操作都读文件
 */
public class VaultRepository {

    private static final String VAULT_FILE = "vault.dat";
    private static final String PREFS_NAME = "secure_memo_prefs";
    private static final String KEY_PATTERN_HASH = "pattern_hash";  // 存储图案密码哈希
    private static final String KEY_SETUP_DONE = "setup_done";      // 是否已完成初始设置

    private static VaultRepository instance;
    private final Context context;

    // 内存缓存：解密后的数据
    private DataModel.Vault cachedVault;
    // 当前会话的 AES 密钥（从图案密码派生）
    private SecretKey currentKey;

    private VaultRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized VaultRepository getInstance(Context context) {
        if (instance == null) {
            instance = new VaultRepository(context);
        }
        return instance;
    }

    // ==================== 密码管理 ====================

    /**
     * 检查是否已设置图案密码
     */
    public boolean isSetupDone() {
        return getPrefs().getBoolean(KEY_SETUP_DONE, false);
    }

    /**
     * 首次设置图案密码
     *
     * @param patternPassword 图案密码字符串（如 "0-1-2-4-7"）
     */
    public void setupPattern(String patternPassword) throws Exception {
        String hash = AesGcmCrypto.hashPattern(patternPassword);
        getPrefs().edit()
            .putString(KEY_PATTERN_HASH, hash)
            .putBoolean(KEY_SETUP_DONE, true)
            .apply();

        // 派生密钥并初始化空 Vault
        currentKey = AesGcmCrypto.deriveKeyFromPattern(patternPassword);
        cachedVault = new DataModel.Vault();
        // 添加默认分组
        DataModel.Group defaultGroup = new DataModel.Group(
            generateId(), "默认", "#607D8B"
        );
        cachedVault.groups.add(defaultGroup);
        saveVault();
    }

    /**
     * 验证图案密码
     *
     * @param patternPassword 用户输入的图案密码
     * @return true 验证通过
     */
    public boolean verifyPattern(String patternPassword) throws Exception {
        String storedHash = getPrefs().getString(KEY_PATTERN_HASH, "");
        String inputHash = AesGcmCrypto.hashPattern(patternPassword);
        if (storedHash.equals(inputHash)) {
            // 验证通过，派生密钥并解密数据
            currentKey = AesGcmCrypto.deriveKeyFromPattern(patternPassword);
            loadVault();
            return true;
        }
        return false;
    }

    /**
     * 重置所有数据（忘记密码时使用，不可恢复）
     */
    public void resetAll() {
        // 删除加密文件
        File vaultFile = new File(context.getFilesDir(), VAULT_FILE);
        if (vaultFile.exists()) vaultFile.delete();

        // 清除 SharedPreferences
        getPrefs().edit().clear().apply();

        // 清除内存缓存
        cachedVault = null;
        currentKey = null;
    }

    // ==================== 数据读写 ====================

    /**
     * 从文件解密并加载 Vault 到内存
     */
    public void loadVault() throws Exception {
        File vaultFile = new File(context.getFilesDir(), VAULT_FILE);
        if (!vaultFile.exists()) {
            // 文件不存在，初始化空 Vault
            cachedVault = new DataModel.Vault();
            return;
        }

        // 读取加密文件内容
        FileInputStream fis = new FileInputStream(vaultFile);
        byte[] encryptedBytes = new byte[(int) vaultFile.length()];
        fis.read(encryptedBytes);
        fis.close();

        String encryptedBase64 = new String(encryptedBytes, StandardCharsets.UTF_8);

        // AES-GCM 解密
        String jsonStr = AesGcmCrypto.decrypt(encryptedBase64, currentKey);

        // 解析 JSON
        cachedVault = DataModel.Vault.fromJson(new JSONObject(jsonStr));
    }

    /**
     * 将内存中的 Vault 加密并写入文件
     */
    public void saveVault() throws Exception {
        if (cachedVault == null || currentKey == null) {
            throw new IllegalStateException("Vault 未初始化，请先解锁");
        }

        // 序列化为 JSON 字符串
        String jsonStr = cachedVault.toJson().toString();

        // AES-GCM 加密
        String encryptedBase64 = AesGcmCrypto.encrypt(jsonStr, currentKey);

        // 写入文件
        File vaultFile = new File(context.getFilesDir(), VAULT_FILE);
        FileOutputStream fos = new FileOutputStream(vaultFile);
        fos.write(encryptedBase64.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    /**
     * 获取内存中的 Vault（已解密）
     */
    public DataModel.Vault getVault() {
        return cachedVault;
    }

    /**
     * 获取加密文件（用于 WebDAV 备份）
     */
    public File getVaultFile() {
        return new File(context.getFilesDir(), VAULT_FILE);
    }

    /**
     * 用外部下载的文件替换本地 vault.dat（WebDAV 还原）
     * 替换后需要重新验证密码才能解密
     */
    public void replaceVaultFile(byte[] newFileContent) throws Exception {
        File vaultFile = new File(context.getFilesDir(), VAULT_FILE);
        FileOutputStream fos = new FileOutputStream(vaultFile);
        fos.write(newFileContent);
        fos.close();
        // 清除内存缓存，强制重新解密
        cachedVault = null;
    }

    // ==================== 工具方法 ====================

    private SharedPreferences getPrefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 生成简单唯一 ID（时间戳 + 随机数）
     */
    public static String generateId() {
        return Long.toHexString(System.currentTimeMillis()) +
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
}
