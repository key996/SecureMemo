package com.securememo.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * WebDAV 同步工具类
 *
 * 支持：坚果云、Nextcloud、任何标准 WebDAV 服务
 * 操作：上传 vault.dat / 下载 vault.dat
 */
public class WebDavSync {

    private static final String PREFS_NAME = "webdav_prefs";
    private static final String KEY_SERVER = "server_url";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMOTE_PATH = "remote_path";

    private final Context context;

    public WebDavSync(Context context) {
        this.context = context.getApplicationContext();
    }

    // ==================== 配置管理 ====================

    public void saveConfig(String serverUrl, String username, String password, String remotePath) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER, serverUrl)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_REMOTE_PATH, remotePath)
            .apply();
    }

    public String[] loadConfig() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new String[]{
            prefs.getString(KEY_SERVER, ""),
            prefs.getString(KEY_USERNAME, ""),
            prefs.getString(KEY_PASSWORD, ""),
            prefs.getString(KEY_REMOTE_PATH, "/SecureMemo/vault.dat")
        };
    }

    public boolean isConfigured() {
        String[] config = loadConfig();
        return !config[0].isEmpty() && !config[1].isEmpty();
    }

    // ==================== 上传（备份） ====================

    /**
     * 将本地 vault.dat 上传到 WebDAV 服务器
     *
     * @param localFile 本地加密文件
     * @throws Exception 上传失败时抛出异常，包含错误信息
     */
    public void upload(File localFile) throws Exception {
        String[] config = loadConfig();
        String serverUrl = config[0];
        String username = config[1];
        String password = config[2];
        String remotePath = config[3];

        // 确保远程目录存在（MKCOL 请求）
        ensureRemoteDirectory(serverUrl, username, password, remotePath);

        // 构建完整 URL
        String uploadUrl = buildUrl(serverUrl, remotePath);

        // 读取本地文件
        FileInputStream fis = new FileInputStream(localFile);
        byte[] fileData = new byte[(int) localFile.length()];
        fis.read(fileData);
        fis.close();

        // 发送 PUT 请求
        URL url = new URL(uploadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", buildBasicAuth(username, password));
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("Content-Length", String.valueOf(fileData.length));
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        OutputStream os = conn.getOutputStream();
        os.write(fileData);
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        conn.disconnect();

        if (responseCode != 200 && responseCode != 201 && responseCode != 204) {
            throw new Exception("上传失败，服务器返回：" + responseCode);
        }
    }

    // ==================== 下载（还原） ====================

    /**
     * 从 WebDAV 服务器下载 vault.dat
     *
     * @return 下载的文件内容（字节数组）
     * @throws Exception 下载失败时抛出异常
     */
    public byte[] download() throws Exception {
        String[] config = loadConfig();
        String serverUrl = config[0];
        String username = config[1];
        String password = config[2];
        String remotePath = config[3];

        String downloadUrl = buildUrl(serverUrl, remotePath);

        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", buildBasicAuth(username, password));
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        int responseCode = conn.getResponseCode();
        if (responseCode == 404) {
            throw new Exception("远程文件不存在，请先备份");
        }
        if (responseCode != 200) {
            throw new Exception("下载失败，服务器返回：" + responseCode);
        }

        InputStream is = conn.getInputStream();
        // 兼容 API 26：使用 ByteArrayOutputStream 而不是 readAllBytes()
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        is.close();
        conn.disconnect();

        return buffer.toByteArray();
    }

    // ==================== 工具方法 ====================

    /**
     * 确保远程目录存在（WebDAV MKCOL）
     */
    private void ensureRemoteDirectory(String serverUrl, String username,
                                        String password, String remotePath) {
        try {
            // 提取目录路径（去掉文件名）
            int lastSlash = remotePath.lastIndexOf('/');
            if (lastSlash <= 0) return;
            String dirPath = remotePath.substring(0, lastSlash);
            String dirUrl = buildUrl(serverUrl, dirPath);

            URL url = new URL(dirUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("MKCOL");
            conn.setRequestProperty("Authorization", buildBasicAuth(username, password));
            conn.setConnectTimeout(10000);
            conn.getResponseCode(); // 忽略结果（目录可能已存在）
            conn.disconnect();
        } catch (Exception ignored) {
            // 目录创建失败不影响上传（目录可能已存在）
        }
    }

    /**
     * 构建 Basic Auth 头
     */
    private String buildBasicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(
            credentials.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 拼接服务器 URL 和路径
     */
    private String buildUrl(String serverUrl, String path) {
        String base = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        String p = path.startsWith("/") ? path : "/" + path;
        return base + p;
    }
}
