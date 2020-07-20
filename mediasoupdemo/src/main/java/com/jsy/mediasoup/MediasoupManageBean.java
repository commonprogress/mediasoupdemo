package com.jsy.mediasoup;

import org.threeten.bp.Instant;

public class MediasoupManageBean {
    private boolean isEmpty;
    private boolean isCreateMediasoup;
    private String isRegister;
    private String curActiveId;
    private String curRConvId;
    private String curUserId;
    private String curClientId;
    private String curDisplayName;
    private MediasoupManagement.MediasoupHandler mediasoupHandler;
    private MediasoupManagement.UserChangedHandler userChangedHandler;

    private MediasoupConstant.CallType mediasoupCallType;
    private MediasoupConstant.ConvType mediasoupConvType;
    private boolean isShouldRing = false;
    private MediasoupConstant.VideoState mediasoupVideoState;

    private MediasoupConstant.MeidasoupState meidasoupStart;
    private MediasoupConstant.MeidasoupState meidasoupTimely;
    private String incomingUserId;
    private String incomingClientId;
    private String incomingDisplayName;
    private long incomingMsgTime;

    private Instant startTime = Instant.now();//the time we start/receive a call - always the time at which the call info object was created
    private Instant joinedTime; //the time the call was joined, if any
    private Instant estabTime; //the time that a joined call was established, if any
    private Instant endTime;
    private MediasoupConstant.ClosedReason endReason;

    public MediasoupManageBean() {
    }

    public MediasoupManageBean(boolean isEmpty) {
        this.isEmpty = isEmpty;
    }

    public MediasoupManageBean(String isRegister, String userId, String clientId, String activeId, String displayName, MediasoupManagement.MediasoupHandler mediasoupH) {
        this.isEmpty = false;
        this.isRegister = isRegister;
        this.curActiveId = activeId;
        this.curUserId = userId;
        this.curClientId = clientId;
        this.curDisplayName = displayName;
        this.mediasoupHandler = mediasoupH;
        this.isCreateMediasoup = true;
    }

    public void setStartMediasoup(String rConvId, MediasoupConstant.MeidasoupState meidasoupStart, MediasoupConstant.MeidasoupState meidasoupTimely, MediasoupConstant.CallType mediasoupCallType, MediasoupConstant.ConvType mediasoupConvType, boolean isShouldRing) {
        this.curRConvId = rConvId;
        this.meidasoupStart = meidasoupStart;
        this.meidasoupTimely = meidasoupTimely;
        this.mediasoupCallType = mediasoupCallType;
        this.mediasoupConvType = mediasoupConvType;
        this.isShouldRing = isShouldRing;
    }

    public void setStartMediasoupUser(String rConvId, String userId, String clientId, String incomingDisplayName, long msgTime) {
//        this.curRConvId = rConvId;
        this.incomingUserId = userId;
        this.incomingClientId = clientId;
        this.incomingDisplayName = incomingDisplayName;
        this.incomingMsgTime = msgTime;
    }

    public void setIncomingMediasoup(String rConvId, String userId, String clientId, String incomingDisplayName, long msgTime, MediasoupConstant.CallType mediasoupCallType, MediasoupConstant.ConvType mediasoupConvType, boolean isShouldRing, MediasoupConstant.MeidasoupState meidasoupStart, MediasoupConstant.MeidasoupState meidasoupTimely) {
        this.curRConvId = rConvId;
        this.incomingUserId = userId;
        this.incomingClientId = clientId;
        this.incomingDisplayName = incomingDisplayName;
        this.incomingMsgTime = msgTime;
        this.mediasoupCallType = mediasoupCallType;//0音频，1视频，2强制音频
        this.mediasoupConvType = mediasoupConvType;//0一对一模式，1群聊模式，2会议模式
        this.isShouldRing = isShouldRing;
        this.meidasoupStart = meidasoupStart;
        this.meidasoupTimely = meidasoupStart;
    }

    public void setAnswerMediasoup(String rConvId, MediasoupConstant.CallType mediasoupCallType, MediasoupConstant.ConvType mediasoupConvType, MediasoupConstant.MeidasoupState meidasoupStart, MediasoupConstant.MeidasoupState meidasoupTimely) {
        this.curRConvId = rConvId;
//        this.curAudioCbr = audioCbr;
        this.mediasoupCallType = mediasoupCallType;
        this.mediasoupConvType = mediasoupConvType;
        this.meidasoupStart = meidasoupStart;
        this.meidasoupTimely = meidasoupTimely;
    }

