package io.methinks.android.mtkrtc;

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
    private boolean enableDataChannel;

    private void initDataChannel(MTKVideoChatSession session){
        this.session = session;
        DataChannel.Init init = new DataChannel.Init();
        init.ordered = false;
        this.dataChannel = peerConnection.createDataChannel("mtkrtcdata", init);

        this.dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
                Log.e("DataChannel onBufferedAmountChange : " + l);
            }

            @Override
            public void onStateChange() {
                Log.e("DataChannel onStateChange : " + dataChannel.state());
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
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
                    }
                });
            }
        });
    }

    boolean isPublisher;
    MTKPeerConnectionClient(Context context, PeerConnectionFactory factory, PeerConnection.RTCConfiguration config, MTKVideoChatSession session, boolean enableDataChannelr) {
        this.context = context;
        this.enableDataChannel = enableDataChannel;
        peerConnection = factory.createPeerConnection(config, this);
        initDataChannel(session);
    }

    MTKPeerConnectionClient(Context context, PeerConnectionFactory factory, PeerConnection.RTCConfiguration config, MTKVideoChatSession session, boolean enableDataChannel, boolean isPublisher) {
        this.context = context;
        this.enableDataChannel = enableDataChannel;
        peerConnection = factory.createPeerConnection(config, this);
        this.isPublisher = isPublisher;
        initDataChannel(session);
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.e("onSignalingChange SignalingState : " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.e("onIceConnectionChange PeerConnectionState : " + iceConnectionState);
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.e("onIceConnectionReceivingChange : " + b);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.e("onIceGatheringChange IceGatheringState : " + iceGatheringState);
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.e("onIceCandidate");
//        Util.printAllObject(TAG, iceCandidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        Log.e("onIceCandidatesRemoved");

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.e("onAddStream");
//        Util.printAllObject(TAG, mediaStream);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.e("onRemoveStream");
//        Util.printAllObject(TAG, mediaStream);
    }


    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.e("DataChannel onDataChannel state : " + dataChannel.state());
//        Util.printAllObject(TAG, dataChannel);
//        this.dataChannel = dataChannel;
        dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
                Log.e("DataChannel onBufferedAmountChange : " + l);
            }

            @Override
            public void onStateChange() {
                Log.e("DataChannel onStateChange : " + dataChannel.state());
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                if(enableDataChannel){
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ByteBuffer body = buffer.data;
                                byte[] bytes = new byte[body.remaining()];
                                body.get(bytes);
                                final String jsonStr = new String(bytes);
                                JSONObject response = new JSONObject(jsonStr);
                                Log.e("onMessage DataChannel : " + response.toString());
                                MTKDataStore.getInstance().client.listener.onReceivedBroadcastSignal(MTKDataStore.getInstance().client, response);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.e("onRenegotiationNeeded");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {Log.e("onAddTrack");}
}
