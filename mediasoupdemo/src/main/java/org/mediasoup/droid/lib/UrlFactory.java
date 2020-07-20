package org.mediasoup.droid.lib;

import java.util.Locale;

/**
 * 服务器连接域名配置
 */
public class UrlFactory {

  /**
   * 域名
   */
//  private static final String HOSTNAME = "v3demo.mediasoup.org";
//    private static final String HOSTNAME = "192.168.3.66";
//  private static final String HOSTNAME = "13.76.41.238";
//    private static final String HOSTNAME = "27.124.45.160";
//    private static final String HOSTNAME = "106.75.225.178";
  private static final String HOSTNAME = "192.168.0.103";
  /**
   * 端口号
   */
  private static final int PORT = 4443;
//  private static final int PORT = 3000;

  /*
   * 得到视频邀请连接
   * @param roomId
   * @param forceH264
   * @param forceVP9
   * @return
   */
  public static String getInvitationLink(String roomId, boolean forceH264, boolean forceVP9) {
    return getInvitationLink(HOSTNAME, PORT, roomId, forceH264, forceVP9);
  }

  public static String getInvitationLink(String hostname, int port, String roomId, boolean forceH264, boolean forceVP9) {
    String url = String.format(Locale.US, "https://%s/?roomId=%s", HOSTNAME, roomId);
    //https://192.168.3.21:3000/?info=true&roomId=dongxl
    url = String.format(Locale.US, "https://%s:%d/info=true?roomId=%s", hostname, port, roomId);
    if (forceH264) {
      url += "&forceH264=true";
    } else if (forceVP9) {
      url += "&forceVP9=true";
    }
      return url;
  }

  /**
   * 获取连接的信息
   * @param roomId
   * @param peerId
   * @param forceH264
   * @param forceVP9
   * @return
   */
  public static String getProtooUrl(
          String roomId, String peerId, boolean forceH264, boolean forceVP9) {
    return getProtooUrl(HOSTNAME, PORT, roomId, peerId, forceH264, forceVP9);
  }

  public static String getProtooUrl(String hostname, int port,
                                      String roomId, String peerId, boolean forceH264, boolean forceVP9) {
//    String url =
//        String.format(
//            Locale.US, "wss://%s:%d/?roomId=%s&peerId=%s", hostname, port, roomId, peerId);
//    if (forceH264) {
//      url += "&forceH264=true";
//    } else if (forceVP9) {
//      url += "&forceVP9=true";
//    }
//    return url;
    String domainPort = String.format(Locale.US, "wss://%s:%d", hostname, port);
    return getProtooUrl(domainPort, roomId, peerId, forceH264, forceVP9);
  }

  public static String getProtooUrl(String domainPort, String roomId, String peerId, boolean forceH264, boolean forceVP9) {
    String url =
        String.format(
            Locale.US, "%s/?roomId=%s&peerId=%s", domainPort, roomId, peerId);
    if (forceH264) {
      url += "&forceH264=true";
    } else if (forceVP9) {
      url += "&forceVP9=true";
    }
    return url;
  }
}
