package io.methinks.android.rtc;

import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.WebSocket;

public class MTKTransactionUtil {
    private static final String TAG = MTKTransactionUtil.class.getSimpleName();

    private static HashMap<String, JSONObject> transactionMap = new HashMap<>();

    protected static JSONObject getTransactionData(String transactionId){
        if(transactionMap.containsKey(transactionId)){
            return transactionMap.get(transactionId);
        }else{
            return null;
        }
    }

    protected static boolean removeTransaction(String transactionId){
        if(transactionMap.containsKey(transactionId)){
            transactionMap.remove(transactionId);
            return true;
        }else{
            return false;
        }
    }

    protected static String createSession(WebSocket socket){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.create.name());

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String destroySession(WebSocket socket, MTKVideoChatSession session){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.destroy.name());
            json.put("session_id", session.sessionId);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String sendKeepAlive(WebSocket socket, MTKVideoChatSession sessionId){
        try{
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.keepalive);
            json.put("session_id", sessionId.sessionId);
            Log.d("keepAlive " + json.toString());
            return send(socket, json);
        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }
    }

    public static String attachPlugin(WebSocket socket, MTKVideoChatSession session, String plugin){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.attach.name());
            json.put("session_id", session.sessionId);
            json.put("plugin", plugin);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String detachPublisher(WebSocket socket, MTKVideoChatSession session, long handleId){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.detach.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", handleId);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String joinPublisher(WebSocket socket, MTKVideoChatSession session, String display, long handleId){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.message.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", handleId);

            JSONObject body = new JSONObject();
            body.put("request", "join");
            body.put("ptype", "publisher");
            body.put("room", MTKDataStore.getInstance().roomId);
            body.put("pin", MTKDataStore.getInstance().roomPin);
            body.put("data", true);
            body.put("display", display);

            json.put("body", body);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

