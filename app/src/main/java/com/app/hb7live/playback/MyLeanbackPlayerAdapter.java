package com.app.hb7live.playback;

/*
 * Created by Issam ELGUERCH on 10/19/2020.
 * mail: issamelguerch@gmail.com
 * All rights reserved.
 */



import android.content.Context;
import android.os.Handler;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import androidx.annotation.Nullable;
import androidx.leanback.R;
import androidx.leanback.media.PlaybackGlueHost;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.media.SurfaceHolderGlueHost;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.DefaultControlDispatcher;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.DiscontinuityReason;
import com.google.android.exoplayer2.Player.TimelineChangeReason;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.video.VideoListener;

/** Leanback {@code PlayerAdapter} implementation for {@link Player}. */
public final class MyLeanbackPlayerAdapter extends PlayerAdapter implements Runnable {

    static {
        ExoPlayerLibraryInfo.registerModule("goog.exo.leanback");
    }

    private final Context context;
    private final Player player;
    private final Handler handler;
    private final ComponentListener componentListener;
    private final int updatePeriodMs;

    @Nullable private PlaybackPreparer playbackPreparer;
    private ControlDispatcher controlDispatcher;
    @Nullable private ErrorMessageProvider<? super ExoPlaybackException> errorMessageProvider;
    @Nullable private SurfaceHolderGlueHost surfaceHolderGlueHost;
    private boolean hasSurface;
    private boolean lastNotifiedPreparedState;

    /**
     * Builds an instance. Note that the {@code PlayerAdapter} does not manage the lifecycle of the
     * {@link Player} instance. The caller remains responsible for releasing the player when it's no
     * longer required.
     *
     * @param context The current context (activity).
     * @param player Instance of your exoplayer that needs to be configured.
     * @param updatePeriodMs The delay between player control updates, in milliseconds.
     */
    public MyLeanbackPlayerAdapter(Context context, Player player, final int updatePeriodMs) {
        this.context = context;
        this.player = player;
        this.updatePeriodMs = updatePeriodMs;
        handler = new Handler();
        componentListener = new ComponentListener();
        controlDispatcher = new DefaultControlDispatcher();

    }

    /**
     * Sets the {@link PlaybackPreparer}.
     *
     * @param playbackPreparer The {@link PlaybackPreparer}.
     */
    public void setPlaybackPreparer(@Nullable PlaybackPreparer playbackPreparer) {
        this.playbackPreparer = playbackPreparer;
    }

    /**
     * Sets the {@link ControlDispatcher}.
     *
     * @param controlDispatcher The {@link ControlDispatcher}, or null to use
     *     {@link DefaultControlDispatcher}.
     */
    public void setControlDispatcher(@Nullable ControlDispatcher controlDispatcher) {
        this.controlDispatcher = controlDispatcher == null ? new DefaultControlDispatcher()
                : controlDispatcher;
    }

    /**
     * Sets the optional {@link ErrorMessageProvider}.
     *
     * @param errorMessageProvider The {@link ErrorMessageProvider}.
     */
    public void setErrorMessageProvider(
            @Nullable ErrorMessageProvider<? super ExoPlaybackException> errorMessageProvider) {
        this.errorMessageProvider = errorMessageProvider;
    }

    // PlayerAdapter implementation.

    @Override
    public void onAttachedToHost(PlaybackGlueHost host) {
        if (host instanceof SurfaceHolderGlueHost) {
            surfaceHolderGlueHost = ((SurfaceHolderGlueHost) host);
            surfaceHolderGlueHost.setSurfaceHolderCallback(componentListener);
        }
        notifyStateChanged();
        player.addListener(componentListener);
        Player.VideoComponent videoComponent = player.getVideoComponent();
        if (videoComponent != null) {
            videoComponent.addVideoListener(componentListener);
        }
    }

    @Override
    public void onDetachedFromHost() {
        player.removeListener(componentListener);
        Player.VideoComponent videoComponent = player.getVideoComponent();
        if (videoComponent != null) {
            videoComponent.removeVideoListener(componentListener);
        }
        if (surfaceHolderGlueHost != null) {
            removeSurfaceHolderCallback(surfaceHolderGlueHost);
            surfaceHolderGlueHost = null;
        }
        hasSurface = false;
        Callback callback = getCallback();
        callback.onBufferingStateChanged(this, false);
        callback.onPlayStateChanged(this);
        maybeNotifyPreparedStateChanged(callback);
    }

    @Override
    public void setProgressUpdatingEnabled(boolean enabled) {
        handler.removeCallbacks(this);
        if (enabled) {
            handler.post(this);
        }
    }

    @Override
    public boolean isPlaying() {
        int playbackState = player.getPlaybackState();
        return playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED
                && player.getPlayWhenReady();
    }

