package org.protoojs.droid;

import androidx.annotation.NonNull;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * WebSocket 消息体
 */
public class Message {

    private static final String TAG = "message";
    private static final String KEY_REQUEST = "request";
    private static final String KEY_RESPONSE = "response";
    private static final String KEY_NOTIFICATION = "notification";
    private static final String KEY_METHOD = "method";
    private static final String KEY_ROOMID = "roomId";
    private static final String KEY_PEERID = "peerId";
    private static final String KEY_ID = "id";
    private static final String KEY_OK = "ok";
    private static final String KEY_DATA = "data";
    private static final String KEY_ERRORCODE = "errorCode";
    private static final String KEY_ERRORREASON = "errorReason";
    private static final String KEY_TOID = "toId";


    // message data.
    private JSONObject mData;

    public Message() {
    }

    public Message(JSONObject data) {
        mData = data;
    }

    public JSONObject getData() {
        return mData;
    }

    public void setData(JSONObject data) {
        mData = data;
    }

    /**
     * 请求消息
     */
    public static class Request extends Message {

        private boolean mRequest = true;
        private String mMethod;
        private long mId;
        private String mRoomId;
        private String mPeerId;
        private String mToId;

        public Request(String method, long id, JSONObject data) {
            super(data);
            mMethod = method;
            mId = id;
        }

        public Request(String method, long id, String roomId, String peerId, String toId, JSONObject data) {
            super(data);
            mMethod = method;
            mId = id;
            this.mRoomId = roomId;
            this.mPeerId = peerId;
            this.mToId = toId;
        }

        public boolean isRequest() {
            return mRequest;
        }

        public void setRequest(boolean request) {
            mRequest = request;
        }

        public long getId() {
            return mId;
        }

        public void setId(long id) {
            mId = id;
        }

        public String getMethod() {
            return mMethod;
        }

        public void setMethod(String method) {
            mMethod = method;
        }

        public String getRoomId() {
            return mRoomId;
        }

        public void setRoomId(String roomId) {
            this.mRoomId = roomId;
        }

        public String getPeerId() {
            return mPeerId;
        }

        public void setPeerId(String peerId) {
            this.mPeerId = peerId;
        }

        public String getToId() {
            return mToId;
        }

        public void setToId(String toId) {
            this.mToId = toId;
        }
    }

    /**
     * 响应消息
     */
    public static class Response extends Message {

        private boolean mResponse = true;
        private long mId;
        private boolean mOK;
        private long mErrorCode;
        private String mErrorReason;
        private String mRoomId;
        private String mPeerId;
        private String mToId;

        public Response(long id, JSONObject data) {
            super(data);
            mId = id;
            mOK = true;
        }

        public Response(long id, String roomId, String peerId, String toId, JSONObject data) {
            super(data);
            mId = id;
            mOK = true;
            this.mRoomId = roomId;
            this.mPeerId = peerId;
            this.mToId = toId;
        }

        public Response(long id, long errorCode, String errorReason) {
            mId = id;
            mOK = false;
            mErrorCode = errorCode;
            mErrorReason = errorReason;
        }

        public Response(long id, String roomId, String peerId, String toId, long errorCode, String errorReason) {
            mId = id;
            mOK = false;
            mErrorCode = errorCode;
            mErrorReason = errorReason;
            this.mRoomId = roomId;
            this.mPeerId = peerId;
            this.mToId = toId;
        }

        public boolean isResponse() {
            return mResponse;
        }

        public void setResponse(boolean response) {
            mResponse = response;
        }

        public long getId() {
            return mId;
        }

        public void setId(long id) {
            mId = id;
        }

        public boolean isOK() {
            return mOK;
        }

        public void setOK(boolean OK) {
            mOK = OK;
        }

        public long getErrorCode() {
            return mErrorCode;
        }

        public void setErrorCode(long errorCode) {
            mErrorCode = errorCode;
        }

        public String getErrorReason() {
            return mErrorReason;
        }

        public void setErrorReason(String errorReason) {
            mErrorReason = errorReason;
        }

        public String getRoomId() {
            return mRoomId;
        }

        public void setRoomId(String roomId) {
            this.mRoomId = roomId;
        }

        public String getPeerId() {
            return mPeerId;
        }

        public void setPeerId(String peerId) {
            this.mPeerId = peerId;
        }

        public String getToId() {
            return mToId;
        }

