package com.example.android.newsapp;

public class News {

    private String mTitle, mUrl, mDate, mCategory, mAuthor, mTrailText;

    public News(String title, String trailText, String url, String date, String category, String author) {
        mTitle = title;
        mTrailText = trailText;
        mUrl = url;
        mDate = date;
        mCategory = category;
        mAuthor = author;
    }

    public String getmTitle() {
        return mTitle;
    }

    public String getmUrl() {
        return mUrl;
    }

    public String getmDate() {
        return mDate;
    }

    public String getmCategory() {
        return mCategory;
    }

    public String getmAuthor() {
        return mAuthor;
    }

    public String getmTrailText() {
        return mTrailText;
    }
}
