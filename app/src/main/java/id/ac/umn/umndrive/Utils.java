package id.ac.umn.umndrive;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;

public class Utils {
    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void storeStringToPref(Context context, String key, String value) {
        SharedPreferences pref = context.getSharedPreferences("main", Context.MODE_PRIVATE);
        pref.edit().putString(key, value).apply();
    }

    public static String getStringFromPref(Context context, String key) {
        SharedPreferences pref = context.getSharedPreferences("main", Context.MODE_PRIVATE);
        return pref.getString(key, "");
    }

    public static void clearPref(Context context) {
        SharedPreferences pref = context.getSharedPreferences("main", Context.MODE_PRIVATE);
        pref.edit().clear().apply();
    }

    public static int getImageOrientation(String imagePath){
        int rotate = 0;
        try {

            File imageFile = new File(imagePath);
            ExifInterface exif = new ExifInterface(
                    imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotate;
    }
}
