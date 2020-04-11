package org.protoojs.droid.transports;

import org.json.JSONObject;
import org.protoojs.droid.Message;

public abstract class AbsWebSocketTransport {

  public interface Listener {

    /**
     * WebSocket连接成功
     */
    void onOpen();

    /** Connection could not be established in the first place. WebSocket 连接失败 */
    void onFail();

    /** @param message {@link Message}  WebSocket 连接 成功后消息的接收 */
    void onMessage(Message message);

    /** A previously established connection was lost unexpected. WebSocket 连接断开*/
    void onDisconnected();

    /**
     * WebSocket 关闭连接
     */
    void onClose();
  }

  // WebSocket URL.
  protected String mUrl;

  public AbsWebSocketTransport(String url) {
    this.mUrl = url;
  }

  /**
   * 连接mediasoup服务器
   * @param listener
   */
  public abstract void connect(Listener listener);

  /**
   * WebSocket连接成功后 发送消息
   * @param message
   * @return
   */
  public abstract String sendMessage(JSONObject message);

  /**
   * 关闭WebSocket连接
   */
  public abstract void close();

  /**
   * 判断 WebSocket 连接 是否关闭
   * @return
   */
  public abstract boolean isClosed();
}
