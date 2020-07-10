package org.mediasoup.droid.lib;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peer;
import org.protoojs.droid.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间消息处理
 */
class RoomMessageHandler {

  static final String TAG = "RoomClient";

  // Stored Room States.
  @NonNull
  final RoomStore mStore;
  // mediasoup Consumers. 消费者管理集合
  @NonNull final Map<String, ConsumerHolder> mConsumers;

  /**
   * Consumer（消费者）和peerid 绑定
   */
  static class ConsumerHolder {
    @NonNull final String peerId;
    @NonNull final Consumer mConsumer;

    ConsumerHolder(@NonNull String peerId, @NonNull Consumer consumer) {
      this.peerId = peerId;
      mConsumer = consumer;
    }
  }

  RoomMessageHandler(@NonNull RoomStore store) {
    this.mStore = store;
    this.mConsumers = new ConcurrentHashMap<>();
  }

  @WorkerThread
  void handleNotification(Message.Notification notification) throws JSONException {
    JSONObject data = notification.getData();
    Logger.d(TAG, "handleNotification notification.method " + notification.getMethod()+", data:"+data.toString());
    switch (notification.getMethod()) {
      case "producerScore":
        {
          // {"producerId":"bdc2e83e-5294-451e-a986-a29c7d591d73","score":[{"score":10,"ssrc":196184265}]}
          String producerId = data.optString("producerId");
          JSONArray score = data.optJSONArray("score");
          mStore.setProducerScore(producerId, score);
          break;
        }
      case "newPeer":
        {
          String id = data.optString(Peer.KEY_PEER_ID);
          String displayName = data.optString(Peer.KEY_PEER_NAME);
          mStore.addPeer(id, data);
          mStore.addNotify(displayName + " has joined the room");
          break;
        }
      case "peerClosed":
        {
          String peerId = data.optString("peerId");
          mStore.removePeer(peerId);
          break;
        }
      case "peerDisplayNameChanged":
        {
          String peerId = data.optString("peerId");
          String displayName = data.optString(Peer.KEY_PEER_NAME);
          String oldDisplayName = data.optString("oldDisplayName");
          mStore.setPeerDisplayName(peerId, displayName);
          mStore.addNotify(oldDisplayName + " is now " + displayName);
          break;
        }
      case "consumerClosed":
        {
          String consumerId = data.optString("consumerId");
          ConsumerHolder holder = mConsumers.remove(consumerId);
          if (holder == null) {
            break;
          }
          holder.mConsumer.close();
          mConsumers.remove(consumerId);
          mStore.removeConsumer(holder.peerId, holder.mConsumer.getId());
          break;
        }
      case "consumerPaused":
        {
          String consumerId = data.optString("consumerId");
          ConsumerHolder holder = mConsumers.get(consumerId);
          if (holder == null) {
            break;
          }
          mStore.setConsumerPaused(holder.mConsumer.getId(), "remote");
          break;
        }
      case "consumerResumed":
        {
          String consumerId = data.optString("consumerId");
          ConsumerHolder holder = mConsumers.get(consumerId);
          if (holder == null) {
            break;
          }
          mStore.setConsumerResumed(holder.mConsumer.getId(), "remote");
          break;
        }
      case "consumerLayersChanged":
        {
          String consumerId = data.optString("consumerId");
          int spatialLayer = data.optInt("spatialLayer");
          int temporalLayer = data.optInt("temporalLayer");
          ConsumerHolder holder = mConsumers.get(consumerId);
          if (holder == null) {
            break;
          }
          mStore.setConsumerCurrentLayers(consumerId, spatialLayer, temporalLayer);
          break;
        }
      case "consumerScore":
        {
          String consumerId = data.optString("consumerId");
          JSONArray score = data.optJSONArray("score");
          ConsumerHolder holder = mConsumers.get(consumerId);
          if (holder == null) {
            break;
          }
          mStore.setConsumerScore(consumerId, score);
          break;
        }
      case "dataConsumerClosed":
        {
          // TODO(HaiyangWu); support data consumer
          // String dataConsumerId = data.optString("dataConsumerId");
          break;
        }
      case "activeSpeaker":
        {
          String peerId = data.optString("peerId");
          mStore.setRoomActiveSpeaker(peerId);
          break;
        }
      case "downlinkBwe":
      {
        String desired = data.optString("desiredBitrate");
        String effectiveDesired = data.optString("effectiveDesiredBitrate");
        String available = data.optString("availableBitrate");
        break;
      }
      default:
        {
          Logger.e(TAG, "unknown protoo notification.method " + notification.getMethod());
        }
    }
  }
}
