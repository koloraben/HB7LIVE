package com.app.hb7live;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import com.app.hb7live.playback.VideoContract;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getApplicationContext().getContentResolver().delete(VideoContract.VideoEntry.CONTENT_URI,
                null,new String[]{});
        Log.e("mainActivity", "done deleting videos");
        if (savedInstanceState == null) {
            Fragment fragment = new MainFragment();
            getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }
}
