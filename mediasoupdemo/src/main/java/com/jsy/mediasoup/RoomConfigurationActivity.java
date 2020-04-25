package com.jsy.mediasoup;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class RoomConfigurationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_configuration);
    }

    public void videoModeClick(View view) {
        Intent intent = new Intent();
        intent.putExtra(MediasoupConstant.key_intent_roommode, MediasoupConstant.roommode_video);
        intent.setClass(this, RoomActivity.class);
        startActivity(intent);
        finish();
    }

    public void audioModeClick(View view) {
        Intent intent = new Intent();
        intent.putExtra(MediasoupConstant.key_intent_roommode, MediasoupConstant.roommode_audio);
        intent.setClass(this, RoomActivity.class);
        startActivity(intent);
        finish();
    }

    public void seeModeClick(View view) {
        Intent intent = new Intent();
        intent.putExtra(MediasoupConstant.key_intent_roommode, MediasoupConstant.roommode_see);
        intent.setClass(this, RoomActivity.class);
        startActivity(intent);
        finish();
    }

    public void muteModeClick(View view) {
        Intent intent = new Intent();
        intent.putExtra(MediasoupConstant.key_intent_roommode, MediasoupConstant.roommode_mute);
        intent.setClass(this, RoomActivity.class);
        startActivity(intent);
        finish();
    }

    public void noAllModeClick(View view) {
        Intent intent = new Intent();
        intent.putExtra(MediasoupConstant.key_intent_roommode, MediasoupConstant.roommode_noall);
        intent.setClass(this, RoomActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