    public void setIncomingUser(String userId, String clientId, String displayName) {
        this.incomingUserId = userId;
        this.incomingClientId = clientId;
        this.incomingDisplayName = displayName;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setEmpty(boolean empty) {
        isEmpty = empty;
    }

    public boolean isCreateMediasoup() {
        return isCreateMediasoup;
    }

    public void setCreateMediasoup(boolean createMediasoup) {
        isCreateMediasoup = createMediasoup;
    }

    public String getIsRegister() {
        return isRegister;
    }

    public void setIsRegister(String isRegister) {
        this.isRegister = isRegister;
    }

    public String getCurActiveId() {
        return curActiveId;
    }

    public void setCurActiveId(String curActiveId) {
        this.curActiveId = curActiveId;
    }

    public String getCurRConvId() {
        return curRConvId;
    }

    public void setCurRConvId(String curRConvId) {
        this.curRConvId = curRConvId;
    }

    public String getCurUserId() {
        return curUserId;
    }

    public void setCurUserId(String curUserId) {
        this.curUserId = curUserId;
    }

    public String getCurClientId() {
        return curClientId;
    }

    public void setCurClientId(String curClientId) {
        this.curClientId = curClientId;
    }

    public String getCurDisplayName() {
        return curDisplayName;
    }

    public void setCurDisplayName(String curDisplayName) {
        this.curDisplayName = curDisplayName;
    }

    public MediasoupManagement.MediasoupHandler getMediasoupHandler() {
        return mediasoupHandler;
    }

    public void setMediasoupHandler(MediasoupManagement.MediasoupHandler mediasoupHandler) {
        this.mediasoupHandler = mediasoupHandler;
    }

    public MediasoupManagement.UserChangedHandler getUserChangedHandler() {
        return userChangedHandler;
    }

    public void setUserChangedHandler(MediasoupManagement.UserChangedHandler userChangedHandler) {
        this.userChangedHandler = userChangedHandler;
    }

    public MediasoupConstant.CallType getMediasoupCallType() {
        return mediasoupCallType;
    }

    public void setMediasoupCallType(MediasoupConstant.CallType mediasoupCallType) {
        this.mediasoupCallType = mediasoupCallType;
    }

    public MediasoupConstant.ConvType getMediasoupConvType() {
        return mediasoupConvType;
    }

    public void setMediasoupConvType(MediasoupConstant.ConvType mediasoupConvType) {
        this.mediasoupConvType = mediasoupConvType;
    }

    public boolean isShouldRing() {
        return isShouldRing;
    }

    public void setShouldRing(boolean shouldRing) {
        isShouldRing = shouldRing;
    }

    public MediasoupConstant.VideoState getMediasoupVideoState() {
        return mediasoupVideoState;
    }

    public void setMediasoupVideoState(MediasoupConstant.VideoState videoState) {
        if (null == videoState) {
            mediasoupVideoState = null == mediasoupCallType ? MediasoupConstant.VideoState.Unknown : (mediasoupCallType == MediasoupConstant.CallType.Video ? MediasoupConstant.VideoState.Started : MediasoupConstant.VideoState.Stopped);
        } else {
            mediasoupVideoState = videoState;
        }
    }

    public MediasoupConstant.MeidasoupState getMeidasoupStart() {
        return meidasoupStart;
    }

    public void setMeidasoupStart(MediasoupConstant.MeidasoupState meidasoupStart) {
        this.meidasoupStart = meidasoupStart;
    }

    public MediasoupConstant.MeidasoupState getMeidasoupTimely() {
        return meidasoupTimely;
    }

    public void setMeidasoupTimely(MediasoupConstant.MeidasoupState meidasoupTimely) {
        this.meidasoupTimely = meidasoupTimely;
    }

    public String getIncomingUserId() {
        return incomingUserId;
    }

    public void setIncomingUserId(String incomingUserId) {
        this.incomingUserId = incomingUserId;
    }

    public String getIncomingClientId() {
        return incomingClientId;
    }

    public void setIncomingClientId(String incomingClientId) {
        this.incomingClientId = incomingClientId;
    }

    public String getIncomingDisplayName() {
        return incomingDisplayName;
    }

    public void setIncomingDisplayName(String incomingDisplayName) {
        this.incomingDisplayName = incomingDisplayName;
    }

    public long getIncomingMsgTime() {
        return incomingMsgTime;
    }

    public void setIncomingMsgTime(long incomingMsgTime) {
        this.incomingMsgTime = incomingMsgTime;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getJoinedTime() {
        return joinedTime;
    }

    public void setJoinedTime(Instant joinedTime) {
        this.joinedTime = joinedTime;
    }

    public Instant getEstabTime() {
        return estabTime;
    }

    public void setEstabTime(Instant estabTime) {
        this.estabTime = estabTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public MediasoupConstant.ClosedReason getEndReason() {
        return endReason;
    }

    public void setEndReason(MediasoupConstant.ClosedReason endReason) {
        this.endReason = endReason;
    }

    @Override
    public String toString() {
        return "MediasoupManageBean{" +
            "isEmpty=" + isEmpty +
            ", isCreateMediasoup=" + isCreateMediasoup +
            ", isRegister='" + isRegister + '\'' +
            ", curActiveId='" + curActiveId + '\'' +
            ", curRConvId='" + curRConvId + '\'' +
            ", curUserId='" + curUserId + '\'' +
            ", curClientId='" + curClientId + '\'' +
            ", curDisplayName='" + curDisplayName + '\'' +
            ", mediasoupHandler is null=" + (null == mediasoupHandler) +
            ", userChangedHandler is null=" + (null == userChangedHandler) +
            ", mediasoupCallType=" + mediasoupCallType +
            ", mediasoupConvType=" + mediasoupConvType +
            ", isShouldRing=" + isShouldRing +
            ", mediasoupVideoState=" + mediasoupVideoState +
            ", meidasoupStart=" + meidasoupStart +
            ", meidasoupTimely=" + meidasoupTimely +
            ", incomingUserId='" + incomingUserId + '\'' +
            ", incomingClientId='" + incomingClientId + '\'' +
            ", incomingDisplayName='" + incomingDisplayName + '\'' +
            ", incomingMsgTime=" + incomingMsgTime + '\'' +
            ", startTime='" + startTime + '\'' +
            ", joinedTime='" + joinedTime + '\'' +
            ", estabTime='" + estabTime + '\'' +
            ", endTime=" + endTime + '\'' +
            ", endReason=" + endReason +
            '}';
    }

    /**
     * 是否邀请加入
     *
     * @return
     */
    public boolean isReceiveCall() {
        return null == meidasoupStart ? false : (meidasoupStart == MediasoupConstant.MeidasoupState.OtherCalling);
    }

    public void mediasoupClose() {
        curRConvId = "";
        mediasoupVideoState = null;
        meidasoupStart = null;
        meidasoupTimely = null;
        incomingUserId = "";
        incomingClientId = "";
        incomingDisplayName = "";
        incomingMsgTime = 0L;
        mediasoupConvType = null;
        mediasoupCallType = null;
        isShouldRing = false;
    }

    public void setInstanceNull() {
        isCreateMediasoup = false;
        curActiveId = "";
        curUserId = "";
        curClientId = "";
        curDisplayName = "";
        mediasoupHandler = null;
        userChangedHandler = null;
        isEmpty = true;
    }

    public boolean isSelfCalling() {
        return null == meidasoupStart ? false : (meidasoupStart == MediasoupConstant.MeidasoupState.SelfCalling);
    }

    public boolean isVideoIncoming() {
        return null == mediasoupCallType ? false : mediasoupCallType == MediasoupConstant.CallType.Video;
    }

    public boolean isMediasoupVideoState() {
        return null == mediasoupVideoState ? isVideoIncoming() : mediasoupVideoState == MediasoupConstant.VideoState.Started || mediasoupVideoState == MediasoupConstant.VideoState.Paused;
    }

    public boolean isOneOnOneCall() {
        return null == mediasoupConvType ? true : mediasoupConvType == MediasoupConstant.ConvType.OneOnOne;
    }

    /**
     * 响应了邀请
     *
     * @return
     */
    public boolean answeredJoinMediasoup(boolean isConnected) {
        if (isConnected && (meidasoupTimely == MediasoupConstant.MeidasoupState.SelfJoining || meidasoupTimely == MediasoupConstant.MeidasoupState.SelfConnected)) {
            return false;
        }
        meidasoupTimely = MediasoupConstant.MeidasoupState.SelfJoining;
        return true;
    }

    /**
     * 建立通话
     *
     * @return
     */
    public boolean establishedJoinMediasoup() {
        if (meidasoupTimely == MediasoupConstant.MeidasoupState.SelfConnected) {
            return false;
        }
        meidasoupTimely = MediasoupConstant.MeidasoupState.SelfConnected;
        return true;
    }

    /**
     * 关闭当前通话
     *
     * @return
     */
    public boolean endedMediasoup() {
        if (meidasoupTimely == MediasoupConstant.MeidasoupState.Ended) {
            return false;
        }
        meidasoupTimely = MediasoupConstant.MeidasoupState.Ended;
        return true;
    }
}