        public void setToId(String toId) {
            this.mToId = toId;
        }
    }

    /**
     * 通知消息
     */
    public static class Notification extends Message {

        private boolean mNotification = true;
        private String mMethod;
        private String mRoomId;
        private String mPeerId;
        private String mToId;

        public Notification(String method, JSONObject data) {
            super(data);
            mMethod = method;
        }

        public Notification(String method, String roomId, String peerId, String toId, JSONObject data) {
            super(data);
            mMethod = method;
            this.mRoomId = roomId;
            this.mPeerId = peerId;
            this.mToId = toId;
        }

        public boolean isNotification() {
            return mNotification;
        }

        public void setNotification(boolean notification) {
            mNotification = notification;
        }

        public String getMethod() {
            return mMethod;
        }

        public void setMethod(String method) {
            mMethod = method;
        }

        public String getRoomId() {
            return mRoomId;
        }

        public void setRoomId(String roomId) {
            this.mRoomId = roomId;
        }

        public String getPeerId() {
            return mPeerId;
        }

        public void setPeerId(String peerId) {
            this.mPeerId = peerId;
        }

        public String getToId() {
            return mToId;
        }

        public void setToId(String toId) {
            this.mToId = toId;
        }
    }

    /**
     * WebSocket 连接 成功后消息的接收 解析
     *
     * @param raw
     * @return
     */
    public static Message parse(String raw) {
        Logger.d(TAG, "parse() ");
        JSONObject object;
        try {
            object = new JSONObject(raw);
        } catch (JSONException e) {
            Logger.e(TAG, String.format("parse() | invalid JSON: %s", e.getMessage()));
            return null;
        }
        if (null == object) {
            Logger.e(TAG, String.format("parse() | invalid JSON null == object: %s", raw));
            return null;
        }

        if (object.optBoolean(KEY_REQUEST)) {
            // Request.
            String method = object.optString(KEY_METHOD);
            long id = object.optLong(KEY_ID);

            if (TextUtils.isEmpty(method)) {
                Logger.e(TAG, "parse() | missing/invalid method field. rawData: " + raw);
                return null;
            }
            if (id == 0) {
                Logger.e(TAG, "parse() | missing/invalid id field. rawData: " + raw);
                return null;
            }

//            return new Request(method, id, object.optJSONObject(KEY_DATA));
            return new Request(method, id, object.optString(KEY_ROOMID), object.optString(KEY_PEERID), object.optString(KEY_TOID), object.optJSONObject(KEY_DATA));
        } else if (object.optBoolean(KEY_RESPONSE)) {
            // Response.
            long id = object.optLong(KEY_ID);

            if (id == 0) {
                Logger.e(TAG, "parse() | missing/invalid id field. rawData: " + raw);
                return null;
            }

            if (object.optBoolean(KEY_OK)) {
//                return new Response(id, object.optJSONObject(KEY_DATA));
                return new Response(id, object.optString(KEY_ROOMID), object.optString(KEY_PEERID), object.optString(KEY_TOID), object.optJSONObject(KEY_DATA));
            } else {
//                return new Response(id, object.optLong(KEY_ERRORCODE), object.optString(KEY_ERRORREASON));
                return new Response(id, object.optString(KEY_ROOMID), object.optString(KEY_PEERID), object.optString(KEY_TOID), object.optLong(KEY_ERRORCODE), object.optString(KEY_ERRORREASON));
            }
        } else if (object.optBoolean(KEY_NOTIFICATION)) {
            // Notification.
            String method = object.optString(KEY_METHOD);

            if (TextUtils.isEmpty(method)) {
                Logger.e(TAG, "parse() | missing/invalid method field. rawData: " + raw);
                return null;
            }
//            return new Notification(method, object.optJSONObject(KEY_DATA));

            return new Notification(method, object.optString(KEY_ROOMID), object.optString(KEY_PEERID), object.optString(KEY_TOID), object.optJSONObject(KEY_DATA));
        } else {
            // Invalid.
            Logger.e(TAG, "parse() | missing request/response field. rawData: " + raw);
            return null;
        }
    }

