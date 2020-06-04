package com.waz.avs;

import android.util.Log;

public class AVSystem {
    private static boolean isLoaded = false;

    public static void load() {
        if (isLoaded)
            return;

        System.loadLibrary("avs");
        isLoaded = true;
    }
}
