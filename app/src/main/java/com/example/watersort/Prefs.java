package com.example.watersort;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    public static final int TOTAL_LEVELS = 1000;

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("water_sort", Context.MODE_PRIVATE);
    }

    public static int unlocked(Context c) { return sp(c).getInt("unlocked", 1); }

    public static void unlock(Context c, int level) {
        if (level > unlocked(c) && level <= TOTAL_LEVELS) {
            sp(c).edit().putInt("unlocked", level).apply();
        }
    }

    public static void reset(Context c) { sp(c).edit().putInt("unlocked", 1).apply(); }

    public static boolean sound(Context c) { return sp(c).getBoolean("sound", true); }
    public static boolean vibrate(Context c) { return sp(c).getBoolean("vibrate", true); }

    public static void setSound(Context c, boolean v) { sp(c).edit().putBoolean("sound", v).apply(); }
    public static void setVibrate(Context c, boolean v) { sp(c).edit().putBoolean("vibrate", v).apply(); }

    // Quản lý tiền vàng (Coin Economy)
    public static int coins(Context c) { return sp(c).getInt("coins", 100); }
    public static void addCoins(Context c, int amount) {
        int current = coins(c);
        sp(c).edit().putInt("coins", current + amount).apply();
    }
    public static boolean spendCoins(Context c, int amount) {
        int current = coins(c);
        if (current >= amount) {
            sp(c).edit().putInt("coins", current - amount).apply();
            return true;
        }
        return false;
    }
}
