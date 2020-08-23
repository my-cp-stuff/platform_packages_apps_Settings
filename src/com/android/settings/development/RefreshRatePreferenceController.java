/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.development;

import android.content.Context;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.lang.CharSequence;

public class RefreshRatePreferenceController extends AbstractPreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_REFRESH_RATE = "refresh_rate_setting";
    private int MAX_REFRESH_RATE;

    private ListPreference mRefreshRate;

    public RefreshRatePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(com.android.internal.R.bool.config_hasVariableRefreshRate);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_REFRESH_RATE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (!isAvailable()) {
            setVisible(screen, KEY_REFRESH_RATE, false);
            return;
        }
        mRefreshRate = (ListPreference) screen.findPreference(KEY_REFRESH_RATE);
        MAX_REFRESH_RATE = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_defaultPeakRefreshRate);
        CharSequence[] rrEntries = mRefreshRate.getEntries();
        rrEntries[2] = String.valueOf(MAX_REFRESH_RATE) + "Hz";
        mRefreshRate.setEntries(rrEntries);
        int refreshRate = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.REFRESH_RATE_SETTING, 0);
        mRefreshRate.setValue(String.valueOf(refreshRate));
        mRefreshRate.setOnPreferenceChangeListener(this);
        updateRefreshRate(refreshRate);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int refreshRate = Integer.valueOf((String) newValue);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.REFRESH_RATE_SETTING, refreshRate);
        updateRefreshRate(refreshRate);
        return true;
    }

    public void updateRefreshRate(int refreshRate) {
        switch (refreshRate) {
            case 0:
            default:
                setFPS(0);
                break;
            case 1:
                setFPS(60);
                break;
            case 2:
                setFPS(MAX_REFRESH_RATE);
                break;
        }
        updateRefreshRateSummary(refreshRate);
    }

    public void updateRefreshRateSummary(int refreshRate) {
        if (refreshRate == 1) {
            mRefreshRate.setSummary(R.string.refresh_rate_summary_60);
        } else if (refreshRate == 2) {
            mRefreshRate.setSummary(R.string.refresh_rate_summary_120);
        } else {
            mRefreshRate.setSummary(R.string.refresh_rate_summary_auto);
        }
    }

    private final void setFPS(int v) {
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken("android.ui.ISurfaceComposer");
        data.writeInt(v);
        try {
            ServiceManager.getService("SurfaceFlinger").transact(1035, data, (Parcel) null, 0);
        } catch (RemoteException e) {
            // nothing we can do
        }
        data.recycle();
    }
}
