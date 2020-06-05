package io.methinks.android.mtkrtc;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

public class MTKPerson {
    protected String tempTransactionId;
    protected MTKPeerConnectionClient pcClient;
    protected long handleId;
    protected long feedId;
    public StreamVideoType videoType;
    protected DataChannel dataChannel;
    protected FrameLayout container;
    protected LinearLayout iconContainer;
    protected TextView nameTextView;
    protected SurfaceViewRenderer renderer;

    protected VideoTrack videoTrack;
    protected AudioTrack audioTrack;
    protected ArrayList<IceCandidate> candidatePool = new ArrayList<>();

    protected int iconSize;
    protected ImageView audioMuteIconImageView, videoMuteIconImageView, switchIconImageView;

    protected boolean publishAudio = true;
    protected boolean publishVideo = true;
    protected Rotation currentRotation;
    protected float ratio = 0.0f;

    protected int shareType;

    protected void handleIceCandidatePool() {
        MTKVideoChatClient.executor.execute(() -> {
            synchronized (this) {
                for (IceCandidate candidate : candidatePool) {
                    pcClient.peerConnection.addIceCandidate(candidate);
                }

                candidatePool.clear();
            }
        });
    }

    protected void addIceCandidate(IceCandidate candidate) {
        synchronized (this) {
            if (pcClient.peerConnection != null && pcClient.peerConnection.getRemoteDescription() != null) {
                MTKVideoChatClient.executor.execute(() -> {
                    pcClient.peerConnection.addIceCandidate(candidate);
                });
            } else {
                candidatePool.add(candidate);
            }
        }
    }

    public SurfaceViewRenderer getRenderer(){
        return renderer;
    }

    protected void offer(MTKPublisher publisher, JSONObject data, boolean audioReceive, boolean videoReceive, boolean audioSend, boolean videoSend){ }

    protected void receiveOffer(JSONObject data, String sdp, boolean audioSend, boolean videoSend) {}

    protected void receiveAnswer(String sdp) {}

    public void setAudioMuteIcon(int src) {
        Resources resources = MTKDataStore.getInstance().context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = 18 * (metrics.densityDpi / 160f);
        iconSize = (int)px;
        audioMuteIconImageView = new ImageView(MTKDataStore.getInstance().context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(iconSize, iconSize);
        audioMuteIconImageView.setLayoutParams(params);
        audioMuteIconImageView.setImageResource(src);
        iconContainer.addView(audioMuteIconImageView, 0);
        audioMuteIconImageView.setVisibility(View.GONE);
    }

    public void setVideoMuteIcon(int src) {
        Resources resources = MTKDataStore.getInstance().context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = 18 * (metrics.densityDpi / 160f);
        iconSize = (int)px;
        videoMuteIconImageView = new ImageView(MTKDataStore.getInstance().context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(iconSize, iconSize);
        videoMuteIconImageView.setLayoutParams(params);
        videoMuteIconImageView.setImageResource(src);
        iconContainer.addView(videoMuteIconImageView, 0);
        videoMuteIconImageView.setVisibility(View.GONE);
    }

    public boolean isPublishAudio() {
        return publishAudio;
    }

    public boolean isPublishVideo() {
        return publishVideo;
    }

    public void publishVideo(boolean publish){
        if(videoMuteIconImageView == null)
            return;

        if(publish){
            videoMuteIconImageView.setVisibility(View.GONE);
        }else{
            videoMuteIconImageView.setVisibility(View.VISIBLE);
        }
    }

    public void publishAudio(boolean publish){
        if(audioMuteIconImageView == null)
            return;

        if(publish){
            audioMuteIconImageView.setVisibility(View.GONE);
        }else{
            audioMuteIconImageView.setVisibility(View.VISIBLE);
        }
    }

    public enum StreamVideoType {
        camera(1),
        screen(2);

        private int videoType;

        private StreamVideoType(int type) {
            this.videoType = type;
        }
    }

    public enum Rotation{
        portrait(3),
        landscape(4);
        private int rotation;
        private Rotation(int type) {
            this.rotation = type;
        }
    }
}
