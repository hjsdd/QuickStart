package com.ss.video.rtc.demo.quickstart;

import com.ss.bytertc.engine.live.ByteRTCStreamMixingType;
import com.ss.bytertc.engine.live.LiveTranscoding;

public class VideoTranscoding {
    final String pushUrl = "rtmp://push.hafun.xyz/live/test?expire=1709212545&sign=d04e0315d7f325c25efe6c83c2ab8694";

    protected LiveTranscoding createPK1v1LiveTranscodingConfig(String roomId, String userId, String coHostUserId) {

        LiveTranscoding liveTranscoding = LiveTranscoding.getDefualtLiveTranscode();
        liveTranscoding.setRoomId(roomId);
        liveTranscoding.setUrl(pushUrl);
        liveTranscoding.setMixType(ByteRTCStreamMixingType.STREAM_MIXING_BY_SERVER);

        LiveTranscoding.VideoConfig videoConfig = liveTranscoding.getVideo()
                .setWidth(720)
                .setHeight(1080)
                .setFps(15)
                .setKBitRate(1600);
        liveTranscoding.setVideo(videoConfig);

        LiveTranscoding.AudioConfig audioConfig = liveTranscoding.getAudio()
                .setSampleRate(44100)
                .setChannels(2);
        liveTranscoding.setAudio(audioConfig);

        LiveTranscoding.Layout.Builder layoutBuilder = new LiveTranscoding.Layout.Builder();

        LiveTranscoding.Region selfRegion = new LiveTranscoding.Region()
                .uid(userId)
                .setLocalUser(true)
                .roomId(roomId)
                .position(0, 0.25)
                .size(0.5, 0.5)
                .alpha(1)
                .zorder(0);
        layoutBuilder.addRegion(selfRegion);

        LiveTranscoding.Region hostRegion = new LiveTranscoding.Region()
                .uid(coHostUserId)
                .roomId(roomId)
                .position(0.5, 0.25)
                .size(0.5, 0.5)
                .alpha(1)
                .zorder(0);
        layoutBuilder.addRegion(hostRegion);
        liveTranscoding.setLayout(layoutBuilder.builder());
        return liveTranscoding;
    }

    protected LiveTranscoding createSingleLiveTranscodingConfig(String roomId, String userId) {

        LiveTranscoding liveTranscoding = LiveTranscoding.getDefualtLiveTranscode();
        // set room id
        liveTranscoding.setRoomId(roomId);
        // Set the live address of push stream
        liveTranscoding.setUrl(pushUrl);
        // Set the merge mode, 0 means server merge
        liveTranscoding.setMixType(ByteRTCStreamMixingType.STREAM_MIXING_BY_SERVER);
        LiveTranscoding.VideoConfig videoConfig = liveTranscoding.getVideo()
                .setWidth(720)
                .setHeight(1080)
                .setFps(15)
                .setKBitRate(1600);
        liveTranscoding.setVideo(videoConfig);
        // Set the live transcoding audio parameters, the specific parameters depend on the situation
        LiveTranscoding.AudioConfig audioConfig = liveTranscoding.getAudio()
                .setSampleRate(44100)
                .setChannels(2);
        liveTranscoding.setAudio(audioConfig);
        // Set live transcoding video layout parameters
        LiveTranscoding.Region region = new LiveTranscoding.Region()
                .uid(userId)
                .setLocalUser(true)
                .roomId(roomId)
                .position(0, 0)
                .size(1, 1)
                .alpha(1)
                .zorder(0)
                .renderMode(LiveTranscoding.TranscoderRenderMode.RENDER_HIDDEN);

        LiveTranscoding.Layout layout = new LiveTranscoding.Layout.Builder()
                .addRegion(region)
                .builder();
        liveTranscoding.setLayout(layout);

        return liveTranscoding;
    }
}
