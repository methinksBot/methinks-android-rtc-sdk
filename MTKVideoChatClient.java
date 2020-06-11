package io.methinks.android.rtc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/** these should be changed by baseFeature ( android.rtc.MTKError / android.mtkrtc.MTKError )*/
import static io.methinks.android.rtc.MTKError.Domain.SessionErrorDomain;

public class MTKVideoChatClient {
    private static final String TAG = MTKVideoChatClient.class.getSimpleName();

    protected MTKAudioManager audioManager;
    protected WebSocket janus;

    protected static final ExecutorService executor = Executors.newSingleThreadExecutor();
    protected MTKRTCClientListener listener;
    private Map<Long, Boolean> keepAlives;

    private MTKVideoChatClient(Builder builder) {
        MTKDataStore dataStore = MTKDataStore.getInstance();
        dataStore.clear();
        dataStore.context = builder.context;
        dataStore.bucket = builder.bucket;
        dataStore.secret = builder.secret;
        dataStore.roomType = builder.roomType;
        dataStore.userId = builder.userId;
        dataStore.userName = builder.userName;
        dataStore.profilePicURL = builder.profilePicURL;
        dataStore.role = MTKConst.USER_ROLE_CUSTOMER;
        dataStore.projectId = builder.projectId;
        dataStore.roomId = builder.roomId;
        dataStore.roomPin = builder.roomPin;
        dataStore.roomToken = builder.roomToken;
        dataStore.apiToken = builder.apiToken;
        dataStore.roomStartDate = builder.roomStartDate;
        dataStore.roomEndDate = builder.roomEndDate;
        dataStore.targetServer = builder.targetServer;
        dataStore.eglBase = builder.eglBase;
        dataStore.roomType = builder.roomType;
        dataStore.sId = builder.sId;
        dataStore.janus = this.janus;
        dataStore.client = this;
        dataStore.useExtensionForWebSharing = builder.useExtensionForWebSharing;
        dataStore.url = builder.socketURL;
        dataStore.iceServers = builder.iceServers;
        dataStore.baseFeature = builder.baseFeature;

        this.listener = builder.listener;
        this.keepAlives = new HashMap<>();
    }

    public void connect(){
        listener.onChangedClientState(MTKVideoChatClient.this, MTKVideoChatClientState.connecting);

        executor.execute(() -> {
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(MTKDataStore.getInstance().context).createInitializationOptions());
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

            DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(MTKDataStore.getInstance().eglBase.getEglBaseContext(), true, true);
            DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(MTKDataStore.getInstance().eglBase.getEglBaseContext());

            if(MTKDataStore.getInstance().baseFeature.equals(MTKConst.BASE_FEATURE_BUSINESS)){
                MTKDataStore.getInstance().pcFactory = PeerConnectionFactory.builder()
                        .setVideoEncoderFactory(defaultVideoEncoderFactory)
                        .setVideoDecoderFactory(defaultVideoDecoderFactory)
                        .setOptions(options)
                        .setAudioDeviceModule(createJavaAudioDevice())
                        .createPeerConnectionFactory();
            } else {
                MTKDataStore.getInstance().pcFactory = PeerConnectionFactory.builder()
                        .setVideoEncoderFactory(defaultVideoEncoderFactory)
                        .setVideoDecoderFactory(defaultVideoDecoderFactory)
                        .setOptions(options)
                        .createPeerConnectionFactory();
            }
        });

