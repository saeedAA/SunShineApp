package com.gscasu.sunshineapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.gscasu.sunshineapp.data.WeatherContract;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    //private final String LOG_TAG = getActivity().getClass().getSimpleName();
	
	private SimpleCursorAdapter mForecastAdapter = null;
    private String mLocation;
    private static final int FORECAST_LOADER = 0;

    //Names of the columns to easily call when querying from the database or the cursor
    private final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    //Indices tied to FORECAST_COLUMNS
    public final int COL_WEATHER_ID = 0;
    public final int COL_WEATHER_DATE = 1;
    public final int COL_SHORT_DESC = 2;
    public final int COL_WEATHER_MAX_TEMP = 3;
    public final int COL_WEATHER_MIN_TEMP = 4;
    public final int COL_LOCATION_SETTING = 5;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

	public ForecastFragment() {
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.forecastfragment,menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.action_refresh:
			updateWeather();
			return true;
		case R.id.action_map:
			 viewOnMap();
			return true;
		}
		
		
		return super.onOptionsItemSelected(item);
		
	}

	@Override
	public void onStart() {
		super.onStart();
		updateWeather();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container,
				false);

		//Default Values for the list view
		String[] forecastArray = {"Fetching Weather"
								,"Fetching Weather"
								,"Fetching Weather"
								,"Fetching Weather"
								,"Fetching Weather"
								,"Fetching Weather"
								,"Fetching Weather"};


        mForecastAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.list_item_forecast,
                null,
                // the column names to use to fill the textviews
                new String[]{WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                        WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
                },
                // the textviews to fill with the data pulled from the columns above
                new int[]{R.id.list_item_date_textview,
                        R.id.list_item_forecast_textview,
                        R.id.list_item_high_textview,
                        R.id.list_item_low_textview
                },
                0
        );
		ListView forecastView = (ListView) rootView.findViewById(R.id.listview_forecast);
		forecastView.setAdapter(mForecastAdapter);
		forecastView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				//String s = (String) parent.getItemAtPosition(position);
				//Toast.makeText(getActivity(), (CharSequence)s, Toast.LENGTH_SHORT).show();
				Intent weatherDataToDetail = new Intent(getActivity(),DetailActivity.class);
				weatherDataToDetail.putExtra(Intent.EXTRA_TEXT, "placeholder");
				startActivity(weatherDataToDetail);
			}
		});
		return rootView;
	}
	
	private void updateWeather(){
		//Fetch preferences
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		
		//Fetch Weather Data from online weather API
		FetchWeatherTask fetchweather = new FetchWeatherTask(getActivity());
		fetchweather.execute(sharedPref.getString(getActivity().getString(R.string.pref_location_key),
				getActivity().getString(R.string.pref_location_default)));
	}
	
	private void viewOnMap(){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		/*FetchLocationTask fetchlocation = new FetchLocationTask();
		fetchlocation.execute(sharedPref.getString(getString(R.string.pref_location_key),
			getString(R.string.pref_location_default)));*/
	}

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        String startDate = WeatherContract.getDbDateString(new Date());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                mLocation, startDate);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v("CURSOR BUG", Boolean.toString(data.moveToFirst()));
        mForecastAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //mForecastAdapter.swapCursor(null);
    }
}