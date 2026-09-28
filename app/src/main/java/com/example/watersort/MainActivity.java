package com.example.watersort;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btnPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(v -> {
            Intent i = new Intent(this, GameActivity.class);
            i.putExtra("level", Prefs.unlocked(this));
            startActivity(i);
        });
        findViewById(R.id.btnLevels).setOnClickListener(v ->
                startActivity(new Intent(this, LevelSelectActivity.class)));
        findViewById(R.id.btnHowTo).setOnClickListener(v -> showHowTo());
        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettings());
        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        btnPlay.setText("CHƠI - MÀN " + Prefs.unlocked(this));
    }

    private void showHowTo() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cách chơi")
                .setMessage("• Chạm vào một ống để chọn, chạm ống khác để đổ nước sang.\n"
                        + "• Chỉ đổ được khi ống đích còn chỗ và màu trên cùng giống nhau (hoặc ống trống).\n"
                        + "• Mục tiêu: mỗi ống chỉ còn một màu và đầy.\n"
                        + "• Hoàn tác để lùi một nước, Thêm ống để có thêm 1 ống trống (mỗi màn 1 lần).")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void showSettings() {
        boolean[] checked = {Prefs.sound(this), Prefs.vibrate(this)};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cài đặt")
                .setMultiChoiceItems(new String[]{"Âm thanh", "Rung"}, checked,
                        (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Lưu", (d, w) -> {
                    Prefs.setSound(this, checked[0]);
                    Prefs.setVibrate(this, checked[1]);
                })
                .setNeutralButton("Xóa tiến trình", (d, w) -> confirmReset())
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa tiến trình")
                .setMessage("Bạn sẽ quay lại màn 1. Bạn chắc chứ?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> {
                    Prefs.reset(this);
                    btnPlay.setText("CHƠI - MÀN 1");
                })
                .show();
    }
}