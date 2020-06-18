package io.methinks.android.rtc;

public class MTKConst {
    public static final String TARGET_SERVER_DEV = "dev";
    public static final String TARGET_SERVER_STAG = "stag";
    public static final String TARGET_SERVER_PROD = "prod";

    protected static final String PLUGIN_VIDEOROOM = "janus.plugin.videoroom";
    protected static final String VIDEO_FLEXFEC_FIELDTRIAL = "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/";
    protected static final String WEBSOCKET_HEADER_NAME = "Sec-Websocket-Protocol";
    protected static final String WEBSOCKET_HEADER_VALUE = "janus-protocol";

    public static final String USER_ROLE_BUSINESS = "business";
    public static final String USER_ROLE_CUSTOMER = "customer";
    public static final String USER_ROLE_OBSERVER = "observer";

    protected static final String AUDIO_TRACK_ID = "ARDAMSa0";
    protected static final String VIDEO_TRACK_ID = "ARDAMSv0";
    protected static final String THREAD_NAME_1 = "CaptureThread1";
    protected static final String THREAD_NAME_2 = "CaptureThread2";
    protected static final String LOCAL_MEDIA_ID = "102";
    protected static final String LOCAL_CAPTURE_MEDIA_ID = "103";


    public static final String ROOM_TYPE_INTERVIEW = "interview";
    public static final String ROOM_TYPE_APP_TEST = "apptest";
    public static final String ROOM_TYPE_USER_TEST = "usertest";

    public static final String BASE_FEATURE_THINKER = "thinker_app";
    public static final String BASE_FEATURE_APPTEST = "apptest_sdk";

    public static final int REQUEST_SCREEN_SHARING = 12322;
    public static final int REQUEST_WEB_SHARING = 12312;
}
