package io.methinks.android.rtc;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
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
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import static io.methinks.android.rtc.MTKConst.*;
import static io.methinks.android.rtc.MTKError.ErrorCode.*;


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
                if(videoType == StreamVideoType.camera){
                    if(i >= i1) {
                        currentRotation = Rotation.landscape;
                    } else {
                        currentRotation = Rotation.portrait;
                    }
                }else{
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
        nameTextView.setTextColor(Color.WHITE);
        nameTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        container.addView(nameTextView);
    }

    @Override
    protected void receiveOffer(JSONObject data, String sdp, boolean audioSend, boolean videoSend) {
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
        boolean canReceiveOffer = true;
        if (MTKDataStore.getInstance().mainPublisher != null && MTKDataStore.getInstance().mainPublisher.feedId == finalId) {    // I am sharing my screen
            canReceiveOffer = false;
        } else if (MTKDataStore.getInstance().screenSharingPublisher != null && MTKDataStore.getInstance().screenSharingPublisher.feedId == finalId) {
            canReceiveOffer = false;
        }

        if (!canReceiveOffer) {
            return;
        }

        /*ArrayList<PeerConnection.IceServer> servers = new ArrayList<>();
        servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(servers);*/
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(MTKDataStore.getInstance().iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        boolean enableDataChannel = videoType == StreamVideoType.camera;

        pcClient = new MTKPeerConnectionClient(MTKDataStore.getInstance().context, MTKDataStore.getInstance().pcFactory, rtcConfig, MTKDataStore.getInstance().mainSession, enableDataChannel, false) {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Log.d("onIceCandidate()");
                MTKTransactionUtil.sendSDP(MTKDataStore.getInstance().client.janus, MTKDataStore.getInstance().mainSession, handleId, iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d("onAddStream()");
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
                    videoType = StreamVideoType.camera;

                } else {  // If screen stream, create new internal Subscriber.
                    videoType = StreamVideoType.screen;
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    videoTrack.addSink(renderer);
                    setZOrderOnTop(false);
                    setZOrderMediaOverlay(false);
                    MTKDataStore.getInstance().client.listener.onAddedStream(MTKDataStore.getInstance().client, mediaStream, MTKSubscriber.this, MTKDataStore.getInstance().mainSession);
                });
            }
        };

        pcClient.peerConnection.getStats(rtcStatsReport -> {
        });

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        MTKVideoChatClient.executor.execute(() -> {
            pcClient.peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.d("onCreateSuccess()");
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
                                            new Handler(Looper.getMainLooper()).post(() -> {
                                                MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed creating local SDP\n" + s);
                                                MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                                            });
                                        }

                                        @Override
                                        public void onSetFailure(String s) {
                                            Log.w("setLocalDescription onSetFailure : " + s);
                                            new Handler(Looper.getMainLooper()).post(() -> {
                                                MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed setting local SDP\n" + s);
                                                MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                                            });
                                        }
                                    }, sessionDescription);
                                });
                            }

                            @Override
                            public void onSetSuccess() {

                            }

                            @Override
                            public void onCreateFailure(String s) {
                                Log.w("createAnswer() onCreateFailure : " + s);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed creating Answer\n" + s);
                                    MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                                });
                            }

                            @Override
                            public void onSetFailure(String s) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed setting Answer\n" + s);
                                    MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                                });
                            }
                        }, constraints);
                    });
                }

                @Override
                public void onCreateFailure(String s) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed creating remote SDP\n" + s);
                        MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                    });
                }

                @Override
                public void onSetFailure(String s) {
                    Log.w("setRemoteDescription() onSetFailure : " + s);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        MTKError error = new MTKError(MTKError.Domain.SessionErrorDomain, SubscriberWebRTCError.getErrorCode(), "Failed setting remote SDP\n" + s);
                        MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
                    });
                }
            }, new SessionDescription(SessionDescription.Type.OFFER, sdp));
        });
    }

    /**
     * change renderer the frame size according to the rotation.
     * and consider isMain flag.
     */
    private void processRendererFrameWithRotation(){
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)renderer.getLayoutParams();
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