package io.methinks.android.rtc;

public class Log {
    private static final String TAG = "[MTKPatcher]";

    public static void i(String string) {
        if (MTKDataStore.getInstance().targetServer.equals("dev")) android.util.Log.i(TAG, string);
    }
    public static void e(String string) {
        if (MTKDataStore.getInstance().targetServer.equals("dev")) android.util.Log.e(TAG, string);
    }
    public static void d(String string) {
        if (MTKDataStore.getInstance().targetServer.equals("dev")) android.util.Log.d(TAG, string);
    }
    public static void v(String string) {
        if (MTKDataStore.getInstance().targetServer.equals("dev")) android.util.Log.v(TAG, string);
    }
    public static void w(String string) {
        String logMsg = "" +
                "\n" +
                "################################################################" + "\n" +
                string + "\n" +
                "################################################################";
        if (MTKDataStore.getInstance().targetServer.equals("dev")) android.util.Log.w(TAG, logMsg);
    }

    public static void callEvent(String key, String value) {
        if (MTKDataStore.getInstance().targetServer.equals("dev")) android.util.Log.d(TAG, "Caught event - Event name: " + key + ", Event value: " + value);
    }
}
