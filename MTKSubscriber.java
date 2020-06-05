package io.methinks.android.mtkrtc;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RTCStatsCollectorCallback;
import org.webrtc.RTCStatsReport;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import io.methinks.android.R;

import static io.methinks.android.mtkrtc.MTKError.ErrorCode.SubscriberWebRTCError;

public class MTKSubscriber extends MTKPerson {
    private static final String TAG = MTKSubscriber.class.getSimpleName();

    protected boolean isMain;
    protected String userId;
    protected String userName;
    protected String role;

    public MTKSubscriber() {
        renderer = new SurfaceViewRenderer(MTKDataStore.getInstance().context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        renderer.setLayoutParams(params);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        currentRotation = Rotation.portrait;
        renderer.init(MTKDataStore.getInstance().eglBase.getEglBaseContext(), new RendererCommon.RendererEvents() {
            @Override
            public void onFirstFrameRendered() {

            }

            @Override
            public void onFrameResolutionChanged(int i, int i1, int degree) {
                Log.e("$$$$$$$$", "subscriber onFrameResolutionChanged() values : " + i + "/" + i1 + "/" + degree);
                if(videoType == StreamVideoType.camera){
//                    if(degree == 0 || degree == 180){   // landscape
//                        currentRotation = Rotation.landscape;
//                    }else{  // portrait
//                        currentRotation = Rotation.portrait;
//                    }
                    if(i >= i1) {
                        currentRotation = Rotation.landscape;
                    } else {
                        currentRotation = Rotation.portrait;
                    }
                }else{
//                    if(degree == 0 || degree == 180){   // landscape
//                        currentRotation = Rotation.portrait;
//                    }else{  // portrait
//                        currentRotation = Rotation.landscape;
//                    }
                    if(i >= i1) {
                        currentRotation = Rotation.landscape;
                    } else {
                        currentRotation = Rotation.portrait;
                    }
                }



                if(ratio != 0.0f && ratio != (float)i1 / (float)i){
                    ratio = (float)i1 / (float)i;
                }else if(ratio == 0.0f){
                    ratio = (float)i1 / (float)i;
                }
                processRendererFrameWithRotation();
            }
        });
        renderer.setEnableHardwareScaler(true);
        initViews();
    }

    private void initViews(){
        int smallContainerSize = 90;
        Resources resources = MTKDataStore.getInstance().context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) (smallContainerSize * (metrics.densityDpi / 160f));
        container = new FrameLayout(MTKDataStore.getInstance().context);
        container.setLayoutParams(new ViewGroup.LayoutParams(px, px));
        container.setBackgroundColor(Color.parseColor("#8a000000"));
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
        nameTextView.setTextColor(MTKDataStore.getInstance().context.getResources().getColor(R.color.white));
        nameTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        container.addView(nameTextView);
    }

