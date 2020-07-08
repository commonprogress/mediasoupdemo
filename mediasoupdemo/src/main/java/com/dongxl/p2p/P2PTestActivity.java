package com.dongxl.p2p;

import android.Manifest;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.dongxl.p2p.utils.DisplayUtils;
import com.jsy.mediasoup.R;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

public class P2PTestActivity extends AppCompatActivity implements P2PConnectFragment.P2PConnectInterface {
    private static final int CONNECTION_REQUEST = 1;
    private static final int PERMISSION_REQUEST = 2;
    private EditText editText;
    private TextView textView;
    private P2PConnectFragment p2PConnectFragment;
    private boolean connected;
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
        p2PConnectFragment = P2PConnectFragment.newInstance();
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

    }

    public void copyClick(View view) {

    }

    public void firstClick(View view) {

    }

    public void secondClick(View view) {

    }

    public void thirdClick(View view) {

    }

    @Override
    public void onTest() {

    }
}