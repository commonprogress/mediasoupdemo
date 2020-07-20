package com.jsy.mediasoup;

import android.app.Application;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import android.content.Context;
import android.os.Looper;

import com.jsy.mediasoup.interfaces.PropsLiveDataChange;
import com.jsy.mediasoup.interfaces.RoomStoreObserveCallback;
import com.jsy.mediasoup.vm.MeProps;
import com.jsy.mediasoup.vm.PeerProps;
import com.jsy.mediasoup.vm.RoomProps;
import com.jsy.mediasoup.utils.LogUtils;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.Utils;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Notify;

public class PropsChangeAndNotify {
    private static final String TAG = PropsChangeAndNotify.class.getSimpleName();
    private final Context mContext;
    private LifecycleOwner mLifecycleOwner;

    public PropsChangeAndNotify(Context context) {
        this.mContext = context;
    }

    public PropsChangeAndNotify(Context context, LifecycleOwner owner) {
        this(context);
        setLifecycleOwner(owner);
    }

    public void setLifecycleOwner(LifecycleOwner owner) {
        this.mLifecycleOwner = owner;
    }

    /**
     * room的监听连接
     *
     * @param owner
     * @return
     */
    public RoomProps getRoomPropsAndChange(LifecycleOwner owner) {
        return getRoomPropsAndChange(owner, null);
    }

    /**
     * room的监听连接
     *
     * @param owner
     * @param dataChange
     * @return
     */
    public RoomProps getRoomPropsAndChange(LifecycleOwner owner, PropsLiveDataChange dataChange) {
        return getRoomPropsAndChange(owner, MediasoupLoaderUtils.getInstance().getRoomClient(), MediasoupLoaderUtils.getInstance().getRoomStore(), dataChange);
    }

    /**
     * room的监听连接
     *
     * @param owner
     * @return
     */
    public RoomProps getRoomPropsAndChange(LifecycleOwner owner, RoomClient roomClient, RoomStore roomStore) {
        return getRoomPropsAndChange(owner, roomClient, roomStore, null);
    }

    /**
     * room的监听连接
     *
     * @param owner
     * @param dataChange
     * @return
     */
    public RoomProps getRoomPropsAndChange(LifecycleOwner owner, RoomClient roomClient, RoomStore roomStore, PropsLiveDataChange dataChange) {
        if (!isUIThread()) {
            LogUtils.e(TAG, "getRoomPropsAndChange no main Thread");
        }
        if (null == roomStore || null == mContext) {
            return null;
        }
        owner = null != owner ? owner : mLifecycleOwner;
        if (null == owner) {
            return null;
        }
        Application application = (Application) mContext.getApplicationContext();

        // Room. 连接
//        RoomProps roomProps = ViewModelProviders.of(this, factory).get(RoomProps.class);
//        RoomProps roomProps = new RoomProps(this.getApplication(), roomManagement.getRoomStore());
//        roomProps.setOnPropsLiveDataChange(() -> {
//            setRoomProps(roomProps);
//        });
//        roomProps.connect(this);


//        EdiasProps.Factory factory = new EdiasProps.Factory(application, mRoomStore);
        // Room. 连接
//        FragmentActivity activity = (FragmentActivity) mContext;
//        RoomProps roomProps = ViewModelProviders.of(activity, factory).get(RoomProps.class);

        RoomProps roomProps = new RoomProps(application, roomClient, roomStore);
        if (null != dataChange) {
            roomProps.setOnPropsLiveDataChange(dataChange);
        }
        roomProps.connect(owner);
        return roomProps;
    }

    /**
     * 获取自己的. MeProps
     *
     * @param owner
     * @return
     */
    public MeProps getMePropsAndChange(LifecycleOwner owner) {
        return getMePropsAndChange(owner, MediasoupLoaderUtils.getInstance().getRoomClient(), MediasoupLoaderUtils.getInstance().getRoomStore());
    }

    /**
     * 获取自己的. MeProps
     *
     * @param owner
     * @return
     */
    public MeProps getMePropsAndChange(LifecycleOwner owner, PropsLiveDataChange dataChange) {
        return getMePropsAndChange(owner, MediasoupLoaderUtils.getInstance().getRoomClient(), MediasoupLoaderUtils.getInstance().getRoomStore(), dataChange);
    }

