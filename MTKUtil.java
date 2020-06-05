package io.methinks.android.mtkrtc;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

public class MTKUtil {

    protected static void printMap(String tag, String marker, Map<Long, JSONObject> map){
        for (Long key: map.keySet()){
            JSONObject value = map.get(key);
            Log.e(tag, marker + "::" + key + "//" + value.toString());
        }
    }

    protected static void printJSON(String tag, String marker, JSONObject json){
        Log.e(tag, marker + "::" + json.toString());
    }

    protected static float convertDpToPixel(Context context, float dp){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    protected static String createId(int len){
        String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String id = "";

        for (int i = 0; i < len; i++) {
            int position = (int)Math.floor(Math.random() * charSet.length());
            id += charSet.substring(position, position + 1);
        }

        return id;
    }

    protected static void printAllObject(String tag, Object obj){
        StringBuilder sb = new StringBuilder();
        for (Field field : obj.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                String name = field.getName();
                Object value = null;
                value = field.get(obj);
                sb.append(name + ":" + value + "\n");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        Log.e(tag, "key and value of this object\n" + sb.toString());

    }

    protected static boolean isPublisherTransaction(String transactionId){
        if(MTKDataStore.getInstance().mainPublisher != null && MTKDataStore.getInstance().mainPublisher.tempTransactionId.equals(transactionId)){
            return true;
        }
        if(MTKDataStore.getInstance().screenSharingPublisher != null && MTKDataStore.getInstance().screenSharingPublisher.tempTransactionId.equals(transactionId)){
            return true;
        }

        for(MTKSubscriber subscriber : MTKDataStore.getInstance().subscribers){
            if(subscriber.tempTransactionId.equals(transactionId)){
                return false;
            }
        }

        return false;
    }

    protected static boolean isPublisherFromHandleId(long handleId){
        if(MTKDataStore.getInstance().mainPublisher != null && MTKDataStore.getInstance().mainPublisher.handleId == handleId){
            return true;
        }
        if(MTKDataStore.getInstance().screenSharingPublisher != null && MTKDataStore.getInstance().screenSharingPublisher.handleId == handleId){
            return true;
        }

        if(MTKDataStore.getInstance().subscribers != null){
            for(MTKSubscriber subscriber : MTKDataStore.getInstance().subscribers){
                if(subscriber.handleId == handleId){
                    return false;
                }
            }
        }

        return false;
    }

    protected static MTKPublisher getPublisherFromHandleId(long handleId){
        if(MTKDataStore.getInstance().mainPublisher != null && MTKDataStore.getInstance().mainPublisher.handleId == handleId){
            return MTKDataStore.getInstance().mainPublisher;
        }

        if(MTKDataStore.getInstance().screenSharingPublisher != null && MTKDataStore.getInstance().screenSharingPublisher.handleId == handleId){
            return MTKDataStore.getInstance().screenSharingPublisher;
        }

        return null;
    }

    protected static MTKSubscriber getSubscriberFromUserId(String userId){
        if(MTKDataStore.getInstance().subscribers != null){
            for(MTKSubscriber subscriber : MTKDataStore.getInstance().subscribers){
                if(subscriber.userId.equals(userId)){
                    return subscriber;
                }
            }
        }
        return null;
    }

    protected static MTKSubscriber getSubscriberFromHandleId(long handleId){
        if(MTKDataStore.getInstance().subscribers != null){
            for(MTKSubscriber subscriber : MTKDataStore.getInstance().subscribers){
                if(subscriber.handleId == handleId){
                    return subscriber;
                }
            }
        }
        return null;
    }

    protected static MTKSubscriber getSubscriberFromTransactionId(String transactionId){
        if(MTKDataStore.getInstance().subscribers != null){
            for(MTKSubscriber subscriber : MTKDataStore.getInstance().subscribers){
                if(subscriber.tempTransactionId.equals(transactionId)){
                    return subscriber;
                }
            }
        }

        return null;
    }

    protected static boolean isCameraVideoType(JSONObject data){
        try {
            if(data == null){
                return true;
            }
            JSONObject display = new JSONObject(data.getString("display"));
            String streamType = display.getString("video_type");

            if(streamType.equals(MTKPerson.StreamVideoType.camera.name())){
                return true;
            }else{
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return true;
        }
    }
    protected static String getUserIdForSubscriber(JSONObject data){
        try {
            if(data == null){
                return "";
            }
            JSONObject display = new JSONObject(data.getString("display"));
            return display.getString("user_id");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    protected static String getRoleForSubscriber(JSONObject data){
        Log.e("subscribers count", "subscribers count role check : " + data.toString());
        try {
            if(data == null){
                return "";
            }
            JSONObject display = new JSONObject(data.getString("display"));
            return display.getString("role");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    protected static String getUserNameForSubscriber(JSONObject data){
        try {
            if(data == null){
                return "Unknown";
            }
            JSONObject display = new JSONObject(data.getString("display"));
            if(display.has("name")){
                return display.getString("name");
            }else{
                return "Unknown";
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    protected static int getOrientation(){
        Display display = ((WindowManager)MTKDataStore.getInstance().context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int screenOrientation = display.getRotation();
        return screenOrientation;
    }

    protected static int[] getDeviceResolution(){
//        Display display = ((Activity)MTKDataStore.getInstance().context).getWindowManager().getDefaultDisplay();
        WindowManager windowManager = (WindowManager)MTKDataStore.getInstance().context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.e("Width", "" + width);
        Log.e("height", "" + height);

        return new int[]{width, height};
    }

    protected static MTKPerson.StreamVideoType getVideoType(JSONObject data){
        try {
            if(data == null){
                return MTKPerson.StreamVideoType.camera;
            }
            JSONObject display = new JSONObject(data.getString("display"));
            if(display.has("video_type")){
                return display.getString("video_type").equals(MTKPerson.StreamVideoType.camera.name()) ? MTKPerson.StreamVideoType.camera : MTKPerson.StreamVideoType.screen;
            }else{
                return MTKPerson.StreamVideoType.camera;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return MTKPerson.StreamVideoType.camera;
        }
    }

    protected static int getObserverCount() {
        int observerCount = 0;
        for(MTKSubscriber subscriber : MTKDataStore.getInstance().subscribers){
            if(subscriber.role.equals(MTKConst.USER_ROLE_OBSERVER)){
                observerCount++;
            }
        }
        return observerCount;
    }

    protected static int getSubscriberCountWithoutObserver() {
        return MTKDataStore.getInstance().subscribers.size() - getObserverCount();
    }

    protected static boolean equalObserverCountAndSubscriberCount() {
        return getObserverCount() == MTKDataStore.getInstance().subscribers.size();
    }

    protected static String getRecordingFilename(String videoType){
        StringBuilder sb = new StringBuilder();
//        sb.append("record" + "/");
        sb.append(MTKDataStore.getInstance().targetServer + "_");
        sb.append(MTKDataStore.getInstance().projectId + "_");
        sb.append(MTKDataStore.getInstance().roomToken + "_");
        sb.append(MTKDataStore.getInstance().roomType + "@");
        sb.append(videoType + "_");
        sb.append(MTKDataStore.getInstance().role + "_");
        sb.append(MTKDataStore.getInstance().userId + "_0_");
        sb.append(new Date().getTime());

        return sb.toString();

    }
}
