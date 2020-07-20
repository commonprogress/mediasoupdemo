package com.jsy.mediasoup.adapter;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jsy.mediasoup.PropsChangeAndNotify;
import com.jsy.mediasoup.R;
import com.jsy.mediasoup.view.PeerView;
import com.jsy.mediasoup.vm.PeerProps;

import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peer;

import java.util.LinkedList;
import java.util.List;

public class PeerAdapter extends RecyclerView.Adapter<PeerAdapter.PeerViewHolder> {
    private static final String TAG = PeerAdapter.class.getSimpleName();

    @NonNull
    private RoomStore mRoomStore;
    @NonNull
    private LifecycleOwner mLifecycleOwner;
    @NonNull
    private RoomClient mRoomClient;//房间操作类
    @NonNull
    private final PropsChangeAndNotify mChangeAndNotify;

    /**
     * 连接的用户列表
     */
    private List<Peer> mPeers = new LinkedList<>();

    //item 的高度
    private int containerHeight;

    public PeerAdapter(
        @NonNull LifecycleOwner lifecycleOwner,
        @NonNull PropsChangeAndNotify changeAndNotify,
        @NonNull RoomClient roomClient,
        @NonNull RoomStore roomStore
    ) {
        mLifecycleOwner = lifecycleOwner;
        mChangeAndNotify = changeAndNotify;
        mRoomStore = roomStore;
        mRoomClient = roomClient;
    }

    public void clearPeers() {
        if (null != mPeers) {
            mPeers.clear();
            notifyDataSetChanged();
        }
    }

    public void replacePeers(@NonNull List<Peer> peers) {
        if (null == peers) {
            peers = new LinkedList<>();
        }
        mPeers = peers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        containerHeight = parent.getHeight();
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_remote_peer, parent, false);
//        return new PeerViewHolder(
//            view, new PeerProps(((AppCompatActivity) context).getApplication(), mRoomManagement.getRoomStore()));
        return new PeerViewHolder(view, mChangeAndNotify.getAdapterPeerPropsAndChange(mLifecycleOwner, mRoomClient, mRoomStore));
    }

    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
        // update height
        ViewGroup.LayoutParams layoutParams = holder.mPeerView.getLayoutParams();
        layoutParams.height = getItemHeight();
        holder.mPeerView.setLayoutParams(layoutParams);
        // bind
        holder.bind(mLifecycleOwner, mRoomClient, mRoomStore, mPeers.get(position));
    }

    @Override
    public int getItemCount() {
        return mPeers.size();
    }

    private int getItemHeight() {
        int itemCount = getItemCount();
        if (itemCount <= 1) {
            return containerHeight;
        } else if (itemCount <= 3) {
            return containerHeight / itemCount;
        } else {
            return (int) (containerHeight / 3.2);
        }
    }

    static class PeerViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        final PeerView mPeerView;
        @NonNull
        final PeerProps mPeerProps;

        PeerViewHolder(@NonNull View view, @NonNull PeerProps peerProps) {
            super(view);
            mPeerView = view.findViewById(R.id.remote_peer);
            mPeerProps = peerProps;
        }

        void bind(LifecycleOwner owner, RoomClient roomClient, RoomStore roomStore, @NonNull Peer peer) {
            Logger.d(TAG, "bind() mediasoup id: " + peer.getId() + ", name: " + peer.getDisplayName());
            mPeerProps.connect(owner, peer.getId());
            mPeerView.setProps(mPeerProps, roomClient, roomStore);
        }
    }
}
