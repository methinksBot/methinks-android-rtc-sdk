package io.methinks.android.rtc;

import android.app.Activity;
import android.content.Context;
import android.media.projection.MediaProjection;

public class MediaProjectionCallback extends MediaProjection.Callback {
    private Activity activity;
    private Context context;

    public MediaProjectionCallback(Activity activity) {
        this.activity = activity;
    }

    public MediaProjectionCallback(Context context) {
        this.context = context;
    }

    @Override
    public void onStop() {
    }
}
