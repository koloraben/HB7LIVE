
package com.app.hb7live.utils;

import android.app.IntentService;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.app.hb7live.R;
import com.app.hb7live.playback.Video;
import com.app.hb7live.playback.VideoContract;
import com.app.hb7live.playback.VideoDbBuilder;
import com.app.hb7live.playback.VideoDbHelper;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

/**
 * FetchVideoService is responsible for fetching the videos from the Internet and inserting the
 * results into a local SQLite database.
 */
public class FetchVideoService  extends Service {

    public Context context = this;
    public Handler handler = null;
    public static Runnable runnable = null;
    private static final String TAG = "FetchVideoService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        //Toast.makeText(this, "Service created!", Toast.LENGTH_LONG).show();
        requestServer();

        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                requestServer();
                handler.postDelayed(runnable, 86400000);
            }
        };

        handler.postDelayed(runnable, 86400000);
    }

    @Override
    public void onDestroy() {
        /* IF YOU WANT THIS SERVICE KILLED WITH THE APP THEN UNCOMMENT THE FOLLOWING LINE */
        //handler.removeCallbacks(runnable);
       // Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show();
    }

    private void requestServer(){
        //VideoDbHelper helper = new VideoDbHelper(getApplicationContext());
        //helper.deleteAllPostsAndUsers();
        getApplicationContext().getContentResolver().delete(VideoContract.VideoEntry.CONTENT_URI,
                null,new String[]{});
        VideoDbBuilder builder = new VideoDbBuilder(getApplicationContext());
        try {
            List<ContentValues> contentValuesList =
                    builder.fetch(getResources().getString(R.string.catalog_url));
            ContentValues[] downloadedVideoContentValues =
                    contentValuesList.toArray(new ContentValues[contentValuesList.size()]);
            //context.deleteDatabase(VideoDbHelper.DATABASE_NAME);
            getApplicationContext().getContentResolver().bulkInsert(VideoContract.VideoEntry.CONTENT_URI,
                    downloadedVideoContentValues);
            Log.i(TAG, " downloading videos");
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error occurred in downloading videos");
            e.printStackTrace();
        }
    }
}