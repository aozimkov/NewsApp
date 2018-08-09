package com.example.android.newsapp;


import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Utils {

    private static final String LOG_TAG = Utils.class.getName();
    private static int MAX_PAGES;

    private Utils() {

    }

    public static List<News> extractNews(String jsonResponse) {
        List<News> news = new ArrayList<>();

        //Try to parse

        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONObject response = root.getJSONObject("response");

            //Handle the response status ok
            if (response.getString("status").equals("ok")) {

                //Init Max pages
                int totalPages = response.getInt("pages");
                if (MAX_PAGES == 0 || MAX_PAGES != totalPages) MAX_PAGES = totalPages;

                JSONArray results = response.getJSONArray("results");

                for (int i = 0; i < response.length(); i++) {
                    JSONObject current = results.getJSONObject(i);
                    JSONObject fields = current.getJSONObject("fields");

                    //Handling of optional data
                    String author = "";
                    //Handling byline - author
                    if (fields.has("byline")) author = fields.getString("byline");
                    String trailText = "";
                    //Handling trail text. Strip html tags
                    if (fields.has("trailText"))
                        trailText = Html.fromHtml(fields.getString("trailText")).toString();

                    String date = "";
                    if (current.has("webPublicationDate")) date =
                            current.getString("webPublicationDate");

                    news.add(new News(
                            Html.fromHtml(current.getString("webTitle")).toString(),
                            trailText,
                            current.getString("webUrl"),
                            date,
                            current.getString("sectionName"),
                            author));
                }
            } else {
                return news;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return news;
    }

    /**
     * Fetching Url
     *
     * @param requestUrl
     * @return
     */
    public static List<News> fetchNewsUrl(String requestUrl) {

        // Create URL object
        URL url = createUrl(requestUrl);

        //Create List object for result
        List<News> news;

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error closing input stream", e);
        }

        // Extract relevant fields from the JSON response and create an {@link Event} object
        news = extractNews(jsonResponse);


        // Return the {@link Event}
        return news;
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error with creating URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            inputStream = urlConnection.getInputStream();
            jsonResponse = readFromStream(inputStream);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * method parse and convert ISO 8601 date to yyyy.MM.dd
     *
     * @param datetimetz
     * @return
     */
    public static String formattedDate(String datetimetz) {

        if (datetimetz == "") return "";

        String readyDate;
        String readyTime;

        DateFormat dateFormatParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {

            Date currentData = dateFormatParser.parse(datetimetz);
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            DateFormat timeFormat = new SimpleDateFormat("HH:mm");
            readyTime = timeFormat.format(currentData);
            readyDate = dateFormat.format(currentData);

        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
        return readyDate + " " + readyTime;
    }

    public static int getMaxPages() {
        return MAX_PAGES;
    }
}
