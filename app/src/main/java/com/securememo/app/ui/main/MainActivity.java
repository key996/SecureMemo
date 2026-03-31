package com.securememo.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.securememo.app.R;
import com.securememo.app.data.DataModel;
import com.securememo.app.data.VaultRepository;
import com.securememo.app.ui.note.NoteEditActivity;
import com.securememo.app.ui.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面：按分组展示所有记事，支持搜索
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoteListAdapter adapter;
    private EditText etSearch;
    private FloatingActionButton fab;

    private VaultRepository repo;
    private List<Object> displayItems = new ArrayList<>(); // 混合 Group 标题和 Note 的列表

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = VaultRepository.getInstance(this);

        recyclerView = findViewById(R.id.recyclerView);
        etSearch = findViewById(R.id.etSearch);
        fab = findViewById(R.id.fab);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteListAdapter(displayItems, this::onNoteClick, this::onNoteLongClick);
        recyclerView.setAdapter(adapter);

        // 搜索监听
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshList(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 新建记事按钮
        fab.setOnClickListener(v -> showNewNoteTypeDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList("");
        etSearch.setText("");
    }

    // ==================== 列表刷新 ====================

    /**
     * 刷新列表，支持搜索过滤
     */
    private void refreshList(String query) {
        DataModel.Vault vault = repo.getVault();
        if (vault == null) return;

        displayItems.clear();

        for (DataModel.Group group : vault.groups) {
            List<DataModel.Note> groupNotes = new ArrayList<>();
            for (DataModel.Note note : vault.notes) {
                if (group.id.equals(note.groupId)) {
                    if (query.isEmpty() || note.title.toLowerCase().contains(query.toLowerCase())) {
                        groupNotes.add(note);
                    }
                }
            }
            if (!groupNotes.isEmpty() || query.isEmpty()) {
                displayItems.add(group);  // 分组标题
                displayItems.addAll(groupNotes);
            }
        }

        // 未分组的记事
        List<DataModel.Note> ungrouped = new ArrayList<>();
        for (DataModel.Note note : vault.notes) {
            if (note.groupId == null || note.groupId.isEmpty()) {
                if (query.isEmpty() || note.title.toLowerCase().contains(query.toLowerCase())) {
                    ungrouped.add(note);
                }
            }
        }
        if (!ungrouped.isEmpty()) {
            DataModel.Group fakeGroup = new DataModel.Group("", "未分组", "#9E9E9E");
            displayItems.add(fakeGroup);
            displayItems.addAll(ungrouped);
        }

        adapter.notifyDataSetChanged();
    }

    // ==================== 点击事件 ====================

    private void onNoteClick(DataModel.Note note) {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.putExtra("note_id", note.id);
        startActivity(intent);
    }

    private void onNoteLongClick(DataModel.Note note) {
        new AlertDialog.Builder(this)
            .setTitle("删除记事")
            .setMessage("确定要删除「" + note.title + "」吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                DataModel.Vault vault = repo.getVault();
                vault.notes.removeIf(n -> n.id.equals(note.id));
                try {
                    repo.saveVault();
                } catch (Exception e) {
                    // 保存失败提示
                }
                refreshList("");
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // ==================== 新建记事类型选择 ====================

    private void showNewNoteTypeDialog() {
        String[] types = {"📝 普通笔记", "✅ Todo 清单", "👤 联系人", "🔑 账号密码"};
        String[] typeKeys = {DataModel.TYPE_NOTE, DataModel.TYPE_TODO,
                             DataModel.TYPE_CONTACT, DataModel.TYPE_ACCOUNT};

        new AlertDialog.Builder(this)
            .setTitle("新建记事")
            .setItems(types, (dialog, which) -> {
                Intent intent = new Intent(this, NoteEditActivity.class);
                intent.putExtra("note_type", typeKeys[which]);
                startActivity(intent);
            })
            .show();
    }

    // ==================== 菜单 ====================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_groups) {
            showGroupManageDialog();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ==================== 分组管理 ====================

    private void showGroupManageDialog() {
        DataModel.Vault vault = repo.getVault();
        String[] groupNames = vault.groups.stream()
            .map(g -> g.name).toArray(String[]::new);

        new AlertDialog.Builder(this)
            .setTitle("分组管理")
            .setItems(groupNames, null)
            .setPositiveButton("新建分组", (dialog, which) -> showAddGroupDialog())
            .setNeutralButton("关闭", null)
            .show();
    }

    private void showAddGroupDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("分组名称");
        new AlertDialog.Builder(this)
            .setTitle("新建分组")
            .setView(input)
            .setPositiveButton("创建", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    DataModel.Group group = new DataModel.Group(
                        VaultRepository.generateId(), name, "#607D8B"
                    );
                    repo.getVault().groups.add(group);
                    try { repo.saveVault(); } catch (Exception ignored) {}
                    refreshList("");
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
}
