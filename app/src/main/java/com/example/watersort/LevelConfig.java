package com.example.watersort;

import java.util.Random;

public class LevelConfig {
    /** Kiểm tra xem màn này có sự kiện nước màu bị ẩn (?) hay không */
    public static boolean hasHidden(int level) {
        if (level < 20) return false;
        Random rnd = new Random(level * 7919L);
        return rnd.nextFloat() < 0.35f; // 35% xác suất ngẫu nhiên có từ màn 20
    }

    /** Kiểm tra xem màn này có sự kiện giới hạn thời gian hay không */
    public static boolean hasTimer(int level) {
        if (level < 40) return false;
        Random rnd = new Random(level * 3137L);
        return rnd.nextFloat() < 0.25f; // 25% xác suất ngẫu nhiên có từ màn 40
    }
}
