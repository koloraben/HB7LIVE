package com.app.hb7live;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.app.hb7live.live.DetailViewActivity;
import com.app.hb7live.playback.PlaybackActivity;
import com.app.hb7live.playback.Video;
import com.app.hb7live.playback.VideoDbBuilder;

public class StartActivityOnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            Video video = new Video.VideoBuilder()
                    .title("HB7")
                    .description("")
                    .category("live")
                    .cardImageUrl("https://storage.googleapis.com/android-tv/Sample%20videos/Google%2B/Google%2B_%20Instant%20Upload/card.jpg")
                    .bgImageUrl("https://storage.googleapis.com/android-tv/Sample%20videos/Google%2B/Google%2B_%20Instant%20Upload/bg.jpg")
                    .videoUrl("https://storage.googleapis.com/android-tv/Sample%20videos/Google%2B/Google%2B_%20Instant%20Upload.mp4")
                    .build();

            Intent i = new Intent(context, PlaybackActivity.class);
            i.putExtra(DetailViewActivity.LIVE,video);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}