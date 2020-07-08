package com.dongxl.mediasoup;

import androidx.lifecycle.LifecycleOwner;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jsy.mediasoup.MediasoupLoaderUtils;
import com.jsy.mediasoup.PropsChangeAndNotify;
import com.jsy.mediasoup.R;
import com.jsy.mediasoup.utils.LogUtils;
import com.jsy.mediasoup.view.PeerView;
import com.jsy.mediasoup.vm.PeerProps;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Info;
import org.mediasoup.droid.lib.model.Peer;

import java.util.List;

public class PeersVideoAdapter extends RecyclerView.Adapter<PeersVideoAdapter.PeerViewHolder> {
    private static final String TAG = PeersVideoAdapter.class.getSimpleName();
    private final Context mContext;
    private PropsChangeAndNotify changeAndNotify;
    private LifecycleOwner lifecycleOwner;
    private List<Info> peers;

    public PeersVideoAdapter(Context context, LifecycleOwner lifecycleOwner, PropsChangeAndNotify changeAndNotify) {
        this.mContext = context;
        this.lifecycleOwner = lifecycleOwner;
        this.changeAndNotify = changeAndNotify;
    }

    public void setPeers(List<Info> peers) {
        this.peers = peers;
        this.notifyDataSetChanged();
    }

    public void clearAll() {
        if (null != peers) {
            peers.clear();
        }
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return null == peers ? 0 : peers.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LogUtils.i(TAG, "onCreateViewHolder,mediasoup getItemCount:" + getItemCount());
        View view = LayoutInflater.from(mContext).inflate(R.layout.adapter_video_peer, viewGroup, false);
        RoomClient mRoomClient = MediasoupLoaderUtils.getInstance().getRoomClient();
        RoomStore mRoomStore = MediasoupLoaderUtils.getInstance().getRoomStore();
        PeerProps peerProps = changeAndNotify.getAdapterPeerPropsAndChange(lifecycleOwner, mRoomClient, mRoomStore);
        return new PeerViewHolder(view, peerProps);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder peerViewHolder, int i) {
        LogUtils.i(TAG, "onBindViewHolder,mediasoup getItemCount:" + getItemCount());
//        ViewGroup.LayoutParams layoutParams = peerViewHolder.peerView.getLayoutParams();
//        layoutParams.height = 700;
//        peerViewHolder.peerView.setLayoutParams(layoutParams);
//        peerViewHolder.peerView.setNeatView(false);
        Peer peer = (Peer) peers.get(i);
        RoomClient mRoomClient = MediasoupLoaderUtils.getInstance().getRoomClient();
        RoomStore mRoomStore = MediasoupLoaderUtils.getInstance().getRoomStore();
        peerViewHolder.peerProps.connect(lifecycleOwner, peer.getId());
        peerViewHolder.peerView.setProps(peerViewHolder.peerProps, mRoomClient, mRoomStore);
    }

    class PeerViewHolder extends RecyclerView.ViewHolder {

        PeerProps peerProps;
        PeerView peerView;

        public PeerViewHolder(@NonNull View itemView) {
            super(itemView);
            peerView = itemView.findViewById(R.id.peer_view);
        }

        public PeerViewHolder(@NonNull View itemView, PeerProps peerProps) {
            this(itemView);
            this.peerProps = peerProps;
        }
    }
}
