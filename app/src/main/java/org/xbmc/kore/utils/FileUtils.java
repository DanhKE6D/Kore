package org.xbmc.kore.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.xbmc.kore.jsonrpc.type.PlaylistType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by danh.le on 8/16/15.
 */
public class FileUtils {
    static final String TAG = FileUtils.class.getSimpleName();
    static final String appDirInSDCard = "Kore";
    static final String playlistDir = "Playlists";
    static final String playlistDirNet = "Net";
    static final String playlistDirLocal = "Local";

    static boolean createDirIfNotExists(String dir) {
        boolean ret = true;

        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + dir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                ret = false;
            }
        }
        return ret;
    }

    static void CreateDefaultDirsOnSDCard() {
        createDirIfNotExists(appDirInSDCard);
        createDirIfNotExists(appDirInSDCard +"/"+ playlistDir);
        createDirIfNotExists(appDirInSDCard + "/" + playlistDir + "/" + playlistDirNet);
        createDirIfNotExists(appDirInSDCard + "/" + playlistDir + "/" + playlistDirLocal);
    }

    public static boolean playlistDirExisted() {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + appDirInSDCard);
        if (!file.exists()) {
            CreateDefaultDirsOnSDCard();
        }
        return true;
    }

    public static String getPlaylistDirectory() {
        return Environment.getExternalStorageDirectory().getPath() +
                "/" + appDirInSDCard + "/" + playlistDir + "/";
    }

    public static boolean savePlaylistToFile(final File playlistFile, final String jsonPlaylistContent) {
        if (!playlistDirExisted()) {
            // if top directory exists, the lower directory existed too
            CreateDefaultDirsOnSDCard();
        }
        Thread t = new Thread() {
            public void run() {
                try {
                    playlistFile.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(playlistFile);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append(jsonPlaylistContent);
                    myOutWriter.close();
                    fOut.close();
                } catch (Exception e) {
                    Log.e(TAG, "Unable to write to file");
                }

            }
        };

        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread did not complete exception");
        }
        return true;
    }

    public static String readFromFile(final File playlistFile) {
        final String [] ret = new String[1];
        ret[0] = "";    // work around for the inner class value problem
        if (!playlistDirExisted()) {
            // if top directory exists, the lower directory existed too
            CreateDefaultDirsOnSDCard();
            return ret[0];
        }
        Thread t = new Thread() {
            public void run() {
                try {
                    InputStream inputStream = new FileInputStream(playlistFile);
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        String receiveString = "";
                        StringBuilder stringBuilder = new StringBuilder();
                        while ( (receiveString = bufferedReader.readLine()) != null ) {
                            stringBuilder.append(receiveString);
                        }

                        inputStream.close();
                        ret[0] = stringBuilder.toString();
                    }
                catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found: " + e.toString());
                } catch (IOException e) {
                    Log.e(TAG, "Can not read file: " + e.toString());
                }

            }
        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread did not complete exception");
        }
        return ret[0];
    }
}