    /**
     * 创建请求消息
     *
     * @param method
     * @param data
     * @return
     */
    public static JSONObject createRequest(String method, JSONObject data) {
        JSONObject request = new JSONObject();
        try {
            request.put(KEY_REQUEST, true);
            request.put(KEY_METHOD, method);
            request.put(KEY_ID, Utils.generateRandomNumber());
            request.put(KEY_DATA, data != null ? data : new JSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return request;
    }

    /**
     * 创建请求消息
     *
     * @param method
     * @param data
     * @return
     */
    public static JSONObject createRequest(String method, String roomId, String peerId, String toId, JSONObject data) {
        JSONObject request = new JSONObject();
        try {
            request.put(KEY_REQUEST, true);
            request.put(KEY_METHOD, method);
            request.put(KEY_ID, Utils.generateRandomNumber());
            if (!TextUtils.isEmpty(roomId)) {
                request.put(KEY_ROOMID, roomId);
            }
            if (!TextUtils.isEmpty(peerId)) {
                request.put(KEY_PEERID, peerId);
            }
            if (!TextUtils.isEmpty(toId)) {
                request.put(KEY_TOID, toId);
            }
            request.put(KEY_DATA, data != null ? data : new JSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return request;
    }

    /**
     * 创建响应消息 成功的
     *
     * @param request
     * @param data
     * @return
     */
    @NonNull
    public static JSONObject createSuccessResponse(@NonNull Request request, JSONObject data) {
        JSONObject response = new JSONObject();
        try {
            response.put(KEY_RESPONSE, true);
            response.put(KEY_METHOD, null == request ? "" : request.getMethod());
            String roomId = null == request ? "" : request.getRoomId();
            String peerId = null == request ? "" : request.getPeerId();
            String toId = null == request ? "" : request.getToId();
            if (!TextUtils.isEmpty(roomId)) {
                response.put(KEY_ROOMID, roomId);
            }
            if (!TextUtils.isEmpty(peerId)) {
                response.put(KEY_PEERID, peerId);
            }
            if (!TextUtils.isEmpty(toId)) {
                response.put(KEY_TOID, toId);
            }
            response.put(KEY_ID, null == request ? 0L : request.getId());
            response.put(KEY_OK, true);
            response.put(KEY_DATA, data != null ? data : new JSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 创建响应消息 错误的
     *
     * @param request
     * @param errorCode
     * @param errorReason
     * @return
     */
    @NonNull
    public static JSONObject createErrorResponse(
            @NonNull Request request, long errorCode, String errorReason) {
        JSONObject response = new JSONObject();
        try {
            response.put(KEY_RESPONSE, true);
            response.put(KEY_METHOD, null == request ? "" : request.getMethod());
            String roomId = null == request ? "" : request.getRoomId();
            String peerId = null == request ? "" : request.getPeerId();
            String toId = null == request ? "" : request.getToId();
            if (!TextUtils.isEmpty(roomId)) {
                response.put(KEY_ROOMID, roomId);
            }
            if (!TextUtils.isEmpty(peerId)) {
                response.put(KEY_PEERID, peerId);
            }
            if (!TextUtils.isEmpty(toId)) {
                response.put(KEY_TOID, toId);
            }
            response.put(KEY_ID, null == request ? 0L : request.getId());
            response.put(KEY_OK, false);
            response.put(KEY_ERRORCODE, errorCode);
            response.put(KEY_ERRORREASON, errorReason);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 创建通知消息
     *
     * @param method
     * @param data
     * @return
     */
    @NonNull
    public static JSONObject createNotification(String method, JSONObject data) {
        JSONObject notification = new JSONObject();
        try {
            notification.put(KEY_NOTIFICATION, true);
            notification.put(KEY_METHOD, method);
            notification.put(KEY_DATA, data != null ? data : new JSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return notification;
    }

    /**
     * 创建通知消息
     *
     * @param method
     * @param data
     * @return
     */
    @NonNull
    public static JSONObject createNotification(String method, String roomId, String peerId, String toId, JSONObject data) {
        JSONObject notification = new JSONObject();
        try {
            notification.put(KEY_NOTIFICATION, true);
            notification.put(KEY_METHOD, method);
            if (!TextUtils.isEmpty(roomId)) {
                notification.put(KEY_ROOMID, roomId);
            }
            if (!TextUtils.isEmpty(peerId)) {
                notification.put(KEY_PEERID, peerId);
            }
            if (!TextUtils.isEmpty(toId)) {
                notification.put(KEY_TOID, toId);
            }
            notification.put(KEY_DATA, data != null ? data : new JSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return notification;
    }
}
