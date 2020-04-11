package org.protoojs.droid;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;


import org.json.JSONException;
import org.json.JSONObject;
import org.protoojs.droid.transports.AbsWebSocketTransport;

import java.util.HashMap;
import java.util.Map;

/**
 * 连接WebSocket 和请求返回相关
 */
public class Peer implements AbsWebSocketTransport.Listener {

  private static final String TAG = "Peer";

  /**
   * 连接WebSocket 和请求返回 监听
   */
  public interface Listener {

    /**
     * WebSocket连接成功
     */
    void onOpen();

    /**
     * WebSocket 连接失败
     */
    void onFail();

    /**
     * 消息的接收 请求消息
     * @param request
     * @param handler
     */
    void onRequest(@NonNull Message.Request request, @NonNull ServerRequestHandler handler);

    /**
     * 消息的接收 通知消息
     * @param notification
     */
    void onNotification(@NonNull Message.Notification notification);

    /**
     * WebSocket 连接中断
     */
    void onDisconnected();

    /**
     * WebSocket 连接关闭
     */
    void onClose();
  }

  /**
   * WebSocket 服务端请求处理监听
   */
  public interface ServerRequestHandler {

    default void accept() {
      accept(null);
    }
//接受
    void accept(String data);
    //拒绝
    void reject(long code, String errorReason);
  }

  /**
   * WebSocket 客户端请求处理监听
   */
  public interface ClientRequestHandler {
    //解析
    void resolve(String data);
    //拒绝
    void reject(long error, String errorReason);
  }

  /**
   * 客户端请求处理 代理
   */
  class ClientRequestHandlerProxy implements ClientRequestHandler, Runnable {

    long mRequestId;
    String mMethod;
    ClientRequestHandler mClientRequestHandler;

    ClientRequestHandlerProxy(
        long requestId,
        String method,
        long timeoutDelayMillis,
        ClientRequestHandler clientRequestHandler) {
      mRequestId = requestId;
      mMethod = method;
      mClientRequestHandler = clientRequestHandler;
      mTimerCheckHandler.postDelayed(this, timeoutDelayMillis);
    }

    @Override
    public void run() {
      mSends.remove(mRequestId);
      // TODO (HaiyangWu): error code redefine. use http timeout
      if (mClientRequestHandler != null) {
        mClientRequestHandler.reject(408, "request timeout");
      }
    }
    //解析
    @Override
    public void resolve(String data) {
      Logger.d(TAG, "request() " + mMethod + " success, " + data);
      if (mClientRequestHandler != null) {
        mClientRequestHandler.resolve(data);
      }
    }
    //拒绝
    @Override
    public void reject(long error, String errorReason) {
      Logger.w(TAG, "request() " + mMethod + " fail, " + error + ", " + errorReason);
      if (mClientRequestHandler != null) {
        mClientRequestHandler.reject(error, errorReason);
      }
    }
//关闭
    void close() {
      // stop timeout check. 停止超时时间
      mTimerCheckHandler.removeCallbacks(this);
    }
  }

  // Closed flag.
  private boolean mClosed = false;
  // Transport.
  @NonNull
  private final AbsWebSocketTransport mTransport;
  // Listener.
  @NonNull private final Listener mListener;
  // Handler for timeout check.
  @NonNull private final Handler mTimerCheckHandler;
  // Connected flag.
  private boolean mConnected;
  // Custom data object.
  private JSONObject mData;
  // Map of pending sent request objects indexed by request id. 发送 消息的集合
  @SuppressLint("UseSparseArrays")
  private Map<Long, ClientRequestHandlerProxy> mSends = new HashMap<>();

  public Peer(@NonNull AbsWebSocketTransport transport, @NonNull Listener listener) {
    mTransport = transport;
    mListener = listener;
    mTimerCheckHandler = new Handler(Looper.getMainLooper());
    handleTransport();
  }

  /**
   * 是否关闭
   * @return
   */
  public boolean isClosed() {
    return mClosed;
  }

  /**
   * 是否连接
   * @return
   */
  public boolean isConnected() {
    return mConnected;
  }

  public JSONObject getData() {
    return mData;
  }

  /**
   * 关闭连接
   */
  public void close() {
    if (mClosed) {
      return;
    }

    Logger.d(TAG, "close()");
    mClosed = true;
    mConnected = false;

    // Close Transport.
    mTransport.close();

    // Close every pending sent.
    for (ClientRequestHandlerProxy proxy : mSends.values()) {
      proxy.close();
    }
    mSends.clear();

    // Emit 'close' event.
    mListener.onClose();
  }

