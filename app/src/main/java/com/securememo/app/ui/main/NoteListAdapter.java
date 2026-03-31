package com.securememo.app.ui.main;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.securememo.app.R;
import com.securememo.app.data.DataModel;

import java.util.List;

/**
 * 主列表适配器：支持分组标题 + 记事条目两种 ViewType
 */
public class NoteListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_GROUP = 0;
    private static final int TYPE_NOTE = 1;

    private final List<Object> items;
    private final OnNoteClickListener clickListener;
    private final OnNoteLongClickListener longClickListener;

    public interface OnNoteClickListener {
        void onClick(DataModel.Note note);
    }

    public interface OnNoteLongClickListener {
        void onLongClick(DataModel.Note note);
    }

    public NoteListAdapter(List<Object> items,
                           OnNoteClickListener clickListener,
                           OnNoteLongClickListener longClickListener) {
        this.items = items;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof DataModel.Group ? TYPE_GROUP : TYPE_NOTE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_GROUP) {
            View view = inflater.inflate(R.layout.item_group_header, parent, false);
            return new GroupViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_note, parent, false);
            return new NoteViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof GroupViewHolder) {
            DataModel.Group group = (DataModel.Group) items.get(position);
            ((GroupViewHolder) holder).bind(group);
        } else {
            DataModel.Note note = (DataModel.Note) items.get(position);
            ((NoteViewHolder) holder).bind(note, clickListener, longClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ==================== ViewHolder ====================

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupName;
        View colorBar;

        GroupViewHolder(View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            colorBar = itemView.findViewById(R.id.colorBar);
        }

        void bind(DataModel.Group group) {
            tvGroupName.setText(group.name);
            try {
                colorBar.setBackgroundColor(Color.parseColor(group.color));
            } catch (Exception e) {
                colorBar.setBackgroundColor(Color.GRAY);
            }
        }
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvType;
        TextView tvPreview;

        NoteViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvType = itemView.findViewById(R.id.tvType);
            tvPreview = itemView.findViewById(R.id.tvPreview);
        }

        void bind(DataModel.Note note,
                  OnNoteClickListener clickListener,
                  OnNoteLongClickListener longClickListener) {
            tvTitle.setText(note.title.isEmpty() ? "（无标题）" : note.title);

            // 类型标签
            switch (note.type) {
                case DataModel.TYPE_NOTE:    tvType.setText("📝"); break;
                case DataModel.TYPE_TODO:    tvType.setText("✅"); break;
                case DataModel.TYPE_CONTACT: tvType.setText("👤"); break;
                case DataModel.TYPE_ACCOUNT: tvType.setText("🔑"); break;
                default: tvType.setText("📄");
            }

            // 预览文本
            String preview = "";
            switch (note.type) {
                case DataModel.TYPE_NOTE:
                    preview = note.content != null ? note.content : "";
                    break;
                case DataModel.TYPE_TODO:
                    int done = (int) note.todoItems.stream().filter(i -> i.checked).count();
                    preview = done + "/" + note.todoItems.size() + " 已完成";
                    break;
                case DataModel.TYPE_CONTACT:
                    preview = note.phone != null ? note.phone : "";
                    break;
                case DataModel.TYPE_ACCOUNT:
                    preview = note.username != null ? note.username : "";
                    break;
            }
            if (preview.length() > 50) preview = preview.substring(0, 50) + "...";
            tvPreview.setText(preview);

            itemView.setOnClickListener(v -> clickListener.onClick(note));
            itemView.setOnLongClickListener(v -> {
                longClickListener.onLongClick(note);
                return true;
            });
        }
    }
}