        OkHttpClient client = new OkHttpClient.Builder().readTimeout(3, TimeUnit.SECONDS).build();
        Request request = new Request.Builder()
                .header(MTKConst.WEBSOCKET_HEADER_NAME, MTKConst.WEBSOCKET_HEADER_VALUE)
                .url(MTKDataStore.getInstance().url)
                .build();
        Log.e("url : " + MTKDataStore.getInstance().url);
        janus = client.newWebSocket(request, janusListener);
        if(!MTKDataStore.getInstance().roomType.equals(MTKConst.ROOM_TYPE_APP_TEST)){
            audioManager = MTKAudioManager.create(MTKDataStore.getInstance().context);
            if(MTKDataStore.getInstance().baseFeature.equals(MTKConst.BASE_FEATURE_BUSINESS)) {
                audioManager.start(new MTKAudioManager.AudioManagerEvents() {
                    @Override
                    public void onAudioDeviceChanged(MTKAudioManager.AudioDevice selectedAudioDevice, Set<MTKAudioManager.AudioDevice> availableAudioDevices) {
                        Log.e("onAudioManagerDevicesChanged: " + availableAudioDevices + ", " + "selected: " + selectedAudioDevice);
                    }
                });
            } else {
                audioManager.start((selectedAudioDevice, availableAudioDevices) -> {
                });
            }
        }

    }

    public void disconnect(){
        if(MTKDataStore.getInstance().mainPublisher != null){
            unpublish(MTKDataStore.getInstance().mainPublisher);
        }
        if(MTKDataStore.getInstance().screenSharingPublisher != null){
            unpublish(MTKDataStore.getInstance().screenSharingPublisher);
        }
        if(MTKDataStore.getInstance().subscribers != null && MTKDataStore.getInstance().subscribers.size() > 0){
            for(MTKSubscriber subscriber : MTKDataStore.getInstance().subscribers){
                unsubscribe(subscriber);
            }
            MTKDataStore.getInstance().subscribers.clear();
        }
        if(audioManager != null){
            audioManager.stop();
            audioManager = null;
        }
    }

    public void publish(MTKPublisher publisher){
        if(MTKDataStore.getInstance().mainSession != null){
            if(MTKDataStore.getInstance().mainPublisher == null){
                MTKDataStore.getInstance().mainPublisher = publisher;
                publisher.tempTransactionId = MTKTransactionUtil.attachPlugin(janus, MTKDataStore.getInstance().mainSession, MTKConst.PLUGIN_VIDEOROOM);
                publisher.session = MTKDataStore.getInstance().mainSession;
            }else if(MTKDataStore.getInstance().screenSharingPublisher == null){
                MTKDataStore.getInstance().subSession = new MTKVideoChatSession();
                MTKDataStore.getInstance().subSession.tempTansactionId = MTKTransactionUtil.createSession(janus);
                MTKDataStore.getInstance().screenSharingPublisher = publisher;
                publisher.session = MTKDataStore.getInstance().subSession;


                final boolean[] flag = {true};
                new Thread(() -> {
                    while(flag[0]){
                        if(MTKDataStore.getInstance().subSession != null && MTKDataStore.getInstance().subSession.sessionId != 0){
                            publisher.session = MTKDataStore.getInstance().subSession;
                            publisher.tempTransactionId = MTKTransactionUtil.attachPlugin(janus, MTKDataStore.getInstance().subSession, MTKConst.PLUGIN_VIDEOROOM);
                            flag[0] = false;
                        }
                    }
                }).start();
            }
        }
    }

    public void unpublish(final MTKPublisher publisher){
        if(publisher != null){
            synchronized (this) {
                if (publisher.pcClient != null && publisher.pcClient.peerConnection != null) {
                    executor.execute(() -> {
                        if (publisher.videoTrack != null){
                            publisher.videoTrack.dispose();
                            publisher.videoTrack = null;
                        }
                        if(publisher.audioTrack != null){
                            publisher.audioTrack.dispose();
                            publisher.audioTrack = null;
                        }

                        if (publisher.pcClient.peerConnection != null) {
                            publisher.pcClient.peerConnection.close();
                            publisher.pcClient.peerConnection = null;
                        }

                        if (publisher.pcClient != null) {
                            if (publisher.pcClient.dataChannel != null) {
                                publisher.pcClient.dataChannel.dispose();
                            }
                            publisher.pcClient.dataChannel = null;
                            publisher.pcClient = null;
                        }


                        if(publisher == MTKDataStore.getInstance().mainPublisher){
                            keepAlives.put(MTKDataStore.getInstance().mainSession.sessionId, false);
                            MTKTransactionUtil.destroySession(janus, MTKDataStore.getInstance().mainSession);
                            MTKDataStore.getInstance().mainPublisher = null;
                            MTKDataStore.getInstance().mainSession = null;
                        }else if(publisher == MTKDataStore.getInstance().screenSharingPublisher){
                            keepAlives.put(MTKDataStore.getInstance().subSession.sessionId, false);
                            MTKTransactionUtil.destroySession(janus, MTKDataStore.getInstance().subSession);
                            MTKDataStore.getInstance().screenSharingPublisher = null;
                            MTKDataStore.getInstance().subSession = null;
                        }
                    });
                }
            }
        }
    }

    public void unsubscribe(MTKSubscriber subscriber){
        if(subscriber.videoTrack != null){
            subscriber.videoTrack.dispose();
            subscriber.videoTrack = null;
        }

        if(subscriber.audioTrack != null){
            subscriber.audioTrack.dispose();
            subscriber.audioTrack = null;
        }

        if (MTKDataStore.getInstance().baseFeature.equals(MTKConst.BASE_FEATURE_BUSINESS) && subscriber != null) {
            if (subscriber.pcClient != null) {
                if (subscriber.pcClient.peerConnection != null) {
                    subscriber.pcClient.peerConnection.close();
                    subscriber.pcClient.peerConnection = null;
                }
                if (subscriber.pcClient.dataChannel != null) {
                    subscriber.pcClient.dataChannel.dispose();
                }
                subscriber.pcClient.dataChannel = null;
                subscriber.pcClient = null;
            }
        }
    }

    protected WebSocketListener janusListener = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            if(MTKDataStore.getInstance().mainSession != null){
                try {
                    throw new IllegalAccessException("Main session already exists.");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }else{
                MTKDataStore.getInstance().mainSession = new MTKVideoChatSession();
                MTKDataStore.getInstance().mainSession.tempTansactionId = MTKTransactionUtil.createSession(janus);
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            listener.onChangedClientState(MTKVideoChatClient.this, MTKVideoChatClientState.disconnected);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            Log.e("onFailure() : " + t.getMessage());
            MTKError error = new MTKError(SessionErrorDomain, MTKError.ErrorCode.SessionUnexpectedGetSessionInfoResponse.getErrorCode(), "Could not open session");
            listener.onError(MTKVideoChatClient.this, error);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            synchronized (this) {
                try {
                    Log.e("websocket protocol receive : " + text);
                    JSONObject receivedJson = new JSONObject(text);
                    JSONObject data = receivedJson.has("data") ? receivedJson.getJSONObject("data") : null;
                    String receivedCommand = receivedJson.getString("janus");

                    if(receivedJson.has("transaction")){
                        String transactionId = receivedJson.getString("transaction");
                        JSONObject savedJson = MTKTransactionUtil.getTransactionData(transactionId);
                        if(savedJson != null) {
                            MTKUtil.printJSON("", "received transaction data", savedJson);
                            String savedCommand = savedJson.getString("janus");
                            if (receivedCommand.equals(MTKTransactionType.ack.name())) { // received ack
                                MTKTransactionUtil.removeTransaction(transactionId);
                            }

                            if (savedCommand.equals(MTKTransactionType.create.name())) {    // created session
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onChangedClientState(MTKVideoChatClient.this, MTKVideoChatClientState.connected);
                                    }
                                });
//                                ((Activity)MTKDataStore.getInstance().context).runOnUiThread(() -> listener.onChangedClientState(MTKVideoChatClient.this, MTKVideoChatClientState.connected));
                                if(MTKDataStore.getInstance().mainSession != null && MTKDataStore.getInstance().mainSession.tempTansactionId.equals(transactionId)){  // created main session
                                    MTKDataStore.getInstance().mainSession.sessionId = data.getLong("id");   // set session id
                                    keepAlives.put(MTKDataStore.getInstance().mainSession.sessionId, true);
                                    sendKeepAlive(MTKDataStore.getInstance().mainSession);
                                    Log.e("set main session id : " + MTKDataStore.getInstance().mainSession.sessionId);
                                }else if(MTKDataStore.getInstance().subSession != null && MTKDataStore.getInstance().subSession.tempTansactionId.equals(transactionId)){ // created sub session
                                    MTKDataStore.getInstance().subSession.sessionId = data.getLong("id");   // set session id
                                    keepAlives.put(MTKDataStore.getInstance().subSession.sessionId, true);
                                    sendKeepAlive(MTKDataStore.getInstance().subSession);
                                    Log.e("set sub session id : " + MTKDataStore.getInstance().subSession.sessionId);
                                }
                            } else if (savedCommand.equals(MTKTransactionType.attach.name()) && receivedCommand.equals(MTKTransactionType.success.name())) { // attached plugin
                                if(MTKUtil.isPublisherTransaction(transactionId)){ // attached publisher's handle
                                    if(MTKDataStore.getInstance().mainPublisher.tempTransactionId.equals(transactionId)){   // attached main publisher
                                        MTKDataStore.getInstance().mainPublisher.handleId = data.getLong("id");
                                        MTKDataStore.getInstance().mainPublisher.joinPublisher();
                                    }else if(MTKDataStore.getInstance().screenSharingPublisher.tempTransactionId.equals(transactionId)){ // attached screen sharing publisher
                                        MTKDataStore.getInstance().screenSharingPublisher.handleId = data.getLong("id");
                                        MTKDataStore.getInstance().screenSharingPublisher.joinPublisher();
                                    }
                                }else{  // attached subscriber's handle
                                    MTKSubscriber subscriber = MTKUtil.getSubscriberFromTransactionId(transactionId);
                                    subscriber.handleId = data.getLong("id");
                                    MTKTransactionUtil.joinSubscriber(MTKDataStore.getInstance().client.janus, subscriber.feedId, subscriber.handleId);
                                }
                            }
                        }
                    }

                    if(receivedJson.has("sender")){
                        Long senderHandleId = receivedJson.getLong("sender");
                        MTKPerson person = null;
                        if(MTKUtil.isPublisherFromHandleId(senderHandleId)){    // publisher handle
//                            MTKPublisher publisher = MTKUtil.getPublisherFromHandleId(senderHandleId);
                            person = MTKUtil.getPublisherFromHandleId(senderHandleId);
                        }else{ // subscriber handle
                            person = MTKUtil.getSubscriberFromHandleId(senderHandleId);
                        }

                        if(receivedCommand.equals("trickle")) {
                            JSONObject candidate = receivedJson.getJSONObject("candidate");
                            person.addIceCandidate(
                                    new IceCandidate(candidate.getString("sdpMid"),
                                            candidate.getInt("sdpMLineIndex"),
                                            candidate.getString("candidate")));
                        }else if(receivedCommand.equals("hangup")){
                            MTKSubscriber subscriber = MTKUtil.getSubscriberFromHandleId(senderHandleId);
                            if(subscriber != null){
                                if (MTKDataStore.getInstance().baseFeature.equals(MTKConst.BASE_FEATURE_BUSINESS)) {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(!subscriber.role.equals(MTKConst.USER_ROLE_OBSERVER)){
                                                listener.onRemovedStreamOfSubscriber(MTKVideoChatClient.this, subscriber);
                                            }
                                            MTKDataStore.getInstance().subscribers.remove(subscriber);
                                            if(MTKDataStore.getInstance().subscribers != null && MTKUtil.equalObserverCountAndSubscriberCount()){
                                                if(MTKDataStore.getInstance().client != null && MTKDataStore.getInstance().client.janus != null && MTKDataStore.getInstance().mainPublisher != null && MTKDataStore.getInstance().mainPublisher.session != null) {
                                                    MTKTransactionUtil.requestConfigureForRecordingStop(MTKDataStore.getInstance().client.janus, MTKDataStore.getInstance().mainPublisher.session, MTKDataStore.getInstance().mainPublisher.handleId);
                                                }
                                            }
                                        }
                                    });
                                } else if (MTKDataStore.getInstance().baseFeature.equals(MTKConst.BASE_FEATURE_APPTEST)){
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        if(!subscriber.role.equals(MTKConst.USER_ROLE_OBSERVER)){
                                            listener.onRemovedStreamOfSubscriber(MTKVideoChatClient.this, subscriber);
                                        }
                                        MTKDataStore.getInstance().subscribers.remove(subscriber);
                                        if(MTKDataStore.getInstance().subscribers != null && MTKUtil.equalObserverCountAndSubscriberCount()){
                                            MTKTransactionUtil.requestConfigureForRecordingStop(MTKDataStore.getInstance().client.janus, MTKDataStore.getInstance().mainPublisher.session, MTKDataStore.getInstance().mainPublisher.handleId);
                                        }
                                    });
                                }
