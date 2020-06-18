package io.methinks.android.rtc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class MTKScreenSharingPermUtil {

    public static boolean checkPermissionCapture(Activity activity, int requestCode, MediaProjectionManager mediaProjectionManager){
        mediaProjectionManager = (MediaProjectionManager) activity.getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent captureIntent =  mediaProjectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(captureIntent, requestCode);

        return false;
    }

    public static boolean checkPermissions(Activity activity, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED)
        {

            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, requestCode);
            return false;
        }

        return true;
    }
}
