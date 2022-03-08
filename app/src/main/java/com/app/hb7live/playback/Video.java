
package com.app.hb7live.playback;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Video is an immutable object that holds the various metadata associated with a single video.
 */
public final class Video implements Parcelable {
  public final long id;
  public final String category;
  public final String title;
  public final String description;
  public final String bgImageUrl;
  public final String cardImageUrl;
  public final String videoUrl;
  public final String currentProg;
  public final Integer studio;

  private Video(
          final long id,
          final String category,
          final String title,
          final String desc,
          final String videoUrl,
          final String bgImageUrl,
          final String cardImageUrl,
          final String currentProg,
          final Integer studio
  ) {
    this.id = id;
    this.category = category;
    this.title = title;
    this.description = desc;
    this.videoUrl = videoUrl;
    this.bgImageUrl = bgImageUrl;
    this.cardImageUrl = cardImageUrl;
    this.currentProg = currentProg;
    this.studio = studio;
  }

  protected Video(Parcel in) {
    id = in.readLong();
    category = in.readString();
    title = in.readString();
    description = in.readString();
    bgImageUrl = in.readString();
    cardImageUrl = in.readString();
    videoUrl = in.readString();
    currentProg = in.readString();
    studio = in.readInt();
  }

  public static final Creator<Video> CREATOR = new Creator<Video>() {
    @Override
    public Video createFromParcel(Parcel in) {
      return new Video(in);
    }

    @Override
    public Video[] newArray(int size) {
      return new Video[size];
    }
  };

  @Override
  public boolean equals(Object m) {
    return m instanceof Video && id == ((Video) m).id;
  }

  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(id);
    dest.writeString(category);
    dest.writeString(title);
    dest.writeString(description);
    dest.writeString(bgImageUrl);
    dest.writeString(cardImageUrl);
    dest.writeString(videoUrl);
    dest.writeString(currentProg);
    dest.writeInt(studio);
  }

  @Override
  public String toString() {
    String s = "Video{";
    s += "id=" + id;
    s += ", category='" + category + "'";
    s += ", title='" + title + "'";
    s += ", videoUrl='" + videoUrl + "'";
    s += ", bgImageUrl='" + bgImageUrl + "'";
    s += ", cardImageUrl='" + cardImageUrl + "'";
    s += ", currentProg='" + currentProg + "'";
    s += ", order='" + studio + "'";
    s += "}";
    return s;
  }

  // Builder for Video object.
  public static class VideoBuilder {
    private long id;
    private String category;
    private String title;
    private String desc;
    private String bgImageUrl;
    private String cardImageUrl;
    private String videoUrl;
    private String currentProg;
    private Integer studio;

    public VideoBuilder id(long id) {
      this.id = id;
      return this;
    }

    public VideoBuilder category(String category) {
      this.category = category;
      return this;
    }

    public VideoBuilder title(String title) {
      this.title = title;
      return this;
    }

    public VideoBuilder description(String desc) {
      this.desc = desc;
      return this;
    }

    public VideoBuilder currentProg(String currentProg) {
      this.currentProg = currentProg;
      return this;
    }

    public VideoBuilder videoUrl(String videoUrl) {
      this.videoUrl = videoUrl;
      return this;
    }

    public VideoBuilder bgImageUrl(String bgImageUrl) {
      this.bgImageUrl = bgImageUrl;
      return this;
    }

    public VideoBuilder cardImageUrl(String cardImageUrl) {
      this.cardImageUrl = cardImageUrl;
      return this;
    }

    public VideoBuilder studio(Integer order) {
      this.studio = order;
      return this;
    }


    public Video build() {
      return new Video(
              id,
              category,
              title,
              desc,
              videoUrl,
              bgImageUrl,
              cardImageUrl,
              currentProg,
              studio
      );
    }
  }
}