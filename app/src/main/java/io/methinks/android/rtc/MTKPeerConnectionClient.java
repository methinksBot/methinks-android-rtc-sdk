package io.methinks.android.rtc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;

import java.nio.ByteBuffer;

class MTKPeerConnectionClient implements PeerConnection.Observer {
    private static final String TAG = PeerConnection.class.getSimpleName();
    protected Context context;
    protected PeerConnection peerConnection;
    protected DataChannel dataChannel;
    protected MTKVideoChatSession session;
//    protected MTKRecordedAudioToFileController saveRecordedAudioToFile;
    private boolean enableDataChannel;

    private void initDataChannel(MTKVideoChatSession session){
        this.session = session;
        DataChannel.Init init = new DataChannel.Init();
        init.ordered = false;
        this.dataChannel = peerConnection.createDataChannel("mtkrtcdata", init);

        this.dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
            }

            @Override
            public void onStateChange() {
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        ByteBuffer data = buffer.data;
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        final String jsonStr = new String(bytes);
                        JSONObject response = new JSONObject(jsonStr);
                        JSONObject msg = new JSONObject(response.getString("msg"));
                        String type = response.getString("type");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    boolean isPublisher;
    MTKPeerConnectionClient(Context context, PeerConnectionFactory factory, PeerConnection.RTCConfiguration config, MTKVideoChatSession session, boolean enableDataChannel, boolean isPublisher) {
        this.context = context;
        this.enableDataChannel = enableDataChannel;
        peerConnection = factory.createPeerConnection(config, this);
        this.isPublisher = isPublisher;
        initDataChannel(session);
    }

    PeerConnection getPeerConnection() {
        return peerConnection;
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        /**
         * Show AlertDialog to notify if peer can't connect to Janus.
         */
//        if(iceConnectionState == PeerConnection.IceConnectionState.FAILED){
//            new Handler(Looper.getMainLooper()).post(() -> {
//                MTKError error = new MTKError(MTKError.Domain.PublisherErrorDomain, SessionStateFailed.getErrorCode(), "Ice connection state is fail.");
//                MTKDataStore.getInstance().client.listener.onError(MTKDataStore.getInstance().client, error);
//            });
//        }
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
    }


    @Override
    public void onDataChannel(DataChannel dataChannel) {
        dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
            }

            @Override
            public void onStateChange() {
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                if(enableDataChannel){
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            ByteBuffer body = buffer.data;
                            byte[] bytes = new byte[body.remaining()];
                            body.get(bytes);
                            final String jsonStr = new String(bytes);
                            JSONObject response = new JSONObject(jsonStr);
                            if(MTKDataStore.getInstance().client != null){
                                MTKDataStore.getInstance().client.listener.onReceivedBroadcastSignal(MTKDataStore.getInstance().client, response);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onRenegotiationNeeded() {
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
    }
}
