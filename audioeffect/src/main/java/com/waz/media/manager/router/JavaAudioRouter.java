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
package com.waz.media.manager.router;


import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.Log;
import com.waz.media.manager.MediamgrPlay;

import java.util.List;

public class JavaAudioRouter {
    private Context _context = null;
    private AudioManager _audio_manager = null;

    private boolean _shouldMuteIncomingSound = false;
    private boolean _shouldMuteOutgoingSound = false;

    private boolean _shouldPreferLoudSpeaker = false;
    private boolean _isAudioFocusRequested = false;

    private OnAudioFocusChangeListener _afListener = null;

    // Set to true to enable debug logs.
    private static final boolean DEBUG = true;

    // wired HS defines
    private static final int STATE_WIRED_HS_INVALID = -1;
    private static final int STATE_WIRED_HS_UNPLUGGED = 0;
    private static final int STATE_WIRED_HS_PLUGGED = 1;

    // Audio Route defines
    public static final int ROUTE_INVALID = -1;
    public static final int ROUTE_EARPIECE = 0;
    public static final int ROUTE_SPEAKER = 1;
    public static final int ROUTE_HEADSET = 2;
    public static final int ROUTE_BT = 3;


    private BluetoothHeadset bluetoothHeadset = null;
    private BluetoothA2dp bluetoothA2dp = null;
    private static BluetoothDevice bluetoothDevice = null;

    private boolean btScoConnected = false;
    //private int _BluetoothScoState = STATE_BLUETOOTH_SCO_INVALID;

    // Stores the audio states for a wired headset
    private int _WiredHsState = STATE_WIRED_HS_UNPLUGGED;

    private MediamgrPlay.AudioRouterCallback _javaListener = null;
    private boolean isHeadsetConnected;
    private boolean isBTDeviceConnected;

