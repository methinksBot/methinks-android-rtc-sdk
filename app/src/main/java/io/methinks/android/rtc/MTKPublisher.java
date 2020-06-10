package io.methinks.android.rtc;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;

import java.util.List;

import static io.methinks.android.rtc.MTKConst.*;
import static io.methinks.android.rtc.MTKError.ErrorCode.*;


public class MTKPublisher extends MTKPerson{
    private static final String TAG = MTKPublisher.class.getSimpleName();
    private static final String ME = "Me";

    protected VideoCapturer capturer;   // for camera
    protected VideoSink screenCapturer; // for screen sharing

    protected boolean audioSend;
    protected boolean videoSend;
    protected Intent data;  // for screen capturer intent data
    protected MediaProjectionCallback mediaProjectionCallback;   // for screen sharing


    protected MTKVideoChatSession session;

    public MTKPublisher(StreamVideoType videoType){
        this.videoType = videoType;
        if(videoType == StreamVideoType.camera){
            CameraEnumerator enumerator = new Camera1Enumerator(false);

            String[] devices = enumerator.getDeviceNames();
            for(String s : devices) {
                if(enumerator.isFrontFacing(s)) {
                    for(String deviceName : enumerator.getDeviceNames()){

                        List<CameraEnumerationAndroid.CaptureFormat> captureFormats =  enumerator.getSupportedFormats(deviceName);
                        for(CameraEnumerationAndroid.CaptureFormat captureFormat : captureFormats){
                        }
                    }

                    this.capturer = enumerator.createCapturer(s, null);
                }
                if(this.capturer != null)
                    break;
            }
            initViews();
        }


    }


    public MTKPublisher(StreamVideoType videoType, Intent data){
        this.videoType = videoType;
        this.data = data;
//        this.mediaProjectionCallback = new MediaProjectionCallback((Activity)MTKDataStore.getInstance().context);
        this.mediaProjectionCallback = new MediaProjectionCallback(MTKDataStore.getInstance().context);
        screenCapturer = new ScreenCapturerAndroid(data, mediaProjectionCallback);

    }

    private boolean useCamera2(){
        return Camera2Enumerator.isSupported(MTKDataStore.getInstance().context);
    }

    private void initViews(){
        int smallContainerSize = 90;

        renderer = new SurfaceViewRenderer(MTKDataStore.getInstance().context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        renderer.setLayoutParams(params);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        renderer.init(MTKDataStore.getInstance().eglBase.getEglBaseContext(), null);
        renderer.setEnableHardwareScaler(true);
        renderer.setMirror(true);

        renderer.setBackgroundColor(MTKDataStore.getInstance().context.getResources().getColor(android.R.color.transparent));

        Resources resources = MTKDataStore.getInstance().context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int)(smallContainerSize * (metrics.densityDpi / 160f));
        container = new FrameLayout(MTKDataStore.getInstance().context);
        container.setLayoutParams(new ViewGroup.LayoutParams(px, px));
        iconContainer = new LinearLayout(MTKDataStore.getInstance().context);
        iconContainer.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.TOP;
        iconContainer.setLayoutParams(p);
        container.addView(iconContainer, 0);

        nameTextView = new TextView(MTKDataStore.getInstance().context);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.BOTTOM;
        nameTextView.setLayoutParams(layoutParams);
        nameTextView.setText(ME);
        nameTextView.setTextColor(Color.WHITE);
        nameTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(nameTextView);
    }

