

package com.app.hb7live.playback;

import android.database.Cursor;

import androidx.leanback.database.CursorMapper;

/**
 * VideoCursorMapper maps a database Cursor to a Video object.
 */
public final class VideoCursorMapper extends CursorMapper {

    private static int idIndex;
    private static int nameIndex;
    private static int descIndex;
    private static int videoUrlIndex;
    private static int currentProgIndex;
    private static int bgImageUrlIndex;
    private static int cardImageUrlIndex;
    private static int categoryIndex;
    private static int studioIndex;

    @Override
    protected void bindColumns(Cursor cursor) {
        idIndex = cursor.getColumnIndex(VideoContract.VideoEntry._ID);
        nameIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_NAME);
        descIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_DESC);
        videoUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_VIDEO_URL);
        currentProgIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CURRENT_PROG);
        bgImageUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL);
        cardImageUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CARD_IMG);
        categoryIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CATEGORY);
        studioIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_STUDIO);
    }

    @Override
    protected Object bind(Cursor cursor) {
//        Log.e("Cursor",cursor.getLong(idIndex)+"");
        // Get the values of the video.
        long id = cursor.getLong(idIndex);
        String category = cursor.getString(categoryIndex);
        String title = cursor.getString(nameIndex);
        String desc = cursor.getString(descIndex);
        String videoUrl = cursor.getString(videoUrlIndex);
        String currentProg = cursor.getString(currentProgIndex);
        String bgImageUrl = cursor.getString(bgImageUrlIndex);
        String cardImageUrl = cursor.getString(cardImageUrlIndex);
        Integer order = cursor.getInt(studioIndex);

        // Build a Video object to be processed.
        return new Video.VideoBuilder()
                .id(id)
                .title(title)
                .category(category)
                .description(desc)
                .videoUrl(videoUrl)
                .bgImageUrl(bgImageUrl)
                .cardImageUrl(cardImageUrl)
                .studio(order)
                .currentProg(currentProg)
                .build();
    }
}
