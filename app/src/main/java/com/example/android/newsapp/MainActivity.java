package com.example.android.newsapp;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<List<News>>, SwipeRefreshLayout.OnRefreshListener {

    private static final int ASYNC_LOADER_ID = 1;
    private static final String ENDPOINT = "https://content.guardianapis.com/";
    private static final String API_KEY_PARAMETER = "api-key";
    private static final String PAGE_PARAMETER = "page";
    private static final String SHOW_FIELDS_PARAMETER = "show-fields";
    private static final String SHOW_FIELDS_BYLINE = "byline";
    private static final String SHOW_FIELDS_TRAILTEXT = "trailText";

    private static String API_KEY;
    private RecyclerView mRecyclerView;
    private NewsRecyclerAdapter mRecyclerAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private TextView mEmptyStateTextView;
    private List<News> mNewsList;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LoaderManager mLoaderManager;
    private String mRequest, mCategory;
    private int pageNumber;
    private int totalPages;
    private boolean loadingState, isNightModeEnabled;
    private static ArrayList<String> mShowFieldsParams;

    /**
     * Construct current request URL
     *
     * @param page
     * @return
     */
    private static String constructUrl(int page, String section) {

        //Setup a show-fields params
        String showFieldsParams = android.text.TextUtils.join(",", mShowFieldsParams);

        //Set search section name for "all" category
        if(section.equals("all")) section = "search";

        //URL Construction
        Uri endpoint = Uri.parse(ENDPOINT);
        Uri.Builder builder = endpoint.buildUpon();
        builder.appendPath(section);
        builder.appendQueryParameter(PAGE_PARAMETER, String.valueOf(page));
        builder.appendQueryParameter(SHOW_FIELDS_PARAMETER, showFieldsParams);
        builder.appendQueryParameter(API_KEY_PARAMETER, API_KEY);
        String request = builder.toString();

        return request;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Load preferences
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        //Night mode
        this.isNightModeEnabled = sharedPreferences.getBoolean(
                getString(R.string.settings_night_key), false);

        if (isNightModeEnabled) {
            setTheme(R.style.MainActivityThemeDark);
        }

        //SET VIEW
        setContentView(R.layout.activity_main);

        mCategory = sharedPreferences.getString(
                getString(R.string.settings_category_key),
                getString(R.string.settings_category_default)
        );

        //Set List for show-field Params.
        mShowFieldsParams = new ArrayList<>();
        mShowFieldsParams.add(SHOW_FIELDS_BYLINE);

        // Trail text checkbox Preference logic
        if(sharedPreferences.getBoolean(getString(R.string.settings_trail_text_key),
                false)){
            mShowFieldsParams.add(SHOW_FIELDS_TRAILTEXT);
        } else {
            mShowFieldsParams.remove(SHOW_FIELDS_TRAILTEXT);
        }

        //Getting API_KEY
        API_KEY = getString(R.string.API_KEY);

        //Number Of Page for API Request
        resetPageCounter();
        //Set loading state
        loadingState = false;

        //Set ArrayList for News
        mNewsList = new ArrayList<>();

        //Set Empty View for empty list
        mEmptyStateTextView = (TextView) findViewById(R.id.not_found);

        //Set up Swipe refresh
        mSwipeRefreshLayout = findViewById(R.id.swipe);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeColors(getResources()
                .getColor(android.R.color.holo_blue_dark));
        mSwipeRefreshLayout.setRefreshing(true);

        //Recycler init
        mRecyclerView = findViewById(R.id.news_recycle);
        mRecyclerAdapter = new NewsRecyclerAdapter(this, mNewsList);
        if (mRecyclerView != null) {
            mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
            mRecyclerView.setLayoutManager(mLinearLayoutManager);
            mRecyclerView.setAdapter(mRecyclerAdapter);
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    //Set handling scroll recycle to bottom
                    if (!recyclerView.canScrollVertically(1)) {
                        if (isConnected() && mNewsList.size() != 0) {
                            loadMore();
                        } else if (isConnected() && mNewsList.size() == 0) {
                            startLoad();
                        } else {
                            notConnected();
                            loader("destroy");
                            resetPageCounter();
                        }
                    }
                }
            });
        }
        startLoad();
    }

    /**
     * Create Settings menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_settings){
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<List<News>> onCreateLoader(int id, Bundle args) {
        return new NewsLoader(this, mRequest);
    }

    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> newsData) {
        //Set text for empty view
        mEmptyStateTextView.setText(R.string.news_not_found);
        // Set total pages
        totalPages = Utils.getMaxPages();
        //Show swipe
        mSwipeRefreshLayout.setRefreshing(true);

        if (newsData != null && !newsData.isEmpty()) {

            // Refresh data in recycler
            mNewsList.addAll(newsData);
            mRecyclerAdapter.notifyDataSetChanged();
            // off the swipe progress
            mSwipeRefreshLayout.setRefreshing(false);
            mEmptyStateTextView.setVisibility(View.GONE);
            loadingState = false;
        } else {
            mEmptyStateTextView.setVisibility(View.VISIBLE);
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<News>> loader) {
        mNewsList.clear();
    }

    @Override
    public void onRefresh() {
        if (isConnected()) {
            resetPageCounter();
            mNewsList.clear();
            startLoad();
        } else {
            notConnected();
            loader("destroy");
        }
    }

    /**
     * Init loader and first load
     */
    private void startLoad() {
        // Test a network state
        if (isConnected()) {

            mRequest = constructUrl(pageNumber, mCategory);
            // Loader Init
            mLoaderManager = getLoaderManager();
            loader("init");
            //Hide empty state
            mEmptyStateTextView.setVisibility(View.GONE);

        } else {
            notConnected();
        }
    }

    /**
     * Load more pages from API
     * <p>
     * Running if previous loading complete and last page number less than response.pages
     */
    private void loadMore() {

        if (!loadingState && totalPages > pageNumber) {
            pageNumber++;
            loadingState = true;
            mRequest = constructUrl(pageNumber, mCategory);
            if (isConnected()) {
                loader("restart");
            } else {
                notConnected();
            }
        }
    }

    /**
     * checking connection state
     *
     * @return boolean
     */
    private boolean isConnected() {

        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo.isConnected();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Run loader init or restart
     *
     * @param event - if init - loader will be init, else - restarted
     */
    private void loader(String event) {

        switch (event) {
            case "init":
                if (mLoaderManager.getLoader(ASYNC_LOADER_ID) != null)
                    mLoaderManager.destroyLoader(ASYNC_LOADER_ID);
                mLoaderManager.initLoader(ASYNC_LOADER_ID, null, this);
                break;
            case "destroy":
                if (mLoaderManager.getLoader(ASYNC_LOADER_ID) != null)
                    mLoaderManager.destroyLoader(ASYNC_LOADER_ID);
                break;
            default:
                mLoaderManager.restartLoader(ASYNC_LOADER_ID, null, this);
                break;
        }
    }

    /**
     * Handler if have no connection state
     */
    private void notConnected() {
        mSwipeRefreshLayout.setRefreshing(false);
        mEmptyStateTextView.setVisibility(View.VISIBLE);
        mEmptyStateTextView.setText(R.string.no_internet);
        mNewsList.clear();
        mRecyclerAdapter.notifyDataSetChanged();
    }

    /**
     * Reset current page num and total page counter to default values
     */
    private void resetPageCounter() {
        pageNumber = 1;
        totalPages = 2;
    }
}
