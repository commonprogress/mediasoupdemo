package org.mediasoup.droid.lib;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Socket Events Enum
 */
public class ActionEvent {

    public static final String GET_ROUTER_RTP_CAPABILITIES = "getRouterRtpCapabilities";
    public static final String CREATE_WEBRTC_TRANSPORT = "createWebRtcTransport";
    public static final String CONNECT_WEBRTC_TRANSPORT = "connectWebRtcTransport";
    // join Room
    public static final String JOIN_ROOM = "join";
    public static final String PRODUCE = "produce";
    public static final String RESUME_PRODUCER = "resumeProducer";
    public static final String PAUSE_PRODUCER = "pauseProducer";
    public static final String CLOSE_PRODUCER = "closeProducer";

    public static final String RESUME_CONSUMER = "resumeConsumer";
    public static final String PAUSE_CONSUMER = "pauseConsumer";
    public static final String CLOSE_CONSUMER = "closeConsumer";
    public static final String RESTART_ICE = "restartIce";
    public static final String REQUEST_CONSUMERKEYFRAME = "requestConsumerKeyFrame";
    public static final String CHANGE_DISPLAYNAME = "changeDisplayName";
    //leave Room
    public static final String LEAVE_ROOM = "leave";

    // newconsumer notification
    public static final String NEW_CONSUMER = "newConsumer";
    public static final String NEW_DATA_CONSUMER = "newDataConsumer";
    public static final String PRODUCER_SCORE = "producerScore";
    public static final String NEW_PEER = "newPeer";
    //peer leave
    public static final String PEER_LEAVE = "peerLeave";
    public static final String PEER_CLOSED = "peerClosed";
    public static final String PEER_DISPLAYNAME_CHANGED = "peerDisplayNameChanged";
    public static final String CONSUMER_CLOSED = "consumerClosed";
    public static final String CONSUMER_PAUSED = "consumerPaused";
    public static final String CONSUMER_RESUMED = "consumerResumed";
    public static final String CONSUMER_LAYERS_CHANGED = "consumerLayersChanged";
    public static final String CONSUMER_SCORE = "consumerScore";
    public static final String DATA_CONSUMER_CLOSED = "dataConsumerClosed";
    public static final String ACTIVE_SPEAKER = "activeSpeaker";
    public static final String DOWNLINK_BWE = "downlinkBwe";



    @StringDef({GET_ROUTER_RTP_CAPABILITIES, CREATE_WEBRTC_TRANSPORT, CONNECT_WEBRTC_TRANSPORT,
        JOIN_ROOM, PRODUCE, RESUME_PRODUCER, PAUSE_PRODUCER, CLOSE_PRODUCER, RESUME_CONSUMER,
        PAUSE_CONSUMER, CLOSE_CONSUMER, LEAVE_ROOM, RESTART_ICE, REQUEST_CONSUMERKEYFRAME,
        CHANGE_DISPLAYNAME, NEW_CONSUMER, NEW_DATA_CONSUMER, PRODUCER_SCORE, NEW_PEER, PEER_LEAVE,
        PEER_CLOSED, PEER_DISPLAYNAME_CHANGED, CONSUMER_CLOSED, CONSUMER_PAUSED, CONSUMER_RESUMED,
        CONSUMER_LAYERS_CHANGED, CONSUMER_SCORE, DATA_CONSUMER_CLOSED, ACTIVE_SPEAKER, DOWNLINK_BWE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Event {
    }
}
