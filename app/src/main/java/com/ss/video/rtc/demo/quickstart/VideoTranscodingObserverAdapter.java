package com.ss.video.rtc.demo.quickstart;

import android.util.Log;

import com.ss.bytertc.engine.live.ByteRTCStreamMixingEvent;
import com.ss.bytertc.engine.live.ByteRTCStreamMixingType;
import com.ss.bytertc.engine.live.ByteRTCTranscoderErrorCode;
import com.ss.bytertc.engine.live.ILiveTranscodingObserver;

import org.webrtc.VideoFrame;

public class VideoTranscodingObserverAdapter implements ILiveTranscodingObserver {
    private static final String TAG = "TranscodingObAdapter";

    @Override
    public boolean isSupportClientPushStream() {
        return false;
    }

    @Override
    public void onStreamMixingEvent(ByteRTCStreamMixingEvent eventType, String taskId, ByteRTCTranscoderErrorCode error, ByteRTCStreamMixingType mixType) {
        Log.d("TranscodingObserver","eventType: "+ eventType.toString());
        Log.d("TranscodingObserver","error: "+ error.toString());

    }

    @Override
    public void onMixingAudioFrame(String taskId, byte[] audioFrame, int frameNum) {

    }

    @Override
    public void onMixingVideoFrame(String taskId, VideoFrame videoFrame) {

    }

    @Override
    public void onDataFrame(String taskId, byte[] dataFrame, long time) {

    }
}
