package com.moutamid.radamsdriver;

import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

public class Constants {
    public static final String IS_LOGGED_IN = "IS_LOGGED_IN";
    public static final String USER_MODEL = "USER_MODEL";
    public static final String TOKEN = "TOKEN";
    public static final String FULL_NAME = "FULLNAME";
    public static final String VEHICLE = "VEHICLE";
    public static final String ROTATION = "ROTATION";
    public static final String AGGREGATE = "aggregate";
    public static final String GRAIN = "grain";

    public static int rotateImage(String path){
        int capturedImageOrientation = 0;

        try {
            ExifInterface exifInterface = new ExifInterface(path);

            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Log.d("PATH123", "Original orientation: " + orientation);

            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    capturedImageOrientation = 0;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    capturedImageOrientation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    capturedImageOrientation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    capturedImageOrientation = 270;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return capturedImageOrientation;
    }

}
