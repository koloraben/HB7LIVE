

package com.app.hb7live.playback;

import android.content.Context;
import android.content.Intent;

import android.widget.Toast;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

import com.app.hb7live.R;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import pl.droidsonroids.gif.GifDrawable;


public class VideoPlayerGlue extends PlaybackTransportControlGlue<MyLeanbackPlayerAdapter> {

    private static final long TEN_SECONDS = TimeUnit.SECONDS.toMillis(10);
    private  PlaybackControlsRow DDD  ;
    private PlaybackControlsRow.MoreActions moreActionsProgram;
    private PlaybackControlsRow.MoreActions epg;
    private PlaybackControlsRow.SkipNextAction nextAction;
    private PlaybackControlsRow.SkipPreviousAction previousAction;
    public static final long EPG_ID = 1010;


    /**
     * Listens for when skip to next and previous actions have been dispatched.
     */
    public interface OnActionClickedListener {

        /**
         * Skip to the previous item in the queue.
         */
        void onPrevious();

        /**
         * Skip to the next item in the queue.
         */
        void onNext();
    }


    public VideoPlayerGlue(
            Context context,
            MyLeanbackPlayerAdapter playerAdapter,
            OnActionClickedListener actionListener) {
        super(context, playerAdapter);
        GifDrawable gifFromResource = null;
        try {
            gifFromResource = new GifDrawable(getContext().getResources(), R.drawable.lv);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DDD = new PlaybackControlsRow(context);

        DDD.setImageDrawable(getContext().getDrawable(R.drawable.info));
        moreActionsProgram = new PlaybackControlsRow.MoreActions(context);
        moreActionsProgram.setIcon(gifFromResource);
        previousAction = new PlaybackControlsRow.SkipPreviousAction(context);
        nextAction = new PlaybackControlsRow.SkipNextAction(context);
        epg = new PlaybackControlsRow.MoreActions(context);
        epg.setId(EPG_ID);
        epg.setIcon(getContext().getDrawable(R.drawable.info));

    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
        super.onCreatePrimaryActions(adapter);
        //adapter.add(previousAction);
        //adapter.add(nextAction);
        //adapter.add(epg);
        //adapter.add(DDD);
        adapter.add(moreActionsProgram);
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter adapter) {
        super.onCreateSecondaryActions(adapter);
    }

    @Override
    public void onActionClicked(Action action) {

        super.onActionClicked(action);


    }

}
