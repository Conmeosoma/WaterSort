package com.example.watersort;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class BoardView extends View {

    public interface Listener {
        void onMoved(int moves);
        void onPour();
        void onWin(int moves);
    }

    public static final int CAP = 4; // sức chứa mỗi ống
    private static final int[] COLORS = {
            0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFDD835, 0xFF8E24AA, 0xFFFB8C00,
            0xFF00ACC1, 0xFFEC407A, 0xFF6D4C41, 0xFF9E9E9E, 0xFFC0CA33, 0xFF3949AB};

    private final ArrayList<ArrayList<Integer>> tubes = new ArrayList<>(); // đáy -> miệng
    private final ArrayList<RectF> rects = new ArrayList<>();
    private final ArrayDeque<int[]> history = new ArrayDeque<>();        // {từ, tới, số đơn vị}
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint streamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();

    private Listener listener;
    private int moves = 0, selected = -1, currentLevel = 1;
    private boolean extraUsed = false, finished = false;

    // Animation state
    private boolean isAnimating = false;
    private ValueAnimator pourAnimator;
    private float pourProgress = 0f;
    private int pourFrom = -1, pourTo = -1, pourColor = -1, pourCount = 0;

    // Hint state
    private int[] hintPair = null;

    public BoardView(Context context) { this(context, null); }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(0xCCFFFFFF);
        stroke.setStrokeWidth(3 * d);
        streamPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xFF94A3B8);
        textPaint.setTextSize(14 * d);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setListener(Listener l) { listener = l; }

    /** Bắt đầu màn `level`. Cùng level luôn ra cùng bố cục (seed cố định). */
    public void startLevel(int level) {
        if (pourAnimator != null && pourAnimator.isRunning()) {
            pourAnimator.cancel();
        }
        isAnimating = false;
        hintPair = null;
        currentLevel = level;
        int colorCount;
        if (level <= 150) {
            colorCount = Math.min(3 + (level - 1) / 25, 7);
        } else if (level <= 400) {
            colorCount = Math.min(7 + (level - 151) / 80, 9);
        } else {
            colorCount = 10;
        }
        int emptyTubeCount = level > 750 ? 1 : 2;
        Random rnd = new Random(level * 7919L);
        ArrayList<Integer> units = new ArrayList<>();
        do {
            units.clear();
            for (int colorIdx = 0; colorIdx < colorCount; colorIdx++)
                for (int i = 0; i < CAP; i++) units.add(colorIdx);
            Collections.shuffle(units, rnd);
            tubes.clear();
            for (int t = 0; t < colorCount; t++)
                tubes.add(new ArrayList<>(units.subList(t * CAP, (t + 1) * CAP)));
            for (int e = 0; e < emptyTubeCount; e++) {
                tubes.add(new ArrayList<>());
            }
        } while (isSolved());

        history.clear();
        moves = 0;
        selected = -1;
        extraUsed = false;
        finished = false;
        layoutTubes();
        invalidate();
        if (listener != null) listener.onMoved(0);
    }

    public void undo() {
        if (history.isEmpty() || finished || isAnimating) return;
        int[] m = history.pop();
        ArrayList<Integer> from = tubes.get(m[0]), to = tubes.get(m[1]);
        for (int i = 0; i < m[2]; i++) from.add(to.remove(to.size() - 1));
        moves--;
        selected = -1;
        hintPair = null;
        invalidate();
        if (listener != null) listener.onMoved(moves);
    }

    /** Thêm 1 ống trống (mỗi màn 1 lần). Trả về false nếu không thêm được. */
    public boolean addTube() {
        if (extraUsed || finished || isAnimating) return false;
        tubes.add(new ArrayList<>());
        extraUsed = true;
        layoutTubes();
        invalidate();
        return true;
    }

    /** Tìm một nước đi hợp lệ để gợi ý cho người chơi. Trả về int[]{from, to} hoặc null. */
    public int[] findHint() {
        if (finished || isAnimating) return null;
        for (int from = 0; from < tubes.size(); from++) {
            ArrayList<Integer> src = tubes.get(from);
            if (src.isEmpty()) continue;
            boolean srcCompleted = true;
            if (src.size() != CAP) {
                srcCompleted = false;
            } else {
                for (int c : src) if (c != src.get(0)) { srcCompleted = false; break; }
            }
            if (srcCompleted) continue;

            int color = src.get(src.size() - 1);
            for (int to = 0; to < tubes.size(); to++) {
                if (from == to) continue;
                ArrayList<Integer> dst = tubes.get(to);
                if (dst.size() >= CAP) continue;
                if (dst.isEmpty()) {
                    if (srcCompleted) continue;
                    return new int[]{from, to};
                }
                if (dst.get(dst.size() - 1) == color) {
                    return new int[]{from, to};
                }
            }
        }
        return null;
    }

    public boolean showHint() {
        hintPair = findHint();
        if (hintPair != null) {
            invalidate();
            postDelayed(() -> {
                hintPair = null;
                invalidate();
            }, 2000);
            return true;
        }
        return false;
    }

    // ---------- Logic đổ nước với Ultra-Smooth Animation ----------
    private boolean tryMove(int from, int to) {
        if (isAnimating) return false;
        ArrayList<Integer> a = tubes.get(from), b = tubes.get(to);
        if (a.isEmpty() || b.size() >= CAP) return false;
        int color = a.get(a.size() - 1);
        if (!b.isEmpty() && b.get(b.size() - 1) != color) return false;

        int block = 0; // số đơn vị cùng màu liên tiếp ở miệng ống nguồn
        for (int i = a.size() - 1; i >= 0 && a.get(i) == color; i--) block++;
        int n = Math.min(block, CAP - b.size());
        if (n <= 0) return false;

        isAnimating = true;
        pourFrom = from;
        pourTo = to;
        pourColor = color;
        pourCount = n;
        pourProgress = 0f;
        hintPair = null;

        if (listener != null) {
            listener.onPour();
        }

        pourAnimator = ValueAnimator.ofFloat(0f, 1f);
        pourAnimator.setDuration(400); // 400ms cho độ mượt tự nhiên, uyển chuyển
        pourAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pourAnimator.addUpdateListener(animation -> {
            pourProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        pourAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                for (int i = 0; i < pourCount; i++) {
                    tubes.get(pourTo).add(tubes.get(pourFrom).remove(tubes.get(pourFrom).size() - 1));
                }
                history.push(new int[]{pourFrom, pourTo, pourCount});
                moves++;
                isAnimating = false;
                pourFrom = -1;
                pourTo = -1;

                if (listener != null) {
                    listener.onMoved(moves);
                }
                if (isSolved()) {
                    finished = true;
                    if (listener != null) listener.onWin(moves);
                }
                invalidate();
            }
        });
        pourAnimator.start();
        return true;
    }

    private boolean isSolved() {
        for (ArrayList<Integer> t : tubes) {
            if (t.isEmpty()) continue;
            if (t.size() != CAP) return false;
            for (int c : t) if (c != t.get(0)) return false;
        }
        return true;
    }

    // ---------- Bố cục & vẽ ----------
    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        layoutTubes();
    }

    private void layoutTubes() {
        rects.clear();
        int n = tubes.size();
        if (getWidth() == 0 || n == 0) return;
        int cols = n <= 6 ? n : (n + 1) / 2;
        int rows = (n + cols - 1) / cols;
        float cellW = getWidth() / (float) cols;
        float cellH = getHeight() / (float) rows;
        float tw = cellW * 0.6f;
        float th = tw * CAP * 1.1f;
        if (th > cellH * 0.8f) {
            th = cellH * 0.8f;
            tw = th / (CAP * 1.1f);
        }
        for (int i = 0; i < n; i++) {
            int r = i / cols, c = i % cols;
            int inRow = Math.min(cols, n - r * cols);
            float offset = (getWidth() - inRow * cellW) / 2f; // căn giữa hàng cuối
            float left = offset + c * cellW + (cellW - tw) / 2f;
            float top = r * cellH + (cellH - th) / 2f;
            rects.add(new RectF(left, top, left + tw, top + th));
        }
    }

    private float lift() { return getHeight() * 0.04f; }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < tubes.size() && i < rects.size(); i++) {
            RectF r = new RectF(rects.get(i));
            if (i == selected && !isAnimating) r.offset(0, -lift());

            boolean isSource = isAnimating && i == pourFrom;

            canvas.save();
            if (isSource) {
                RectF destR = rects.get(pourTo);
                float pivotX = r.centerX();
                float pivotY = r.top;
                // Góc nghiêng uyển chuyển với biên độ 28 độ
                float angle = (destR.centerX() > r.centerX() ? 1f : -1f) * 28f * (float) Math.sin(pourProgress * Math.PI);
                canvas.rotate(angle, pivotX, pivotY);
            }

            float rad = r.width() / 2.2f;
            clip.reset();
            clip.addRoundRect(r, new float[]{0, 0, 0, 0, rad, rad, rad, rad}, Path.Direction.CW);

            canvas.save();
            canvas.clipPath(clip);
            ArrayList<Integer> t = tubes.get(i);
            float seg = r.height() / CAP;
            for (int k = 0; k < t.size(); k++) {
                // Sự kiện ngẫu nhiên: Màn nào có sự kiện ẩn màu thì các lớp dưới đáy bị ẩn (?)
                boolean isHidden = (LevelConfig.hasHidden(currentLevel) && k < t.size() - 1);
                if (isHidden) {
                    fill.setColor(0xFF334155); // Màu xám bí ẩn
                } else {
                    fill.setColor(COLORS[t.get(k)]);
                }
                float bottom = r.bottom - k * seg;
                canvas.drawRect(r.left, bottom - seg, r.right, bottom, fill);

                if (isHidden) {
                    float textY = bottom - seg / 2f + (textPaint.descent() - textPaint.ascent()) / 2f - textPaint.descent();
                    canvas.drawText("?", r.centerX(), textY, textPaint);
                }
            }
            canvas.restore();
            canvas.drawPath(clip, stroke);
            canvas.restore();
        }

        // Vẽ highlight cho Gợi ý (Hint)
        if (hintPair != null) {
            Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            hintPaint.setStyle(Paint.Style.STROKE);
            hintPaint.setColor(0xFFFFD700); // Màu vàng Gold nổi bật
            hintPaint.setStrokeWidth(6f * getResources().getDisplayMetrics().density);
            for (int idx : hintPair) {
                if (idx >= 0 && idx < rects.size()) {
                    RectF hr = new RectF(rects.get(idx));
                    hr.inset(-6, -6);
                    float rad = hr.width() / 2.2f;
                    canvas.drawRoundRect(hr, rad, rad, hintPaint);
                }
            }
        }

        // Vẽ dòng nước chảy mượt mà với hiệu ứng mờ dần (fade in/out alpha)
        if (isAnimating && pourFrom >= 0 && pourTo >= 0 && pourFrom < rects.size() && pourTo < rects.size()) {
            RectF src = rects.get(pourFrom);
            RectF dst = rects.get(pourTo);
            streamPaint.setColor(COLORS[pourColor]);

            float startX = src.centerX();
            float startY = src.top;
            float endX = dst.centerX();
            float endY = dst.top;

            if (pourProgress > 0.05f && pourProgress < 0.95f) {
                // Tính toán alpha mượt mà (0 ở đầu/cuối, đậm nhất ở giữa animation)
                float alphaFactor = 1f - Math.abs(pourProgress - 0.5f) * 2f;
                streamPaint.setAlpha((int) (230 * Math.max(0f, alphaFactor)));

                float streamWidth = src.width() * 0.22f;
                canvas.drawRoundRect(
                        Math.min(startX, endX) - streamWidth / 2,
                        Math.min(startY, endY),
                        Math.max(startX, endX) + streamWidth / 2,
                        Math.max(startY, endY) + src.height() * 0.35f,
                        streamWidth / 2, streamWidth / 2, streamPaint
                );
            }
        }
    }

    // ---------- Chạm ----------
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (isAnimating || finished) return true;
        if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (e.getAction() != MotionEvent.ACTION_UP) return true;
        performClick();

        int hit = -1;
        for (int i = 0; i < rects.size(); i++) {
            RectF r = new RectF(rects.get(i));
            r.inset(-8, -lift());
            if (r.contains(e.getX(), e.getY())) { hit = i; break; }
        }

        if (hit < 0) {
            selected = -1;
        } else if (selected < 0) {
            if (!tubes.get(hit).isEmpty()) selected = hit;
        } else if (selected == hit) {
            selected = -1;
        } else if (tryMove(selected, hit)) {
            selected = -1;
        } else {
            selected = tubes.get(hit).isEmpty() ? -1 : hit; // đổi sang ống vừa chạm
        }
        invalidate();
        return true;
    }

    @Override
    public boolean performClick() { return super.performClick(); }
}