    @Override
    public long getDuration() {
        long durationMs = player.getDuration();
        return durationMs == C.TIME_UNSET ? -1 : durationMs;
    }

    @Override
    public long getCurrentPosition() {
        return player.getPlaybackState() == Player.STATE_IDLE ? -1 : player.getCurrentPosition();
    }

    @Override
    public void play() {
        if (player.getPlaybackState() == Player.STATE_IDLE) {
            if (playbackPreparer != null) {
                playbackPreparer.preparePlayback();
            }
        }
        if (controlDispatcher.dispatchSetPlayWhenReady(player, true)) {
            getCallback().onPlayStateChanged(this);
        }
    }

    @Override
    public void pause() {
        if (controlDispatcher.dispatchSetPlayWhenReady(player, false)) {
            getCallback().onPlayStateChanged(this);
        }
    }



    @Override
    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    @Override
    public boolean isPrepared() {
        return player.getPlaybackState() != Player.STATE_IDLE
                && (surfaceHolderGlueHost == null || hasSurface);
    }

    // Runnable implementation.

    @Override
    public void run() {
        Callback callback = getCallback();
        callback.onCurrentPositionChanged(this);
        callback.onBufferedPositionChanged(this);
        handler.postDelayed(this, updatePeriodMs);
    }

    // Internal methods.

    /* package */ void setVideoSurface(@Nullable Surface surface) {
        hasSurface = surface != null;
        Player.VideoComponent videoComponent = player.getVideoComponent();
        if (videoComponent != null) {
            videoComponent.setVideoSurface(surface);
        }
        maybeNotifyPreparedStateChanged(getCallback());
    }

    /* package */ void notifyStateChanged() {
        int playbackState = player.getPlaybackState();
        Callback callback = getCallback();
        maybeNotifyPreparedStateChanged(callback);
        callback.onPlayStateChanged(this);
        callback.onBufferingStateChanged(this, playbackState == Player.STATE_BUFFERING);
        if (playbackState == Player.STATE_ENDED) {
            callback.onPlayCompleted(this);
        }
    }

    private void maybeNotifyPreparedStateChanged(Callback callback) {
        boolean isPrepared = isPrepared();
        if (lastNotifiedPreparedState != isPrepared) {
            lastNotifiedPreparedState = isPrepared;
            callback.onPreparedStateChanged(this);
        }
    }

    @SuppressWarnings("nullness:argument.type.incompatible")
    private static void removeSurfaceHolderCallback(SurfaceHolderGlueHost surfaceHolderGlueHost) {
        surfaceHolderGlueHost.setSurfaceHolderCallback(null);
    }

    private final class ComponentListener
            implements Player.EventListener, SurfaceHolder.Callback, VideoListener {

        // SurfaceHolder.Callback implementation.

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            setVideoSurface(surfaceHolder.getSurface());
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            // Do nothing.
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            setVideoSurface(null);
        }

        // Player.EventListener implementation.

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, @Player.State int playbackState) {
            notifyStateChanged();
        }

        @Override
        public void onPlayerError(ExoPlaybackException exception) {
            Callback callback = getCallback();
            if (errorMessageProvider != null) {
                Pair<Integer, String> errorMessage = errorMessageProvider.getErrorMessage(exception);
                callback.onError(MyLeanbackPlayerAdapter.this, errorMessage.first, errorMessage.second);
            } else {
                callback.onError(MyLeanbackPlayerAdapter.this, exception.type, context.getString(
                        R.string.lb_media_player_error, exception.type, exception.rendererIndex));
            }
        }

        @Override
        public void onTimelineChanged(Timeline timeline, @TimelineChangeReason int reason) {
            Callback callback = getCallback();
            callback.onDurationChanged(MyLeanbackPlayerAdapter.this);
            callback.onCurrentPositionChanged(MyLeanbackPlayerAdapter.this);
            callback.onBufferedPositionChanged(MyLeanbackPlayerAdapter.this);
        }

        @Override
        public void onPositionDiscontinuity(@DiscontinuityReason int reason) {
            Callback callback = getCallback();
            callback.onCurrentPositionChanged(MyLeanbackPlayerAdapter.this);
            callback.onBufferedPositionChanged(MyLeanbackPlayerAdapter.this);
        }

        // VideoListener implementation.

        @Override
        public void onVideoSizeChanged(
                int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            // There's no way to pass pixelWidthHeightRatio to leanback, so we scale the width that we
            // pass to take it into account. This is necessary to ensure that leanback uses the correct
            // aspect ratio when playing content with non-square pixels.
            //int scaledWidth = Math.round(width * pixelWidthHeightRatio);
            //getCallback().onVideoSizeChanged(MyLeanbackPlayerAdapter.this, scaledWidth, height);
        }

        @Override
        public void onRenderedFirstFrame() {
            // Do nothing.
        }

    }

}