  public void request(String method, String data, ClientRequestHandler clientRequestHandler) {
    try {
      request(method, new JSONObject(data), clientRequestHandler);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  /**
   * 发送一个请求
   * @param method
   * @param data
   * @param clientRequestHandler
   */
  public void request(
      String method, @NonNull JSONObject data, ClientRequestHandler clientRequestHandler) {
    JSONObject request = Message.createRequest(method, data);
    long requestId = request.optLong("id");
    Logger.d(TAG, String.format("request() [method:%s, data:%s]", method, data.toString()));
    String payload = mTransport.sendMessage(request);

    long timeout = (long) (1500 * (15 + (0.1 * payload.length())));
    //把发送消息添加的集合
    mSends.put(
        requestId, new ClientRequestHandlerProxy(requestId, method, timeout, clientRequestHandler));
  }

  public void notify(String method, String data) {
    try {
      notify(method, new JSONObject(data));
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public void notify(String method, JSONObject data) {
    JSONObject notification = Message.createNotification(method, data);
    Logger.d(TAG, String.format("notify() [method:%s]", method));
    mTransport.sendMessage(notification);
  }

  /**
   * 连接WebSocket
   */
  private void handleTransport() {
    if (mTransport.isClosed()) {
      if (mClosed) {
        return;
      }

      mConnected = false;
      mListener.onClose();
      return;
    }

    mTransport.connect(this);
  }

  /**
   * WebSocket 连接 成功后消息的接收 请求消息
   * @param request
   */
  private void handleRequest(final Message.Request request) {
    mListener.onRequest(
        request,
        new ServerRequestHandler() {
          @Override
          public void accept(String data) {
            try {
              JSONObject response;
              if (TextUtils.isEmpty(data)) {
                response = Message.createSuccessResponse(request, new JSONObject());
              } else {
                response = Message.createSuccessResponse(request, new JSONObject(data));
              }
              mTransport.sendMessage(response);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          @Override
          public void reject(long code, String errorReason) {
            JSONObject response = Message.createErrorResponse(request, code, errorReason);
            try {
              mTransport.sendMessage(response);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
  }

  /**
   * WebSocket 连接 成功后消息的接收 响应消息
   * @param response
   */
  private void handleResponse(Message.Response response) {
    ClientRequestHandlerProxy sent = mSends.remove(response.getId());
    if (sent == null) {
      Logger.e(
          TAG, "received response does not match any sent request [id:" + response.getId() + "]");
      return;
    }

    sent.close();
    //响应消息 解析
    if (response.isOK()) {
      sent.resolve(response.getData().toString());
    } else {
      sent.reject(response.getErrorCode(), response.getErrorReason());
    }
  }

  /**
   * WebSocket 连接 成功后消息的接收 通知消息
   * @param notification
   */
  private void handleNotification(Message.Notification notification) {
    mListener.onNotification(notification);
  }

  // implement MyWebSocketTransport$Listener WebSocket连接成功
  @Override
  public void onOpen() {
    if (mClosed) {
      return;
    }
    Logger.d(TAG, "onOpen()");
    mConnected = true;
    mListener.onOpen();
  }

  //WebSocket 连接失败
  @Override
  public void onFail() {
    if (mClosed) {
      return;
    }
    Logger.e(TAG, "onFail()");
    mConnected = false;
    mListener.onFail();
  }

  //WebSocket 连接 成功后消息的接收
  @Override
  public void onMessage(Message message) {
    if (mClosed) {
      return;
    }
    Logger.d(TAG, "onMessage()");
    if (message instanceof Message.Request) {
      handleRequest((Message.Request) message);
    } else if (message instanceof Message.Response) {
      handleResponse((Message.Response) message);
    } else if (message instanceof Message.Notification) {
      handleNotification((Message.Notification) message);
    }
  }

  //WebSocket 断开连接
  @Override
  public void onDisconnected() {
    if (mClosed) {
      return;
    }
    Logger.w(TAG, "onDisconnected()");
    mConnected = false;
    mListener.onDisconnected();
  }

  //WebSocket 连接关闭
  @Override
  public void onClose() {
    if (mClosed) {
      return;
    }
    Logger.w(TAG, "onClose()");
    mClosed = true;
    mConnected = false;
    mListener.onClose();
  }
}
