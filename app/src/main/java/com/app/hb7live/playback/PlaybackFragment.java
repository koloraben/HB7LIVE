package com.app.hb7live.playback;


import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.app.ProgressBarManager;
import androidx.leanback.app.VideoFragment;
import androidx.leanback.app.VideoFragmentGlueHost;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.app.hb7live.R;
import com.app.hb7live.cards.presenters.CardPresenterSelector;
import com.app.hb7live.live.DetailViewActivity;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.PlaybackStats;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory;
import com.google.android.exoplayer2.extractor.ts.TsExtractor;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoFrameMetadataListener;

import static com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES;


public class PlaybackFragment extends VideoFragment {

    private static final int UPDATE_DELAY = 1;
    private VideoPlayerGlue mPlayerGlue;
    private MyLeanbackPlayerAdapter mPlayerAdapter;
    private SimpleExoPlayer mPlayer;
    private DefaultTrackSelector mTrackSelector;
    private PlaylistActionListener mPlaylistActionListener;
    private static final String TAG = "PlaybackFragment";
    private Video mVideo;
    private Playlist mPlaylist;
    private VideoLoaderCallbacks mVideoLoaderCallbacks;
    private CursorObjectAdapter mVideoCursorAdapter;
    private EventLogger eventLogger;

    @Override
    public ProgressBarManager getProgressBarManager() {
        return null;
    }

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

        if (mPlayerGlue.isPlaying()) {
            // Argument equals true to notify the system that the activity
            // wishes to be visible behind other translucent activities
            if (! getActivity().requestVisibleBehind(true)) {
                // App-specific method to stop playback and release resources
                // because call to requestVisibleBehind(true) failed
                mPlayerGlue.pause();
            }
        } else {
            // Argument equals false because the activity is not playing
            getActivity().requestVisibleBehind(false);
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

    @Override
    protected void onVideoSizeChanged(int width, int height) {
        System.out.println("onVideoSizeChanged /////////////////////");
        View rootView = getView();
        SurfaceView surfaceView = getSurfaceView();
        ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
        params.height = rootView.getHeight();
        params.width = rootView.getWidth();
        surfaceView.setLayoutParams(params);

    }

    private void initializePlayer() {


        /* Instantiate a DefaultLoadControl.Builder. */
        DefaultLoadControl.Builder builder = new
                DefaultLoadControl.Builder();
        DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter.Builder(getContext()).build();
        int ms = 1000;
        final int loadControlMaxBufferMs = 1000;
        builder.setPrioritizeTimeOverSizeThresholds(false);

        builder.setTargetBufferBytes(C.LENGTH_UNSET);
        DefaultAllocator allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
        /* Build the actual DefaultLoadControl instance */
        DefaultLoadControl loadControl = new DefaultLoadControl(allocator, 360000, 600000, 2500, 5000, -1, true);
        DefaultRenderersFactory rf = new DefaultRenderersFactory(getContext());
        rf.setPlayClearSamplesWithoutKeys(true);
        rf.setEnableDecoderFallback(true);
        rf.setMediaCodecSelector(new BlackListMediaCodecSelector());
        rf.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        mPlayer = new SimpleExoPlayer.Builder(getContext(), rf)
                .setBandwidthMeter(BANDWIDTH_METER)
                .setLoadControl(loadControl).build();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        mTrackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        mTrackSelector.experimental_allowMultipleAdaptiveSelections();
        mTrackSelector.setParameters(new DefaultTrackSelector.ParametersBuilder(getContext())
                .clearVideoSizeConstraints()
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setExceedVideoConstraintsIfNecessary(true)
        );
        eventLogger = new EventLogger(mTrackSelector);
        mPlayer.addAnalyticsListener(eventLogger);
        mPlayerAdapter = new MyLeanbackPlayerAdapter(getActivity(), mPlayer, UPDATE_DELAY);
        mPlaylistActionListener = new PlaylistActionListener(mPlaylist);
        mPlayerGlue = new VideoPlayerGlue(getActivity(), mPlayerAdapter, mPlaylistActionListener);
        mPlayerGlue.setSeekEnabled(false);
        mPlayerGlue.setHost(new VideoFragmentGlueHost(this));


        play(mVideo);

        ArrayObjectAdapter mRowsAdapter = initializeRelatedVideosRow();
        setAdapter(mRowsAdapter);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mPlayerGlue = null;
            mPlayerAdapter = null;
            mPlaylistActionListener = null;
        }
    }