    private static boolean runningOnJellyBeanOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    private static boolean runningOnJellyBeanMR1OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    private static boolean runningOnJellyBeanMR2OrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    private boolean _inCall = false;

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    public JavaAudioRouter(Context context, MediamgrPlay.AudioRouterCallback javaListener) {
        this._context = context;
        this._javaListener = javaListener;

        Log.i(logTag, "initAudioRouter: incall=" + _inCall + ", _javaListener:" + _javaListener);

        if (context != null) {
            _audio_manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        this.subscribeToRouteUpdates();

        try {
            setupBluetooth();
            registerForBluetoothScoIntentBroadcast(); // Where should we Unregister ??
            registerForWiredHeadsetIntentBroadcast();
        } catch (Exception e) {
            // Bluetooth might not be supported on emulator
        }

        _afListener = new OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                DoLog("DVA: On Audio Focus Change");
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private void btHeadsetService(BluetoothHeadset btHeadset) {
        List<BluetoothDevice> devices;

        bluetoothHeadset = btHeadset;

        devices = bluetoothHeadset.getConnectedDevices();
        Log.i(logTag, "btHeadsetService");
        if (!devices.isEmpty())
            bluetoothDevice = devices.get(0);
        if (bluetoothDevice != null)
            nativeBTDeviceConnected(true);

        Context context = this._context;
        IntentFilter filter = new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);

        /** Receiver which handles changes in BT headset availability. */
        BroadcastReceiver btReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                        BluetoothHeadset.STATE_DISCONNECTED);
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i(logTag, "bluetoothHeadset: connected: btdev=" + bluetoothDevice);
                    nativeBTDeviceConnected(true);
                } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                    Log.i(logTag, "bluetoothHeadset: disconnected");
                    bluetoothDevice = null;
                    int route = GetAudioRoute();
                    if (route == ROUTE_SPEAKER)
                        EnableSpeaker();
                    nativeBTDeviceConnected(false);
                }
            }
        };

        context.registerReceiver(btReceiver, filter);
    }

    private void btA2dpService(BluetoothA2dp btA2dp) {
        List<BluetoothDevice> devices;

        bluetoothA2dp = btA2dp;
        Log.i(logTag, "btA2dpService");
        devices = bluetoothA2dp.getConnectedDevices();
        if (!devices.isEmpty())
            bluetoothDevice = devices.get(0);

        Context context = this._context;
        IntentFilter filter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);

        /** Receiver which handles changes in BT a2dp availability. */
        BroadcastReceiver btReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE,
                        BluetoothA2dp.STATE_DISCONNECTED);
                if (state == BluetoothA2dp.STATE_CONNECTED) {
                    bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i(logTag, "bluetoothA2dp: connected: btdev=" + bluetoothDevice);
                    nativeBTDeviceConnected(true);
                } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                    Log.i(logTag, "bluetootA2dp: disconnected");
                    bluetoothDevice = null;
                    nativeBTDeviceConnected(false);
                }
            }
        };

        context.registerReceiver(btReceiver, filter);
    }

    private void setupBluetooth() {
        // Get the default adapter
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.w(logTag, "bluetooth: no BT adapter present\n");
            return;
        }
        Log.i(logTag, "setupBluetooth");
        BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                Log.i(logTag, "bluetooth: service connected for profile: " + profile);
                if (profile == BluetoothProfile.HEADSET) {
                    Log.i(logTag, "bluetooth: HEADSET connected");
                    btHeadsetService((BluetoothHeadset) proxy);
                }
                if (profile == BluetoothProfile.A2DP) {
                    Log.i(logTag, "bluetooth: A2DP connected");
                    bluetoothA2dp = (BluetoothA2dp) proxy;
                }
            }

            public void onServiceDisconnected(int profile) {
                Log.i(logTag, "bluetooth: service disconnected for profile: " + profile);
                if (profile == BluetoothProfile.HEADSET) {
                    Log.i(logTag, "bluetooth: HEADSET disconnected");
                    bluetoothHeadset = null;
                }
                if (profile == BluetoothProfile.A2DP) {
                    Log.i(logTag, "bluetooth: A2DP disconnected");
                    bluetoothA2dp = null;
                }
            }
        };

        btAdapter.getProfileProxy(this._context, profileListener, BluetoothProfile.HEADSET);
        btAdapter.getProfileProxy(this._context, profileListener, BluetoothProfile.A2DP);
    }


    private int startBluetooth() {
        Log.i(logTag, "startBluetooth bluetoothDevice:" + bluetoothDevice);
        if (bluetoothDevice == null || null == _audio_manager) {
            return -1;
        } else {
            int n = 5;

            _audio_manager.startBluetoothSco();
            while (!btScoConnected && n > 0) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                n--;
            }
            Log.i(logTag, "startBluetooth: scoConnected=" + btScoConnected);
            if (btScoConnected) {
                if (_audio_manager.isSpeakerphoneOn())
                    _audio_manager.setSpeakerphoneOn(false);
                _audio_manager.setBluetoothScoOn(true);
            }

            return 0;
        }
    }

    private void stopBluetooth() {
        Log.i(logTag, "stopBluetooth _audio_manager:" + _audio_manager);
        if (null == _audio_manager) {
            return;
        }
        _audio_manager.stopBluetoothSco();
        _audio_manager.setBluetoothScoOn(false);

    }

    public int EnableSpeaker() {
        DoLog("EnableSpeaker ");
        if (null == _audio_manager) {
            return 0;
        }
        if (_audio_manager.isBluetoothScoOn())
            stopBluetooth();

        _audio_manager.setSpeakerphoneOn(true);

        return 0;
    }

    public int EnableHeadset() {
        DoLog("EnableHeadset ");
        if (null == _audio_manager) {
            return 0;
        }
        if (_audio_manager.isBluetoothScoOn())
            stopBluetooth();

        _audio_manager.setSpeakerphoneOn(false);

        return 0;
    }

    public int EnableEarpiece() {
        DoLog("EnableEarpiece ");
        if (!hasEarpiece() || null == _audio_manager) {
            return -1;
        }
        int cur_route = GetAudioRoute();
        if (cur_route == ROUTE_HEADSET) {
            // Cannot use Earpiece when a HS is plugged in
            return -1;
        }
        if (bluetoothDevice != null)
            _audio_manager.setBluetoothScoOn(true);
        else
            _audio_manager.setSpeakerphoneOn(false);

        return 0;
    }

    public int EnableBTSco() {
        Log.i(logTag, "EnableBTSco: incall=" + _inCall);
        if (_inCall)
            return startBluetooth();
        else
            return -1;
    }


    public int GetAudioRoute() {
        int route = ROUTE_INVALID;
        if (null == _audio_manager) {
            return route;
        }
        if (_audio_manager.isBluetoothScoOn()) {
            route = ROUTE_BT;
            DoLog("GetAudioRoute() BT");
        } else if (_audio_manager.isSpeakerphoneOn()) {
            route = ROUTE_SPEAKER;
            DoLog("GetAudioRoute() Speaker");
        } else if (_WiredHsState == STATE_WIRED_HS_PLUGGED) {
            route = ROUTE_HEADSET;
            DoLog("GetAudioRoute() Headset");
        } else {
            if (hasEarpiece()) {
                route = ROUTE_EARPIECE;
                DoLog("GetAudioRoute() Earpiece");
            } else {
                route = ROUTE_SPEAKER; /* Goes here if a tablet where iSpaekerPhoneOn dosnt tell the truth  */
                DoLog("GetAudioRoute() Speaker \n");
            }
        }
        return route;
    }

    private void UpdateRoute() {
        int route;

        route = GetAudioRoute();
        Log.i(logTag, "UpdateRoute route:" + route);
        // call route change callback
        nativeUpdateRoute(route);
    }

    public void OnStartingCall() {
        Log.i(logTag, "OnStartingCall: incall=" + _inCall);
        if (!_inCall) {
            Log.i(logTag, "OnStartingCall btdev=" + bluetoothDevice);
            _inCall = true;
            if (bluetoothDevice != null && null != _audio_manager) {
                Log.i(logTag, "OnStartingCall: startingBluetooth: scoOn: " + _audio_manager.isBluetoothScoOn());
                startBluetooth();
            } else {
                // Enable earpiece
            }
        }
    }

    public void OnStoppingCall() {
        Log.i(logTag, "OnStoppingCall incall=" + _inCall);

        _inCall = false;
        if (null != _audio_manager && _audio_manager.getMode() != AudioManager.MODE_NORMAL) {
            _audio_manager.setMode(AudioManager.MODE_NORMAL);
        }
        stopBluetooth();
    }

    private void subscribeToRouteUpdates() {
        // TODO: should somehow subscribe to OS route changed updates
    }

    private void registerForWiredHeadsetIntentBroadcast() {
        Context context = this._context;

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);

        /** Receiver which handles changes in wired headset availability. */
        BroadcastReceiver hsReceiver = new BroadcastReceiver() {
            private static final int STATE_UNPLUGGED = 0;
            private static final int STATE_PLUGGED = 1;
            private static final int HAS_NO_MIC = 0;
            private static final int HAS_MIC = 1;

            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra("state", STATE_UNPLUGGED);

                int microphone = intent.getIntExtra("microphone", HAS_NO_MIC);
                String name = intent.getStringExtra("name");
                DoLog("WiredHsBroadcastReceiver.onReceive: a=" + intent.getAction()
                        + ", s=" + state
                        + ", m=" + microphone
                        + ", n=" + name
                        + ", sb=" + isInitialStickyBroadcast());

                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

                int route = GetAudioRoute();

                switch (state) {
                    case STATE_UNPLUGGED:
                        DoLog("Headset Unplugged");
                        _WiredHsState = STATE_WIRED_HS_UNPLUGGED;
                        if (null != _audio_manager && btScoConnected) {
                            _audio_manager.setBluetoothScoOn(true);
                        }
                        if (route == ROUTE_SPEAKER)
                            EnableSpeaker();
                        nativeHeadsetConnected(false);
                        break;
                    case STATE_PLUGGED:
                        DoLog("Headset plugged");
                        _WiredHsState = STATE_WIRED_HS_PLUGGED;
                        nativeHeadsetConnected(true);
                        break;
                    default:
                        DoLog("Invalid state");
                        _WiredHsState = STATE_WIRED_HS_INVALID;
                        break;
                }
                UpdateRoute();
            }
        };

        // Note: the intent we register for here is sticky, so it'll tell us
        // immediately what the last action was (plugged or unplugged).
        // It will enable us to set the speakerphone correctly.
        context.registerReceiver(hsReceiver, filter);
    }


    /**
     * Registers receiver for the broadcasted intent related the existence
     * of a BT SCO channel. Indicates if BT SCO streaming is on or off.
     */
    private void registerForBluetoothScoIntentBroadcast() {
        Context context = this._context;

        IntentFilter filter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);

        /** BroadcastReceiver implementation which handles changes in BT SCO. */
        BroadcastReceiver btReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(
                        AudioManager.EXTRA_SCO_AUDIO_STATE,
                        AudioManager.SCO_AUDIO_STATE_DISCONNECTED);

                Log.i(logTag, "ScoBroadcastReceiver.onReceive: a=" + intent.getAction() +
                        ", s=" + state +
                        ", sb=" + isInitialStickyBroadcast());

                switch (state) {
                    case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                        Log.i(logTag, "SCO_AUDIO_STATE_CONNECTED");
                        btScoConnected = true;
                        break;

                    case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                        Log.i(logTag, "SCO_AUDIO_STATE_DISCONNECTED");
                        btScoConnected = false;
                        break;

                    case AudioManager.SCO_AUDIO_STATE_CONNECTING:
                        Log.i(logTag, "SCO_AUDIO_STATE_CONNECTING");
                        break;

                    default:
                        DoLogErr("Invalid state!");
                }
                //UpdateRoute();
            }
        };

        context.registerReceiver(btReceiver, filter);
    }

	/*
    private void unregisterForBluetoothScoIntentBroadcast() {
        Context context = this._context;

        context.unregisterReceiver(_BluetoothScoReceiver);
        _BluetoothScoReceiver = null;
	}
	*/

    private boolean hasEarpiece() {
        Context context = this._context;
        return (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY));
    }

    final String logTag = "avs JavaAudioRouter";

    private void DoLog(String msg) {
        if (DEBUG) {
            Log.d(logTag, msg);
        }
    }

    private void DoLogErr(String msg) {
        Log.e(logTag, msg);
    }

    private void nativeUpdateRoute(int route) {
        Log.i(logTag, "nativeUpdateRoute _javaListener:" + _javaListener + ", route:" + route);
        if (null != _javaListener) {
            _javaListener.onAudioRouteChanged(route);
        }
    }

    private void nativeHeadsetConnected(boolean connected) {
        Log.i(logTag, "nativeHeadsetConnected _javaListener:" + _javaListener + ", connected:" + connected);
        this.isHeadsetConnected = connected;
    }

    private void nativeBTDeviceConnected(boolean connected) {
        Log.i(logTag, "nativeBTDeviceConnected _javaListener:" + _javaListener + ", connected:" + connected);
        this.isBTDeviceConnected = connected;
    }

    public boolean isHeadsetConnected() {
        return isHeadsetConnected;
    }

    public boolean isBTDeviceConnected() {
        return isBTDeviceConnected;
    }
}
