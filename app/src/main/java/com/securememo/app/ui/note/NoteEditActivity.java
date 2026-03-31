package com.securememo.app.ui.note;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.securememo.app.R;
import com.securememo.app.data.DataModel;
import com.securememo.app.data.VaultRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 记事编辑界面
 *
 * 支持四种类型：普通笔记 / Todo 清单 / 联系人 / 账号密码
 * 根据 note_type 或 note_id 动态显示对应字段
 */
public class NoteEditActivity extends AppCompatActivity {

    private VaultRepository repo;
    private DataModel.Note currentNote;
    private boolean isNewNote = true;

    // 通用字段
    private EditText etTitle;
    private Spinner spinnerGroup;
    private Button btnSave;

    // 普通笔记
    private LinearLayout layoutNote;
    private EditText etContent;

    // Todo 清单
    private LinearLayout layoutTodo;
    private LinearLayout llTodoItems;
    private Button btnAddTodoItem;

    // 联系人
    private LinearLayout layoutContact;
    private EditText etName, etPhone, etEmail, etContactRemark;

    // 账号密码
    private LinearLayout layoutAccount;
    private EditText etUsername, etPassword, etUrl, etAccountRemark;
    private ImageButton btnTogglePassword;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        repo = VaultRepository.getInstance(this);
        initViews();
        setupGroupSpinner();

        String noteId = getIntent().getStringExtra("note_id");
        String noteType = getIntent().getStringExtra("note_type");

        if (noteId != null) {
            // 编辑已有记事
            isNewNote = false;
            loadNote(noteId);
        } else {
            // 新建记事
            isNewNote = true;
            currentNote = new DataModel.Note();
            currentNote.id = VaultRepository.generateId();
            currentNote.type = noteType != null ? noteType : DataModel.TYPE_NOTE;
            showFieldsForType(currentNote.type);
        }

        btnSave.setOnClickListener(v -> saveNote());

        // 返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        spinnerGroup = findViewById(R.id.spinnerGroup);
        btnSave = findViewById(R.id.btnSave);

        layoutNote = findViewById(R.id.layoutNote);
        etContent = findViewById(R.id.etContent);

        layoutTodo = findViewById(R.id.layoutTodo);
        llTodoItems = findViewById(R.id.llTodoItems);
        btnAddTodoItem = findViewById(R.id.btnAddTodoItem);

        layoutContact = findViewById(R.id.layoutContact);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etContactRemark = findViewById(R.id.etContactRemark);

