/*
 * Copyright (C) 2017 Pixel Experiene
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

package com.android.settings.custom;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.Utils;

import com.android.settings.custom.preference.CustomSeekBarPreference;

import com.android.internal.util.custom.NavbarUtils;
import com.android.internal.util.custom.CustomUtils;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "Buttons";

    private static final String POWER_CATEGORY = "power_category";
    private static final String TORCH_POWER_BUTTON_GESTURE = "torch_power_button_gesture";
    //Keys
    private static final String KEY_BUTTON_BRIGHTNESS = "button_brightness";
    private static final String KEY_BACKLIGHT_TIMEOUT = "backlight_timeout";
    private static final String KEY_BUTTON_BRIGHTNESS_SW = "button_brightness_sw";
    private static final String KEY_BUTTON_BACKLIGHT_ON_TOUCH = "button_backlight_on_touch_only";

    // category keys
    private static final String CATEGORY_HWKEY = "hardware_keys";

    private ContentResolver resolver;

    private ListPreference mTorchPowerButton;
    private ListPreference mBacklightTimeout;
    private CustomSeekBarPreference mButtonBrightness;
    private SwitchPreference mButtonBrightness_sw;
    private SwitchPreference mButtonBacklightOnTouch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.buttons);

        resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        if (!CustomUtils.deviceHasFlashlight(getContext())) {
            Preference toRemove = prefScreen.findPreference(POWER_CATEGORY);
            if (toRemove != null) {
                prefScreen.removePreference(toRemove);
            }
        } else {
            mTorchPowerButton = (ListPreference) findPreference(TORCH_POWER_BUTTON_GESTURE);
            int mTorchPowerButtonValue = Settings.Secure.getInt(resolver,
                    Settings.Secure.TORCH_POWER_BUTTON_GESTURE, 0);
            mTorchPowerButton.setValue(Integer.toString(mTorchPowerButtonValue));
            mTorchPowerButton.setSummary(mTorchPowerButton.getEntry());
            mTorchPowerButton.setOnPreferenceChangeListener(this);
        }

        final boolean variableBrightness = getResources().getBoolean(
                com.android.internal.R.bool.config_deviceHasVariableButtonBrightness);
        final boolean hasButtonBacklight = getResources().getBoolean(
                com.android.internal.R.bool.config_deviceHasButtonBacklight);

        final PreferenceCategory hwkeyCat = (PreferenceCategory) prefScreen.findPreference(CATEGORY_HWKEY);
        final boolean needsNavbar = NavbarUtils.hasNavbarByDefault(getActivity());
        mBacklightTimeout = (ListPreference) findPreference(KEY_BACKLIGHT_TIMEOUT);
        mButtonBrightness = (CustomSeekBarPreference) findPreference(KEY_BUTTON_BRIGHTNESS);
        mButtonBrightness_sw = (SwitchPreference) findPreference(KEY_BUTTON_BRIGHTNESS_SW);
        mButtonBacklightOnTouch = (SwitchPreference) findPreference(KEY_BUTTON_BACKLIGHT_ON_TOUCH);

        if (needsNavbar){
            prefScreen.removePreference(hwkeyCat);
        }else{
            if (hasButtonBacklight) {
                if (mBacklightTimeout != null){
                    mBacklightTimeout.setOnPreferenceChangeListener(this);
                    int BacklightTimeout = Settings.System.getInt(getContentResolver(),
                            Settings.System.BUTTON_BACKLIGHT_TIMEOUT, 5000);
                    mBacklightTimeout.setValue(Integer.toString(BacklightTimeout));
                    mBacklightTimeout.setSummary(mBacklightTimeout.getEntry());
                }

                if (variableBrightness){
                    prefScreen.removePreference(mButtonBrightness_sw);
                    if (mButtonBrightness != null) {
                        int ButtonBrightness = Settings.System.getInt(getContentResolver(),
                                Settings.System.BUTTON_BRIGHTNESS, 255);
                        mButtonBrightness.setValue(ButtonBrightness / 1);
                        mButtonBrightness.setOnPreferenceChangeListener(this);
                    }
                }else{
                    prefScreen.removePreference(mButtonBrightness);
                    if (mButtonBrightness_sw != null) {
                        mButtonBrightness_sw.setChecked((Settings.System.getInt(getContentResolver(),
                                Settings.System.BUTTON_BRIGHTNESS, 1) == 1));
                        mButtonBrightness_sw.setOnPreferenceChangeListener(this);
                    }
                }
            }else{
                hwkeyCat.removePreference(mBacklightTimeout);
                hwkeyCat.removePreference(mButtonBrightness);
                hwkeyCat.removePreference(mButtonBrightness_sw);
                hwkeyCat.removePreference(mButtonBacklightOnTouch);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CUSTOM_SETTINGS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTorchPowerButton) {
            int mTorchPowerButtonValue = Integer.valueOf((String) newValue);
            int index = mTorchPowerButton.findIndexOfValue((String) newValue);
            mTorchPowerButton.setSummary(
                    mTorchPowerButton.getEntries()[index]);
            Settings.Secure.putInt(getActivity().getContentResolver(), Settings.Secure.TORCH_POWER_BUTTON_GESTURE,
                    mTorchPowerButtonValue);
            if (mTorchPowerButtonValue == 1) {
                //if doubletap for torch is enabled, switch off double tap for camera
                Settings.Secure.putInt(getActivity().getContentResolver(), Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
                        1);
            }
            return true;
        } else if (preference == mBacklightTimeout) {
            String BacklightTimeout = (String) newValue;
            int BacklightTimeoutValue = Integer.parseInt(BacklightTimeout);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.BUTTON_BACKLIGHT_TIMEOUT, BacklightTimeoutValue);
            int BacklightTimeoutIndex = mBacklightTimeout
                    .findIndexOfValue(BacklightTimeout);
            mBacklightTimeout
                    .setSummary(mBacklightTimeout.getEntries()[BacklightTimeoutIndex]);
            return true;
        } else if (preference == mButtonBrightness) {
            int value = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, value * 1);
            return true;
        } else if (preference == mButtonBrightness_sw) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, value ? 1 : 0);
            return true;
        }else if (preference == mButtonBrightness_sw) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, value ? 1 : 0);
            return true;
        }
        return false;
    }

}
