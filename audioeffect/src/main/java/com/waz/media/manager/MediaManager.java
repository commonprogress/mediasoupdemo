/*
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.media.manager;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager;

import android.media.AudioManager.OnAudioFocusChangeListener;

import android.content.BroadcastReceiver;
import android.os.Process;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import com.waz.avs.AVSystem;
import com.waz.media.manager.player.MediaSource;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Set;
import java.util.Map;

import java.util.HashSet;
import java.util.HashMap;

import java.util.Iterator;

//import com.waz.call.FlowManager;

import com.waz.media.manager.config.Configuration;
import com.waz.media.manager.config.MediaConfiguration;

import com.waz.media.manager.router.AudioRouter;

import com.waz.media.manager.context.IntensityLevel;

import com.waz.media.manager.player.MediaPlayer;
import com.waz.media.manager.player.MediaPlayerListener;

import com.waz.media.manager.player.SoundSource;
import com.waz.media.manager.player.MediaSource;

import androidx.annotation.RequiresApi;
import android.util.Log;

@RequiresApi(api = Build.VERSION_CODES.FROYO)
public class MediaManager implements OnAudioFocusChangeListener {
    static {
        AVSystem.load();
    }
    // Set to true to enable debug logs.
    private static final boolean DEBUG = true;

    private static MediaManager _instance = null;
    private Context ctx;

    private HashSet<MediaManagerListener> _listenerSet = null;

    private int route = AudioRouter.ROUTE_INVALID;

    private AudioManager audioManager = null;

    private int prev_mode = AudioManager.MODE_INVALID;

    private String convId = null;

    private IntensityLevel _intensity = IntensityLevel.FULL;

    public long mmPointer;

    private Configuration config;

//    private AudioRouter audioRouter;

    private Map<String,MediamgrPlay> mapMediamgrs  = new HashMap<>();

    private MediaManager ( final Context context) {
        DoLog("MediaManager supe");
        attach(context);

        setIntensity ( IntensityLevel.FULL );

        ctx = context;

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        try {
//            if(null != context){
//                audioRouter = new AudioRouter(context, audioRouterCallback);
//            }
            audioManager.setSpeakerphoneOn(false);
        }
        catch(Exception e) {
            Log.e("avs MediaManager", "setSpeakerphoneOn failed: " + e);
        }
    }

    public static MediaManager getInstance ( Context context ) {
        if( context == null){
            Log.e("avs MediaManager", "getInstance called with null context. \n");
            return null;
        }
        if ( _instance == null ) {
            _instance = new MediaManager(context);
        }

        if ( _instance.getContext() != context ) {
            _instance = new MediaManager(context);
        }
        Log.i("avs MediaManager","MediaManager getInstance");
        return _instance;
    }

    public Context getContext ( ) {
        return ctx;
    }

    protected void finalize ( ) throws Throwable  {
        if (ctx != null)
            detach();
        DoLog("MediaManager finalize");
        unregisterAllMedia();
        super.finalize();
    }

    public void addListener ( MediaManagerListener listener ) {
        DoLog("addListener");
        if ( listener != null ) {
            this.getListenerSet().add(listener);
        }
    }

    public void removeListener ( MediaManagerListener listener ) {
        DoLog("removeListener");
        if ( listener != null ) {
            this.getListenerSet().remove(listener);
        }
    }

    public IntensityLevel getIntensity ( ) {
        DoLog("MediaManager getIntensity:" + this._intensity);
        return this._intensity;
    }

    public void setIntensity ( IntensityLevel intensity ) {
        this._intensity = intensity;
        DoLog("MediaManager setIntensity:" + this._intensity);
        switch ( intensity ) {
            case NONE: setIntensityNone(); DoLog("setIntensity to NONE \n"); break;
            case SOME: setIntensitySome(); DoLog("setIntensity to SOME \n"); break;
            case FULL: setIntensityAll(); DoLog("setIntensity to FULL \n"); break;
        }
    }

    public void registerMediaFromConfiguration ( JSONObject configuration ) {
        DoLog("registerMediaFromConfiguration configuration:" + (null == configuration ? "null" : configuration.toString()));

        Context context = this.getContext();
        String namespace = context.getPackageName();

        config = new Configuration(configuration);
        HashMap<String, MediaConfiguration> soundMap = config.getSoundMap();

        Set<String> keys = soundMap.keySet();

        for ( String key : keys ) {
            MediaConfiguration value = soundMap.get(key);

            String name = value.getName();
            String path = value.getPath();

            int id = context.getResources().getIdentifier(path, "raw", namespace);
            Uri uri = Uri.parse("android.resource://" + namespace + "/" + id);

            this.registerMediaFileUrl(name, uri);
        }
    }

    public int registerMediaFileUrl ( String Name, Uri file_uri ) {
        unregisterMedia(Name);
        DoLog("registerMediaFileUrl Name:" + Name + ",file_uri:" + file_uri);
        Context context = this.getContext();

        if(config == null){
            DoLogErr("Configuration is null ");
            return -1;
        }

        HashMap<String, MediaConfiguration> soundMap = config.getSoundMap();

        MediaConfiguration value = soundMap.get(Name);

        if(value == null){
            DoLogErr("No configuration for " + Name);
            return -1;
        }

        int stream = android.media.AudioManager.USE_DEFAULT_STREAM_TYPE;
        DoLog("registerMediaFileUrl stream:" + stream + ",value.getIncall():" + value.getIncall());
        if ( value.getIncall() ) {
            stream = android.media.AudioManager.STREAM_VOICE_CALL;
        }
        else {
            stream = android.media.AudioManager.STREAM_NOTIFICATION;
        }

        // HACK ARE ADDED HERE
        if ( Name.equals("ringing_from_me") ) {
            stream = android.media.AudioManager.STREAM_VOICE_CALL;
        }
        if ( Name.equals("ringing_from_me_video") ) {
            stream = android.media.AudioManager.STREAM_VOICE_CALL;
        }
        if ( Name.equals("ringing_from_them") ) {
            stream = android.media.AudioManager.STREAM_RING;
        }
        // END OF HACKS

        SoundSource source = new SoundSource(Name, context, file_uri, stream);

        //source.setShouldLoop(value.getLooping());

        DoLog("register " + Name + " " + file_uri + " " + stream);

        this.registerMedia(Name, value, source);

        return 0;
    }


    public void registerMedia ( String media, JSONObject options, MediaSource source ) {
        if ( media != null && options != null && source != null ) {
            MediaConfiguration config = new MediaConfiguration(media, options);

            DoLog("registerMedia1: " + media);

            this.registerMedia(media, config, source);
        }else{
            DoLogErr("registerMedia1 media:" + media + ",options:" + (null == options ? "null" : options.toString()));
        }
    }

    private void registerMedia ( String media, MediaConfiguration config, MediaSource source ) {
        MediaPlayer player = new MediaPlayer(source);

        boolean mixing = config.getMixing();
        boolean incall = config.getIncall();

        boolean looping = config.getLooping();

        boolean requirePlaybackMode = config.getRequirePlaybackMode();
        boolean requireRecordingMode = config.getRequireRecordingMode();

        int intensity = config.getIntensity();

        player.setName(media);
        player.setShouldLoop(looping);
        DoLog("registerMedia2 mixing:" + mixing + ", incall:" + incall + ", looping:" + looping + ", requirePlaybackMode:" + requirePlaybackMode
                + ", requireRecordingMode:" + requireRecordingMode + ", intensity:" + intensity + ", media:" + media);
        //player.setListener(this);

        this.registerMedia(media, player, (boolean)mixing, (boolean)incall, intensity);
    }

    public void unregisterAllMedia ( ) {
        DoLog("unregisterAllMedia");
        // Should be a native function
        for (Map.Entry<String, MediamgrPlay> entry : mapMediamgrs.entrySet()) {
            unregisterMedia(entry.getKey());
        }
        mapMediamgrs.clear();
    }

    public boolean isLoudSpeakerOn() {
        boolean isSpeakerphoneOn = null == audioManager ? false : audioManager.isSpeakerphoneOn();
        boolean isMicrophoneMute = null == audioManager ? false : audioManager.isMicrophoneMute();
        DoLog("isLoudSpeakerOn isSpeakerphoneOn:" + isSpeakerphoneOn + ",isMicrophoneMute:" + isMicrophoneMute + ",route:" + route);
        return isSpeakerphoneOn;
//        if (this.route == AudioRouter.ROUTE_SPEAKER) {
//            return true;
//        } else {
//            return false;
//        }
    }

    public boolean isLoudSpeakerOff() {
        boolean isSpeakerphoneOn = null == audioManager ? false : audioManager.isSpeakerphoneOn();
        boolean isMicrophoneMute = null == audioManager ? false : audioManager.isMicrophoneMute();
        DoLog("isLoudSpeakerOff isSpeakerphoneOn:" + isSpeakerphoneOn + ",isMicrophoneMute:" + isMicrophoneMute + ",route:" + route);
        return !isSpeakerphoneOn;
//        if (this.route != AudioRouter.ROUTE_SPEAKER) {
//            return true;
//        } else {
//            return false;
//        }
    }

    public void turnLoudSpeakerOn ( ) {
        DoLog("turnLoudSpeakerOn");
        this.EnableSpeaker(true);
    }

    public void turnLoudSpeakerOff ( ) {
        DoLog("turnLoudSpeakerOff");
        this.EnableSpeaker(false);
    }

    public boolean isWiredHsOn(){
        if (null == this.audioManager) {
            return false;
        }
        return this.audioManager.isWiredHeadsetOn();
    }

    public void onFinishedPlaying ( MediaPlayer player ) {
        // ToDo Maybe
        DoLog("onFinishedPlaying");
        this.stopMedia(player.getName());
    }

    public void onPlaybackRouteChanged ( int route ) {
        // SSJ will there ever be more than one listener ??
        DoLog("onPlaybackRouteChanged route:" + route);
        this.route = route;

        HashSet<MediaManagerListener>  listenerSet = (HashSet<MediaManagerListener>)((HashSet)this.getListenerSet()).clone();
        Iterator<MediaManagerListener> iterator = listenerSet.iterator();

        while ( iterator.hasNext() ) {
            MediaManagerListener listener = iterator.next();

            listener.onPlaybackRouteChanged(route);
        }
    }

    public void onMediaCategoryChanged(boolean call_catagory){
        DoLog("OnMediaCategoryChanged \n");
        int mCat = 0;
//        if ( call_catagory ) {
//            mCat = FlowManager.MCAT_CALL;
//        } else {
//            mCat = FlowManager.MCAT_NORMAL;
//        }

        HashSet<MediaManagerListener>  listenerSet = (HashSet<MediaManagerListener>)((HashSet)this.getListenerSet()).clone();
        Iterator<MediaManagerListener> iterator = listenerSet.iterator();

        while ( iterator.hasNext() ) {
            MediaManagerListener listener = iterator.next();

            listener.mediaCategoryChanged ( this.convId, mCat );
        }
    }

    public void onAudioFocusChange ( int focusChange ) {
        DoLog("onAudioFocusChange focusChange = " + focusChange);
    }

    public void onEnterCall(){
        boolean isMicrophoneMute = null == audioManager ? false : audioManager.isMicrophoneMute();
        boolean isSpeakerphoneOn = null == audioManager ? false : audioManager.isSpeakerphoneOn();
        DoLog("onEnterCall: mode=" + (null != this.audioManager ? this.audioManager.getMode() : "null") + ",isSpeakerphoneOn:" + isSpeakerphoneOn + ",isMicrophoneMute:" + isMicrophoneMute);
        if (null == this.audioManager) {
            return;
        }
        if(this.prev_mode == AudioManager.MODE_INVALID){
            this.prev_mode = this.audioManager.getMode();
        }
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);

        this.audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    public void onAudioFocus() {
        DoLog("onAudioFocus: mode=" + (null != this.audioManager ? this.audioManager.getMode() : "null"));
        if (null == this.audioManager) {
            return;
        }
        this.audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        this.audioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);
    }

    public void onExitCall(){
        boolean isMicrophoneMute = null == audioManager ? false : audioManager.isMicrophoneMute();
        boolean isSpeakerphoneOn = null == audioManager ? false : audioManager.isSpeakerphoneOn();
        DoLog("onExitCall: mode=" + (null != this.audioManager ? this.audioManager.getMode() : "null") + ",isSpeakerphoneOn:" + isSpeakerphoneOn + ",isMicrophoneMute:" + isMicrophoneMute);
        if (null == this.audioManager) {
            return;
        }
        audioManager.abandonAudioFocus(this);
        DoLog("onExitCall prev_mode=" + this.prev_mode);
        if(this.prev_mode != AudioManager.MODE_INVALID){
            this.audioManager.setMode(this.prev_mode);
        }
        this.prev_mode = AudioManager.MODE_INVALID;
        this.audioManager.setSpeakerphoneOn(false);
        DoLog("onExitCall()");
    }

    public void setCallState(String convId, boolean incall) {
        if(incall){
            if(this.convId != null){
                DoLog("EnterCall called without an ExitCall");
            }
            this.convId = convId;
        } else {
            this.convId = null;
        }
        setCallState(incall);
    }

    public void setVideoCallState(String convId) {
        if(this.convId != convId){
            DoLog("setVideoCallState called without an StartCall");
        }
        setVideoCallState();
    }

    private HashSet<MediaManagerListener> getListenerSet ( ) {
        if ( this._listenerSet == null ) {
            this._listenerSet = new HashSet<MediaManagerListener>();
        }

        return this._listenerSet;
    }

    final String logTag = "avs MediaManager";

    private void DoLog(String msg) {
        if(DEBUG){
            Log.d(logTag, msg);
        }
    }

    private void DoLogErr(String msg) {
        Log.e(logTag, msg);
    }

    private native void attach ( Context context );
    private native void detach ( );
    //    public native void playMedia (String name);
//    public native void stopMedia (String name);
//    public native void EnableSpeaker(boolean enable);
//    public native void registerMedia(String name, MediaPlayer mp, boolean mixing, boolean incall, int intensity);
//    public native void unregisterMedia(String name);
    private native void setCallState(boolean incall);
    private native void setVideoCallState();
    private native void setIntensityAll();
    private native void setIntensitySome();
    private native void setIntensityNone();


    public void playMedia(String name) {
        MediamgrPlay mediamgrPlay = getCurMediamgrPlay(name);
        DoLog("playMedia name:" + name + ", null == mediamgrPlay:" + (null == mediamgrPlay));
        if (null != mediamgrPlay) {
            mediamgrPlay.playMedia(name);
        }
    }

    public void stopMedia(String name) {
        MediamgrPlay mediamgrPlay = getCurMediamgrPlay(name);
        DoLog("stopMedia name:" + name + ", null == mediamgrPlay:" + (null == mediamgrPlay));
        if (null != mediamgrPlay) {
            mediamgrPlay.stopMedia(name);
        }
    }

    /**
     * 喇叭扩音器打开或关闭.
     * @param enable true 打开
     */
    private void EnableSpeaker(boolean enable) {
        int curRoute = AudioRouter.ROUTE_INVALID;
        if (null != audioManager) {
            boolean isMicrophoneMute = null == audioManager ? false : audioManager.isMicrophoneMute();
            boolean isSpeakerphoneOn = null == audioManager ? false : audioManager.isSpeakerphoneOn();
            DoLog("changeEnableSpeaker enable:" + enable + ",this.route:" + this.route + ",isSpeakerphoneOn:" + isSpeakerphoneOn + ",isMicrophoneMute:" + isMicrophoneMute + ", mode=" + (null != this.audioManager ? this.audioManager.getMode() : "null"));
            if (isSpeakerphoneOn == enable) {
                curRoute = enable ? AudioRouter.ROUTE_SPEAKER : (this.route != AudioRouter.ROUTE_SPEAKER ? this.route : AudioRouter.ROUTE_INVALID);
            } else {
                if (!enable) {
                    audioManager.setSpeakerphoneOn(false);//关闭扬声器
                    audioManager.setRouting(AudioManager.MODE_NORMAL, AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
//            ((Activity)ctx).setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                    //把声音设定成Earpiece（听筒）出来，设定为正在通话中
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    curRoute = this.route != AudioRouter.ROUTE_SPEAKER ? this.route : AudioRouter.ROUTE_INVALID;
//            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                } else {
                    // 为true打开喇叭扩音器；为false关闭喇叭扩音器.
                    audioManager.setSpeakerphoneOn(true);
                    // 2016年06月18日 添加的代码，恢复系统声音设置
//                    audioManager.setMode(AudioManager.STREAM_SYSTEM);
                    audioManager.setMode(AudioManager.MODE_NORMAL);
//            ((Activity)ctx).setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
//            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    curRoute = AudioRouter.ROUTE_SPEAKER;
                }
            }
        } else {
            DoLogErr("changeEnableSpeaker null== audioManager ,enable:" + enable + ",this.route:" + this.route);
        }
        onPlaybackRouteChanged(curRoute);
    }

    public void registerMedia(String name, MediaPlayer mp, boolean mixing, boolean incall, int intensity) {
        DoLog("registerMedia name:"+name);
        MediamgrPlay mediamgrPlay = new MediamgrPlay(name, mp, (boolean) mixing, (boolean) incall, intensity);
        mapMediamgrs.put(name, mediamgrPlay);
    }

    public void unregisterMedia(String name) {
        DoLog("unregisterMedia name:"+name);
        MediamgrPlay mediamgrPlay = getCurMediamgrPlay(name);
        if (null != mediamgrPlay) {
            mediamgrPlay.unregisterMedia();
        }
    }

    private MediamgrPlay getCurMediamgrPlay(String name){
        if(!mapMediamgrs.isEmpty() && mapMediamgrs.containsKey(name)){
            return mapMediamgrs.get(name);
        }
        return null;
    }

    private MediamgrPlay.AudioRouterCallback audioRouterCallback = new MediamgrPlay.AudioRouterCallback(){
        @Override
        public void onAudioRouteChanged(int route) {
            DoLog("audioRouterCallback onAudioRouteChanged route:"+route);
            onPlaybackRouteChanged(route);
        }
    };

}