    @Override
    protected void receiveOffer(JSONObject data, String sdp, boolean audioSend, boolean videoSend) {
        Log.e(TAG, "receiveOffer : " + data);
        long id = 0;
        try {
            id = data.getLong("id");    // feed id.
        } catch (JSONException e) {
            e.printStackTrace();
        }
        long finalId = id;
        feedId = id;
        userId = MTKUtil.getUserIdForSubscriber(data);
        userName = MTKUtil.getUserNameForSubscriber(data);
        Log.e(TAG, "receiveOffer user id : " + userId);
        boolean canReceiveOffer = true;
        if (MTKDataStore.getInstance().mainPublisher != null && MTKDataStore.getInstance().mainPublisher.feedId == finalId) {    // I am sharing my screen
            canReceiveOffer = false;
        } else if (MTKDataStore.getInstance().screenSharingPublisher != null && MTKDataStore.getInstance().screenSharingPublisher.feedId == finalId) {
            canReceiveOffer = false;
        }

        if (!canReceiveOffer) {
            Log.e(TAG, "cannot receive offer!!");
            return;
        }

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(MTKDataStore.getInstance().iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        boolean enableDataChannel = videoType == StreamVideoType.camera;
        Log.e(TAG, "enableDataChannel check : " + enableDataChannel);

        pcClient = new MTKPeerConnectionClient(MTKDataStore.getInstance().context, MTKDataStore.getInstance().pcFactory, rtcConfig, MTKDataStore.getInstance().mainSession, enableDataChannel) {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Log.e(TAG, "onIceCandidate()");
                MTKTransactionUtil.sendSDP(MTKDataStore.getInstance().client.janus, MTKDataStore.getInstance().mainSession, handleId, iceCandidate);
            }

//            @Override
//            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
//                if (newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
//                    Log.e(TAG, "onConnectionChange() disconnected feed id : " + finalId);
////                    Subscriber droppedSubscriber = janus.session.subscribers.get(finalId);
////                    ((Activity)context).runOnUiThread(() -> session.sessionListener.onStreamDropped(session, droppedSubscriber.stream));
//
//                }
//            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.e(TAG, "onAddStream()");
                if(mediaStream.videoTracks != null && mediaStream.videoTracks.size() > 0){
                    videoTrack = mediaStream.videoTracks.get(0);
                }
                if(mediaStream.audioTracks != null && mediaStream.audioTracks.size() > 0){
                    audioTrack = mediaStream.audioTracks.get(0);
                }


                if (MTKDataStore.getInstance().mainPublisher != null && MTKDataStore.getInstance().mainPublisher.feedId == finalId) {    // I am sharing my screen
                    return;
                } else if (MTKDataStore.getInstance().screenSharingPublisher != null && MTKDataStore.getInstance().screenSharingPublisher.feedId == finalId) {
                    return;
                }

                if (MTKUtil.isCameraVideoType(data)) {    // If camera stream, callback session listener
                    Log.e("screens", "111111111111111this is camera stream\n" + data.toString());
                    videoType = StreamVideoType.camera;

                } else {  // If screen stream, create new internal Subscriber.
                    Log.e("screens", "2222222222222this is screen stream\n" + data.toString());
                    videoType = StreamVideoType.screen;
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(videoTrack == null || renderer == null) return;

                        videoTrack.addSink(renderer);
                        setZOrderOnTop(false);
                        setZOrderMediaOverlay(false);
                        MTKDataStore.getInstance().client.listener.onAddedStream(MTKDataStore.getInstance().client, mediaStream, MTKSubscriber.this, MTKDataStore.getInstance().mainSession);

                        Log.e(TAG, "track check==================================================");
                        if(videoTrack != null){
                            Log.e(TAG, "track check video track : " + videoTrack.enabled());
                        }else{
                            Log.e(TAG, "track check video track is null");
                        }
                        if(audioTrack != null){
                            Log.e(TAG, "track check audio track : " + audioTrack.enabled());
                        }else{
                            Log.e(TAG, "track check audio track is null");
                        }
                    }
                });

//                ((Activity) MTKDataStore.getInstance().context).runOnUiThread(() -> {
//
//                    videoTrack.addSink(renderer);
//                    setZOrderOnTop(false);
//                    setZOrderMediaOverlay(false);
//                    MTKDataStore.getInstance().client.listener.onAddedStream(MTKDataStore.getInstance().client, mediaStream, MTKSubscriber.this, MTKDataStore.getInstance().mainSession);
//
//                    Log.e(TAG, "track check==================================================");
//                    if(videoTrack != null){
//                        Log.e(TAG, "track check video track : " + videoTrack.enabled());
//                    }else{
//                        Log.e(TAG, "track check video track is null");
//                    }
//                    if(audioTrack != null){
//                        Log.e(TAG, "track check audio track : " + audioTrack.enabled());
//                    }else{
//                        Log.e(TAG, "track check audio track is null");
//                    }
//                });
            }
        };

        pcClient.peerConnection.getStats(new RTCStatsCollectorCallback() {
            @Override
            public void onStatsDelivered(RTCStatsReport rtcStatsReport) {
            }
        });

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        MTKVideoChatClient.executor.execute(() -> {
            pcClient.peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.e(TAG, "onCreateSuccess() : \n" + sessionDescription);
                }

                @Override
                public void onSetSuccess() {
                    MTKVideoChatClient.executor.execute(() -> {
                        pcClient.peerConnection.createAnswer(new SdpObserver() {
                            @Override
                            public void onCreateSuccess(SessionDescription sessionDescription) {
                                MTKVideoChatClient.executor.execute(() -> {
                                    pcClient.peerConnection.setLocalDescription(new SdpObserver() {
                                        @Override
                                        public void onCreateSuccess(SessionDescription sessionDescription) {

                                        }

                                        @Override
                                        public void onSetSuccess() {
                                            MTKTransactionUtil.start(MTKDataStore.getInstance().client.janus, handleId, sessionDescription);
                                        }

                                        @Override
                                        public void onCreateFailure(String s) {
                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed creating local SDP\n" + s);
                                                    MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                                                }
                                            });
//                                            ((Activity) MTKDataStore.getInstance().context).runOnUiThread(() -> {
//                                                MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed creating local SDP\n" + s);
//                                                MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
//                                            });
                                        }

                                        @Override
                                        public void onSetFailure(String s) {
                                            Log.e(TAG, "setLocalDescription onSetFailure : " + s + "\n" + sdp);
                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed setting local SDP\n" + s);
                                                    MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                                                }
                                            });
//                                            ((Activity) MTKDataStore.getInstance().context).runOnUiThread(() -> {
//                                                MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed setting local SDP\n" + s);
//                                                MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
//                                            });
                                        }
                                    }, sessionDescription);
                                });
                            }

                            @Override
                            public void onSetSuccess() {

                            }

                            @Override
                            public void onCreateFailure(String s) {
                                Log.e(TAG, "createAnswer() onCreateFailure : " + s);
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed creating Answer\n" + s);
                                        MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                                    }
                                });