//    protected static String joinSubscriber(WebSocket socket, MTKVideoChatClient client, long publisherId, long handleId){
//        try {
//            JSONObject json = new JSONObject();
//            json.put("janus", MTKTransactionType.message.name());
//            json.put("session_id", client.mainSession.sessionId);
//            json.put("handle_id", handleId);
//
//            JSONObject body = new JSONObject();
//            body.put("request", "join");
//            body.put("ptype", "subscriber");
//            body.put("room", client.roomId);
//            body.put("feed", publisherId);
//            body.put("data", true);
//            body.put("private_id", client.mainSession.privateId);
//
//            json.put("body", body);
//
//            return send(socket, json, client);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    protected static String joinSubscriber(WebSocket socket, long publisherId, long handleId){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.message.name());
            json.put("session_id", MTKDataStore.getInstance().mainSession.sessionId);
            json.put("handle_id", handleId);

            JSONObject body = new JSONObject();
            body.put("request", "join");
            body.put("ptype", "subscriber");
            body.put("room", MTKDataStore.getInstance().roomId);
            body.put("pin", MTKDataStore.getInstance().roomPin);
            body.put("feed", publisherId);
            body.put("data", true);
            body.put("private_id", MTKDataStore.getInstance().mainSession.privateId);

            json.put("body", body);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String hangUp(WebSocket socket){
        JSONObject empty = new JSONObject();
        return send(socket, empty);
    }

    protected static String start(WebSocket socket, long handleId, SessionDescription sessionDescription){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.message.name());
            json.put("session_id", MTKDataStore.getInstance().mainSession.sessionId);
            json.put("handle_id", handleId);

            JSONObject body = new JSONObject();
            body.put("request", "start");
            body.put("room", MTKDataStore.getInstance().roomId);

            JSONObject jsep = new JSONObject();
            jsep.put("type", "answer");
            jsep.put("sdp", sessionDescription.description);

            json.put("body", body);
            json.put("jsep", jsep);

            return send(socket, json);
        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }

    }

    protected static String sendSDP(WebSocket socket, MTKVideoChatSession session, long handleId, IceCandidate iceCandidate){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.trickle.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", handleId);

            JSONObject msg = new JSONObject();
            msg.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            msg.put("sdpMid", iceCandidate.sdpMid);
            msg.put("candidate", iceCandidate.sdp);

            json.put("candidate", msg);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String sendCandidate(WebSocket socket, MTKVideoChatSession session, long handleId, IceCandidate iceCandidate){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.trickle.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", handleId);

            JSONObject candidate = new JSONObject();
            if (iceCandidate == null) {
                candidate.put("completed", true);
            }else {
                candidate.put("candidate", iceCandidate.sdp);
                candidate.put("sdpMid", iceCandidate.sdpMid);
                candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            }

            json.put("candidate", candidate);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String requestInfo(WebSocket socket){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.info.name());
            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


    protected static String requestConfigureForOffer(WebSocket socket, MTKVideoChatSession session, long handleId, SessionDescription sessionDescription){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.message.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", handleId);

            JSONObject body = new JSONObject();
            body.put("request", MTKTransactionType.MessageType.configure);
            body.put("audio", false);
            body.put("video", false);
            body.put("data", true);
            body.put("record", false);

            JSONObject jsep = new JSONObject();
            jsep.put("type", "offer");
            jsep.put("sdp", sessionDescription.description);

            json.put("body", body);
            json.put("jsep", jsep);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String requestConfigureForRecordingStart(WebSocket socket, MTKPublisher publisher, MTKVideoChatSession session, long handleId, boolean audioSend, boolean videoSend){
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("/home/ubuntu/");
            sb.append("record" + "/");
            sb.append(MTKDataStore.getInstance().bucket + "_");
            sb.append(MTKDataStore.getInstance().targetServer + "_");
            sb.append(MTKDataStore.getInstance().projectId + "_");
            sb.append(MTKDataStore.getInstance().roomToken + "_");
            sb.append(MTKDataStore.getInstance().roomType + "@");
            sb.append(publisher.videoType + "_");
            sb.append(MTKDataStore.getInstance().role + "_");

            if(MTKDataStore.getInstance().roomType.equals(MTKConst.ROOM_TYPE_APP_TEST) && !TextUtils.isEmpty(MTKDataStore.getInstance().sId)){
                sb.append(MTKDataStore.getInstance().sId + "_0_");
            }else{
                sb.append(MTKDataStore.getInstance().userId + "_0_");
            }
            sb.append(new Date().getTime());

            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.message.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", handleId);

            JSONObject body = new JSONObject();
            body.put("request", MTKTransactionType.MessageType.configure);
            body.put("audio", audioSend);
            body.put("video", videoSend);
            body.put("data", true);
            body.put("record", true);
            body.put("filename", sb.toString());



            json.put("body", body);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String requestConfigureForRecordingStop(WebSocket socket, MTKVideoChatSession session, long handleId){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.message.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", handleId);

            JSONObject body = new JSONObject();
            body.put("request", MTKTransactionType.MessageType.configure);
            body.put("audio", false);
            body.put("video", false);
            body.put("data", true);
            body.put("record", false);

            json.put("body", body);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String requestConfigure(WebSocket socket, MTKPublisher publisher, MTKVideoChatSession session, long handleId, SessionDescription sessionDescription, boolean audioSend, boolean videoSend){
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("/home/ubuntu/");
            sb.append("record" + "/");
            sb.append(MTKDataStore.getInstance().bucket + "_");
            sb.append(MTKDataStore.getInstance().targetServer + "_");
            sb.append(MTKDataStore.getInstance().projectId + "_");
            sb.append(MTKDataStore.getInstance().roomToken + "_");
            sb.append(MTKDataStore.getInstance().roomType + "@");
            sb.append(publisher.videoType + "_");
            sb.append(MTKDataStore.getInstance().role + "_");
            if(MTKDataStore.getInstance().roomType.equals(MTKConst.ROOM_TYPE_APP_TEST) && !TextUtils.isEmpty(MTKDataStore.getInstance().sId)){
                sb.append(MTKDataStore.getInstance().sId + "_0_");
            }else{
                sb.append(MTKDataStore.getInstance().userId + "_0_");
            }

            sb.append(new Date().getTime());

            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.message.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", handleId);

            JSONObject body = new JSONObject();
            body.put("request", MTKTransactionType.MessageType.configure);
            body.put("audio", audioSend);
            body.put("video", videoSend);
            body.put("data", true);
            body.put("record", true);
            body.put("filename", sb.toString());

            JSONObject jsep = new JSONObject();
            jsep.put("type", "offer");
            jsep.put("sdp", sessionDescription.description);

            json.put("body", body);
            json.put("jsep", jsep);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String startRTPForwarding(WebSocket socket, MTKPublisher publisher, MTKVideoChatSession session){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.message.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", publisher.handleId);

            JSONObject body = new JSONObject();
            body.put("request", MTKTransactionType.MessageType.rtp_forward);
            body.put("room", MTKDataStore.getInstance().roomId);
            body.put("secret", MTKDataStore.getInstance().secret);
            body.put("publisher_id", publisher.feedId);    // publisher feed id
//            body.put("video_port", 5000);
            body.put("video_port", 10000);
            body.put("video_rtcp_port", 5001);
//            body.put("audio_port", 5002);
            body.put("audio_port", 10002);
            body.put("audio_rtcp_port", 5003);
            body.put("audio_pt", 111);
            body.put("video_pt", 98);
            body.put("data_port", 5005);
//            body.put("host", "35.169.191.118");           // amazon medialive
//            body.put("host", "34.238.245.195");            // media-server-1 -> ffmpeg
            body.put("host", "3.84.189.62");    // media-server-2 -> Gstreamer


            json.put("body", body);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String startRTPForwarding(WebSocket socket, MTKPublisher publisher, MTKVideoChatSession session, int videoPort, int audioPort){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.message.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", publisher.handleId);

            JSONObject body = new JSONObject();
            body.put("request", MTKTransactionType.MessageType.rtp_forward);
            body.put("room", MTKDataStore.getInstance().roomId);
            body.put("secret", MTKDataStore.getInstance().secret);
            body.put("publisher_id", publisher.feedId);    // publisher feed id
            body.put("video_port", videoPort);
            body.put("video_rtcp_port", 5001);
            body.put("audio_port", audioPort);
            body.put("audio_rtcp_port", 5003);
            body.put("audio_pt", 111);
            body.put("video_pt", 98);
            body.put("data_port", 5005);
//            body.put("host", "35.169.191.118");           // amazon medialive
//            body.put("host", "34.238.245.195");            // media-server-1 -> ffmpeg
            body.put("host", "3.84.189.62");    // media-server-2 -> Gstreamer

            json.put("body", body);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static String stopRTPForwarding(WebSocket socket, MTKPublisher publisher, MTKVideoChatSession session){
        try {
            JSONObject json = new JSONObject();
            json.put("janus", MTKTransactionType.message.name());
            json.put("session_id", session.sessionId);
            json.put("handle_id", publisher.handleId);

            JSONObject body = new JSONObject();
            body.put("request", MTKTransactionType.MessageType.stop_rtp_forward);
            body.put("room", MTKDataStore.getInstance().roomId);
            body.put("secret", "kqtoixA5wL1559875454878");
            body.put("publisher_id", publisher.feedId);    // publisher feed id
//            body.put("audio_port", );
//            body.put("audio_pt", );
//            body.put("video_port", );
//            body.put("video_pt", );
//            body.put("host", );

            json.put("body", body);

            return send(socket, json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String send(WebSocket socket, JSONObject json){
        try {
            String transactionId = createTransactionId();
            json.put("transaction", transactionId);
            json.put("token", MTKDataStore.getInstance().apiToken);

            if(!(json.has("janus") && json.getString("janus").equals("keepalive"))){
//                Log.d(TAG, "websocket send Message text : " + json);
            }


//            Log.e("websocket protocol send : " + json.toString());

            socket.send(json.toString());
            transactionMap.put(transactionId, json);

            return transactionId;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String hmac(String key, String msg){
        try {
            final String hmacSHA1 = "HmacSHA1";
            Mac mac = Mac.getInstance(hmacSHA1);
            mac.init(new SecretKeySpec(key.getBytes(), hmacSHA1));
            byte[] result = mac.doFinal(msg.getBytes());
            String hash = Base64.encodeToString(result, Base64.DEFAULT);

            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0, v; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String createTransactionId(){
        int len = 12;
        String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String transactionId = "";

        for (int i = 0; i < len; i++) {
            int position = (int) Math.floor(Math.random() * charSet.length());
            transactionId += charSet.substring(position, position + 1);
        }

        return transactionId;
    }
}
