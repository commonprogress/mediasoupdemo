package com.jsy.mediasoup.fw

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.fragment.app.Fragment
import com.jsy.mediasoup.utils.LogUtils

import java.lang.reflect.InvocationTargetException


object FloatingWindowUtils {
    private val TAG by lazy { FloatingWindowUtils::class.java.simpleName }

    /**
     * 系统悬浮窗权限
     */
    private const val OP_SYSTEM_ALERT_WINDOW = 24

    @JvmStatic
    fun checkSystemAlterWindow(context: Context): Boolean {
        return checkAlertWindowsPermission(context, OP_SYSTEM_ALERT_WINDOW)
    }

    @JvmStatic
    fun checkNofication(context: Context): Boolean {
        return checkAlertWindowsPermission(context, 25)
    }

    /**
     * 判断 悬浮窗口权限是否打开
     *
     * @param context
     * @return true 允许  false禁止
     */
    @JvmStatic
    fun checkAlertWindowsPermission(context: Context, op: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return true
        }
        try {
            val `object` = context.getSystemService(Context.APP_OPS_SERVICE) ?: return false
            val localClass: Class<*> = `object`.javaClass
            val arrayOfClass: Array<Class<*>?> = arrayOfNulls(3)
            arrayOfClass[0] = Integer.TYPE
            arrayOfClass[1] = Integer.TYPE
            arrayOfClass[2] = String::class.java
            val method = localClass.getMethod("checkOp", *arrayOfClass) ?: return false
            val arrayOfObject1 = arrayOfNulls<Any>(3)
            arrayOfObject1[0] = op
            arrayOfObject1[1] = Binder.getCallingUid()
            arrayOfObject1[2] = context.packageName
            val m = method.invoke(`object`, arrayOfObject1) as Int
            return m == AppOpsManager.MODE_ALLOWED
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            LogUtils.e(
                TAG,
                "checkAlertWindowsPermission NoSuchMethodException:" + e.localizedMessage
            )
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            LogUtils.e(
                TAG,
                "checkAlertWindowsPermission IllegalAccessException:" + e.localizedMessage
            )
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            LogUtils.e(
                TAG,
                "checkAlertWindowsPermission IllegalArgumentException:" + e.localizedMessage
            )
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
            LogUtils.e(
                TAG,
                "checkAlertWindowsPermission InvocationTargetException:" + e.localizedMessage
            )
        }
        return false
    }

    /**
     * 判断悬浮窗权限是否开启
     *
     * @param context
     * @return
     */
    @JvmStatic
    fun checkFloatPermission(context: Context?): Boolean {
        LogUtils.i(TAG, "checkFloatPermission start Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return true
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                LogUtils.i(TAG, "checkFloatPermission mid 1")
                var cls = Class.forName("android.content.Context")
                val declaredField = cls.getDeclaredField("APP_OPS_SERVICE")
                declaredField.isAccessible = true
                var obj: Any? = declaredField[cls] as? String ?: return false
                val str2 = obj as String
                obj = cls.getMethod("getSystemService", String::class.java).invoke(context, str2)
                cls = Class.forName("android.app.AppOpsManager")
                val declaredField2 = cls.getDeclaredField("MODE_ALLOWED")
                declaredField2.isAccessible = true
                val checkOp =
                    cls.getMethod("checkOp", Integer.TYPE, Integer.TYPE, String::class.java)
                val result = checkOp.invoke(
                    obj,
                    OP_SYSTEM_ALERT_WINDOW,
                    Binder.getCallingUid(),
                    context!!.packageName
                ) as Int
                result == declaredField2.getInt(cls)
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
                LogUtils.e(TAG, "checkFloatPermission NoSuchMethodException:" + e.localizedMessage)
                false
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
                LogUtils.e(TAG, "checkFloatPermission IllegalAccessException:" + e.localizedMessage)
                false
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                LogUtils.e(
                    TAG,
                    "checkFloatPermission IllegalArgumentException:" + e.localizedMessage
                )
                false
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
                LogUtils.e(
                    TAG,
                    "checkFloatPermission InvocationTargetException:" + e.localizedMessage
                )
                false
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                LogUtils.e(TAG, "checkFloatPermission ClassNotFoundException:" + e.localizedMessage)
                false
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
                LogUtils.e(TAG, "checkFloatPermission NoSuchFieldException:" + e.localizedMessage)
                false
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val appOpsMgr = context!!.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                    ?: return false
                val mode = appOpsMgr.checkOpNoThrow(
                    "android:system_alert_window",
                    Process.myUid(),
                    context.packageName
                )
                LogUtils.i(TAG, "checkFloatPermission mid 3 mode:$mode, ${Settings.canDrawOverlays(context)}")
                mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED
            } else {
                LogUtils.i(TAG, "checkFloatPermission mid 2")
                Settings.canDrawOverlays(context)
            }
        }
    }

    /**
     * AppOpsManager.MODE_ALLOWED —— 表示授予了权限并且重新打开了应用程序
     * AppOpsManager.MODE_IGNORED —— 表示授予权限并返回应用程序
     * AppOpsManager.MODE_ERRORED —— 表示当前应用没有此权限
     * AppOpsManager.MODE_DEFAULT —— 表示默认值，有些手机在没有开启权限时，mode的值就是这个
     *
     * @param activity
     * @param requestCode
     */
    @JvmStatic
    fun applyFloatPermission(activity: Activity?, requestCode: Int): Boolean {
        return try {
            if (null == activity || activity.isFinishing ||
                activity.isDestroyed || checkFloatPermission(activity)
            ) {
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //8.0以上
                //            第一种：会进入到悬浮窗权限应用列表
                //            使用以下代码，会进入到悬浮窗权限的列表，列表中是手机中需要悬浮窗权限的应用列表，你需要在此列表中找到自己的应用，然后点进去，才可以打开悬浮窗权限
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                //添加这行跳转指定应用，不添加跳转到应用列表，需要用户自己选择。网上有说加上这行华为手机有问题
                intent.data = Uri.parse("package:" + activity.packageName)
                activity.startActivityForResult(intent, requestCode)
                true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //6.0-8.0
                //            第二种：直接进入到自己应用的悬浮窗权限开启界面
                //            使用以下代码，则不会到上述所说的应用列表，而是直接进入到自己应用的悬浮窗权限开启界面
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:" + activity.packageName)
                activity.startActivityForResult(intent, requestCode)
                true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //4.4-6.0
                //无需处理了
                false
            } else { //4.4以下
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e(TAG, "applyFloatPermission Exception:" + e.localizedMessage)
            false
        }
    }

    /**
     * AppOpsManager.MODE_ALLOWED —— 表示授予了权限并且重新打开了应用程序
     * AppOpsManager.MODE_IGNORED —— 表示授予权限并返回应用程序
     * AppOpsManager.MODE_ERRORED —— 表示当前应用没有此权限
     * AppOpsManager.MODE_DEFAULT —— 表示默认值，有些手机在没有开启权限时，mode的值就是这个
     *
     * @param fragment
     * @param requestCode
     */
    @JvmStatic
    fun applyFloatPermission(fragment: Fragment?, requestCode: Int): Boolean {
        return try {
            if (null == fragment || !fragment.isAdded || checkFloatPermission(fragment.context)) {
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //8.0以上
                //            第一种：会进入到悬浮窗权限应用列表
                //            使用以下代码，会进入到悬浮窗权限的列表，列表中是手机中需要悬浮窗权限的应用列表，你需要在此列表中找到自己的应用，然后点进去，才可以打开悬浮窗权限
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                //添加这行跳转指定应用，不添加跳转到应用列表，需要用户自己选择。网上有说加上这行华为手机有问题
                intent.data = Uri.parse("package:" + fragment.context!!.packageName)
                fragment.startActivityForResult(intent, requestCode)
                true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //6.0-8.0
                //            第二种：直接进入到自己应用的悬浮窗权限开启界面
                //            使用以下代码，则不会到上述所说的应用列表，而是直接进入到自己应用的悬浮窗权限开启界面
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:" + fragment.context!!.packageName)
                fragment.startActivityForResult(intent, requestCode)
                true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //4.4-6.0
                //无需处理了
                false
            } else { //4.4以下
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e(TAG, "applyFloatPermission Exception:" + e.localizedMessage)
            false
        }
    }
}