    /**
     * 获取自己的. MeProps
     *
     * @param owner
     * @return
     */
    public MeProps getMePropsAndChange(LifecycleOwner owner, RoomClient roomClient, RoomStore roomStore) {
        return getMePropsAndChange(owner, roomClient, roomStore, null);
    }

    /**
     * 获取自己的. MeProps
     *
     * @param owner
     * @return
     */
    public MeProps getMePropsAndChange(LifecycleOwner owner, RoomClient roomClient, RoomStore roomStore, PropsLiveDataChange dataChange) {
        if (!isUIThread()) {
            LogUtils.e(TAG, "getMePropsAndChange no main Thread");
        }
        if (null == roomStore || null == mContext) {
            return null;
        }
        owner = null != owner ? owner : mLifecycleOwner;
        if (null == owner) {
            return null;
        }
        Application application = (Application) mContext.getApplicationContext();

        //        MeProps meProps = ViewModelProviders.of(this, factory).get(MeProps.class);
//        MeProps meProps = new MeProps(this.getApplication(), roomManagement.getRoomStore());
//        meProps.connect(this);

//        EdiasProps.Factory factory = new EdiasProps.Factory(application, mRoomStore);
//        FragmentActivity activity = (FragmentActivity) mContext;
//        MeProps meProps = ViewModelProviders.of(activity, factory).get(MeProps.class);

        MeProps meProps = new MeProps(application, roomClient, roomStore);
        if (null != dataChange) {
            meProps.setOnPropsLiveDataChange(dataChange);
        }
        meProps.connect(owner);
        return meProps;
    }

    /**
     * 获取他人的PeerProps
     *
     * @param owner
     * @return
     */
    public PeerProps getPeerPropsAndChange(LifecycleOwner owner, String peerId) {
        return getPeerPropsAndChange(owner, MediasoupLoaderUtils.getInstance().getRoomClient(), MediasoupLoaderUtils.getInstance().getRoomStore(), peerId);
    }

    /**
     * 获取他人的PeerProps
     *
     * @param owner
     * @return
     */
    public PeerProps getPeerPropsAndChange(LifecycleOwner owner, String peerId, PropsLiveDataChange dataChange) {
        return getPeerPropsAndChange(owner, MediasoupLoaderUtils.getInstance().getRoomClient(), MediasoupLoaderUtils.getInstance().getRoomStore(), peerId, dataChange);
    }

    /**
     * 获取他人的PeerProps
     *
     * @param owner
     * @return
     */
    public PeerProps getPeerPropsAndChange(LifecycleOwner owner, RoomClient roomClient, RoomStore roomStore, String peerId) {
        return getPeerPropsAndChange(owner, roomClient, roomStore, peerId, null);
    }

    /**
     * 获取他人的PeerProps
     *
     * @param owner
     * @return
     */
    public PeerProps getPeerPropsAndChange(LifecycleOwner owner, RoomClient roomClient, RoomStore roomStore, String peerId, PropsLiveDataChange dataChange) {
        if (!isUIThread()) {
            LogUtils.e(TAG, "getPeerPropsAndChange no main Thread");
        }
        if (null == roomStore || null == mContext || Utils.isEmptyString(peerId)) {
            return null;
        }
        owner = null != owner ? owner : mLifecycleOwner;
        if (null == owner) {
            return null;
        }
        Application application = (Application) mContext.getApplicationContext();

        //        PeerProps peerProps = ViewModelProviders.of(this, factory).get(PeerProps.class);
//        PeerProps peerProps = new PeerProps(this.getApplication(), roomManagement.getRoomStore());
//        peerProps.connect(this);

//        EdiasProps.Factory factory = new EdiasProps.Factory(application, mRoomStore);
//        FragmentActivity activity = (FragmentActivity) mContext;
//        PeerProps peerProps = ViewModelProviders.of(activity, factory).get(PeerProps.class);

        PeerProps peerProps = new PeerProps(application, roomClient, roomStore);
        if (null != dataChange) {
            peerProps.setOnPropsLiveDataChange(dataChange);
        }
        peerProps.connect(owner, peerId);
        return peerProps;
    }


