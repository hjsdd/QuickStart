package com.ss.video.rtc.demo.quickstart;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ss.bytertc.engine.IAudioFrameObserver;
import com.ss.bytertc.engine.RTCEngine;
import com.ss.bytertc.engine.RTCRoom;
import com.ss.bytertc.engine.RTCRoomConfig;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.UserInfo;
import com.ss.bytertc.engine.VideoCanvas;
import com.ss.bytertc.engine.VideoEncoderConfig;
import com.ss.bytertc.engine.data.AudioChannel;
import com.ss.bytertc.engine.data.AudioFormat;
import com.ss.bytertc.engine.data.AudioFrameCallbackMethod;
import com.ss.bytertc.engine.data.AudioRoute;
import com.ss.bytertc.engine.data.AudioSampleRate;
import com.ss.bytertc.engine.data.CameraId;
import com.ss.bytertc.engine.data.RemoteStreamKey;
import com.ss.bytertc.engine.data.StreamIndex;
import com.ss.bytertc.engine.data.VideoFrameInfo;
import com.ss.bytertc.engine.handler.IRTCEngineEventHandler;
import com.ss.bytertc.engine.handler.IRTCVideoEventHandler;
import com.ss.bytertc.engine.live.ByteRTCStreamMixingEvent;
import com.ss.bytertc.engine.live.ByteRTCStreamMixingType;
import com.ss.bytertc.engine.live.ByteRTCTranscoderErrorCode;
import com.ss.bytertc.engine.live.ILiveTranscodingObserver;
import com.ss.bytertc.engine.live.LiveTranscoding;
import com.ss.bytertc.engine.type.AudioProfileType;
import com.ss.bytertc.engine.type.ChannelProfile;
import com.ss.bytertc.engine.type.MediaDeviceError;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.VideoDeviceType;
import com.ss.bytertc.engine.utils.IAudioFrame;
import com.ss.rtc.demo.quickstart.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.VideoFrame;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
//import okhttp3.Call;
import okhttp3.Callback;

//import com.google.gson.Gson;
/**
 * VolcEngineRTC 视频通话的主页面
 * 本示例不限制房间内最大用户数；同时最多渲染四个用户的视频数据（自己和三个远端用户视频数据）；
 *
 * 包含如下简单功能：
 * - 创建引擎
 * - 设置视频发布参数
 * - 渲染自己的视频数据
 * - 创建房间
 * - 加入音视频通话房间
 * - 打开/关闭麦克风
 * - 打开/关闭摄像头
 * - 渲染远端用户的视频数据
 * - 离开房间
 * - 销毁引擎
 *
 * 实现一个基本的音视频通话的流程如下：
 * 1.创建 IRTCVideo 实例。
 *   RTCVideo.createRTCVideo(Context context, String appId, IRTCVideoEventHandler handler,
 *     Object eglContext, JSONObject parameters)
 * 2.视频发布端设置期望发布的最大分辨率视频流参数，包括分辨率、帧率、码率、缩放模式、网络不佳时的回退策略等。
 *   RTCVideo.setVideoEncoderConfig(VideoEncoderConfig maxSolution)
 * 3.开启本地视频采集。 RTCVideo.startVideoCapture()
 * 4.设置本地视频渲染时，使用的视图，并设置渲染模式。
 *   RTCVideo.setLocalVideoCanvas(StreamIndex streamIndex, VideoCanvas videoCanvas)
 * 5.创建房间。RTCVideo.createRTCRoom(String roomId)
 * 6.加入音视频通话房间。
 *   RTCRoom.joinRoom(String token, UserInfo userInfo, RTCRoomConfig roomConfig)
 * 7.SDK 接收并解码远端视频流首帧后，设置用户的视频渲染视图。
 *   RTCVideo.setRemoteVideoCanvas(String userId, StreamIndex streamIndex, VideoCanvas videoCanvas)
 * 8.在用户离开房间之后移出用户的视频渲染视图。
 * 9.离开音视频通话房间。RTCRoom.leaveRoom()
 * 10.调用 RTCRoom.destroy() 销毁房间对象。
 * 11.调用 RTCVideo.destroyRTCVideo() 销毁引擎。
 *
 * 详细的API文档参见{https://www.volcengine.com/docs/6348/70080}
 */
