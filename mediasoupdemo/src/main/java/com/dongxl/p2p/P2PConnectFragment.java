package com.dongxl.p2p;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jsy.mediasoup.R;

import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link P2PConnectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class P2PConnectFragment extends Fragment {
    private P2PConnectInterface p2pInterface;

    public P2PConnectFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment P2PConnectFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static P2PConnectFragment newInstance() {
        P2PConnectFragment fragment = new P2PConnectFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public interface P2PConnectInterface {
        void onTest();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof P2PConnectInterface) {
            p2pInterface = (P2PConnectInterface) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement P2PConnectInterface");
        }
    }

    private SurfaceViewRenderer localSurface, remoteSurface;
    private P2PConnectFactory p2PConnectFactory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_p2p_connect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        localSurface = view.findViewById(R.id.local_surface);
        remoteSurface = view.findViewById(R.id.local_surface);
        localSurface.init(PeerConnectionUtils.getEglContext(), null);
        localSurface.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        //镜像
        localSurface.setMirror(true);
        localSurface.setEnableHardwareScaler(false);
        localSurface.setZOrderMediaOverlay(true);

        remoteSurface.init(PeerConnectionUtils.getEglContext(), null);
        remoteSurface.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        remoteSurface.setMirror(false);
        remoteSurface.setEnableHardwareScaler(true);
        remoteSurface.setZOrderMediaOverlay(true);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (null == p2PConnectFactory) {
            p2PConnectFactory = new P2PConnectFactory(getActivity(), new RoomStore(), localSurface,
                    remoteSurface);
        }
        p2PConnectFactory.initRoom(1);
        //是否发起方
        boolean isFaqifang = getArguments().getBoolean("isFaqifang", true);
        p2PConnectFactory.onCreateRoom();
        if (isFaqifang) {
//1 发起：
            p2PConnectFactory.faqifang1("");
//2 对方响应
            p2PConnectFactory.faqifang2("",null);
//3 建立连接
            p2PConnectFactory.faqifang3("",null);
        } else {
//1 接收发起
            p2PConnectFactory.jieshoufang1("",null);
//2 对方响应
            p2PConnectFactory.jieshoufang2("",null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != p2PConnectFactory) {
            p2PConnectFactory.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != p2PConnectFactory) {
            p2PConnectFactory.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (null != p2PConnectFactory) {
            p2PConnectFactory.destroy();
        }
        p2PConnectFactory = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        p2pInterface = null;

    }
}