        layoutAccount = findViewById(R.id.layoutAccount);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etUrl = findViewById(R.id.etUrl);
        etAccountRemark = findViewById(R.id.etAccountRemark);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);

        // 密码可见性切换
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                etPassword.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );
                btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
            } else {
                etPassword.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                );
                btnTogglePassword.setImageResource(android.R.drawable.ic_secure);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // 添加 Todo 子项
        btnAddTodoItem.setOnClickListener(v -> addTodoItemView("", false));
    }

    private void setupGroupSpinner() {
        DataModel.Vault vault = repo.getVault();
        List<String> groupNames = new ArrayList<>();
        groupNames.add("（无分组）");
        for (DataModel.Group g : vault.groups) groupNames.add(g.name);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, groupNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroup.setAdapter(adapter);
    }

    private void loadNote(String noteId) {
        DataModel.Vault vault = repo.getVault();
        for (DataModel.Note note : vault.notes) {
            if (note.id.equals(noteId)) {
                currentNote = note;
                break;
            }
        }
        if (currentNote == null) {
            Toast.makeText(this, "记事不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 填充通用字段
        etTitle.setText(currentNote.title);

        // 设置分组 Spinner
        DataModel.Vault vault = repo.getVault();
        for (int i = 0; i < vault.groups.size(); i++) {
            if (vault.groups.get(i).id.equals(currentNote.groupId)) {
                spinnerGroup.setSelection(i + 1);
                break;
            }
        }

        showFieldsForType(currentNote.type);

        // 填充类型特定字段
        switch (currentNote.type) {
            case DataModel.TYPE_NOTE:
                etContent.setText(currentNote.content);
                break;
            case DataModel.TYPE_TODO:
                for (DataModel.TodoItem item : currentNote.todoItems) {
                    addTodoItemView(item.text, item.checked);
                }
                break;
            case DataModel.TYPE_CONTACT:
                etName.setText(currentNote.name);
                etPhone.setText(currentNote.phone);
                etEmail.setText(currentNote.email);
                etContactRemark.setText(currentNote.remark);
                break;
            case DataModel.TYPE_ACCOUNT:
                etUsername.setText(currentNote.username);
                etPassword.setText(currentNote.password);
                etUrl.setText(currentNote.url);
                etAccountRemark.setText(currentNote.remark);
                break;
        }
    }

    private void showFieldsForType(String type) {
        layoutNote.setVisibility(View.GONE);
        layoutTodo.setVisibility(View.GONE);
        layoutContact.setVisibility(View.GONE);
        layoutAccount.setVisibility(View.GONE);

        switch (type) {
            case DataModel.TYPE_NOTE:    layoutNote.setVisibility(View.VISIBLE); break;
            case DataModel.TYPE_TODO:    layoutTodo.setVisibility(View.VISIBLE); break;
            case DataModel.TYPE_CONTACT: layoutContact.setVisibility(View.VISIBLE); break;
            case DataModel.TYPE_ACCOUNT: layoutAccount.setVisibility(View.VISIBLE); break;
        }
    }

    /**
     * 动态添加一个 Todo 子项视图
     */
    private void addTodoItemView(String text, boolean checked) {
        View itemView = getLayoutInflater().inflate(R.layout.item_todo_edit, llTodoItems, false);
        CheckBox checkBox = itemView.findViewById(R.id.checkBox);
        EditText etItem = itemView.findViewById(R.id.etTodoItem);
        ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteItem);

        checkBox.setChecked(checked);
        etItem.setText(text);
        btnDelete.setOnClickListener(v -> llTodoItems.removeView(itemView));

        llTodoItems.addView(itemView);
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }

        currentNote.title = title;
        currentNote.updatedAt = System.currentTimeMillis();

        // 设置分组
        int groupPos = spinnerGroup.getSelectedItemPosition();
        DataModel.Vault vault = repo.getVault();
        if (groupPos > 0 && groupPos - 1 < vault.groups.size()) {
            currentNote.groupId = vault.groups.get(groupPos - 1).id;
        } else {
            currentNote.groupId = "";
        }

        // 收集类型特定字段
        switch (currentNote.type) {
            case DataModel.TYPE_NOTE:
                currentNote.content = etContent.getText().toString();
                break;
            case DataModel.TYPE_TODO:
                currentNote.todoItems.clear();
                for (int i = 0; i < llTodoItems.getChildCount(); i++) {
                    View child = llTodoItems.getChildAt(i);
                    CheckBox cb = child.findViewById(R.id.checkBox);
                    EditText et = child.findViewById(R.id.etTodoItem);
                    String itemText = et.getText().toString().trim();
                    if (!itemText.isEmpty()) {
                        currentNote.todoItems.add(
                            new DataModel.TodoItem(itemText, cb.isChecked())
                        );
                    }
                }
                break;
            case DataModel.TYPE_CONTACT:
                currentNote.name = etName.getText().toString().trim();
                currentNote.phone = etPhone.getText().toString().trim();
                currentNote.email = etEmail.getText().toString().trim();
                currentNote.remark = etContactRemark.getText().toString().trim();
                break;
            case DataModel.TYPE_ACCOUNT:
                currentNote.username = etUsername.getText().toString().trim();
                currentNote.password = etPassword.getText().toString();
                currentNote.url = etUrl.getText().toString().trim();
                currentNote.remark = etAccountRemark.getText().toString().trim();
                break;
        }

        // 新建时加入列表
        if (isNewNote) {
            vault.notes.add(currentNote);
        }

        try {
            repo.saveVault();
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
