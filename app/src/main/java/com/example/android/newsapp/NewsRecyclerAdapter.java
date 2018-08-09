package com.example.android.newsapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class NewsRecyclerAdapter extends RecyclerView.Adapter<NewsRecyclerAdapter.NewsHolder> {

    private Context mContext;
    private List<News> mNews;

    public NewsRecyclerAdapter(Context context, List<News> news) {
        mContext = context;
        mNews = news;
    }

    public class NewsHolder extends RecyclerView.ViewHolder {
        public TextView newsHeader, newsDate, newsCategory, newsAuthor, newsTrailText;
        public LinearLayout listItem;

        public NewsHolder(View view) {
            super(view);

            listItem = view.findViewById(R.id.list_item_layout);
            newsHeader = view.findViewById(R.id.news_header);
            newsTrailText = view.findViewById(R.id.news_trail_text);
            newsDate = view.findViewById(R.id.news_date);
            newsCategory = view.findViewById(R.id.news_category);
            newsAuthor = view.findViewById(R.id.news_author);
        }
    }

    @NonNull
    @Override
    public NewsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.list_item, parent, false);
        return new NewsHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final NewsHolder holder, int position) {
        //get current news
        final News current = mNews.get(position);

        //construct holder
        holder.newsHeader.setText(current.getmTitle());
        holder.newsDate.setText(Utils.formattedDate(current.getmDate()));
        holder.newsCategory.setText(current.getmCategory());
        holder.newsAuthor.setText(current.getmAuthor());

        if(!current.getmTrailText().equals("")) {
            holder.newsTrailText.setText(current.getmTrailText());
            holder.newsTrailText.setVisibility(View.VISIBLE);
        } else {
            holder.newsTrailText.setVisibility(View.GONE);
        }

        holder.newsCategory.setBackgroundColor(setCategoryColor(current.getmCategory()));

        // Handling on click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newsUrl = current.getmUrl();
                Intent newsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(newsUrl));
                mContext.startActivity(newsIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mNews.size();
    }

    private int setCategoryColor(String categoryName){

        int categoryColor;
        Resources res = mContext.getResources();

        switch (categoryName){
            case "News":
                categoryColor = res.getColor(R.color.news);
                break;
            case "Music":
            case "Television & radio":
            case "Life and style":
                categoryColor = res.getColor(R.color.music);
                break;
            case "Science":
            case "Technology":
                categoryColor = res.getColor(R.color.science);
                break;
            case "Business":
            case "World news":
            case "Global development":
                categoryColor = res.getColor(R.color.business);
                break;
            case "Sport":
            case "Football":
            case "Environment":
                categoryColor = res.getColor(R.color.sport);
                break;
                default:
                    categoryColor = res.getColor(R.color.default_color);
        }

        return categoryColor;
    }


}