//                                ((Activity) MTKDataStore.getInstance().context).runOnUiThread(() -> {
//                                    MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed creating Answer\n" + s);
//                                    MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
//                                });
                            }

                            @Override
                            public void onSetFailure(String s) {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed setting Answer\n" + s);
                                        MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                                    }
                                });
//                                ((Activity) MTKDataStore.getInstance().context).runOnUiThread(() -> {
//                                    MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed setting Answer\n" + s);
//                                    MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
//                                });
                            }
                        }, constraints);
                    });
                }

                @Override
                public void onCreateFailure(String s) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed creating remote SDP\n" + s);
                            MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                        }
                    });
//                    ((Activity) MTKDataStore.getInstance().context).runOnUiThread(() -> {
//                        MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed creating remote SDP\n" + s);
//                        MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
//                    });
                }

                @Override
                public void onSetFailure(String s) {
                    Log.e(TAG, "setRemoteDescription() onSetFailure : " + s + "\n" + sdp);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed setting remote SDP\n" + s);
                            MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                        }
                    });
//                    ((Activity) MTKDataStore.getInstance().context).runOnUiThread(() -> {
//                        MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed setting remote SDP\n" + s);
//                        MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
//                    });
                }
            }, new SessionDescription(SessionDescription.Type.OFFER, sdp));
        });
    }

    /**
     * change renderer the frame size according to the rotation.
     * and consider isMain flag.
     */
    private void processRendererFrameWithRotation(){
        Log.e("$$$$$$$$", " current : " + MTKUtil.getOrientation() + ", " + "subscriber's stream : " + currentRotation);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)renderer.getLayoutParams();
        int[] screenResolution = MTKUtil.getDeviceResolution();
//        int longLength = screenResolution[0] >= screenResolution[1] ? screenResolution[0] : screenResolution[1];
//        int shortLength = screenResolution[0] >= screenResolution[1] ? screenResolution[1] : screenResolution[0];



//        if(isMain){
//            if(currentRotation == Rotation.landscape){
//                if(MTKUtil.getOrientation() == 0){   // current device orientation is portrait
////                    params.width = longLength;
////                    params.height = (int)((float)longLength * ratio);
//                    params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
//                    params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
//                }else if(MTKUtil.getOrientation() == 90 || MTKUtil.getOrientation() == 270){ // current device orientation is landscape
//                    params.width = longLength;
//                    params.height = (int)((float)longLength * ratio);
//                }
//
//
//
//
//
//                Log.e("######", "screen widht : " + screenResolution[0] + ", screen height : " + screenResolution[1]);
//
////                params.width = longLength;
////                params.height = (int)((float)longLength * ratio);
//            }else if(currentRotation == Rotation.portrait){
//                if(MTKUtil.getOrientation() == 0){   // current device orientation is portrait
//                    params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
//                    params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
//                    Log.e(TAG, "444444444444444");
//                }else if(MTKUtil.getOrientation() == 90 || MTKUtil.getOrientation() == 270){ // current device orientation is landscape
//                    params.width = longLength;
//                    params.height = (int)((float)longLength * ratio);
////                    params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
////                    params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
//                    Log.e(TAG, "555555555555");
//                }
//            }
//        }else{
//            params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
//            params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
////            params.width = FrameLayout.LayoutParams.MATCH_PARENT;
////            params.height = FrameLayout.LayoutParams.MATCH_PARENT;
//        }
        params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
        params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        new Handler(Looper.getMainLooper()).post(() -> renderer.setLayoutParams(params));
    }

    protected void setLayoutParams(FrameLayout.LayoutParams params) {
        renderer.setLayoutParams(params);
    }

    public boolean isMain() {
        return isMain;
    }

    public void setZOrderMediaOverlay(boolean enable) {
        renderer.setZOrderMediaOverlay(enable);
    }

    public void setZOrderOnTop(boolean enable) {
        renderer.setZOrderOnTop(enable);
    }

    public String getUserName() {
        return userName;
    }

    public TextView getNameTextView(){
        return nameTextView;
    }

    public void setPublishVideo(boolean publishVideo){
        this.publishVideo = publishVideo;
        super.publishVideo(publishVideo);
    }

    public void setPublishAudio(boolean publishAudio){
        this.publishAudio = publishAudio;
        super.publishAudio(publishAudio);
    }

    public void setAsMain() {
        isMain = true;
    }

    public void setAsNotMain() {
        isMain = false;
    }

    public String getUserId() {
        return userId;
    }

    public long getFeedId() {
        return feedId;
    }

    public FrameLayout getContainer() {
        return container;
    }

    public SurfaceViewRenderer getRenderer(){
        processRendererFrameWithRotation();

        return super.getRenderer();
    }

}