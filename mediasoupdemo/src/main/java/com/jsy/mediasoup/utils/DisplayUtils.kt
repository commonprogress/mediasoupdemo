package com.jsy.mediasoup.utils

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Build.VERSION
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager

object DisplayUtils {

    /**
     * dp2px
     */
    @JvmStatic
    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    @JvmStatic
    fun getScreenResolution(context: Context): Point? {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val screenResolution = Point()
        if (VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(screenResolution)
        } else {
            screenResolution[display.width] = display.height
        }
        return screenResolution
    }

    @JvmStatic
    fun getScreenOrientation(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        return when {
            display.width == display.height -> {
                3
            }
            display.width < display.height -> {
                1
            }
            else -> {
                2
            }
        }
    }

    var statusbarheight = 0

    //获取状态栏高度
    @JvmStatic
    fun getStatusBarHeight(context: Context): Int {
        if (statusbarheight == 0) {
            try {
                val c = Class.forName("com.android.internal.R\$dimen")
                val o = c.newInstance()
                val field = c.getField("status_bar_height")
                val x = field[o] as Int
                statusbarheight =
                    context.applicationContext.resources.getDimensionPixelSize(x)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (statusbarheight == 0) {
            statusbarheight = dip2px(context, 25f)
        }
        return statusbarheight
    }

    var navigationBarHeight = -1

    //获取状态栏高度
    @JvmStatic
    fun getNavigationBarHeight(context: Context): Int {
        if (navigationBarHeight < 0) {
            try {
                navigationBarHeight = if (checkDeviceHasNavigationBar(context)) {
                    val c =
                        Class.forName("com.android.internal.R\$dimen")
                    val o = c.newInstance()
                    val field = c.getField("navigation_bar_height")
                    val x = field[o] as Int
                    context.applicationContext.resources.getDimensionPixelSize(x)
                } else {
                    0
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return navigationBarHeight
    }


    private var screenWidth = 0
    @JvmStatic
    fun getScreenWidth(context: Context?): Int {
        if (screenWidth > 0) {
            return screenWidth
        }
        if (context == null) {
            return 0
        }
        return if (context is Activity) {
            val localDisplayMetrics = DisplayMetrics()
            context.windowManager.defaultDisplay
                .getMetrics(localDisplayMetrics)
            screenWidth = localDisplayMetrics.widthPixels
            Math.max(0, screenWidth)
        } else {
            val displayMetrics = context.resources.displayMetrics
            screenWidth = displayMetrics.widthPixels
            Math.max(0, screenWidth)
        }
    }

    private var screenHeight = 0
    @JvmStatic
    fun getScreenHeight(context: Context?): Int {
        if (screenHeight > 0) {
            return screenHeight
        }
        if (context == null) {
            return 0
        }
        return if (context is Activity) {
            val localDisplayMetrics = DisplayMetrics()
            context.windowManager.defaultDisplay
                .getMetrics(localDisplayMetrics)
            screenHeight = localDisplayMetrics.heightPixels - getStatusBarHeight(context)
            Math.max(0, screenHeight)
        } else {
            val displayMetrics = context.resources.displayMetrics
            screenHeight = displayMetrics.heightPixels - getStatusBarHeight(context)
            Math.max(0, screenHeight)
        }
    }

    //获取是否存在NavigationBar
    @JvmStatic
    fun checkDeviceHasNavigationBar(context: Context): Boolean {
        var hasNavigationBar = false
        val rs = context.resources
        val id = rs.getIdentifier("config_showNavigationBar", "bool", "android")
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id)
        }
        try {
            val systemPropertiesClass =
                Class.forName("android.os.SystemProperties")
            val m =
                systemPropertiesClass.getMethod("get", String::class.java)
            val navBarOverride =
                m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as String
            if ("1" == navBarOverride) {
                hasNavigationBar = false
            } else if ("0" == navBarOverride) {
                hasNavigationBar = true
            }
        } catch (e: Exception) {
        }
        return hasNavigationBar
    }

    @JvmStatic
    fun getSystemUiVisibility(): Int {
        var flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        if (VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        return flags
    }
}
