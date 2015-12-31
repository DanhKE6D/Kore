package org.xbmc.kore.utils;

import android.util.Log;
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
    public static boolean savePlaylistToFile(final File playlistFile, final String jsonPlaylistContent) {
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
