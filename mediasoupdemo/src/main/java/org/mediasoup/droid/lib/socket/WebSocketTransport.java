package org.mediasoup.droid.lib.socket;

import android.os.Handler;
import android.os.HandlerThread;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.protoojs.droid.Message;
import org.protoojs.droid.transports.AbsWebSocketTransport;

import java.security.cert.CertificateException;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;

import static org.apache.http.conn.ssl.SSLSocketFactory.SSL;

/**
 * WebSocket 连接 断开 回应
 */
public class WebSocketTransport extends AbsWebSocketTransport {

  // Log tag.
  private static final String TAG = "WebSocketTransport";
  private static final String HEADER_PROTOCOL_VALUE = "protoo";
  // Closed flag.
  private boolean mClosed;
  // Connected flag.
  private boolean mConnected;
  // OKHttpClient.
  private final OkHttpClient mOkHttpClient;
  // Handler associate to current thread.
  private final Handler mHandler;
  // Retry operation.
  private final RetryStrategy mRetryStrategy;
  // WebSocket instance.
  private WebSocket mWebSocket;
  // Listener.
  private Listener mListener;

  /**
   * 重连策略
   */
  private static class RetryStrategy {

    private final int retries;
    private final int factor;
    private final int minTimeout;
    private final int maxTimeout;
//已经重试次数
    private int retryCount = 1;

    /**
     *
     * @param retries 重连次数
     * @param factor 获取重连间隔时间幂的底数
     * @param minTimeout 最小间隔时间
     * @param maxTimeout 最大间隔时间
     */
    RetryStrategy(int retries, int factor, int minTimeout, int maxTimeout) {
      this.retries = retries;
      this.factor = factor;
      this.minTimeout = minTimeout;
      this.maxTimeout = maxTimeout;
    }

    /**
     * 失败后重试次数+1
     */
    void retried() {
      retryCount++;
    }

    /**
     * 获取重连间隔时间
     * @return
     */
    int getReconnectInterval() {
      if (retryCount > retries) {
        return -1;
      }
      int reconnectInterval = (int) (minTimeout * Math.pow(factor, retryCount));
      reconnectInterval = Math.min(reconnectInterval, maxTimeout);
      return reconnectInterval;
    }

    /**
     * 重置重试次数
     */
    void reset() {
      if (retryCount != 0) {
        retryCount = 0;
      }
    }
  }

  /**
   * 初始化连接mediasoup服务器 一些参数
   * @param url @see UrlFactory.getProtooUrl
   */
  public WebSocketTransport(String url) {
    super(url);
    mOkHttpClient = getUnsafeOkHttpClient();
    HandlerThread handlerThread = new HandlerThread("socket");
    handlerThread.start();
    mHandler = new Handler(handlerThread.getLooper());
    mRetryStrategy = new RetryStrategy(10, 2, 1000, 8 * 1000);
  }

  /**
   * 连接mediasoup服务器
   * @param listener 连接的回调
   */
  @Override
  public void connect(Listener listener) {
    Logger.d(TAG, "connect()");
    mListener = listener;
    mHandler.post(this::newWebSocket);
  }

  /**
   * 创建的新的WebSocket 连接
   * addHeader("Sec-WebSocket-Protocol", "protoo")
   */
  private void newWebSocket() {
    mWebSocket = null;
    Logger.d(TAG, "newWebSocket()mUrl: "+mUrl);
    mOkHttpClient.newWebSocket(
        new Request.Builder().url(mUrl).addHeader("Sec-WebSocket-Protocol", HEADER_PROTOCOL_VALUE).build(),
        new ProtooWebSocketListener());
  }

  /**
   * 安排重新连接WebSocket
   * @return
   */
  private boolean scheduleReconnect() {
    int reconnectInterval = mRetryStrategy.getReconnectInterval();
    if (reconnectInterval == -1) {
      return false;
    }
    Logger.d(TAG, "scheduleReconnect() ");
    mHandler.postDelayed(
        () -> {
          if (mClosed) {
            return;
          }
          Logger.w(TAG, "doing reconnect job, retryCount: " + mRetryStrategy.retryCount);
          mOkHttpClient.dispatcher().cancelAll();
          newWebSocket();
          mRetryStrategy.retried();
        },
        reconnectInterval);
    return true;
  }

