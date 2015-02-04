package com.gscasu.sunshineapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.gscasu.sunshineapp.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Class the fetches the weather data and all related data and introduces them to the UI
 */
 public class FetchWeatherTask extends AsyncTask<String, Void, Void>{
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private Context mContext;

    FetchWeatherTask(Context context){
        mContext = context;

    }

    /*private String getReadableDateString(long time){
        // Because the API returns a Unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date);
    }*/

    public void getWeatherDataFromJson(String forecastJsonStr, int numDays, String locationSetting)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DATETIME = "dt";
        final String OWM_DESCRIPTION = "main";
        final String OWM_COORD = "coord";
        final String OWM_LAT = "lat";
        final String OWM_LON = "lon";
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";
        final String OWM_WEATHER_ID = "id";

        //Set double
        double cityLongitude, cityLatitude;
        JSONObject weatherJson = new JSONObject(forecastJsonStr);
        JSONObject coordJson = weatherJson.getJSONObject(OWM_CITY).getJSONObject(OWM_COORD);

        String cityName = weatherJson.getJSONObject(OWM_CITY).getString(OWM_CITY_NAME);
        //Set Coordinates values to those recieved in JSON
        cityLongitude = coordJson.getDouble(OWM_LAT);
        cityLatitude = coordJson.getDouble(OWM_LON);

        Log.v(LOG_TAG, "long: " + cityLongitude + " Lat: " + cityLatitude);

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
        long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

        ArrayList<ContentValues> weatherContentValues = new ArrayList<>();

        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;

            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;

            int weatherId;
            //String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this Saturday".
            long dateTime = dayForecast.getLong(OWM_DATETIME);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            ContentValues weatherValue = new ContentValues();

            weatherValue.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
            weatherValue.put(WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                   WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
            weatherValue.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValue.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValue.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValue.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValue.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValue.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValue.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValue.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

            weatherContentValues.add(weatherValue);


        }

        /*for (int i = 0; i < weatherContentValues.size(); i++) {
            mContext.getContentResolver().insert(WeatherContract.WeatherEntry.CONTENT_URI,
                    weatherContentValues.get(i)
            );*/


        if(weatherContentValues.size() > 0) {
            ContentValues[] weatherContentValuesArray = new ContentValues[weatherContentValues.size()];
            weatherContentValues.toArray(weatherContentValuesArray);

            int rowsInserted = mContext.getContentResolver().bulkInsert(
                    WeatherContract.WeatherEntry.CONTENT_URI,
                    weatherContentValuesArray
            );

            Log.v(LOG_TAG, "inserted " + Integer.toString(rowsInserted) + " rows");
        }

    }

    @Override
    protected Void doInBackground(String... params) {


        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;
        int numdays = 14;
        String locationQuery = params[0];
        String[] formattedWeather = new String [numdays];

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            Uri uri = Uri.parse("http://api.openweathermap.org/data")
                    .buildUpon().appendPath("2.5")
                    .appendPath("forecast")
                    .appendPath("daily")
                    .appendQueryParameter("q",params[0])
                    .appendQueryParameter("mode", "json")
                    .appendQueryParameter("units", "metric")
                    .appendQueryParameter("cnt", Integer.toString(numdays)).build();
            URL url = new URL(uri.toString());

            Log.v(LOG_TAG,url.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                Log.w(LOG_TAG, "null stream");
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                Log.w(LOG_TAG,"empty stream");
                return null;
            }
            forecastJsonStr = buffer.toString();
            Log.v(LOG_TAG,forecastJsonStr);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error 1", e);
            // If the code didn't successfully get the weather com.gscasu.sunshineapp.data, there's no point in attempting
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            getWeatherDataFromJson(forecastJsonStr, numdays, params[0]);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }
        return null;
    }
    private long addLocation(String locationSetting, String cityName, double lat, double lon){

        Cursor cursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                null,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING +  " = ?",
                new String[] {locationSetting},
                null
                );
        if(cursor.moveToFirst()){
            Log.i(LOG_TAG,"location already exists");
            return cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
        }
        else{
            ContentValues contentValues = new ContentValues();
            contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            Uri insertUri =  mContext.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    contentValues
            );

            return ContentUris.parseId(insertUri);
        }
    }
}
