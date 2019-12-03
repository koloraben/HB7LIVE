package com.app.hb7live.playback;


import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v17.leanback.app.VideoFragment;
import android.support.v17.leanback.app.VideoFragmentGlueHost;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;

import com.app.hb7live.R;
import com.app.hb7live.cards.presenters.CardPresenterSelector;
import com.app.hb7live.live.DetailViewActivity;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;


public class PlaybackFragment extends VideoFragment {

    private static final int UPDATE_DELAY = 1;

    private VideoPlayerGlue mPlayerGlue;
    private LeanbackPlayerAdapter mPlayerAdapter;
    private SimpleExoPlayer mPlayer;
    private TrackSelector mTrackSelector;
    private PlaylistActionListener mPlaylistActionListener;
    private static final String TAG = "PlaybackFragment";
    private Video mVideo;
    private Playlist mPlaylist;
    private VideoLoaderCallbacks mVideoLoaderCallbacks;
    private CursorObjectAdapter mVideoCursorAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVideo = (Video) getActivity().getIntent()
                .getParcelableExtra(DetailViewActivity.LIVE);
        mPlaylist = new Playlist();
        mVideoLoaderCallbacks = new VideoLoaderCallbacks(mPlaylist);

        // Loads the playlist.
        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, mVideo.category);
        getLoaderManager()
                .initLoader(VideoLoaderCallbacks.QUEUE_VIDEOS_LOADER, args, mVideoLoaderCallbacks);

        mVideoCursorAdapter = setupRelatedVideosCursor();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || mPlayer == null)) {
            initializePlayer();
        }
    }

    /**
     * Pauses the player.
     */
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onPause() {
        super.onPause();
        //Toast.makeText(getContext(), "pause called!", Toast.LENGTH_SHORT).show();
        if (mPlayerGlue != null && mPlayerGlue.isPlaying()) {
            // mPlayerGlue.pause();
        }
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //Toast.makeText(getContext(), "stop called!", Toast.LENGTH_SHORT).show();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        mTrackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        mPlayer = ExoPlayerFactory.newSimpleInstance(getActivity(), mTrackSelector);
        mPlayerAdapter = new LeanbackPlayerAdapter(getActivity(), mPlayer, UPDATE_DELAY);
        mPlaylistActionListener = new PlaylistActionListener(mPlaylist);
        mPlayerGlue = new VideoPlayerGlue(getActivity(), mPlayerAdapter, mPlaylistActionListener);
        mPlayerGlue.setHost(new VideoFragmentGlueHost(this));
        mPlayerGlue.playWhenPrepared();

        play(mVideo);

        ArrayObjectAdapter mRowsAdapter = initializeRelatedVideosRow();
        setAdapter(mRowsAdapter);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mTrackSelector = null;
            mPlayerGlue = null;
            mPlayerAdapter = null;
            mPlaylistActionListener = null;
        }
    }

    private void play(Video video) {
        mPlayerGlue.setTitle(video.title);
        //mPlayerGlue.setSubtitle(video.description);
        prepareMediaForPlaying(Uri.parse(video.videoUrl));
        //prepareMediaForPlaying(Uri.parse("http://xxultraxx.com:80/admin_833746/C5OqgYwX/26807"));
        mPlayerGlue.play();
    }

    private void prepareMediaForPlaying(Uri mediaSourceUri) {
        MediaSource mediaSource = null;
        /*MediaSource mediaSource =
                new ExtractorMediaSource(
                        mediaSourceUri,
                        new DefaultDataSourceFactory(getActivity(), userAgent),
                        new DefaultExtractorsFactory(),
                        null,
                        null);*/
        Log.e("f", "mediaSourceUri.getHost ;; " + mediaSourceUri.getScheme());
        if (mediaSourceUri.getScheme().equals("rtmp")) {
            Log.e("f", "rtmp extractor ;; ");
            mediaSource = new ExtractorMediaSource.Factory(new RtmpDataSourceFactory())
                    .createMediaSource(mediaSourceUri);
        } else if (mediaSourceUri.getLastPathSegment().contains("m3u8")) {
            mediaSource = new HlsMediaSource.Factory(new DefaultHttpDataSourceFactory("exoplayer-codelab"))
                    .createMediaSource(mediaSourceUri);
        } else {
            mediaSource = new ExtractorMediaSource.Factory(new DefaultDataSourceFactory(getContext(), "exoplayer-codelab"))
                    .createMediaSource(mediaSourceUri);
        }


        final LoopingMediaSource loopingSource = new LoopingMediaSource(mediaSource);

        mPlayer.prepare(mediaSource);
        mPlayer.addListener(new Player.EventListener() {


            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
                Log.v(TAG, "Listener-onTracksChanged... " + timeline);

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                // Log.v(TAG, "Listener-onTracksChanged... "+trackSelections.get(0).blacklist(0,0L));
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                Log.v(TAG, "Listener-onLoadingChanged..." + isLoading);
                if (isLoading == false) {
                    Log.v(TAG, "onLoadingChanged ////////////////////");
                    mPlayer.stop();
                    mPlayer.prepare(loopingSource);
                    mPlayer.setPlayWhenReady(true);
                }
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Log.v(TAG, "Listener-onPlayerStateChanged..." + playbackState + "|||isDrawingCacheEnabled():");
                if (playbackState == 4) {
                    Log.v(TAG, "playback is at 4 ////////////////////");
                    mPlayer.stop();
                    mPlayer.prepare(loopingSource);
                    mPlayer.setPlayWhenReady(true);
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
                Log.v(TAG, "Listener-onLoadingChanged..." + repeatMode);
            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
                Log.v(TAG, "Listener-onShuffleModeEnabledChanged..." + shuffleModeEnabled);
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                switch (error.type) {
                    case ExoPlaybackException.TYPE_SOURCE:
                        mPlayer.stop();
                        mPlayer.prepare(loopingSource);
                        mPlayer.setPlayWhenReady(true);
                        Log.e(TAG, "TYPE_SOURCE: " + error.getSourceException().getMessage());
                        break;

                    case ExoPlaybackException.TYPE_RENDERER:
                        mPlayer.stop();
                        mPlayer.prepare(loopingSource);
                        mPlayer.setPlayWhenReady(true);
                        Log.e(TAG, "TYPE_RENDERER: " + error.getRendererException().getMessage());
                        break;

                    case ExoPlaybackException.TYPE_UNEXPECTED:
                        mPlayer.stop();
                        mPlayer.prepare(loopingSource);
                        mPlayer.setPlayWhenReady(true);
                        Log.e(TAG, "TYPE_UNEXPECTED: " + error.getUnexpectedException().getMessage());
                        break;
                    default:
                        mPlayer.stop();
                        mPlayer.prepare(loopingSource);
                        mPlayer.setPlayWhenReady(true);
                        Log.e(TAG, "UNKNOWN ERROR: " + error.getUnexpectedException().getMessage());
                        break;
                }

            }

            @Override
            public void onPositionDiscontinuity(int reason) {
                Log.v(TAG, "Listener-onPositionDiscontinuity..." + reason);
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                Log.v(TAG, "Listener-onPlaybackParametersChanged..." + playbackParameters.speed);
            }

            @Override
            public void onSeekProcessed() {
                Log.v(TAG, "Listener-onLoadingChanged...");
            }
        });
    }

    private ArrayObjectAdapter initializeRelatedVideosRow() {
        /*
         * To add a new row to the mPlayerAdapter and not lose the controls row that is provided by the
         * glue, we need to compose a new row with the controls row and our related videos row.
         *
         * We start by creating a new {@link ClassPresenterSelector}. Then add the controls row from
         * the media player glue, then add the related videos row.
         */
        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(
                mPlayerGlue.getControlsRow().getClass(), mPlayerGlue.getPlaybackRowPresenter());
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(presenterSelector);

        rowsAdapter.add(mPlayerGlue.getControlsRow());

        HeaderItem header = new HeaderItem(getString(R.string.related_movies));
        ListRow row = new ListRow(header, mVideoCursorAdapter);
        rowsAdapter.add(row);

        setOnItemViewClickedListener(new ItemViewClickedListener());

        return rowsAdapter;
    }

    private CursorObjectAdapter setupRelatedVideosCursor() {
        CursorObjectAdapter videoCursorAdapter = new CursorObjectAdapter(new CardPresenterSelector(getContext()));
        videoCursorAdapter.setMapper(new VideoCursorMapper());

        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, mVideo.category);
        getLoaderManager().initLoader(VideoLoaderCallbacks.RELATED_VIDEOS_LOADER, args, mVideoLoaderCallbacks);

        return videoCursorAdapter;
    }

    /**
     * Opens the video details page when a related video has been clicked.
     */
    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {
            if (item instanceof Video) {

                Intent intent = new Intent(getActivity().getBaseContext(),
                        DetailViewActivity.class);
                intent.putExtra(DetailViewActivity.LIVE, (Video) item);
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailViewActivity.SHARED_ELEMENT_NAME)
                        .toBundle();
                startActivity(intent, bundle);
                mPlayerGlue.pause();
            }
            if (item instanceof PlaybackControlsRow.MoreActions) {
                //Intent intent = new Intent(getContext(),SettingsActivity.class);
                //getContext().startActivity(intent);
            }

        }
    }

    /**
     * Loads a playlist with videos from a cursor and also updates the related videos cursor.
     */
    protected class VideoLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        static final int RELATED_VIDEOS_LOADER = 1;
        static final int QUEUE_VIDEOS_LOADER = 2;

        private final VideoCursorMapper mVideoCursorMapper = new VideoCursorMapper();

        private final Playlist playlist;

        private VideoLoaderCallbacks(Playlist playlist) {
            this.playlist = playlist;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            // When loading related videos or videos for the playlist, query by category.
            String category = args.getString(VideoContract.VideoEntry.COLUMN_CATEGORY);
            return new CursorLoader(
                    getActivity(),
                    VideoContract.VideoEntry.CONTENT_URI,
                    null,
                    VideoContract.VideoEntry.COLUMN_CATEGORY + " = ?",
                    new String[]{category},
                    "studio ASC");
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            int id = loader.getId();
            Video video = (Video) mVideoCursorMapper.convert(cursor);
            //Log.e("viiiidd",video.title);
            if (id == QUEUE_VIDEOS_LOADER) {
                playlist.clear();
                do {
                    video = (Video) mVideoCursorMapper.convert(cursor);

                    // Set the current position to the selected video.
                    if (video.id == mVideo.id) {
                        playlist.setCurrentPosition(playlist.size());
                    }

                    playlist.add(video);

                } while (cursor.moveToNext());
            } else if (id == RELATED_VIDEOS_LOADER) {
                mVideoCursorAdapter.changeCursor(cursor);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mVideoCursorAdapter.changeCursor(null);
        }
    }

    class PlaylistActionListener implements VideoPlayerGlue.OnActionClickedListener {

        private Playlist mPlaylist;

        PlaylistActionListener(Playlist playlist) {
            this.mPlaylist = playlist;
        }

        @Override
        public void onPrevious() {
            play(mPlaylist.previous());
        }

        @Override
        public void onNext() {
            play(mPlaylist.next());
        }
    }
}