  /**
   * WebSocket连接成功后 发送消息
   * @param message
   * @return
   */
  @Override
  public String sendMessage(JSONObject message) {
    if (mClosed) {
      throw new IllegalStateException("transport closed");
    }
    String payload = message.toString();
    mHandler.post(
        () -> {
          if (mClosed) {
            return;
          }
          if (mWebSocket != null) {
            mWebSocket.send(payload);
          }
        });
    return payload;
  }

  /**
   * 关闭WebSocket连接
   */
  @Override
  public void close() {
    if (mClosed) {
      return;
    }
    mClosed = true;
    Logger.d(TAG, "close()");
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    mHandler.post(
        () -> {
          if (mWebSocket != null) {
            mWebSocket.close(1000, "bye");
            mWebSocket = null;
          }
          countDownLatch.countDown();
        });
    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   *  判断 WebSocket 连接 是否关闭
   * @return
   */
  @Override
  public boolean isClosed() {
    return mClosed;
  }

  /**
   * WebSocket 连接回调
   */
  private class ProtooWebSocketListener extends WebSocketListener {

    /**
     * WebSocket连接成功
     * @param webSocket
     * @param response
     */
    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
      if (mClosed) {
        return;
      }
      Logger.d(TAG, "onOpen() ");
      mWebSocket = webSocket;
      mConnected = true;
      if (mListener != null) {
        mListener.onOpen();
      }
      mRetryStrategy.reset();
    }

    /**
     * WebSocket 关闭连接
     * @param webSocket
     * @param code
     * @param reason
     */
    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
      Logger.w(TAG, "onClosed() code:" + code + ",reason:" + reason);
      if (mClosed) {
        return;
      }
      mClosed = true;
      mConnected = false;
      mRetryStrategy.reset();
      if (mListener != null) {
        mListener.onClose();
      }
    }

    /**
     * WebSocket 连接 关闭中。。。。。
     * @param webSocket
     * @param code
     * @param reason
     */
    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
      Logger.w(TAG, "onClosing() code:" + code + ",reason:" + reason);
    }

    /**
     * WebSocket 连接失败
     * @param webSocket
     * @param t
     * @param response
     */
    @Override
    public void onFailure(
            @NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
      Logger.w(TAG, "onFailure() Throwable:" + t.getLocalizedMessage());
      if (mClosed) {
        return;
      }
      //安排重新连接WebSocket
      if (scheduleReconnect()) {
        if (mListener != null) {
          if (mConnected) {
            //连接成功之后在断开 连接中断
            mListener.onDisconnected();
          } else {
            //连接中直接断开 连接失败
            mListener.onFail();
          }
        }
      } else {
        //关闭连接
        Logger.e(TAG, "give up reconnect. notify closed");
        mClosed = true;
        if (mListener != null) {
          mListener.onClose();
        }
        mRetryStrategy.reset();
      }
    }

    /**
     * WebSocket 连接 成功后消息的接收 -文本
     * @param webSocket
     * @param text
     */
    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
      Logger.d(TAG, "onMessage() text:" + text);
      if (mClosed) {
        return;
      }
      Message message = Message.parse(text);
      if (message == null) {
        return;
      }
      if (mListener != null) {
        mListener.onMessage(message);
      }
    }

    /**
     * WebSocket 连接 成功后消息的接收 -byte
     * @param webSocket
     * @param bytes
     */
    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
      Logger.d(TAG, "onMessage()");
    }
  }

  /**
   * okhttp创建 认证  忽略认证
   * @return
   */
  private OkHttpClient getUnsafeOkHttpClient() {
    try {
      final TrustManager[] trustAllCerts =
          new TrustManager[] {
            new X509TrustManager() {

              @Override
              public void checkClientTrusted(
                  java.security.cert.X509Certificate[] chain, String authType)
                  throws CertificateException {}

              @Override
              public void checkServerTrusted(
                  java.security.cert.X509Certificate[] chain, String authType)
                  throws CertificateException {}

              // Called reflectively by X509TrustManagerExtensions.
              public void checkServerTrusted(
                      java.security.cert.X509Certificate[] chain, String authType, String host) {}

              @Override
              public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
              }
            }
          };

      final SSLContext sslContext = SSLContext.getInstance(SSL);
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      HttpLoggingInterceptor httpLoggingInterceptor =
          new HttpLoggingInterceptor(s -> Logger.d(TAG, s));
      httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

      OkHttpClient.Builder builder =
          new OkHttpClient.Builder()
              .addInterceptor(httpLoggingInterceptor)
              .retryOnConnectionFailure(true);
      builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);

      builder.hostnameVerifier((hostname, session) -> true);

      return builder.build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
