package com.app.hb7live.live;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.app.hb7live.MainActivity;
import com.app.hb7live.R;
import com.app.hb7live.playback.PlaybackActivity;

public class DetailViewActivity extends Activity {
  public static final String SHARED_ELEMENT_NAME = "HB7";
  public static final String LIVE = "Live";
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_detail_example);

    if (savedInstanceState == null) {
      DetailViewFragment fragment = new DetailViewFragment();
      getFragmentManager().beginTransaction()
              .replace(R.id.details_fragment, fragment)
              .commit();
    }
  }

  @Override
  public void onBackPressed() {
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
  }
}