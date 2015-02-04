package com.gscasu.sunshineapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Sa'eed Abdullah on 003, 3, 2, 2015.
 */
public class Utility {
    public static String getPreferredLocation(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }
}