    void joinPublisher(){
        try {
            MTKVideoChatSession session = MTKDataStore.getInstance().subSession != null ? MTKDataStore.getInstance().subSession : MTKDataStore.getInstance().mainSession;

            JSONObject display = new JSONObject();
            display.put("user_id", MTKDataStore.getInstance().userId);
            display.put("role", MTKDataStore.getInstance().role);
            display.put("video_type", this.videoType);
            display.put("name", MTKDataStore.getInstance().userName);
            if(!TextUtils.isEmpty(MTKDataStore.getInstance().profilePicURL)){
                display.put("profilePicUrl", MTKDataStore.getInstance().profilePicURL);
            }

            MTKTransactionUtil.joinPublisher(MTKDataStore.getInstance().client.janus, session, display.toString(), handleId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void joinedPublisher(JSONObject dataOfPluginData){
        new Handler(Looper.getMainLooper()).post(() -> {
            try{
                String event = dataOfPluginData.getString("videoroom");
                JSONArray joinedUsers = dataOfPluginData.has("publishers") ? dataOfPluginData.getJSONArray("publishers") : null;
                if(event.equals("joined")) {    // set publisher id, private id. and send offer
                    if(videoType == StreamVideoType.camera){
                        offer(MTKPublisher.this, dataOfPluginData, true, true, true,true);
                    }else if(videoType == StreamVideoType.screen){
                        boolean enableAudio = !MTKDataStore.getInstance().roomType.equals(ROOM_TYPE_APP_TEST);
                        offer(MTKPublisher.this, dataOfPluginData, false, false, enableAudio ,true);
                    }

                    feedId = dataOfPluginData.getLong("id");
                    session.privateId = dataOfPluginData.getLong("private_id");
                }

                if((event.equals("joined") || event.equals("event")) && joinedUsers != null) { // joined subscriber
                    if(videoType == StreamVideoType.camera){
                        attachSubscribers(joinedUsers);
                    }
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
        });
    }

    private void attachSubscribers(JSONArray joinedUsers){
        try {
            if(joinedUsers != null && joinedUsers.length() > 0){
                for(int i = 0; i < joinedUsers.length(); i++){
                    JSONObject joinedUser = joinedUsers.getJSONObject(i);
                    MTKSubscriber subscriber = new MTKSubscriber();
                    subscriber.videoType = MTKUtil.getVideoType(joinedUser);
                    subscriber.feedId = joinedUser.getLong("id");
                    subscriber.userId = MTKUtil.getUserIdForSubscriber(joinedUser);
                    subscriber.role = MTKUtil.getRoleForSubscriber(joinedUser);
                    subscriber.userName = MTKUtil.getUserNameForSubscriber(joinedUser);
                    subscriber.tempTransactionId = MTKTransactionUtil.attachPlugin(MTKDataStore.getInstance().client.janus, session, MTKConst.PLUGIN_VIDEOROOM);
                    if(MTKDataStore.getInstance().subscribers != null){
                        MTKDataStore.getInstance().subscribers.add(subscriber);
                    }
                }

                if(MTKDataStore.getInstance().roomType.equals(ROOM_TYPE_INTERVIEW) && MTKUtil.getSubscriberCountWithoutObserver() > 0) {
                    MTKTransactionUtil.requestConfigureForRecordingStart(MTKDataStore.getInstance().client.janus, MTKPublisher.this, session, MTKPublisher.this.handleId, MTKPublisher.this.audioSend, MTKPublisher.this.videoSend);
                }else if(MTKDataStore.getInstance().roomType.equals(ROOM_TYPE_APP_TEST)){
                    MTKTransactionUtil.requestConfigureForRecordingStart(MTKDataStore.getInstance().client.janus, MTKPublisher.this, session, MTKPublisher.this.handleId, MTKPublisher.this.audioSend, MTKPublisher.this.videoSend);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void offer(MTKPublisher publisher, JSONObject data, boolean audioReceive, boolean videoReceive, boolean audioSend, boolean videoSend) {
        MTKPublisher.this.audioSend = audioSend;
        MTKPublisher.this.videoSend = videoSend;

        MTKVideoChatClient.executor.execute(() -> {
            PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(MTKDataStore.getInstance().iceServers);
            rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
            boolean enableDataChannel = videoType == StreamVideoType.camera;
            MTKPublisher.this.pcClient = new MTKPeerConnectionClient(MTKDataStore.getInstance().context, MTKDataStore.getInstance().pcFactory, rtcConfig, MTKPublisher.this.session, enableDataChannel, true) {
                @Override
                public void onIceCandidate(IceCandidate iceCandidate) {
                    super.onIceCandidate(iceCandidate);
                    MTKTransactionUtil.sendCandidate(MTKDataStore.getInstance().client.janus, MTKPublisher.this.session, MTKPublisher.this.handleId, iceCandidate);
                }

                @Override
                public void onAddStream(MediaStream mediaStream) {
                }
            };

            boolean isCamera = false;
            VideoCapturer capturer = null;
            SurfaceTextureHelper helper = null;
            if (publisher.videoType == StreamVideoType.camera) {
                helper = SurfaceTextureHelper.create(MTKConst.THREAD_NAME_1, MTKDataStore.getInstance().eglBase.getEglBaseContext());
                capturer = publisher.capturer;
                isCamera = true;
            } else if(publisher.videoType == StreamVideoType.screen) {
                helper = SurfaceTextureHelper.create(MTKConst.THREAD_NAME_2, MTKDataStore.getInstance().eglBase.getEglBaseContext());
                capturer = (VideoCapturer) publisher.screenCapturer;
                isCamera = false;
            }

            VideoSource videoSource = MTKDataStore.getInstance().pcFactory.createVideoSource(capturer.isScreencast());
            capturer.initialize(helper, MTKDataStore.getInstance().context, videoSource.getCapturerObserver());

            int[] resolution = MTKUtil.getDeviceResolution();
            int fps = 15;
            Log.d("Resolution width : " + resolution[0] + ", height: " + resolution[1] + ", isCamera : " + isCamera);
            Log.e("Resolution width : " + resolution[0] + ", height: " + resolution[1] + ", isCamera : " + isCamera);
            System.out.println("Resolution width : " + resolution[0] + ", height: " + resolution[1] + ", isCamera : " + isCamera);
            if(resolution[0] >= resolution[1]){ // landscape
                if(resolution[0] > 1280){
                    resolution[0] = 1280;
                    resolution[1] = 720;
                }
                videoSource.adaptOutputFormat(resolution[0], resolution[1], resolution[1], resolution[0], fps);
                if(!isCamera){  // screen
//                    capturer.startCapture(resolution[1], resolution[0], fps);
                    capturer.startCapture(resolution[0], resolution[1], fps);
                }else{
                    capturer.startCapture(resolution[0], resolution[1], fps);
                }

            }else{  // portrait
                if(resolution[1] > 1280){
                    resolution[1] = 1280;
                    resolution[0] = 720;
                }
                videoSource.adaptOutputFormat(resolution[1], resolution[0], resolution[0], resolution[1], fps);
                if(!isCamera){
                    capturer.startCapture(resolution[0], resolution[1], fps);
                }else{
                    capturer.startCapture(resolution[1], resolution[0], fps);
                }
            }
            Log.d("recommended resolution width/height : " + resolution[0] + "/" + resolution[1]);

            videoTrack = MTKDataStore.getInstance().pcFactory.createVideoTrack(MTKConst.VIDEO_TRACK_ID, videoSource);
            MTKPublisher.this.pcClient.peerConnection.addTrack(videoTrack);
            new Handler(Looper.getMainLooper()).post(() -> {
                if(videoTrack != null) {
                    if(MTKPublisher.this.videoType == StreamVideoType.camera){
                        videoTrack.addSink(renderer);
                    }else if(MTKPublisher.this.videoType == StreamVideoType.screen){
                        videoTrack.addSink(screenCapturer);
                    }
                    videoTrack.setEnabled(publishVideo);
                }
                setZOrderMediaOverlay(true);
            });


            if (audioSend) {
                MediaConstraints mediaConstraints = new MediaConstraints();
                AudioSource audioSource = MTKDataStore.getInstance().pcFactory.createAudioSource(mediaConstraints);
                audioTrack = MTKDataStore.getInstance().pcFactory.createAudioTrack(MTKConst.AUDIO_TRACK_ID, audioSource);
                MTKPublisher.this.pcClient.peerConnection.addTrack(audioTrack);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if(audioTrack != null) {
                        audioTrack.setEnabled(publishAudio);
                    }
                });
            }

            MTKVideoChatClient.executor.execute(() -> {
                MediaConstraints constraints = new MediaConstraints();
                constraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", Boolean.toString(audioReceive)));
                constraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", Boolean.toString(videoReceive)));
                constraints.mandatory.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

                MTKPublisher.this.pcClient.peerConnection.createOffer(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(final SessionDescription sessionDescription) {
                        MTKVideoChatClient.executor.execute(() -> {
                            if (MTKPublisher.this.pcClient.peerConnection != null && sessionDescription != null)
                                MTKPublisher.this.pcClient.peerConnection.setLocalDescription(new SdpObserver() {
                                    @Override
                                    public void onCreateSuccess(SessionDescription sessionDescription) {
                                        Log.d("Offer onCreateSuccess()");
                                    }

                                    @Override
                                    public void onSetSuccess() {
                                        Log.d("Offer onSetSuccess()");
                                        if(MTKDataStore.getInstance().subscribers.size() > 0){
                                            if(MTKUtil.equalObserverCountAndSubscriberCount()){
                                                MTKTransactionUtil.requestConfigureForOffer(MTKDataStore.getInstance().client.janus, MTKPublisher.this.session, MTKPublisher.this.handleId, sessionDescription);
                                            }else{
                                                MTKTransactionUtil.requestConfigure(MTKDataStore.getInstance().client.janus, MTKPublisher.this, MTKPublisher.this.session, MTKPublisher.this.handleId, sessionDescription, MTKPublisher.this.audioSend, MTKPublisher.this.videoSend);
                                            }
                                        }else{
                                            if(MTKDataStore.getInstance().mainPublisher.videoType == StreamVideoType.screen && MTKDataStore.getInstance().mainPublisher == MTKPublisher.this){
                                                MTKTransactionUtil.requestConfigure(MTKDataStore.getInstance().client.janus, MTKPublisher.this, MTKPublisher.this.session, MTKPublisher.this.handleId, sessionDescription, true, true);
                                            }else{
                                                MTKTransactionUtil.requestConfigureForOffer(MTKDataStore.getInstance().client.janus, MTKPublisher.this.session, MTKPublisher.this.handleId, sessionDescription);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCreateFailure(String s) {
                                        Log.e("Offer onCreateFailure() : " + s);
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            MTKError error = new MTKError(MTKError.Domain.PublisherErrorDomain, PublisherWebRTCError.getErrorCode(), "Failed creating local SDP\n" + s);
                                            MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                                        });
                                    }

                                    @Override
                                    public void onSetFailure(String s) {
                                        Log.d("Offer onSetFailure() : " + s);
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            MTKError error = new MTKError(MTKError.Domain.PublisherErrorDomain, PublisherWebRTCError.getErrorCode(), "Failed setting local SDP\n" + s);
                                            MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                                        });
                                    }
                                }, new SessionDescription(sessionDescription.type, sessionDescription.description));
                        });
                    }

                    @Override
                    public void onSetSuccess() {
                        Log.d("Offer onSetSuccess()");
                    }

                    @Override
                    public void onCreateFailure(String s) {
                        Log.d("Offer onCreateFailur() : " + s);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            MTKError error = new MTKError(MTKError.Domain.PublisherErrorDomain, PublisherWebRTCError.getErrorCode(), "Failed creating Offer\n" + s);
                            MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                        });
                    }

                    @Override
                    public void onSetFailure(String s) {
                        MTKError error = new MTKError(MTKError.Domain.PublisherErrorDomain, 1113, "Failed setting Offer\n" + s);
                        MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                    }

                }, constraints);
            });
        });
    }

    @Override
    protected void receiveAnswer(String sdp) {
        MTKVideoChatClient.executor.execute(() -> pcClient.peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
            }

            @Override
            public void onSetSuccess() {
                MTKPublisher.this.handleIceCandidatePool();
                MTKDataStore.getInstance().client.listener.onStartedPublishing(MTKDataStore.getInstance().client, MTKPublisher.this);
            }

            @Override
            public void onCreateFailure(String s) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    MTKError error = new MTKError(MTKError.Domain.PublisherErrorDomain, PublisherInternalError.getErrorCode(), "Failed creating SDP object\n" + s);
                    MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                });
            }

            @Override
            public void onSetFailure(String s) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    MTKError error = new MTKError(MTKError.Domain.PublisherErrorDomain, PublisherInternalError.getErrorCode(), "Failed setting SDP object to PeerConnection\n" + s);
                    MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                });
            }
        }, new SessionDescription(SessionDescription.Type.ANSWER, sdp)));
    }

    public void setSwitchIcon(int src) {
        Resources resources = MTKDataStore.getInstance().context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = 18 * (metrics.densityDpi / 160f);
        iconSize = (int)px;
        switchIconImageView = new ImageView(MTKDataStore.getInstance().context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(iconSize, iconSize);
        params.gravity = Gravity.TOP| Gravity.END;
        switchIconImageView.setLayoutParams(params);
        switchIconImageView.setImageResource(src);
        container.addView(switchIconImageView, 0);
        switchIconImageView.setVisibility(View.VISIBLE);
    }

    public FrameLayout getContainer(){
        return container;
    }

    public void setZOrderMediaOverlay(boolean enable){
        if(renderer != null){
            renderer.setZOrderMediaOverlay(enable);
        }
    }

    public void setPublishAudio(boolean publishAudio) {
        this.publishAudio = publishAudio;
        if(audioTrack != null){
            try{
                audioTrack.setEnabled(publishAudio);
                publishAudio(publishAudio);
            }catch (Exception e){
                MTKError error = new MTKError(MTKError.Domain.PublisherErrorDomain, 1500, e);
                MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
            }
        }
    }

    public void setPublishVideo(boolean publishVideo) {
        this.publishVideo = publishVideo;
        if(videoTrack != null){
            try{
                videoTrack.setEnabled(publishVideo);
                super.publishVideo(publishVideo);
            }catch (Exception e){
                MTKError error = new MTKError(MTKError.Domain.PublisherErrorDomain, PublisherUnableToPublish.getErrorCode(), e);
                MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
            }
        }
    }

    public void cycleCamera(){
        if(this.capturer == null){
            Log.w("Capturer is not initialized yet.");
        }else{
            if(videoType == StreamVideoType.camera){
                CameraVideoCapturer cvc = (CameraVideoCapturer) capturer;
                cvc.switchCamera(null);
            }
        }
    }

}