    /**
     * 获取他人的PeerProps
     *
     * @param owner
     * @return
     */
    public PeerProps getAdapterPeerPropsAndChange(LifecycleOwner owner, RoomClient roomClient, RoomStore roomStore) {
        if (!isUIThread()) {
            LogUtils.e(TAG, "getAdapterPeerPropsAndChange no main Thread");
        }
        if (null == roomStore || null == mContext) {
            return null;
        }
//        owner = null != owner ? owner : mLifecycleOwner;
//        if (null == owner) {
//            return null;
//        }

//        new PeerProps(((AppCompatActivity) context).getApplication(), mRoomManagement.getRoomStore())

        Application application = (Application) mContext.getApplicationContext();
        PeerProps peerProps = new PeerProps(application, roomClient, roomStore);
        return peerProps;
    }

    /**
     * RoomStore订阅监听
     *
     * @param owner
     * @param observeCallback
     */
    public void observePeersAndNotify(LifecycleOwner owner, RoomStoreObserveCallback observeCallback) {
        observePeersAndNotify(owner, MediasoupLoaderUtils.getInstance().getRoomClient(), MediasoupLoaderUtils.getInstance().getRoomStore(), observeCallback);
    }

    /**
     * RoomStore订阅监听
     *
     * @param owner
     * @param observeCallback
     */
    public void observePeersAndNotify(LifecycleOwner owner, RoomClient roomClient, RoomStore roomStore, RoomStoreObserveCallback observeCallback) {
        if (!isUIThread()) {
            LogUtils.e(TAG, "observePeersAndNotify no main Thread");
        }
        if (null == roomStore || null == mContext) {
            return;
        }
        owner = null != owner ? owner : mLifecycleOwner;
        if (null == owner) {
            return;
        }
        roomStore
                .getPeers()
                .observe(
                        owner,
                        peers -> {
                            if (null != observeCallback) {
                                observeCallback.onObservePeers(peers);
                            }
                        });

        // Notify
        final Observer<Notify> notifyObserver =
                notify -> {
                    if (null != observeCallback) {
                        observeCallback.onObserveNotify(notify);
                    }
                };
        roomStore.getNotify().observe(owner, notifyObserver);


        //        roomManagement.getRoomStore()
//            .getPeers()
//            .observe(
//                this,
//                peers -> {
//                    List<Peer> peersList = peers.getAllPeers();
//                    if (peersList.isEmpty()) {
//                        remotePeers.setVisibility(View.GONE);
//                        roomState.setVisibility(View.VISIBLE);
//                    } else {
//                        remotePeers.setVisibility(View.VISIBLE);
//                        roomState.setVisibility(View.GONE);
//                    }
//                    mPeerAdapter.replacePeers(peersList);
//                });
//
//        // Notify
//        final Observer<Notify> notifyObserver =
//            notify -> {
//                if (notify == null) {
//                    return;
//                }
//                Logger.d(TAG, "notifyObserver, notify.getType(): " + (null == notify ? "" : notify.getType()) + ",notify.getText():" + (null == notify ? "" : notify.getText()));
//                if ("error".equals(notify.getType())) {
//                    Toast toast = Toast.makeText(this, notify.getText(), notify.getTimeout());
//                    TextView toastMessage = toast.getView().findViewById(android.R.id.message);
//                    toastMessage.setTextColor(Color.RED);
//                    toast.show();
//                } else {
//                    Toast.makeText(this, notify.getText(), notify.getTimeout()).show();
//                }
//            };
//        roomManagement.getRoomStore().getNotify().observe(this, notifyObserver);
    }

    /**
     * 判断操作是否在ui线程中
     *
     * @return
     */
    private boolean isUIThread() {
        //方法一：
        return Looper.getMainLooper() == Looper.myLooper();
        // 方法二：
//        return Looper.getMainLooper().getThread() == Thread.currentThread();
        // 方法三：
//        return Looper.getMainLooper().getThread().getId() == Thread.currentThread().getId();
    }

    public void destroy() {
        MediasoupConstant.mediaProjectionPermissionResultCode = 0;
        MediasoupConstant.mediaProjectionPermissionResultData = null;
        MediasoupConstant.extraVideoFileAsCamera = null;
    }
}
