package com.app.hb7live.live;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.app.DetailsFragment;
import androidx.leanback.app.DetailsFragmentBackgroundController;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.app.hb7live.R;
import com.app.hb7live.cards.presenters.DetailsDescriptionPresenter;
import com.app.hb7live.playback.PlaybackActivity;
import com.app.hb7live.playback.Video;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

public class DetailViewFragment extends DetailsFragment implements OnItemViewClickedListener,
        OnItemViewSelectedListener {

  public static final String TRANSITION_NAME = "t_for_transition";
  public static final String EXTRA_CARD = "card";

  private static final long ACTION_WATCH = 1;
  private static final long ACTION_WISHLIST = 2;
  //private static final long ACTION_RELATED = 3;
  private Video mSelectedVideo;

  private Action mActionWatch;
  private Action mActionWishList;
  private Action mActionRelated;
  private ArrayObjectAdapter mRowsAdapter;
  private final DetailsFragmentBackgroundController mDetailsBackground =
          new DetailsFragmentBackgroundController(this);

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mSelectedVideo = (Video) getActivity().getIntent()
            .getParcelableExtra(DetailViewActivity.LIVE);

    setupUi();
    setupEventListeners();
  }

  private void setupUi() {


    FullWidthDetailsOverviewRowPresenter rowPresenter = new FullWidthDetailsOverviewRowPresenter(
            new DetailsDescriptionPresenter(getActivity())) {

      @Override
      protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
        // Customize Actionbar and Content by using custom colors.
        RowPresenter.ViewHolder viewHolder = super.createRowViewHolder(parent);

        View actionsView = viewHolder.view.
                findViewById(R.id.details_overview_actions_background);
        actionsView.setBackgroundColor(getActivity().getResources().
                getColor(R.color.detail_view_actionbar_background));

        View detailsView = viewHolder.view.findViewById(R.id.details_frame);
        detailsView.setBackgroundColor(
                getResources().getColor(R.color.detail_view_background));
        return viewHolder;
      }
    };

    FullWidthDetailsOverviewSharedElementHelper mHelper = new FullWidthDetailsOverviewSharedElementHelper();
    mHelper.setSharedElementEnterTransition(getActivity(), TRANSITION_NAME);
    rowPresenter.setListener(mHelper);
    rowPresenter.setParticipatingEntranceTransition(false);
    prepareEntranceTransition();

    ListRowPresenter shadowDisabledRowPresenter = new ListRowPresenter();
    shadowDisabledRowPresenter.setShadowEnabled(false);

    // Setup PresenterSelector to distinguish between the different rows.
    ClassPresenterSelector rowPresenterSelector = new ClassPresenterSelector();
    rowPresenterSelector.addClassPresenter(DetailsOverviewRow.class, rowPresenter);
    //rowPresenterSelector.addClassPresenter(CardListRow.class, shadowDisabledRowPresenter);
    rowPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
    mRowsAdapter = new ArrayObjectAdapter(rowPresenterSelector);

    // Setup action and detail row.
    DetailsOverviewRow detailsOverview = new DetailsOverviewRow(mSelectedVideo);

    RequestOptions options = new RequestOptions()
            .error(R.drawable.background_canyon)
            .dontAnimate();

    Glide.with(this)
            .asBitmap()
            .load(mSelectedVideo.cardImageUrl)
            .apply(options)
            .into(new SimpleTarget<Bitmap>() {
              @Override
              public void onResourceReady(
                      Bitmap resource,
                      Transition<? super Bitmap> transition) {
                detailsOverview.setImageBitmap(getActivity(), resource);
                startEntranceTransition();
              }
            });
    ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter();

    mActionWatch = new Action(ACTION_WATCH, getString(R.string.action_watch));
    //mActionWishList = new Action(ACTION_WISHLIST, getString(R.string.action_wishlist));
    //mActionRelated = new Action(ACTION_RELATED, getString(R.string.action_related));

    actionAdapter.add(mActionWatch);
    //actionAdapter.add(mActionWishList);
    //actionAdapter.add(mActionRelated);
    detailsOverview.setActionsAdapter(actionAdapter);
    mRowsAdapter.add(detailsOverview);

     /*     // Setup related row.
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenterSelector(getActivity()));
     for (Video characterCard : data.getCharacters()) listRowAdapter.add(characterCard);
        HeaderItem header = new HeaderItem(0, getString(R.string.header_related));
        mRowsAdapter.add(new CardListRow(header, listRowAdapter, null));
        // Setup recommended row.
        listRowAdapter = new ArrayObjectAdapter(new CardPresenterSelector(getActivity()));
        //for (Video card : data.getRecommended()) listRowAdapter.add(card);
        HeaderItem header = new HeaderItem(1, getString(R.string.header_recommended));
        mRowsAdapter.add(new ListRow(header, listRowAdapter));
*/
    setAdapter(mRowsAdapter);
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        startEntranceTransition();
      }
    }, 500);
    initializeBackground();
  }

  private void initializeBackground() {
    mDetailsBackground.enableParallax();

    RequestOptions options = new RequestOptions()
            .error(R.drawable.background_canyon)
            .dontAnimate();

    Glide.with(this)
            .asBitmap()
            .load(mSelectedVideo.bgImageUrl)
            .apply(options)
            .into(new SimpleTarget<Bitmap>() {
              @Override
              public void onResourceReady(
                      Bitmap resource,
                      Transition<? super Bitmap> transition) {
                mDetailsBackground.setCoverBitmap(resource);
                startEntranceTransition();
              }
            });
  }

  private void setupEventListeners() {
    setOnItemViewSelectedListener(this);
    setOnItemViewClickedListener(this);
  }

  @Override
  public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                            RowPresenter.ViewHolder rowViewHolder, Row row) {
    if (item instanceof Action){
      Action action = (Action) item;


       /* if (action.getId() == ACTION_RELATED) {
            setSelectedPosition(1);
        }*/
      if(action.getId() == ACTION_WATCH){
        Intent intent = new Intent(getActivity(), PlaybackActivity.class);
        intent.putExtra(DetailViewActivity.LIVE, mSelectedVideo);
        startActivity(intent);
      }
      if(item instanceof Video){
        Intent intent = new Intent(getActivity().getBaseContext(),
                DetailViewActivity.class);
        intent.putExtra(DetailViewActivity.LIVE,(Video) item);
        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                ((ImageCardView) itemViewHolder.view).getMainImageView(),
                DetailViewActivity.SHARED_ELEMENT_NAME)
                .toBundle();
        startActivity(intent, bundle);
      }}
  }

  @Override
  public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                             RowPresenter.ViewHolder rowViewHolder, Row row) {
    if (mRowsAdapter.indexOf(row) > 0) {
      int backgroundColor = getResources().getColor(R.color.detail_view_related_background);
      getView().setBackgroundColor(backgroundColor);
    } else {
      getView().setBackground(null);
    }
  }
}