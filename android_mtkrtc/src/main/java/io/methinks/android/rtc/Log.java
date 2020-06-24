

public class Log {
    // subtree test
    private static final String TAG = "[MTKRTC]";

    public static void i(String string) {
        android.util.Log.i(TAG, string);
    }
    public static void e(String string) {
        android.util.Log.e(TAG, string);
    }
    public static void d(String string) {
        android.util.Log.d(TAG, string);
    }
    public static void v(String string) {
        android.util.Log.v(TAG, string);
    }
    public static void w(String string) {
        String logMsg = "" +
                "\n" +
                "################################################################" + "\n" +
                string + "\n" +
                "################################################################";
        android.util.Log.w(TAG, logMsg);
    }

    public static void callEvent(String key, String value) {
        android.util.Log.d(TAG, "Caught event - Event name: " + key + ", Event value: " + value);
    }
}
