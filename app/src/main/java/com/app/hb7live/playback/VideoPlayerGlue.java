

package com.app.hb7live.playback;

import android.content.Context;
import android.content.Intent;
import android.support.v17.leanback.media.PlaybackBannerControlGlue;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.widget.Toast;

import com.app.hb7live.R;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import pl.droidsonroids.gif.GifDrawable;

/**
 * Manages customizing the actions in the {@link PlaybackControlsRow}. Adds and manages the
 * following actions to the primary and secondary controls:
 *
 * <ul>
 *   <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.RepeatAction}
 *   <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsDownAction}
 *   <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsUpAction}
 *   <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.SkipPreviousAction}
 *   <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.SkipNextAction}
 *   <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction}
 *   <li>{@link android.support.v17.leanback.widget.PlaybackControlsRow.RewindAction}
 * </ul>
 *
 * Note that the superclass, {@link PlaybackTransportControlGlue}, manages the playback controls
 * row.
 */
public class VideoPlayerGlue extends PlaybackTransportControlGlue<LeanbackPlayerAdapter> {

    private static final long TEN_SECONDS = TimeUnit.SECONDS.toMillis(10);
    private PlaybackControlsRow.MoreActions moreActionsProgram;


    /** Listens for when skip to next and previous actions have been dispatched. */
    public interface OnActionClickedListener {

        /** Skip to the previous item in the queue. */
        void onPrevious();

        /** Skip to the next item in the queue. */
        void onNext();
    }



    public VideoPlayerGlue(
            Context context,
            LeanbackPlayerAdapter playerAdapter,
            OnActionClickedListener actionListener) {
        super(context, playerAdapter);
        GifDrawable gifFromResource = null;
        try {
             gifFromResource = new GifDrawable( getContext().getResources(), R.drawable.lv );
        } catch (IOException e) {
            e.printStackTrace();
        }

        moreActionsProgram = new PlaybackControlsRow.MoreActions(context);
        moreActionsProgram.setIcon(gifFromResource);
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter adapter) {
        super.onCreatePrimaryActions(adapter);
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
