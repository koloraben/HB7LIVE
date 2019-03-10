package com.app.hb7live;


import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.content.CursorLoader;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.app.BrowseSupportFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.app.hb7live.cards.presenters.CardPresenterSelector;
import com.app.hb7live.cards.presenters.ImageCardViewPresenter;
import com.app.hb7live.live.DetailViewActivity;
import com.app.hb7live.models.Card;
import com.app.hb7live.models.CardRow;
import com.app.hb7live.playback.Video;
import com.app.hb7live.playback.VideoContract;
import com.app.hb7live.playback.VideoCursorMapper;
import com.app.hb7live.utils.FetchVideoService;
import com.app.hb7live.utils.Utils;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;


public class MainFragment extends BrowseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private ArrayObjectAdapter mRowsAdapter;
    private LoaderManager mLoaderManager;
    private static final int CATEGORY_LOADER = 123321;
    private Map<Integer, CursorObjectAdapter> mVideoCursorAdapters;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mVideoCursorAdapters = new HashMap<>();

        // Start loading the categories from the database.
        getLoaderManager().initLoader(CATEGORY_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
       // setupData();
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
        setTitle(getString(R.string.browse_title));
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
        if (id == CATEGORY_LOADER) {
            return new CursorLoader(
                    getContext(),
                    VideoContract.VideoEntry.CONTENT_URI, // Table to query
                    new String[]{"DISTINCT " + VideoContract.VideoEntry.COLUMN_CATEGORY},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );
        } else {
            // Assume it is for a video.
            String category = args.getString(VideoContract.VideoEntry.COLUMN_CATEGORY);

            // This just creates a CursorLoader that gets all videos.
            return new CursorLoader(
                    getContext(),
                    VideoContract.VideoEntry.CONTENT_URI, // Table to query
                    null, // Projection to return - null means return all fields
                    VideoContract.VideoEntry.COLUMN_CATEGORY + " = ?", // Selection clause
                    new String[]{category},  // Select based on the category id.
                    null // Default sort order
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data != null && data.moveToFirst()) {
            final int loaderId = loader.getId();

            if (loaderId == CATEGORY_LOADER) {

                // Every time we have to re-get the category loader, we must re-create the sidebar.
                mRowsAdapter.clear();

                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {

                    int categoryIndex =
                            data.getColumnIndex(VideoContract.VideoEntry.COLUMN_CATEGORY);
                    String category = data.getString(categoryIndex);
                    Log.e("category :;;", (categoryIndex) + "");
                    // Create header for this category.

                    int videoLoaderId = category.hashCode(); // Create unique int from category.
                    CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
                    if (existingAdapter == null) {

                        // Map video results from the database to Video objects.
                        CursorObjectAdapter videoCursorAdapter =
                                new CursorObjectAdapter(new ImageCardViewPresenter());
                        videoCursorAdapter.setMapper(new VideoCursorMapper());
                        mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

                        ListRow row = new ListRow(videoCursorAdapter);
                        mRowsAdapter.add(row);

                        // Start loading the videos from the database for a particular category.
                        Bundle args = new Bundle();
                        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, category);
                        getLoaderManager().initLoader(videoLoaderId, args, this);
                    } else {
                        ListRow row = new ListRow(existingAdapter);
                        mRowsAdapter.add(row);
                    }

                    data.moveToNext();
                }

                // Create a row for this special case with more samples.

                startEntranceTransition(); // TODO: Move startEntranceTransition to after all
                // cursors have loaded.
            } else {
                // The CursorAdapter contains a Cursor pointing to all videos.
                mVideoCursorAdapters.get(loaderId).changeCursor(data);
            }
        } else {
            // Start an Intent to fetch the videos.
            setupData();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

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

    public static class WebViewFragment extends Fragment implements MainFragmentAdapterProvider {
        private MainFragmentAdapter mMainFragmentAdapter = new MainFragmentAdapter(this);
        private WebView mWebview;

        @Override
        public MainFragmentAdapter getMainFragmentAdapter() {
            return mMainFragmentAdapter;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getMainFragmentAdapter().getFragmentHost().showTitleView(false);
        }

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            FrameLayout root = new FrameLayout(getActivity());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            lp.setMarginStart(32);
            mWebview = new WebView(getActivity());
            mWebview.setWebViewClient(new WebViewClient());
            mWebview.getSettings().setJavaScriptEnabled(true);
            root.addView(mWebview, lp);
            return root;
        }

        @Override
        public void onResume() {
            super.onResume();
            mWebview.loadUrl("https://www.google.com/policies/terms");
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }
    }
}
