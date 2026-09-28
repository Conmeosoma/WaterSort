package com.example.watersort;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class LevelSelectActivity extends AppCompatActivity {

    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);
        rv = findViewById(R.id.rvLevels);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        rv.setAdapter(new LevelAdapter()); // tạo lại để cập nhật màn đã mở khóa
    }

    private class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(@NonNull View v) {
            super(v);
            tv = v.findViewById(R.id.tvLevel);
        }
    }

    private class LevelAdapter extends RecyclerView.Adapter<VH> {
        private final int unlocked = Prefs.unlocked(LevelSelectActivity.this);

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_level, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            int level = position + 1;
            boolean open = level <= unlocked;
            h.tv.setText(open ? String.valueOf(level) : "🔒");
            h.itemView.setAlpha(open ? 1f : 0.4f);
            h.itemView.setOnClickListener(v -> {
                if (open) {
                    Intent i = new Intent(LevelSelectActivity.this, GameActivity.class);
                    i.putExtra("level", level);
                    startActivity(i);
                } else {
                    Toast.makeText(LevelSelectActivity.this,
                            "Hãy hoàn thành màn trước để mở khóa", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() { return Prefs.TOTAL_LEVELS; }
    }
}