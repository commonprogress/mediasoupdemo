package org.mediasoup.droid.lib;

import android.app.Activity;
import android.content.Context;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import java.util.Random;

public class Utils {

  private static final String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

  /**
   * 获取一个随机 String
   * @param sizeOfRandomString
   * @return
   */
  public static String getRandomString(final int sizeOfRandomString) {
    final Random random = new Random();
    final StringBuilder sb = new StringBuilder(sizeOfRandomString);
    for (int i = 0; i < sizeOfRandomString; ++i)
      sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
    return sb.toString();
  }

  public static boolean isEmptyString(String str) {
    if (TextUtils.isEmpty(str)) {
      return true;
    }
    return str.equals("null");
  }

  /**
   * 获取共享屏幕需要参数
   */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public static boolean reqShareScreenIntentData(Activity activity, int requestCode) {
    if (null == activity || activity.isFinishing() || activity.isDestroyed()) {
      return false;
    }
    MediaProjectionManager mediaProjectionManager =
            (MediaProjectionManager) activity.getApplication().getSystemService(
                    Context.MEDIA_PROJECTION_SERVICE);
    activity.startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(), requestCode);
    return true;
  }

  /**
   * 获取共享屏幕需要参数
   */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public static boolean reqShareScreenIntentData(Fragment fragment, int requestCode) {
    if (null != fragment && fragment.isAdded()) {
      MediaProjectionManager mediaProjectionManager =
              (MediaProjectionManager) fragment.getActivity().getApplication().getSystemService(
                      Context.MEDIA_PROJECTION_SERVICE);
      fragment.startActivityForResult(
              mediaProjectionManager.createScreenCaptureIntent(), requestCode);
      return true;
    }
    return false;
  }
}
