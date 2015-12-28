package org.xbmc.kore.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by danh.le on 9/3/15.
 */
public class SharedPreferencesUtils {
    static final String TAG = SharedPreferencesUtils.class.getSimpleName();

    public static Object getObject(Context context, String key, Class clazz) {
        String value = PreferenceManager.getDefaultSharedPreferences(context).getString(key, null);
        if (value != null) {
            Log.d(TAG, "getObject " + key + "=" + value);
            return (Object) Config.getGson().fromJson(value, clazz);
        } else {
            return null;
        }
    }

    public static void putObject(Context context, String key, Object obj) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (obj != null) {
            String value = Config.getGson().toJson(obj);
            Log.d(TAG, "putObject "+key+"="+ value);
            prefs.edit().putString(key, value).commit();
        } else {
            prefs.edit().remove(key).commit();
        }
    }

    public static boolean getBoolean(Context context, String key,boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(key, value).commit();
    }

    public static String getString(Context context, String key,String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
    }

    public static void putString(Context context, String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(key, value).commit();
    }
}
