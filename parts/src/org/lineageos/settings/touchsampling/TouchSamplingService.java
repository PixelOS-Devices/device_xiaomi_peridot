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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import org.lineageos.settings.R;
import org.lineageos.settings.touchsampling.TouchSamplingUtils;
import org.lineageos.settings.utils.FileUtils;

public class TouchSamplingService extends Service {
    private static final String TAG = "TouchSamplingService";

    private BroadcastReceiver mScreenUnlockReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TouchSamplingService started");

        // Register a broadcast receiver for screen unlock and screen on events
        mScreenUnlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_USER_PRESENT.equals(intent.getAction()) ||
                    Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    Log.d(TAG, "Screen turned on or device unlocked. Reapplying touch sampling rate.");
                    applyTouchSamplingRate();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT); // Triggered when the user unlocks the device
        filter.addAction(Intent.ACTION_SCREEN_ON);    // Triggered when the screen turns on
        registerReceiver(mScreenUnlockReceiver, filter);

        // Apply the touch sampling rate initially
        applyTouchSamplingRate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "TouchSamplingService stopped");

        
        // PugzAreCute: Fix to allow disabling HSTR.
        applyTouchSamplingRate(0);
	
        // Unregister the broadcast receiver
        if (mScreenUnlockReceiver != null) {
            unregisterReceiver(mScreenUnlockReceiver);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void applyTouchSamplingRate() {
        SharedPreferences sharedPref = getSharedPreferences(
                TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        boolean htsrEnabled = sharedPref.getBoolean(TouchSamplingSettingsFragment.HTSR_STATE, false);
        int state = htsrEnabled ? 1 : 0;

        String currentState = FileUtils.readOneLine(TouchSamplingUtils.HTSR_FILE);
        if (currentState == null || !currentState.equals(Integer.toString(state))) {
            Log.d(TAG, "Applying touch sampling rate: " + state);
            FileUtils.writeLine(TouchSamplingUtils.HTSR_FILE, Integer.toString(state));
        }
    }

    // PugzAreCute: Fix to allow disabling HSTR.
    private void applyTouchSamplingRate(int state) {
        String currentState = FileUtils.readOneLine(TouchSamplingUtils.HTSR_FILE);
        if (currentState == null || !currentState.equals(Integer.toString(state))) {
            Log.d(TAG, "Applying temporary touch sampling rate: " + state);
            FileUtils.writeLine(TouchSamplingUtils.HTSR_FILE, Integer.toString(state));
	    }
    }
}
