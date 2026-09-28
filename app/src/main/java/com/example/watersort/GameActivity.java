package com.example.watersort;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class GameActivity extends AppCompatActivity implements BoardView.Listener {

    private BoardView board;
    private TextView tvLevel, tvMoves, tvTimer, tvCoins;
    private MaterialButton btnAdd, btnHint;
    private ToneGenerator tone;
    private int level;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        level = getIntent().getIntExtra("level", 1);
        board = findViewById(R.id.board);
        tvLevel = findViewById(R.id.tvLevel);
        tvMoves = findViewById(R.id.tvMoves);
        tvTimer = findViewById(R.id.tvTimer);
        tvCoins = findViewById(R.id.tvCoins);
        btnAdd = findViewById(R.id.btnAdd);
        btnHint = findViewById(R.id.btnHint);
        board.setListener(this);

        updateCoinsDisplay();

        findViewById(R.id.btnMenu).setOnClickListener(v -> finish());
        findViewById(R.id.btnUndo).setOnClickListener(v -> board.undo());
        findViewById(R.id.btnRestart).setOnClickListener(v -> startLevel(level));
        
        btnAdd.setOnClickListener(v -> {
            if (board.addTube()) {
                btnAdd.setEnabled(false);
            } else {
                Toast.makeText(this, "Bạn đã dùng thêm ống ở màn này rồi", Toast.LENGTH_SHORT).show();
            }
        });

        btnHint.setOnClickListener(v -> {
            if (Prefs.spendCoins(this, 15)) {
                updateCoinsDisplay();
                if (!board.showHint()) {
                    Toast.makeText(this, "Không tìm thấy gợi ý nào khả dụng!", Toast.LENGTH_SHORT).show();
                    Prefs.addCoins(this, 15); // Hoàn lại vàng nếu không dùng được
                    updateCoinsDisplay();
                }
            } else {
                Toast.makeText(this, "Không đủ vàng! Cần 15 💰 để dùng Gợi ý", Toast.LENGTH_SHORT).show();
            }
        });

        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
        } catch (RuntimeException e) {
            tone = null;
        }
        startLevel(level);
    }

    private void updateCoinsDisplay() {
        if (tvCoins != null) {
            tvCoins.setText("💰 " + Prefs.coins(this));
        }
    }

    private void startLevel(int lv) {
        level = lv;
        tvLevel.setText("Màn " + lv);
        btnAdd.setEnabled(true);
        board.startLevel(lv);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        boolean hasTimer = LevelConfig.hasTimer(level);
        boolean hasHidden = LevelConfig.hasHidden(level);

        StringBuilder eventStr = new StringBuilder("Màn " + level);
        if (hasHidden || hasTimer) {
            eventStr.append(" • Sự kiện: ");
            if (hasHidden) eventStr.append("🕵️ Ẩn màu ");
            if (hasTimer) eventStr.append("⏱️ Giới hạn 75s");
            Toast.makeText(this, eventStr.toString(), Toast.LENGTH_SHORT).show();
        }

        if (hasTimer) {
            tvTimer.setVisibility(View.VISIBLE);
            countDownTimer = new CountDownTimer(75000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long secs = millisUntilFinished / 1000;
                    tvTimer.setText("⏱ " + secs + "s");
                }

                @Override
                public void onFinish() {
                    tvTimer.setText("⏱ 0s");
                    showTimeOutDialog();
                }
            }.start();
        } else {
            tvTimer.setVisibility(View.GONE);
        }
    }

    private void showTimeOutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("⏰ Hết thời gian!")
                .setMessage("Bạn không hoàn thành màn " + level + " kịp thời gian cho phép.")
                .setCancelable(false)
                .setNegativeButton("Menu", (d, w) -> finish())
                .setPositiveButton("Thử lại", (d, w) -> startLevel(level))
                .show();
    }

    @Override
    public void onMoved(int moves) {
        tvMoves.setText("Nước đi: " + moves);
    }

    @Override
    public void onPour() {
        if (Prefs.sound(this) && tone != null) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60);
        if (Prefs.vibrate(this)) vibrate(25);
    }

    @Override
    public void onWin(int moves) {
        if (countDownTimer != null) countDownTimer.cancel();
        Prefs.unlock(this, level + 1);

        // Thưởng 50 coins khi qua màn
        Prefs.addCoins(this, 50);
        updateCoinsDisplay();

        // Tính sao (Star Rating) dựa trên số nước đi
        String stars = "⭐⭐⭐";
        if (moves > 40) stars = "⭐";
        else if (moves > 25) stars = "⭐⭐";

        boolean hasNext = level < Prefs.TOTAL_LEVELS;
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this)
                .setTitle("🎉 Hoàn thành màn " + level)
                .setMessage(stars + "\n\n" + (hasNext ? "Bạn xong trong " + moves + " nước đi.\nNhận +50 💰 thưởng!"
                        : "Bạn đã hoàn thành tất cả các màn! Nhận +50 💰 thưởng!"))
                .setCancelable(false)
                .setNegativeButton("Menu", (d, w) -> finish())
                .setNeutralButton("Chơi lại", (d, w) -> startLevel(level));
        if (hasNext) {
            b.setPositiveButton("Màn tiếp", (d, w) -> startLevel(level + 1));
        }
        b.show();
    }

    @SuppressWarnings("deprecation")
    private void vibrate(int ms) {
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(ms);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (tone != null) tone.release();
    }
}