    private void play(Video video) {
        mPlayerGlue.setTitle(video.title);
        //mPlayerGlue.setSubtitle(video.description);
        prepareMediaForPlaying(Uri.parse(video.videoUrl));
        //prepareMediaForPlaying(Uri.parse("https://ns372429.ip-188-165-194.eu/stream/channelid/556010138?ticket=D81D1AF3F0554066E44F445BA3328978317D6317&profile=custom"));
        //prepareMediaForPlaying(Uri.parse("https://d2e1asnsl7br7b.cloudfront.net/7782e205e72f43aeb4a48ec97f66ebbe/index_5.m3u8"));
        mPlayerGlue.playWhenPrepared();
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
        Log.e("f", "mediaSourceUri.getHost ;; " + mediaSourceUri);
        if (mediaSourceUri.getScheme().equals("rtmp")) {
            Log.e("f", "rtmp extractor ;; ");
            mediaSource = new ProgressiveMediaSource.Factory(new RtmpDataSourceFactory())
                    .createMediaSource(mediaSourceUri);
        } else if (mediaSourceUri.getLastPathSegment().contains("m3u8")) {
            mediaSource = new HlsMediaSource.Factory(new DefaultHttpDataSourceFactory("exoplayer-codelab"))
                    .createMediaSource(mediaSourceUri);
        } else {
            DefaultExtractorsFactory extractorFactory = new DefaultExtractorsFactory()
                    .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS)
                    .setTsExtractorFlags(FLAG_ALLOW_NON_IDR_KEYFRAMES)
                    .setTsExtractorMode(TsExtractor.MODE_HLS);
            mediaSource = new ProgressiveMediaSource
                    .Factory(new DefaultHttpDataSourceFactory("hb7tvPlayer"))
                    .setCustomCacheKey("hb7tvPlayer")
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
                if (playbackState == PlaybackStats.PLAYBACK_STATE_BUFFERING) {
                    Log.v(TAG, "PLAYBACK_STATE_BUFFERING ////////////////////");

                }
                if (playbackState == PlaybackStats.PLAYBACK_STATE_PAUSED_BUFFERING) {
                    Log.v(TAG, "PLAYBACK_STATE_PAUSED_BUFFERING ////////////////////");

                }
                if (playbackState == PlaybackStats.PLAYBACK_STATE_SUPPRESSED_BUFFERING) {
                    Log.v(TAG, "PLAYBACK_STATE_SUPPRESSED_BUFFERING ////////////////////");

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
                        reloadPlayer(loopingSource);
                        Log.e(TAG, "TYPE_SOURCE: " + error.getSourceException().getMessage());
                        break;

                    case ExoPlaybackException.TYPE_RENDERER:
                        reloadPlayer(loopingSource);
                        Log.e(TAG, "TYPE_RENDERER: " + error.getRendererException().getMessage());
                        break;

                    case ExoPlaybackException.TYPE_UNEXPECTED:
                        reloadPlayer(loopingSource);
                        Log.e(TAG, "TYPE_UNEXPECTED: " + error.getUnexpectedException().getMessage());
                        break;
                    default:
                        reloadPlayer(loopingSource);
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

    private void reloadPlayer(LoopingMediaSource loopingSource) {
        mPlayer.stop();
        mPlayer.prepare(loopingSource);
        mPlayer.setPlayWhenReady(true);
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
                if(((PlaybackControlsRow.MoreActions) item).getId()==VideoPlayerGlue.EPG_ID){
                    Intent intent = new Intent(getContext(),SettingsActivity.class);
                    getContext().startActivity(intent);
                    mPlayerGlue.play();
                }

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