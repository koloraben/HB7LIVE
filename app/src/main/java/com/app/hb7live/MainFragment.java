package com.app.hb7live;


import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

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
    ProgressDialog pdLoading;
    AlertDialog alertDialog; //Read Update
    private boolean isDataEmpty = true;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pdLoading.dismiss();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle("Oops!");
        alertDialog.setMessage("Pas de données, Veuillez essayer ultérieurement.");
        alertDialog.setButton("ressayer", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                getActivity().recreate();
            }
        });

        alertDialog.setButton2("Quiter", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                getActivity().finish();
            }
        });

        pdLoading = new ProgressDialog(getContext());
        pdLoading.setMessage("\tLoading...");
        pdLoading.show();
        mVideoCursorAdapters = new CursorObjectAdapter(new CardPresenterSelector(getContext()));
        mVideoCursorAdapters.setMapper(new VideoCursorMapper());
        // Start loading the categories from the database.

        setupData();
        getLoaderManager().initLoader(0, null, this);
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
        Handler handler = new Handler();
        if (data.getCount() == 0) {
            isDataEmpty = true;
        } else {
            isDataEmpty = false;
            mRowsAdapter.add(row);
        }

        handler.postDelayed(new Runnable() {
            public void run() {
                if (isDataEmpty) {
                    alertDialog.show();
                } else {
                    alertDialog.hide();
                }

            }
        }, 2000);  // 1500 seconds

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
            intent.putExtra(DetailViewActivity.LIVE, (Video) item);
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
