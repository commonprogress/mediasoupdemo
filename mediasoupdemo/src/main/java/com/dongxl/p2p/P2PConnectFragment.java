package com.dongxl.p2p;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jsy.mediasoup.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link P2PConnectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class P2PConnectFragment extends Fragment implements P2PConnectCallback {

    public P2PConnectFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param faqifangPeerId
     * @param jieshoufangPeerId
     * @return A new instance of fragment P2PConnectFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static P2PConnectFragment newInstance(String faqifangPeerId, String jieshoufangPeerId) {
        P2PConnectFragment fragment = new P2PConnectFragment();
        Bundle args = new Bundle();
        args.putString("faqifangPeerId", faqifangPeerId);
        args.putString("jieshoufangPeerId", jieshoufangPeerId);
        fragment.setArguments(args);
        return fragment;
    }

    private P2PConnectInterface p2pInterface;
    private TextView zijiText;
    private SurfaceViewRenderer localSurface, remoteSurface;
    private P2PConnectFactory p2PConnectFactory;
    private boolean isFaqifang;//是否发起方
    private String faqifangPeerId, jieshoufangPeerId;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof P2PConnectInterface) {
            p2pInterface = (P2PConnectInterface) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement P2PConnectInterface");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        faqifangPeerId = getArguments().getString("faqifangPeerId", "dongxl1");
        jieshoufangPeerId = getArguments().getString("jieshoufangPeerId", "dongxl2");
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
        zijiText = view.findViewById(R.id.zijistate_txt);
        localSurface = view.findViewById(R.id.local_surface);
        localSurface.init(PeerConnectionUtils.getEglContext(), null);
        localSurface.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        //镜像
//        localSurface.setMirror(true);
        localSurface.setEnableHardwareScaler(false);
        localSurface.setZOrderMediaOverlay(true);

        remoteSurface = view.findViewById(R.id.remote_surface);
        remoteSurface.init(PeerConnectionUtils.getEglContext(), null);
        remoteSurface.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
//        remoteSurface.setMirror(false);
        remoteSurface.setEnableHardwareScaler(true);
        remoteSurface.setZOrderMediaOverlay(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (null == p2PConnectFactory) {
            p2PConnectFactory = new P2PConnectFactory(getActivity(), this, new RoomStore(), localSurface,
                    remoteSurface);
        }
        p2PConnectFactory.initRoom(1);
        p2PConnectFactory.onCreateRoom();
//        if (isFaqifang) {
////1 发起： 创建createOffer
//            p2PConnectFactory.faqifang1("");
////2 响应对方AnswerSdp 设置setRemoteDescription
//            p2PConnectFactory.faqifang2("", null);
////3  设置ice 建立连接
//            p2PConnectFactory.faqifang3("", null);
//        } else {
////1 响应对方OfferSdp 设置setRemoteDescription 创建createAnswer
//            p2PConnectFactory.jieshoufang1("", null);
////2 设置ice 建立连接
//            p2PConnectFactory.jieshoufang2("", null);
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != p2PConnectFactory) {
            p2PConnectFactory.resume();
        }
    }

    @Override
    public void onSendSelfState(String peerId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zijiText.setText("我是发起方(" + faqifangPeerId + "),接收方(" + jieshoufangPeerId + ")");
            }
        });
    }

    @Override
    public void onReceiveSelfState(String peerId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zijiText.setText("我是接收方(" + jieshoufangPeerId + "),发起方(" + faqifangPeerId + ")");
            }
        });
    }

    @Override
    public void sendOfferSdpToReceive(String peerId, String type, String description) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zijiText.setText(zijiText.getText().toString() + "，发送OfferSdp");
                if (null != p2pInterface) {
                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("type", type);
                        payload.put("sdp", description);
                        p2pInterface.sendOfferSdp(isFaqifang, payload.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void sendAnswerSdpToSend(String peerId, String type, String description) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                zijiText.setText(zijiText.getText().toString() + "，发送AnswerSdp");
                if (null != p2pInterface) {
                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("type", type);
                        payload.put("sdp", description);
                        p2pInterface.sendAnswerSdp(isFaqifang, payload.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void sendIceCandidateOtherSide(String socketId, String sdpMid, int sdpMLineIndex, String sdp, String serverUrl, Integer bitMask) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zijiText.setText(zijiText.getText().toString() + "，发送Ice");
                if (null != p2pInterface) {
                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("sdpMid", sdpMid);
                        payload.put("sdpMLineIndex", sdpMLineIndex);
                        payload.put("sdp", sdp);
                        p2pInterface.sendIceCandidate(isFaqifang, payload.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onP2PConnectSuc(String socketId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zijiText.setText(zijiText.getText().toString() + "，连接成功");
                if (null != p2pInterface) {
                    p2pInterface.onP2PConnectSuc(isFaqifang);
                }
            }
        });
    }

    /**
     * 1 发起： 创建createOffer
     *
     * @param peerId
     * @param isFaqifang
     */
    public void createOfferSdp(String peerId, boolean isFaqifang) {
        this.isFaqifang = isFaqifang;
        if (null != p2PConnectFactory) {
            p2PConnectFactory.faqifang1(peerId);
        }
    }

    /**
     * 2 响应对方AnswerSdp 设置setRemoteDescription
     *
     * @param peerId
     * @param data
     * @param isFaqifang
     */
    public void setRemoteSdp(String peerId, JSONObject data, boolean isFaqifang) {
        this.isFaqifang = isFaqifang;
        if (null != p2PConnectFactory) {
            p2PConnectFactory.faqifang2(peerId, data);
        }
    }

    /**
     * 1 响应对方OfferSdp 设置setRemoteDescription 创建createAnswer
     *
     * @param peerId
     * @param data
     * @param isFaqifang
     */
    public void setRemoteAndCreateAnswerSdp(String peerId, JSONObject data, boolean isFaqifang) {
        this.isFaqifang = isFaqifang;
        if (null != p2PConnectFactory) {
            p2PConnectFactory.jieshoufang1(peerId, data);
        }
    }

    /**
     * 3  设置ice 建立连接
     *
     * @param peerId
     * @param data
     * @param isFaqifang
     */
    public void setIceCandidateData(String peerId, JSONObject data, boolean isFaqifang) {
        this.isFaqifang = isFaqifang;
        if (null != p2PConnectFactory) {
            if (isFaqifang) {
                //3  设置ice 建立连接
                p2PConnectFactory.faqifang3(peerId, data);
            } else {
                //2 设置ice 建立连接
                p2PConnectFactory.jieshoufang2(peerId, data);
            }
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