//                                ((Activity)MTKDataStore.getInstance().context).runOnUiThread(() -> {
//                                    if(!subscriber.role.equals(MTKConst.USER_ROLE_OBSERVER)){
//                                        listener.onRemovedStreamOfSubscriber(MTKVideoChatClient.this, subscriber);
//                                    }
//                                    MTKDataStore.getInstance().subscribers.remove(subscriber);
//                                    if(MTKDataStore.getInstance().subscribers != null & MTKDataStore.getInstance().subscribers.size() == 0){
////                                        MTKTransactionUtil.requestConfigureForRecordingStop(MTKDataStore.getInstance().client.janus, MTKDataStore.getInstance().mainPublisher.session, MTKDataStore.getInstance().mainPublisher.handleId);
////                                        int pid = MTKDataStore.getInstance().recordingPids.get(0);
////                                        new MTKMediaServerConnector().close(pid, new MTKMediaServerConnector.Callback() {
////                                            @Override
////                                            public void getResult(JSONObject json) {
////                                                MTKTransactionUtil.stopRTPForwarding(MTKDataStore.getInstance().client.janus, MTKDataStore.getInstance().mainPublisher, MTKDataStore.getInstance().mainPublisher.session);
////                                            }
////                                        });
////                                        MTKTransactionUtil.stopRTPForwarding(MTKDataStore.getInstance().client.janus, MTKDataStore.getInstance().mainPublisher, MTKDataStore.getInstance().mainPublisher.session);
//                                        MTKTransactionUtil.requestConfigureForRecordingStop(MTKDataStore.getInstance().client.janus, MTKDataStore.getInstance().mainPublisher.session, MTKDataStore.getInstance().mainPublisher.handleId);
//
//                                    }
//                                });
                            }
                        }
                        if(receivedJson.has("jsep")) {
                            JSONObject jsepJson = receivedJson.getJSONObject("jsep");
                            if(jsepJson.getString("type").equals("offer")) {
                                JSONObject pluginData = receivedJson.getJSONObject("plugindata");
                                JSONObject dataOfPluginData = pluginData.has("data") ? pluginData.getJSONObject("data") : null;
                                person.receiveOffer(dataOfPluginData, jsepJson.getString("sdp"), false, false);
                            }else {
                                person.receiveAnswer(jsepJson.getString("sdp"));
                            }
                        }
                        if(receivedJson.has("plugindata") && receivedJson.getJSONObject("plugindata") != null){
                            JSONObject pluginData = receivedJson.getJSONObject("plugindata");
                            JSONObject dataOfPluginData = pluginData.has("data") ? pluginData.getJSONObject("data") : null;
                            if(MTKUtil.isPublisherFromHandleId(senderHandleId)){
                                MTKPublisher publisher = MTKUtil.getPublisherFromHandleId(senderHandleId);
                                publisher.joinedPublisher(dataOfPluginData);
                            }

                            if(dataOfPluginData.has("leaving")){
                                if(dataOfPluginData.has("reason")){
                                    if(dataOfPluginData.getString("reason").equals("kicked")){
                                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                MTKError error = new MTKError(SessionErrorDomain, MTKError.ErrorCode.PublisherUnableToPublish.getErrorCode(), "Publish is kicked");
                                                listener.onError(MTKDataStore.getInstance().client, error);
                                            }
                                        });
                                    }
                                }

                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private Timer mTimer;
    private void sendKeepAlive(MTKVideoChatSession session){
        Log.d("Start keepAlive call");
        if (MTKDataStore.getInstance().baseFeature.equals(MTKConst.BASE_FEATURE_BUSINESS)) {
            new Thread(() -> {
                while (keepAlives.get(session.sessionId)) {
                    try {
                        Thread.sleep(50000);
                        MTKTransactionUtil.sendKeepAlive(janus, session);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            mTimer = new Timer();
            mTimer.schedule(new CustomTimer(session), 0, 5000);
        }
    }

    class CustomTimer extends TimerTask {
        private MTKVideoChatSession session;
        public CustomTimer(MTKVideoChatSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            Log.e("##################@@@@@@@@@!!!!!!!!!######");
            MTKTransactionUtil.sendKeepAlive(janus, session);
        }
    }

    public void sendSignal(JSONObject data){
        ByteBuffer buffer = ByteBuffer.wrap(data.toString().getBytes());
        MTKDataStore.getInstance().mainPublisher.pcClient.dataChannel.send(new DataChannel.Buffer(buffer, false));
    }

    public interface MTKRTCClientListener{
        void onChangedClientState(MTKVideoChatClient client, MTKVideoChatClientState statestart);
        void onStartedPublishing(MTKVideoChatClient client, MTKPublisher publisher);
        void onAddedStream(MTKVideoChatClient client, MediaStream mediaStream, MTKSubscriber subscriber, MTKVideoChatSession session);
        void onRemovedStreamOfSubscriber(MTKVideoChatClient client, MTKSubscriber subscriber);
        void onReceivedBroadcastSignal(MTKVideoChatClient client, JSONObject data);  // from data channel
        void onCreatedLocalExternalSampleCapturer(MTKVideoChatClient client, JSONObject data); // TODO: 13/05/2019 needs changing param
        void getStats(MTKVideoChatClient client, ArrayList<String> stats);
        void onError(MTKVideoChatClient client, MTKError e);
    }

    public enum MTKVideoChatClientState{
        connecting,
        connected,
        disconnected
    }

    public static final class Builder {
        private Context context;
        private String bucket;
        private String userId;
        private String userName;
        private String profilePicURL;
        private String projectId;
        private String secret;
        private String socketURL;
        private long roomId;
        private String roomPin;
        private String apiToken;
        private String roomToken;
        private long roomStartDate;
        private long roomEndDate;
        private String targetServer;
        private EglBase eglBase;
        private String roomType;
        private MTKRTCClientListener listener;
        private boolean useExtensionForWebSharing;
        private ArrayList<PeerConnection.IceServer> iceServers;
        private String sId; // for appTest
        private String baseFeature;

        public Builder() {
        }

        public Builder context(Context val) {
            context = val;
            return this;
        }

        public Builder bucket(String val){
            bucket = val;
            return this;
        }

        public Builder userId(String val) {
            userId = val;
            return this;
        }

        public Builder userName(String val) {
            userName = val;
            return this;
        }

        public Builder profilePicURL(String val) {
            profilePicURL = val;
            return this;
        }

        public Builder projectId(String val) {
            projectId = val;
            return this;
        }

        public Builder roomId(long val) {
            roomId = val;
            return this;
        }

        public Builder roomPin(String val) {
            roomPin = val;
            return this;
        }

        public Builder apiToken(String val) {
            apiToken = val;
            return this;
        }

        public Builder roomStartDate(long val) {
            roomStartDate = val;
            return this;
        }
        public Builder socketURL(String val) {
            socketURL = val;
            return this;
        }

        public Builder roomEndDate(long val) {
            roomEndDate = val;
            return this;
        }

        public Builder targetServer(String val) {
            targetServer = val;
            return this;
        }

        public Builder eglBase(EglBase val) {
            eglBase = val;
            return this;
        }

        public Builder roomType(String val) {
            roomType = val;
            return this;
        }

        public Builder roomToken(String val) {
            roomToken = val;
            return this;
        }

        public Builder secret(String val){
            secret = val;
            return this;
        }

        public Builder listener(MTKRTCClientListener val){
            listener = val;
            return this;
        }

        public Builder useExtensionForWebSharing(boolean val) {
            useExtensionForWebSharing = val;
            return this;
        }

        public Builder iceServers(ArrayList<PeerConnection.IceServer> val) {
            iceServers = val;
            return this;
        }

        public Builder sId(String val){
            sId = val;
            return this;
        }

        public Builder baseFeature(String val) {
            baseFeature = val;
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "context=" + context +
                    ", bucket='" + bucket + '\'' +
                    ", userId='" + userId + '\'' +
                    ", userName='" + userName + '\'' +
                    ", profilePicURL='" + profilePicURL + '\'' +
                    ", projectId='" + projectId + '\'' +
                    ", secret='" + secret + '\'' +
                    ", socketURL='" + socketURL + '\'' +
                    ", roomId=" + roomId +
                    ", roomPin='" + roomPin + '\'' +
                    ", apiToken='" + apiToken + '\'' +
                    ", roomToken='" + roomToken + '\'' +
                    ", roomStartDate=" + roomStartDate +
                    ", roomEndDate=" + roomEndDate +
                    ", targetServer='" + targetServer + '\'' +
                    ", eglBase=" + eglBase +
                    ", roomType='" + roomType + '\'' +
                    ", listener=" + listener +
                    ", baseFeature='" + baseFeature + '\'' +
                    ", useExtensionForWebSharing=" + useExtensionForWebSharing +
                    ", iceServers=" + iceServers +
                    '}';
        }

        public MTKVideoChatClient build() {
            return new MTKVideoChatClient(this);
        }

    }


    ////////////////////////////////////////// audio device
    AudioDeviceModule createJavaAudioDevice() {
        // Enable/disable OpenSL ES playback.

        // Set audio record error callbacks.
        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = new JavaAudioDeviceModule.AudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e("onWebRtcAudioRecordInitError: " + errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e("onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e("onWebRtcAudioRecordError: " + errorMessage);
            }
        };

        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = new JavaAudioDeviceModule.AudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e("onWebRtcAudioTrackInitError: " + errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(
                    JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e("onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e("onWebRtcAudioTrackError: " + errorMessage);
            }
        };

        return JavaAudioDeviceModule.builder(MTKDataStore.getInstance().context)
                .setUseHardwareAcousticEchoCanceler(false)
                .setUseHardwareNoiseSuppressor(false)
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .createAudioDeviceModule();
    }
}
