package com.dongxl.mediasoup;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jsy.mediasoup.R;

import org.mediasoup.droid.lib.model.Info;
import org.mediasoup.droid.lib.model.Peer;

import java.util.List;

public class ConnectPeerAdapter extends RecyclerView.Adapter<ConnectPeerAdapter.UserViewHolder> {

    private final Context mContext;
    private List<Peer> peers;

    public ConnectPeerAdapter(Context context) {
        this.mContext = context;
    }

    public void setPeers(List<Peer> peers) {
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

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.adapter_connect_peer, viewGroup, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder userViewHolder, int i) {
        Info info = peers.get(i);
        userViewHolder.textView.setText(info.getDisplayName());
    }


    class UserViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.peerText);
        }
    }
}
