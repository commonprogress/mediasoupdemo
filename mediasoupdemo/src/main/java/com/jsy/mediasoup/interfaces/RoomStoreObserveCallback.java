package com.jsy.mediasoup.interfaces;

import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.Peers;

public interface RoomStoreObserveCallback {
    void onObservePeers(Peers peers);

    void onObserveNotify(Notify notify);
}