public class RTCRoomActivity extends AppCompatActivity {

    private ImageView mSpeakerIv;
    private ImageView mAudioIv;
    private ImageView mVideoIv;

    private boolean mIsSpeakerPhone = true;
    private boolean mIsMuteAudio = false;
    private boolean mIsMuteVideo = false;
    private CameraId mCameraID = CameraId.CAMERA_ID_FRONT;

    private FrameLayout mSelfContainer;
    private final FrameLayout[] mRemoteContainerArray = new FrameLayout[3];
    private final TextView[] mUserIdTvArray = new TextView[3];
    private final String[] mShowUidArray = new String[3];

    private RTCVideo mRTCVideo;
    private RTCRoom mRTCRoom;
    //added by Connor

    private Switch mliveTranscodingIv;
    private String localUserId="";
    private String mRoomId="";
    private String token="";
    private int joinRoomRes = 1;
    private TextView self_onRoomStateChanged;

    private boolean mIsRTCTranscoding = false;

    private final ILiveTranscodingObserver mILiveTranscodingObserver = new ILiveTranscodingObserver(){

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
    };

    private IAudioFrameObserver mAudioFrameObserver = new IAudioFrameObserver() {
        @Override
        public void onRecordAudioFrame(IAudioFrame audioFrame) {

            Log.d("IAudioFrameObserver", "audioFrame: ");
        }

        @Override
        public void onPlaybackAudioFrame(IAudioFrame audioFrame) {

        }

        @Override
        public void onRemoteUserAudioFrame(RemoteStreamKey stream_info, IAudioFrame audioFrame) {

        }

        @Override
        public void onMixedAudioFrame(IAudioFrame audioFrame) {

        }
    };
    private RTCRoomEventHandlerAdapter mIRtcRoomEventHandler = new RTCRoomEventHandlerAdapter() {

        /**
         * 远端主播角色用户加入房间回调。
         */
        @Override
        public void onRoomStateChanged(String roomId, String uid, int state, String extraInfo) {
            super.onRoomStateChanged(roomId, uid, state, extraInfo);
            self_onRoomStateChanged.setText(String.format("onRoomState:%s",state ));
            Log.d("IRTCRoomEventHandler", "onRoomStateChanged: " + state);
        }
        @Override
        public void onUserJoined(UserInfo userInfo, int elapsed) {
            super.onUserJoined(userInfo, elapsed);
            Log.d("IRTCRoomEventHandler", "onUserJoined: " + userInfo.getUid());
        }

        /**
         * 远端用户离开房间回调。
         */
        @Override
        public void onUserLeave(String uid, int reason) {
            super.onUserLeave(uid, reason);
            Log.d("IRTCRoomEventHandler", "onUserLeave: " + uid);
            runOnUiThread(() -> removeRemoteView(uid));

            startOrStopRTCTranscoding();
        }
    };

