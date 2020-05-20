package org.mediasoup.droid.lib.webrtc;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.webrtc.EglBase;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoEncoderFallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class CustomVideoEncoderFactory implements VideoEncoderFactory {
    public final VideoEncoderFactory hardwareVideoEncoderFactory;
    private final VideoEncoderFactory softwareVideoEncoderFactory = new SoftwareVideoEncoderFactory();
    private Map<String, SoftwareHardwareEncoder> softwareHardware = new HashMap<>();

    public CustomVideoEncoderFactory(EglBase.Context eglContext, boolean enableIntelVp8Encoder, boolean enableH264HighProfile) {
        this.hardwareVideoEncoderFactory = new HardwareVideoEncoderFactory(eglContext, enableIntelVp8Encoder, enableH264HighProfile);
    }

    CustomVideoEncoderFactory(VideoEncoderFactory hardwareVideoEncoderFactory) {
        this.hardwareVideoEncoderFactory = hardwareVideoEncoderFactory;
    }

    @Nullable
    public VideoEncoder createEncoder(VideoCodecInfo info) {
        VideoEncoder softwareEncoder = this.softwareVideoEncoderFactory.createEncoder(info);
        VideoEncoder hardwareEncoder = this.hardwareVideoEncoderFactory.createEncoder(info);
        String mapKey = getVideoType(info);
        softwareHardware.remove(mapKey);
        if (hardwareEncoder != null && softwareEncoder != null) {
            softwareHardware.put(mapKey, new SoftwareHardwareEncoder(softwareEncoder, hardwareEncoder));
            return new VideoEncoderFallback(softwareEncoder, hardwareEncoder);
        } else {
            return hardwareEncoder != null ? hardwareEncoder : softwareEncoder;
        }
    }

    public VideoCodecInfo[] getSupportedCodecs() {
        LinkedHashSet<VideoCodecInfo> supportedCodecInfos = new LinkedHashSet();
        supportedCodecInfos.addAll(Arrays.asList(this.softwareVideoEncoderFactory.getSupportedCodecs()));
        supportedCodecInfos.addAll(Arrays.asList(this.hardwareVideoEncoderFactory.getSupportedCodecs()));
        return (VideoCodecInfo[]) supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);
    }

    public VideoEncoder getSoftwareEncoder(VideoCodecInfo info) {
        String mapKey = getVideoType(info);
        SoftwareHardwareEncoder encoder = softwareHardware.containsKey(mapKey) ? softwareHardware.get(mapKey) : null;
        return null == encoder ? null : encoder.getSoftwareEncoder();
    }

    public VideoEncoder getHardwareEncoder(VideoCodecInfo info) {
        String mapKey = getVideoType(info);
        SoftwareHardwareEncoder encoder = softwareHardware.containsKey(mapKey) ? softwareHardware.get(mapKey) : null;
        return null == encoder ? null : encoder.getHardwareEncoder();
    }

    public String getVideoType(VideoCodecInfo info) {
        String name = info.name;
        Map<String, String> params = info.params;
        int length = null == params ? 0 : params.size();
        String levelId = (length > 0 && params.containsKey("profile-level-id")) ? params.get("profile-level-id") : "";
        return TextUtils.isEmpty(levelId) ? name : (name + "-" + levelId);
    }

    class SoftwareHardwareEncoder {
        private VideoEncoder softwareEncoder;
        private VideoEncoder hardwareEncoder;

        public SoftwareHardwareEncoder(VideoEncoder softwareEncoder, VideoEncoder hardwareEncoder) {
            this.softwareEncoder = softwareEncoder;
            this.hardwareEncoder = hardwareEncoder;
        }

        public VideoEncoder getSoftwareEncoder() {
            return softwareEncoder;
        }

        public VideoEncoder getHardwareEncoder() {
            return hardwareEncoder;
        }
    }
}
