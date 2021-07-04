/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.touchsampling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import org.lineageos.settings.R;
import org.lineageos.settings.touchsampling.TouchSamplingUtils;
import org.lineageos.settings.utils.FileUtils;

public class TouchSamplingSettingsFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String HTSR_ENABLE_KEY = "htsr_enable";
    public static final String SHAREDHTSR = "SHAREDHTSR";
    public static final String HTSR_STATE = "htsr_state";

    private SwitchPreference mHTSRPreference;
    private SharedPreferences mPrefs;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.htsr_settings);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        mHTSRPreference = (SwitchPreference) findPreference(HTSR_ENABLE_KEY);
        mPrefs = getActivity().getSharedPreferences(SHAREDHTSR, Context.MODE_PRIVATE);

        // Set the initial state of the switch
        boolean htsrEnabled = mPrefs.getBoolean(HTSR_STATE, false);
        mHTSRPreference.setChecked(htsrEnabled);

        // Enable the switch and set its listener
        mHTSRPreference.setOnPreferenceChangeListener(this);

        // Start the service if the toggle is enabled
        if (htsrEnabled) {
            startTouchSamplingService(true);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (HTSR_ENABLE_KEY.equals(preference.getKey())) {
            boolean isEnabled = (Boolean) newValue;

            // Save the state in shared preferences
            mPrefs.edit().putBoolean(HTSR_STATE, isEnabled).apply();

            // Start or stop the service based on the toggle state
            startTouchSamplingService(isEnabled);
        }
        return true;
    }

    private void startTouchSamplingService(boolean enable) {
        Intent serviceIntent = new Intent(getActivity(), TouchSamplingService.class);
        if (enable) {
            getActivity().startService(serviceIntent);
        } else {
            getActivity().stopService(serviceIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }
}

