package com.securememo.app.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据模型类
 *
 * JSON 数据结构示例：
 * {
 *   "version": 1,
 *   "groups": [
 *     { "id": "g1", "name": "工作", "color": "#FF5722" }
 *   ],
 *   "notes": [
 *     {
 *       "id": "n1",
 *       "groupId": "g1",
 *       "type": "note",          // note | todo | contact | account
 *       "title": "标题",
 *       "content": "内容",       // 普通笔记
 *       "items": [...],          // Todo 子项
 *       "name": "姓名",          // 联系人
 *       "phone": "电话",
 *       "email": "邮箱",
 *       "username": "用户名",    // 账号密码
 *       "password": "密码",
 *       "url": "网址",
 *       "remark": "备注",
 *       "createdAt": 1700000000000,
 *       "updatedAt": 1700000000000
 *     }
 *   ]
 * }
 */
public class DataModel {

    // ==================== 分组模型 ====================
    public static class Group {
        public String id;
        public String name;
        public String color;  // 十六进制颜色，如 "#FF5722"

        public Group() {}

        public Group(String id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        public JSONObject toJson() throws Exception {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("color", color);
            return obj;
        }

        public static Group fromJson(JSONObject obj) throws Exception {
            Group g = new Group();
            g.id = obj.getString("id");
            g.name = obj.getString("name");
            g.color = obj.optString("color", "#607D8B");
            return g;
        }
    }

    // ==================== 记事类型常量 ====================
    public static final String TYPE_NOTE = "note";       // 普通笔记
    public static final String TYPE_TODO = "todo";       // Todo 清单
    public static final String TYPE_CONTACT = "contact"; // 联系人
    public static final String TYPE_ACCOUNT = "account"; // 账号密码

    // ==================== Todo 子项 ====================
    public static class TodoItem {
        public String text;
        public boolean checked;

        public TodoItem(String text, boolean checked) {
            this.text = text;
            this.checked = checked;
        }

        public JSONObject toJson() throws Exception {
            JSONObject obj = new JSONObject();
            obj.put("text", text);
            obj.put("checked", checked);
            return obj;
        }

        public static TodoItem fromJson(JSONObject obj) throws Exception {
            return new TodoItem(
                obj.getString("text"),
                obj.optBoolean("checked", false)
            );
        }
    }

    // ==================== 记事模型（统一结构） ====================
    public static class Note {
        public String id;
        public String groupId;
        public String type;       // TYPE_NOTE / TYPE_TODO / TYPE_CONTACT / TYPE_ACCOUNT
        public String title;
        public long createdAt;
        public long updatedAt;

        // 普通笔记字段
        public String content;

        // Todo 字段
        public List<TodoItem> todoItems = new ArrayList<>();

        // 联系人字段
        public String name;
        public String phone;
        public String email;

        // 账号密码字段
        public String username;
        public String password;
        public String url;

        // 通用备注
        public String remark;

        public Note() {
            long now = System.currentTimeMillis();
            this.createdAt = now;
            this.updatedAt = now;
        }

        public JSONObject toJson() throws Exception {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("groupId", groupId != null ? groupId : "");
            obj.put("type", type);
            obj.put("title", title != null ? title : "");
            obj.put("createdAt", createdAt);
            obj.put("updatedAt", updatedAt);

            // 根据类型序列化对应字段
            switch (type) {
                case TYPE_NOTE:
                    obj.put("content", content != null ? content : "");
                    break;
                case TYPE_TODO:
                    JSONArray items = new JSONArray();
                    for (TodoItem item : todoItems) {
                        items.put(item.toJson());
                    }
                    obj.put("items", items);
                    break;
                case TYPE_CONTACT:
                    obj.put("name", name != null ? name : "");
                    obj.put("phone", phone != null ? phone : "");
                    obj.put("email", email != null ? email : "");
                    obj.put("remark", remark != null ? remark : "");
                    break;
                case TYPE_ACCOUNT:
                    obj.put("username", username != null ? username : "");
                    obj.put("password", password != null ? password : "");
                    obj.put("url", url != null ? url : "");
                    obj.put("remark", remark != null ? remark : "");
                    break;
            }
            return obj;
        }

        public static Note fromJson(JSONObject obj) throws Exception {
            Note note = new Note();
            note.id = obj.getString("id");
            note.groupId = obj.optString("groupId", "");
            note.type = obj.optString("type", TYPE_NOTE);
            note.title = obj.optString("title", "");
            note.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            note.updatedAt = obj.optLong("updatedAt", System.currentTimeMillis());

            switch (note.type) {
                case TYPE_NOTE:
                    note.content = obj.optString("content", "");
                    break;
                case TYPE_TODO:
                    JSONArray items = obj.optJSONArray("items");
                    if (items != null) {
                        for (int i = 0; i < items.length(); i++) {
                            note.todoItems.add(TodoItem.fromJson(items.getJSONObject(i)));
                        }
                    }
                    break;
                case TYPE_CONTACT:
                    note.name = obj.optString("name", "");
                    note.phone = obj.optString("phone", "");
                    note.email = obj.optString("email", "");
                    note.remark = obj.optString("remark", "");
                    break;
                case TYPE_ACCOUNT:
                    note.username = obj.optString("username", "");
                    note.password = obj.optString("password", "");
                    note.url = obj.optString("url", "");
                    note.remark = obj.optString("remark", "");
                    break;
            }
            return note;
        }
    }

    // ==================== 根数据容器 ====================
    public static class Vault {
        public int version = 1;
        public List<Group> groups = new ArrayList<>();
        public List<Note> notes = new ArrayList<>();

        public JSONObject toJson() throws Exception {
            JSONObject root = new JSONObject();
            root.put("version", version);

            JSONArray groupsArr = new JSONArray();
            for (Group g : groups) groupsArr.put(g.toJson());
            root.put("groups", groupsArr);

            JSONArray notesArr = new JSONArray();
            for (Note n : notes) notesArr.put(n.toJson());
            root.put("notes", notesArr);

            return root;
        }

        public static Vault fromJson(JSONObject root) throws Exception {
            Vault vault = new Vault();
            vault.version = root.optInt("version", 1);

            JSONArray groupsArr = root.optJSONArray("groups");
            if (groupsArr != null) {
                for (int i = 0; i < groupsArr.length(); i++) {
                    vault.groups.add(Group.fromJson(groupsArr.getJSONObject(i)));
                }
            }

            JSONArray notesArr = root.optJSONArray("notes");
            if (notesArr != null) {
                for (int i = 0; i < notesArr.length(); i++) {
                    vault.notes.add(Note.fromJson(notesArr.getJSONObject(i)));
                }
            }

            return vault;
        }
    }
}
