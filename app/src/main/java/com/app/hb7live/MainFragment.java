package com.app.hb7live;


import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;

import com.app.hb7live.cards.presenters.CardPresenterSelector;
import com.app.hb7live.cards.presenters.ImageCardViewPresenter;
import com.app.hb7live.live.DetailViewActivity;
import com.app.hb7live.playback.Video;
import com.app.hb7live.playback.VideoContract;
import com.app.hb7live.playback.VideoCursorMapper;
import com.app.hb7live.utils.FetchVideoService;


public class MainFragment extends BrowseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private ArrayObjectAdapter mRowsAdapter;
    private LoaderManager mLoaderManager;
    private static final int CATEGORY_LOADER = 123321;
    private CursorObjectAdapter mVideoCursorAdapters;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mVideoCursorAdapters = new CursorObjectAdapter(new CardPresenterSelector(getContext()));
        mVideoCursorAdapters.setMapper(new VideoCursorMapper());
        // Start loading the categories from the database.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupData();
        setupUIElements();
        setupRowAdapter();
        setupEventListeners();
    }

    private void setupData() {
        Intent serviceIntent = new Intent(getActivity(), FetchVideoService.class);
        getActivity().startService(serviceIntent);
    }

    private void setupRowAdapter() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
    }


    private void setupUIElements() {
        setTitle("");
        //setBadgeDrawable(getResources().getDrawable(R.drawable.title_android_tv, null));
        setHeadersState(HEADERS_DISABLED);
        setHeadersTransitionOnBackEnabled(true);
        //setBrandColor(getResources().getColor(R.color.fastlane_background));
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            // This just creates a CursorLoader that gets all videos.
            return new CursorLoader(
                    getContext(),
                    VideoContract.VideoEntry.CONTENT_URI, // Table to query
                    null, // Projection to return - null means return all fields
                    null, // Selection clause
                    null,  // Select based on the category id.
                    "studio ASC" // Default sort order
            );

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mRowsAdapter.clear();
        mVideoCursorAdapters.changeCursor(data);
        ListRow row = new ListRow(mVideoCursorAdapters);
        mRowsAdapter.add(row);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mVideoCursorAdapters.changeCursor(null);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            Intent intent = new Intent(getActivity().getBaseContext(),
                    DetailViewActivity.class);
            intent.putExtra(DetailViewActivity.LIVE,(Video) item);
            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                    DetailViewActivity.SHARED_ELEMENT_NAME)
                    .toBundle();
            startActivity(intent, bundle);
        }

    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {

        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
        }
    }

}
