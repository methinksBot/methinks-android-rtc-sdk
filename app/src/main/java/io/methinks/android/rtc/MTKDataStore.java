package io.methinks.android.rtc;

import android.content.Context;
import android.media.projection.MediaProjection;

import org.webrtc.EglBase;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;

import java.util.ArrayList;

import okhttp3.WebSocket;

public class MTKDataStore {

    private MTKDataStore instance;

    protected Context context;
    protected MediaProjection mediaProjection;
    protected MTKVideoChatClient client;
    protected WebSocket janus;
    protected PeerConnectionFactory pcFactory;

    protected String bucket;        // S3 bucket name
    protected String sId;           // for appTest
    protected String secret;
    protected String projectId;     // Parse Campaign object id
    protected String userId;
    protected String role;
    protected String userName;
    protected String profilePicURL;
    protected long roomId;
    protected String roomToken;
    protected String roomPin;
    protected long roomStartDate = 0;
    protected long roomEndDate = 0;
    protected String apiToken;      // for authentication.
    protected String roomType;
    protected String targetServer;  // dev, stag or prod
    protected String baseFeature;
    protected EglBase eglBase;
    protected MTKVideoChatSession mainSession;
    protected MTKVideoChatSession subSession; // for screen sharing
    protected MTKPublisher mainPublisher;
    protected MTKPublisher screenSharingPublisher;
//    protected LinkedHashMap<Long, MTKSubscriber> subscribers;   // key: feed id, value: MTKSubscriber object
    protected ArrayList<MTKSubscriber> subscribers;   // key: feed id, value: MTKSubscriber object

    protected String url;
    protected ArrayList<Integer> recordingPids;

    protected ArrayList<PeerConnection.IceServer> iceServers;

    private MTKDataStore(){
        this.subscribers = new ArrayList<>();
        this.recordingPids = new ArrayList<>();
    }

    protected static MTKDataStore getInstance(){
        return LazyHolder.INSTANCE;
    }

    protected void clearSession(){
        mainSession = null;
        subSession = null;
    }

    protected void clear(){
        LazyHolder.INSTANCE.context = null;
        LazyHolder.INSTANCE.sId = null;
        LazyHolder.INSTANCE.mediaProjection = null;
        LazyHolder.INSTANCE.client = null;
        LazyHolder.INSTANCE.janus = null;
        LazyHolder.INSTANCE.pcFactory = null;
        LazyHolder.INSTANCE.bucket = null;
        LazyHolder.INSTANCE.secret = null;
        LazyHolder.INSTANCE.projectId = null;
        LazyHolder.INSTANCE.userId = null;
        LazyHolder.INSTANCE.role = null;
        LazyHolder.INSTANCE.userName = null;
        LazyHolder.INSTANCE.profilePicURL = null;
        LazyHolder.INSTANCE.roomId = 0;
        LazyHolder.INSTANCE.roomToken = null;
        LazyHolder.INSTANCE.roomPin = null;
        LazyHolder.INSTANCE.roomStartDate = 0;
        LazyHolder.INSTANCE.roomEndDate = 0;
        LazyHolder.INSTANCE.apiToken = null;
        LazyHolder.INSTANCE.roomType = null;
        LazyHolder.INSTANCE.targetServer = null;
        LazyHolder.INSTANCE.roomType = null;
        LazyHolder.INSTANCE.targetServer = null;
        LazyHolder.INSTANCE.eglBase = null;
        LazyHolder.INSTANCE.mainSession = null;
        LazyHolder.INSTANCE.subSession = null;
        LazyHolder.INSTANCE.mainPublisher = null;
        LazyHolder.INSTANCE.screenSharingPublisher = null;
        LazyHolder.INSTANCE.subscribers = null;
        LazyHolder.INSTANCE.recordingPids = null;
        LazyHolder.INSTANCE.iceServers = null;
        this.subscribers = new ArrayList<>();
        this.recordingPids = new ArrayList<>();
    }

    private static class LazyHolder{
        private static final MTKDataStore INSTANCE = new MTKDataStore();
    }


}