    private IRTCVideoEventHandler mIRtcVideoEventHandler = new IRTCVideoEventHandler() {

        /*
        * onVideoDeviceStateChanged 处理
        * */
        public void onVideoDeviceStateChanged(String device_id, VideoDeviceType device_type, int device_state, int device_error) {
            super.onVideoDeviceStateChanged(device_id,device_type,device_state,device_error);
            if (device_error == MediaDeviceError.MEDIA_DEVICE_ERROR_DEVICEDISCONNECTED) {
                mRTCVideo.startVideoCapture();
                mRTCVideo.startAudioCapture();
            }
        }
        /**
         * SDK收到第一帧远端视频解码数据后，用户收到此回调。
        */
        @Override
        public void onFirstRemoteVideoFrameDecoded(RemoteStreamKey remoteStreamKey, VideoFrameInfo frameInfo) {
            super.onFirstRemoteVideoFrameDecoded(remoteStreamKey, frameInfo);
            Log.d("IRTCVideoEventHandler", "onFirstRemoteVideoFrame: " + remoteStreamKey.toString());
            runOnUiThread(() -> setRemoteView(remoteStreamKey.getRoomId(), remoteStreamKey.getUserId()));
            if (mliveTranscodingIv.isChecked()){
                startOrUpdateSingleRTCTranscoding(createPK1v1LiveTranscodingConfig(mRoomId,localUserId,remoteStreamKey.getUserId()));
            }//added by Connor
        }
        /* 公共流的首帧视频解码成功
        */
        @Override
        public void onFirstPublicStreamVideoFrameDecoded(String publicStreamId, VideoFrameInfo frameInfo) {
            super.onFirstPublicStreamVideoFrameDecoded(publicStreamId, frameInfo);
            Log.d("IRTCVideoEventHandler", "onFirstRemoteVideoFrame: " + publicStreamId.toString());
            runOnUiThread(() -> setPublicRemoteView(publicStreamId));
        }
        /**
         * 警告回调，详细可以看 {https://www.volcengine.com/docs/6348/70082#warncode}
         */
        @Override
        public void onWarning(int warn) {
            super.onWarning(warn);
            Log.d("IRTCVideoEventHandler", "onWarning: " + warn);
        }

        /**
         * 错误回调，详细可以看 {https://www.volcengine.com/docs/6348/70082#errorcode}
         */
        @Override
        public void onError(int err) {
            super.onError(err);
            Log.d("IRTCVideoEventHandler", "onError: " + err);
            showAlertDialog(String.format(Locale.US, "error: %d", err));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        Intent intent = getIntent();
        String roomId = intent.getStringExtra(Constants.ROOM_ID_EXTRA);
        String userId = intent.getStringExtra(Constants.USER_ID_EXTRA);

        localUserId = userId;
        mRoomId = roomId;

        initUI(roomId, userId);
        initEngineAndJoinRoom(roomId, userId);
        //startOrUpdateSingleRTCTranscoding(liveTranscoding);
        //mliveTranscodingIv.setOnClickListener((v) -> startOrUpdateSingleRTCTranscoding(liveTranscoding)); //adde by Connor on 2023/11/29

    }

    private void initUI(String roomId, String userId) {
        mSelfContainer = findViewById(R.id.self_video_container);
        mRemoteContainerArray[0] = findViewById(R.id.remote_video_0_container);
        mRemoteContainerArray[1] = findViewById(R.id.remote_video_1_container);
        mRemoteContainerArray[2] = findViewById(R.id.remote_video_2_container);
        mUserIdTvArray[0] = findViewById(R.id.remote_video_0_user_id_tv);
        mUserIdTvArray[1] = findViewById(R.id.remote_video_1_user_id_tv);
        mUserIdTvArray[2] = findViewById(R.id.remote_video_2_user_id_tv);

        findViewById(R.id.switch_camera).setOnClickListener((v) -> onSwitchCameraClick());
        mSpeakerIv = findViewById(R.id.switch_audio_router);
        mAudioIv = findViewById(R.id.switch_local_audio);
        mVideoIv = findViewById(R.id.switch_local_video);

        mliveTranscodingIv = findViewById(R.id.liveTranscoding);
        mliveTranscodingIv.setOnCheckedChangeListener((v , isChecked) -> startOrStopRTCTranscoding());

        //findViewById(R.id.hang_up).setOnClickListener((v) -> onBackPressed());//added by Connor
        findViewById(R.id.hang_up).setOnClickListener((v) -> finish());
        mSpeakerIv.setOnClickListener((v) -> updateSpeakerStatus());
        mAudioIv.setOnClickListener((v) -> updateLocalAudioStatus());
        mVideoIv.setOnClickListener((v) -> updateLocalVideoStatus());

        TextView roomIDTV = findViewById(R.id.room_id_text);
        TextView userIDTV = findViewById(R.id.self_video_user_id_tv);
        // self_onRoomStateChanged added by Connor
        self_onRoomStateChanged = findViewById(R.id.self_onRoomStateChanged);

        roomIDTV.setText(String.format("RoomID:%s", roomId));
        userIDTV.setText(String.format("UserID:%s", userId));
    }

    // 获取 RTC token
    private void fetchToken(String userID,String roomID, RTCRoomConfig roomConfig){
        Log.i("TAG", "something here: fetchToken" );
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {
            json.put("userID", userID);
            json.put("roomID", roomID);
            //json.put("role", tokenRole);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(JSON, String.valueOf(json));
        Request request = new Request.Builder()
                //.url("https://baidu.com")
                .url("http://10.37.39.134:8080/access_token")
                .header("Content-Type", "application/json; charset=UTF-8")
                .post(requestBody)
                .build();
        Log.i("TAG", "something here: sent request" );
        okhttp3.Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.i("TAG", "something here: send request failed"+ e.toString() );
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    Log.i("TAG", "response is Successful here:" );
                    Gson gson = new Gson();
                    String result = response.body().string();
                    Log.i("TAG", "something here: result is  "+ result);
                    Map map = gson.fromJson(result, Map.class);
                    Thread t2 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Log.i("TAG", "something here: token is 1  " );
                           token = map.get("token").toString();
                            Log.i("TAG", "something here: token is 2 "+ token );
                            // 如果用户未加入频道，使用 token 加入频道
                            if(joinRoomRes !=0 ) {
                                Log.i("TAG", "before joinRoom token : " + token);
                                joinRoomRes = mRTCRoom.joinRoom(token, UserInfo.create(userID, ""), roomConfig);

                            }else {
                                //Log.i("TAG", result);
                                mRTCRoom.updateToken(token);
                            }
                            Log.i("TAG", "initEngineAndJoinRoom: " + joinRoomRes);
                            //ChannelMediaOptions options = new ChannelMediaOptions();
                            //if (joined != 0){joined = mRtcEngine.joinChannel(token, channelName, 1234, options);}
                            // 如果用户已加入频道，调用 renewToken 重新生成 token
                           // else {mRtcEngine.renewToken(token);}
                        }
                    });
                    t2.start();
                }
            }
        });
    }

    private void initEngineAndJoinRoom(String roomId, String userId) {
        // 创建引擎
        JSONObject params = new JSONObject();
        JSONObject common_extra_info = new JSONObject();
        try {
            common_extra_info.put("app_version","app_version_value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            common_extra_info.put("app_channel","local_test");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            params.put("rtc.aid","0000");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            params.put("rtc.common_extra_info",common_extra_info.toString());
            Log.i("something here", common_extra_info.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        mRTCVideo = RTCVideo.createRTCVideo(getApplicationContext(), Constants.APPID, mIRtcVideoEventHandler, null, params);

        mRTCVideo.setRuntimeParameters(params);
        // 设置视频发布参数
        VideoEncoderConfig videoEncoderConfig = new VideoEncoderConfig(360, 640, 15, 800);
        mRTCVideo.setVideoEncoderConfig(videoEncoderConfig);
        setLocalRenderView(userId);
        // 开启本地视频采集
        mRTCVideo.startVideoCapture();
        // 开启本地音频采集
        mRTCVideo.startAudioCapture();
        // 加入房间
        mRTCRoom = mRTCVideo.createRTCRoom(roomId);
        mRTCRoom.setRTCRoomEventHandler(mIRtcRoomEventHandler);
        RTCRoomConfig roomConfig = new RTCRoomConfig(ChannelProfile.CHANNEL_PROFILE_COMMUNICATION,
                true, true, true);

        int joinRoomRes = mRTCRoom.joinRoom(Constants.TOKEN,
                UserInfo.create(userId, ""), roomConfig);
        Log.i("TAG", "initEngineAndJoinRoom: " + joinRoomRes);
        //JSONObject common_extra_info = new JSONObject(); 添加自定义字段上报到trace
        //common_extra_info["app_version"] = "1.0";
       // common_extra_info["isTester"] = "ture";

        //开启本地音频数据回调
        AudioFormat format = new AudioFormat(AudioSampleRate.AUDIO_SAMPLE_RATE_AUTO, AudioChannel.AUDIO_CHANNEL_MONO);
        mRTCVideo.enableAudioFrameCallback(AudioFrameCallbackMethod.AUDIO_FRAME_CALLBACK_RECORD, format);
        mRTCVideo.registerAudioFrameObserver(mAudioFrameObserver);
        //fetchToken(userId,roomId,roomConfig);


    }
    private  void startOrStopRTCTranscoding() {
        if (mliveTranscodingIv.isChecked()) {
            Log.i("mliveTranscodingIv",mliveTranscodingIv.toString());
            LiveTranscoding mLiveTranscoding;
            if (mShowUidArray[1] != null) {
                mLiveTranscoding = createPK1v1LiveTranscodingConfig(mRoomId,localUserId,mShowUidArray[1]);
            }else{
                mLiveTranscoding = createSingleLiveTranscodingConfig(mRoomId,localUserId);
            }
            startOrUpdateSingleRTCTranscoding(mLiveTranscoding);
        }else {
            mRTCVideo.stopLiveTranscoding("");
            mIsRTCTranscoding = false;
        }
    }
    private void startOrUpdateSingleRTCTranscoding(LiveTranscoding transcoding) {
        if (mRTCVideo != null) {
            Log.d("mIsRTCTranscoding " , String.valueOf(mIsRTCTranscoding));
            if (mIsRTCTranscoding) {
                mRTCVideo.updateLiveTranscoding("", transcoding);
            } else {
                mIsRTCTranscoding = true;
                mRTCVideo.startLiveTranscoding("", transcoding, mILiveTranscodingObserver);
            }
        }
    }

    private LiveTranscoding createSingleLiveTranscodingConfig(String roomId, String userId) {

        final String pushUrl = "rtmp://push.hafun.xyz/live/test?expire=1709212545&sign=d04e0315d7f325c25efe6c83c2ab8694";

        LiveTranscoding liveTranscoding = LiveTranscoding.getDefualtLiveTranscode();
        // set room id
        liveTranscoding.setRoomId(roomId);
        // Set the live address of push stream
        liveTranscoding.setUrl(pushUrl);
        // Set the merge mode, 0 means server merge
        liveTranscoding.setMixType(ByteRTCStreamMixingType.STREAM_MIXING_BY_SERVER);
        LiveTranscoding.VideoConfig videoConfig = liveTranscoding.getVideo()
                .setWidth(720)
                .setHeight(1280)
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

    private LiveTranscoding createPK1v1LiveTranscodingConfig(String roomId, String userId, String coHostUserId) {

        final String pushUrl = "rtmp://push.hafun.xyz/live/test?expire=1709212545&sign=d04e0315d7f325c25efe6c83c2ab8694";

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

    private void setLocalRenderView(String uid) {
        VideoCanvas videoCanvas = new VideoCanvas();
        TextureView renderView = new TextureView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mSelfContainer.removeAllViews();
        mSelfContainer.addView(renderView, params);
        videoCanvas.renderView = renderView;
        videoCanvas.uid = uid;
        videoCanvas.isScreen = false;
        videoCanvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
        // 设置本地视频渲染视图
        mRTCVideo.setLocalVideoCanvas(StreamIndex.STREAM_INDEX_MAIN, videoCanvas);
    }

    private void setRemoteRenderView(String roomId, String uid, FrameLayout container) {
        TextureView renderView = new TextureView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        container.removeAllViews();
        container.addView(renderView, params);

        VideoCanvas videoCanvas = new VideoCanvas();
        videoCanvas.renderView = renderView;
        videoCanvas.roomId = roomId;
        videoCanvas.uid = uid;
        videoCanvas.isScreen = false;
        videoCanvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
        // 设置远端用户视频渲染视图
        mRTCVideo.setRemoteVideoCanvas(uid, StreamIndex.STREAM_INDEX_MAIN, videoCanvas);

    }
    /* 为指定公共流绑定内部渲染视图 */
    private void setPublicRemoteView( String streamId) {
        int emptyInx = -1;
        for (int i = 0; i < mShowUidArray.length; i++) {
            if (TextUtils.isEmpty(mShowUidArray[i]) && emptyInx == -1) {
                emptyInx = i;
            } else if (TextUtils.equals(streamId, mShowUidArray[i])) {
                return;
            }
        }
        if (emptyInx < 0) {
            return;
        }
        mShowUidArray[emptyInx] = streamId;
        mUserIdTvArray[emptyInx].setText(String.format("streamId:%s", streamId));
        setPublicRemoteRenderView(streamId, mRemoteContainerArray[emptyInx]);
    }
    private void setPublicRemoteRenderView(String streamId, FrameLayout container) {
        TextureView renderView = new TextureView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        container.removeAllViews();
        container.addView(renderView, params);

        VideoCanvas videoCanvas = new VideoCanvas();
        videoCanvas.renderView = renderView;
        //videoCanvas.roomId = roomId;
        //videoCanvas.uid = uid;
        videoCanvas.isScreen = false;
        videoCanvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
        // 设置远端用户视频渲染视图
        mRTCVideo.setPublicStreamVideoCanvas(streamId, videoCanvas);
    }
    private void setRemoteView(String roomId, String uid) {
        int emptyInx = -1;
        for (int i = 0; i < mShowUidArray.length; i++) {
            if (TextUtils.isEmpty(mShowUidArray[i]) && emptyInx == -1) {
                emptyInx = i;
            } else if (TextUtils.equals(uid, mShowUidArray[i])) {
                return;
            }
        }
        if (emptyInx < 0) {
            return;
        }
        mShowUidArray[emptyInx] = uid;
        mUserIdTvArray[emptyInx].setText(String.format("UserId:%s", uid));
        setRemoteRenderView(roomId, uid, mRemoteContainerArray[emptyInx]);
    }

    private void removeRemoteView(String uid) {
        for (int i = 0; i < mShowUidArray.length; i++) {
            if (TextUtils.equals(uid, mShowUidArray[i])) {
                mShowUidArray[i] = null;
                mUserIdTvArray[i].setText(null);
                mRemoteContainerArray[i].removeAllViews();
            }
        }
    }

    private void onSwitchCameraClick() {
        // 切换前置/后置摄像头（默认使用前置摄像头）
        if (mCameraID.equals(CameraId.CAMERA_ID_FRONT)) {
            mCameraID = CameraId.CAMERA_ID_BACK;
        } else {
            mCameraID = CameraId.CAMERA_ID_FRONT;
        }
        mRTCVideo.switchCamera(mCameraID);
        // setAudioProfile
        //mRTCVideo.setAudioProfile(AudioProfileType.AUDIO_PROFILE_FLUENT);
        mRTCVideo.startPlayPublicStream("kangnan");
        
    }

    private void updateSpeakerStatus() {
        mIsSpeakerPhone = !mIsSpeakerPhone;
        // 设置使用哪种方式播放音频数据
        mRTCVideo.setAudioRoute(mIsSpeakerPhone ? AudioRoute.AUDIO_ROUTE_SPEAKERPHONE
                : AudioRoute.AUDIO_ROUTE_EARPIECE);
        mSpeakerIv.setImageResource(mIsSpeakerPhone ? R.drawable.speaker_on : R.drawable.speaker_off);
    }

    private void updateLocalAudioStatus() {
        mIsMuteAudio = !mIsMuteAudio;
        // 开启/关闭本地音频发送
        if (mIsMuteAudio) {
            mRTCRoom.unpublishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
            mRTCVideo.stopAudioCapture();//added by Connor 2023/11/16
        } else {
            mRTCVideo.startAudioCapture();//added by Connor 2023/11/16
            mRTCRoom.publishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
        }
        mAudioIv.setImageResource(mIsMuteAudio ? R.drawable.mute_audio : R.drawable.normal_audio);
    }

    private void updateLocalVideoStatus() {
        mIsMuteVideo = !mIsMuteVideo;
        if (mIsMuteVideo) {
            // 关闭视频采集
            mRTCVideo.stopVideoCapture();
        } else {
            // 开启视频采集
            mRTCVideo.startVideoCapture();
        }
        mVideoIv.setImageResource(mIsMuteVideo ? R.drawable.mute_video : R.drawable.normal_video);
    }

    private void showAlertDialog(String message) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(message);
            builder.setPositiveButton("知道了", (dialog, which) -> dialog.dismiss());
            builder.create().show();
        });
    }

    @Override
    public void finish() {
        super.finish();
        // 离开房间
        if (mRTCRoom != null) {
            mRTCRoom.leaveRoom();
            mRTCRoom.destroy();
        }
        mRTCRoom = null;
        mRTCVideo.stopLiveTranscoding("");
        // 销毁引擎
        RTCVideo.destroyRTCVideo();
        mIRtcVideoEventHandler = null;
        mIRtcRoomEventHandler = null;
        mRTCVideo = null;
        //

    }
}