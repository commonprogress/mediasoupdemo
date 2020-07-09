package com.dongxl.p2p;

import android.Manifest;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dongxl.p2p.utils.DisplayUtils;
import com.jsy.mediasoup.R;
import com.jsy.mediasoup.utils.ClipboardCopy;
import com.jsy.mediasoup.utils.LogUtils;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.lib.Utils;

public class P2PTestActivity extends AppCompatActivity implements P2PConnectInterface {
    private static final int CONNECTION_REQUEST = 1;
    private static final int PERMISSION_REQUEST = 2;
    private static final String TAG = P2PTestActivity.class.getSimpleName();
    private EditText editText;
    private TextView textView;
    private Button firstBtn, secondBtn, thirdBtn;
    private P2PConnectFragment p2PConnectFragment;
    private boolean isFaqifang;
    private String faqifangPeerId = "dongxl1";
    private String jieshoufangPeerId = "dongxl2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(DisplayUtils.getSystemUiVisibility());
        setContentView(R.layout.activity_p2p_test);
        textView = findViewById(R.id.self_sdp_txt);
        editText = findViewById(R.id.other_sdp_txt);
        firstBtn = findViewById(R.id.button3);
        secondBtn = findViewById(R.id.button4);
        thirdBtn = findViewById(R.id.button5);
        p2PConnectFragment = P2PConnectFragment.newInstance(faqifangPeerId, jieshoufangPeerId);
        addConnectFragment(p2PConnectFragment);
    }

    private void addConnectFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.p2p_connect_container, fragment)
                .commitAllowingStateLoss();
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        String rationale = "Please provide permissions";
        Permissions.Options options =
                new Permissions.Options().setRationaleDialogTitle("Info").setSettingsDialogTitle("Warning");
        Permissions.check(this, permissions, rationale, options, new PermissionHandler() {
            @Override
            public void onGranted() {

            }
        });
    }


    public void clearEditClick(View view) {
        editText.setText("");
        textView.setText("");
        firstBtn.setClickable(true);
        secondBtn.setClickable(true);
        thirdBtn.setClickable(false);
    }

    public void copyClick(View view) {
        String copyData = textView.getText().toString();
        LogUtils.i(TAG, "copyClick copyData:" + copyData);
        if (Utils.isEmptyString(copyData)) {
            return;
        }
        ClipboardCopy.clipboardCopy(this, copyData, R.string.invite_link_copied);
    }

    public void firstClick(View view) {
        String jsonData = editText.getText().toString();
        if (null  == p2PConnectFragment || !p2PConnectFragment.isAdded() || !Utils.isEmptyString(jsonData)) {
            return;
        }
        this.isFaqifang = true;
        if (isFaqifang) {//发起方
            p2PConnectFragment.createOfferSdp(jieshoufangPeerId, isFaqifang);
        }
        firstBtn.setClickable(false);
        secondBtn.setClickable(true);
        thirdBtn.setClickable(false);
        LogUtils.i(TAG, "firstClick isFaqifang:" + isFaqifang + ",faqifangPeerId:" + faqifangPeerId + ",jieshoufangPeerId:" + jieshoufangPeerId);
    }

    public void secondClick(View view) {
        String jsonData = editText.getText().toString();
        if (null  == p2PConnectFragment || !p2PConnectFragment.isAdded() || Utils.isEmptyString(jsonData)) {
            return;
        }
        LogUtils.i(TAG, "secondClick isFaqifang:" + isFaqifang + ",faqifangPeerId:" + faqifangPeerId + ",jieshoufangPeerId:" + jieshoufangPeerId + ",jsonData:" + jsonData);
        try {
            JSONObject payload = new JSONObject(jsonData);
            if (isFaqifang) {//发起方
                p2PConnectFragment.setRemoteSdp(jieshoufangPeerId, payload, isFaqifang);
            } else {//接收方
                isFaqifang = false;
                p2PConnectFragment.setRemoteAndCreateAnswerSdp(faqifangPeerId, payload, isFaqifang);
            }
            thirdBtn.setClickable(true);
            firstBtn.setClickable(false);
            secondBtn.setClickable(false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void thirdClick(View view) {
        String jsonData = editText.getText().toString();
        if (null  == p2PConnectFragment || !p2PConnectFragment.isAdded() || Utils.isEmptyString(jsonData)) {
            return;
        }
        LogUtils.i(TAG, "thirdClick isFaqifang:" + isFaqifang + ",faqifangPeerId:" + faqifangPeerId + ",jieshoufangPeerId:" + jieshoufangPeerId + ",jsonData:" + jsonData);
        try {
            JSONObject payload = new JSONObject(jsonData);
            if (isFaqifang) {//发起方
                p2PConnectFragment.setIceCandidateData(jieshoufangPeerId, payload, isFaqifang);
            } else {//接收方
                p2PConnectFragment.setIceCandidateData(faqifangPeerId, payload, isFaqifang);
            }
            firstBtn.setClickable(false);
            secondBtn.setClickable(false);
            thirdBtn.setClickable(false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendOfferSdp(boolean isFaqifang, String jsonData) {
        this.isFaqifang = isFaqifang;
        textView.setText(jsonData);
        LogUtils.i(TAG, "sendOfferSdp isFaqifang:" + isFaqifang + ",faqifangPeerId:" + faqifangPeerId + ",jieshoufangPeerId:" + jieshoufangPeerId + ",jsonData:" + jsonData);
    }

    @Override
    public void sendAnswerSdp(boolean isFaqifang, String jsonData) {
        this.isFaqifang = isFaqifang;
        textView.setText(jsonData);
        LogUtils.i(TAG, "sendAnswerSdp isFaqifang:" + isFaqifang + ",faqifangPeerId:" + faqifangPeerId + ",jieshoufangPeerId:" + jieshoufangPeerId + ",jsonData:" + jsonData);
    }

    @Override
    public void sendIceCandidate(boolean isFaqifang, String jsonData) {
        this.isFaqifang = isFaqifang;
        textView.setText(jsonData);
        LogUtils.i(TAG, "sendIceCandidate isFaqifang:" + isFaqifang + ",faqifangPeerId:" + faqifangPeerId + ",jieshoufangPeerId:" + jieshoufangPeerId + ",jsonData:" + jsonData);
    }

    @Override
    public void onP2PConnectSuc(boolean isFaqifang) {
        this.isFaqifang = isFaqifang;
        textView.setText("连接成功");
    }
}