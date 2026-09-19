package com.android.settings.system;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.settings.core.BasePreferenceController;

public class QsBrightnessSliderPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY_SETTING = Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER;
    public static final int BRIGHTNESS_SLIDER_NEVER = 0;
    public static final int BRIGHTNESS_SLIDER_EXPANDED_ONLY = 1;
    public static final int BRIGHTNESS_SLIDER_ALWAYS = 2;

    private ListPreference mPreference;

    public QsBrightnessSliderPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            int currentVal = Settings.Secure.getIntForUser(
                    mContext.getContentResolver(),
                    KEY_SETTING,
                    BRIGHTNESS_SLIDER_EXPANDED_ONLY,
                    UserHandle.myUserId());
            mPreference.setValue(String.valueOf(currentVal));
            updateSummary(mPreference, String.valueOf(currentVal));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String stringValue = (String) newValue;
        int mode = Integer.parseInt(stringValue);
        Settings.Secure.putIntForUser(
                mContext.getContentResolver(),
                KEY_SETTING,
                mode,
                UserHandle.myUserId());
        updateSummary(preference, stringValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mPreference != null) {
            int currentVal = Settings.Secure.getIntForUser(
                    mContext.getContentResolver(),
                    KEY_SETTING,
                    BRIGHTNESS_SLIDER_EXPANDED_ONLY,
                    UserHandle.myUserId());
            mPreference.setValue(String.valueOf(currentVal));
            updateSummary(mPreference, String.valueOf(currentVal));
        }
    }

    private void updateSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int index = listPref.findIndexOfValue(value);
            if (index >= 0 && index < listPref.getEntries().length) {
                listPref.setSummary(listPref.getEntries()[index]);
            }
        }
    }
}
