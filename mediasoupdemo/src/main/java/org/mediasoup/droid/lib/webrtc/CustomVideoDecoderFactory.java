package org.mediasoup.droid.lib.webrtc;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.webrtc.EglBase;
import org.webrtc.HardwareVideoDecoderFactory;
import org.webrtc.PlatformSoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoDecoder;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoDecoderFallback;
import org.webrtc.VideoEncoder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class CustomVideoDecoderFactory implements VideoDecoderFactory {
    private final VideoDecoderFactory hardwareVideoDecoderFactory;
    private final VideoDecoderFactory softwareVideoDecoderFactory = new SoftwareVideoDecoderFactory();
    @Nullable
    private final VideoDecoderFactory platformSoftwareVideoDecoderFactory;
    private Map<String, SoftwareHardwareDecoder> softwareHardware = new HashMap<>();

    public CustomVideoDecoderFactory(@Nullable EglBase.Context eglContext) {
        this.hardwareVideoDecoderFactory = new HardwareVideoDecoderFactory(eglContext);
        this.platformSoftwareVideoDecoderFactory = new PlatformSoftwareVideoDecoderFactory(eglContext);
    }

    CustomVideoDecoderFactory(VideoDecoderFactory hardwareVideoDecoderFactory) {
        this.hardwareVideoDecoderFactory = hardwareVideoDecoderFactory;
        this.platformSoftwareVideoDecoderFactory = null;
    }

    @Nullable
    public VideoDecoder createDecoder(VideoCodecInfo codecType) {
        VideoDecoder softwareDecoder = this.softwareVideoDecoderFactory.createDecoder(codecType);
        VideoDecoder hardwareDecoder = this.hardwareVideoDecoderFactory.createDecoder(codecType);
        if (softwareDecoder == null && this.platformSoftwareVideoDecoderFactory != null) {
            softwareDecoder = this.platformSoftwareVideoDecoderFactory.createDecoder(codecType);
        }
        String mapKey = getVideoType(codecType);
        softwareHardware.remove(mapKey);
        if (hardwareDecoder != null && softwareDecoder != null) {
            softwareHardware.put(mapKey, new SoftwareHardwareDecoder(softwareDecoder, hardwareDecoder));
            return new VideoDecoderFallback(softwareDecoder, hardwareDecoder);
        } else {
            return hardwareDecoder != null ? hardwareDecoder : softwareDecoder;
        }
    }

    public VideoCodecInfo[] getSupportedCodecs() {
        LinkedHashSet<VideoCodecInfo> supportedCodecInfos = new LinkedHashSet();
        supportedCodecInfos.addAll(Arrays.asList(this.softwareVideoDecoderFactory.getSupportedCodecs()));
        supportedCodecInfos.addAll(Arrays.asList(this.hardwareVideoDecoderFactory.getSupportedCodecs()));
        if (this.platformSoftwareVideoDecoderFactory != null) {
            supportedCodecInfos.addAll(Arrays.asList(this.platformSoftwareVideoDecoderFactory.getSupportedCodecs()));
        }

        return (VideoCodecInfo[]) supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);
    }

    public VideoDecoder getSoftwareDecoder(VideoCodecInfo codecType) {
        String mapKey = getVideoType(codecType);
        SoftwareHardwareDecoder decoder = softwareHardware.containsKey(mapKey) ? softwareHardware.get(mapKey) : null;
        return null == decoder ? null : decoder.getSoftwareDecoder();
    }

    public VideoDecoder getHardwareDecoder(VideoCodecInfo codecType) {
        String mapKey = getVideoType(codecType);
        SoftwareHardwareDecoder decoder = softwareHardware.containsKey(mapKey) ? softwareHardware.get(mapKey) : null;
        return null == decoder ? null : decoder.getHardwareDecoder();
    }

    public String getVideoType(VideoCodecInfo codecType) {
        String name = codecType.name;
        Map<String, String> params = codecType.params;
        int length = null == params ? 0 : params.size();
        String levelId = (length > 0 && params.containsKey("profile-level-id")) ? params.get("profile-level-id") : "";
        return TextUtils.isEmpty(levelId) ? name : (name + "-" + levelId);
    }

    class SoftwareHardwareDecoder {
        private VideoDecoder softwareDecoder;
        private VideoDecoder hardwareDecoder;

        public SoftwareHardwareDecoder(VideoDecoder softwareDecoder, VideoDecoder hardwareDecoder) {
            this.softwareDecoder = softwareDecoder;
            this.hardwareDecoder = hardwareDecoder;
        }

        public VideoDecoder getSoftwareDecoder() {
            return softwareDecoder;
        }

        public VideoDecoder getHardwareDecoder() {
            return hardwareDecoder;
        }
    }
}

