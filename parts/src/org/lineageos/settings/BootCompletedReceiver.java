/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

package org.lineageos.settings;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Display.HdrCapabilities;

import vendor.xiaomi.hw.touchfeature.ITouchFeature;

import org.lineageos.settings.touchsampling.TouchSamplingUtils;
import org.lineageos.settings.touchsampling.TouchSamplingService;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = true;
    private static final String TAG = "XiaomiParts";

    private static final int DOUBLE_TAP_TO_WAKE_MODE = 14;
    private ITouchFeature xiaomiTouchFeatureAidl;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (DEBUG) Log.i(TAG, "Received intent: " + intent.getAction());
        switch (intent.getAction()) {
            case Intent.ACTION_LOCKED_BOOT_COMPLETED:
                handleLockedBootCompleted(context);
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                handleBootCompleted(context);
                break;
        }
    }

    private void handleLockedBootCompleted(Context context) {
        if (DEBUG) Log.i(TAG, "Handling locked boot completed.");
        try {
            // Start necessary services
            startServices(context);

            // Override HDR types
            overrideHdrTypes(context);

            // Restore touch sampling rate
            TouchSamplingUtils.restoreSamplingValue(context);

            // Register observer for Double Tap to Wake
            registerDoubleTapToWakeObserver(context);
        } catch (Exception e) {
            Log.e(TAG, "Error during locked boot completed processing", e);
        }
    }

    private void handleBootCompleted(Context context) {
        if (DEBUG) Log.i(TAG, "Handling boot completed.");
        // Add additional boot-completed actions if needed
    }

    private void startServices(Context context) {
        if (DEBUG) Log.i(TAG, "Starting services...");

        // Start Touch Sampling Service
        context.startServiceAsUser(new Intent(context, TouchSamplingService.class), UserHandle.CURRENT);
    }

    private void overrideHdrTypes(Context context) {
        try {
            final DisplayManager dm = context.getSystemService(DisplayManager.class);
            if (dm != null) {
                dm.overrideHdrTypes(Display.DEFAULT_DISPLAY, new int[]{
                        HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                        HdrCapabilities.HDR_TYPE_HDR10,
                        HdrCapabilities.HDR_TYPE_HLG,
                        HdrCapabilities.HDR_TYPE_HDR10_PLUS
                });
                if (DEBUG) Log.i(TAG, "HDR types overridden successfully.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error overriding HDR types", e);
        }
    }

    private void registerDoubleTapToWakeObserver(Context context) {
        if (DEBUG) Log.i(TAG, "Registering Double Tap to Wake observer.");
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                updateTapToWakeStatus(context);
            }
        };
        context.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DOUBLE_TAP_TO_WAKE),
                true,
                observer
        );
        updateTapToWakeStatus(context);
    }

    private void updateTapToWakeStatus(Context context) {
        if (DEBUG) Log.i(TAG, "Updating Double Tap to Wake status.");
        try {
            if (xiaomiTouchFeatureAidl == null) {
                try {
                    String name = "default";
                    String fqName = vendor.xiaomi.hw.touchfeature.ITouchFeature.DESCRIPTOR + "/" + name;
                    IBinder binder = android.os.Binder.allowBlocking(
                            android.os.ServiceManager.waitForDeclaredService(fqName));
                    xiaomiTouchFeatureAidl = vendor.xiaomi.hw.touchfeature.ITouchFeature.Stub.asInterface(binder);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to initialize Touch Feature service", e);
                    return;
                }
            }
            boolean enabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.DOUBLE_TAP_TO_WAKE,
                    0
            ) == 1;
            xiaomiTouchFeatureAidl.setTouchMode(0, DOUBLE_TAP_TO_WAKE_MODE, enabled ? 1 : 0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update Tap to Wake status", e);
        }
    }
}

