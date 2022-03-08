package com.app.hb7live.cards.presenters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.app.hb7live.R;
import com.app.hb7live.playback.Video;
import com.app.hb7live.utils.ResourceCache;

import javax.inject.Inject;

public class DetailsDescriptionPresenter extends Presenter {

    private ResourceCache mResourceCache = new ResourceCache();
    private Context mContext;

    @Inject
    public DetailsDescriptionPresenter(Context context) {
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.detail_view_content, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        TextView primaryText = mResourceCache.getViewById(viewHolder.view, R.id.primary_text);
        //TextView sndText1 = mResourceCache.getViewById(viewHolder.view, R.id.secondary_text_first);
        // TextView sndText2 = mResourceCache.getViewById(viewHolder.view, R.id.secondary_text_second);
        TextView extraText = mResourceCache.getViewById(viewHolder.view, R.id.extra_text);

        Video card = (Video) item;
        primaryText.setText(card.title);
        extraText.setText(card.description);
        //sndText2.setText(card.getYear() + "");
        // extraText.setText(card.getText());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // Nothing to do here.
    }
}