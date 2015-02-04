package com.gscasu.sunshineapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DetailActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_detail);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new DetailFragment()).commit();
		}
	}

	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.detail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this,SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}*/

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class DetailFragment extends Fragment {
		private final String LOG_TAG = DetailFragment.class.getSimpleName();
		private ShareActionProvider mShareActionProvider;
		private String mWeatherInfo;

		public DetailFragment() {
			setHasOptionsMenu(true);
		}

		/* (non-Javadoc)
		 * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
		 */
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			super.onCreateOptionsMenu(menu, inflater);
			inflater.inflate(R.menu.detail, menu);
			MenuItem item = menu.findItem(R.id.action_share);
			mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
			
			if(mShareActionProvider != null){
				mShareActionProvider.setShareIntent(createShareWeatherIntent());
			}
			else{
				Log.d(LOG_TAG, "Actionprovider equals null");
			}
		}

		/* (non-Javadoc)
		 * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
		 */
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			
			return super.onOptionsItemSelected(item);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_detail,
					container, false);
			//Receive the intent
			Intent intent = getActivity().getIntent();
			
			TextView detailTV = (TextView) rootView.findViewById(R.id.detail_text);
			
			//Publish if com.gscasu.sunshineapp.data received
			if(intent!=null && intent.hasExtra(Intent.EXTRA_TEXT)){
				mWeatherInfo = intent.getExtras().getString(Intent.EXTRA_TEXT);
				detailTV.setText(mWeatherInfo);
			}
			
			
			return rootView;
		}
		
		private Intent createShareWeatherIntent(){
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			shareIntent.putExtra(Intent.EXTRA_TEXT, mWeatherInfo + "#SUNSHINE");
			shareIntent.setType("text/plain");
			return shareIntent;
		}
	}
}
