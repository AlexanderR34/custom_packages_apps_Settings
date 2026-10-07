package com.android.settings.system;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller for Status Bar Chips Limit (0=Disabled, 1, 2, 3) in Settings
 */
public class ChipsLimitPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String SETTING_KEY = "status_bar_chips_limit";
    public static final int DEFAULT_CHIPS_LIMIT = 2;

    private ListPreference mPreference;

    public ChipsLimitPreferenceController(Context context, String preferenceKey) {
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
            int currentVal = Settings.System.getIntForUser(
                    mContext.getContentResolver(), SETTING_KEY, DEFAULT_CHIPS_LIMIT, UserHandle.myUserId());
            mPreference.setValue(String.valueOf(currentVal));
            updateSummary(mPreference, String.valueOf(currentVal));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String stringValue = (String) newValue;
        int value = Integer.parseInt(stringValue);
        Settings.System.putIntForUser(
                mContext.getContentResolver(), SETTING_KEY, value, UserHandle.myUserId());
        updateSummary(preference, stringValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mPreference != null) {
            int currentVal = Settings.System.getIntForUser(
                    mContext.getContentResolver(), SETTING_KEY, DEFAULT_CHIPS_LIMIT, UserHandle.myUserId());
            mPreference.setValue(String.valueOf(currentVal));
            updateSummary(mPreference, String.valueOf(currentVal));
        }
    }

    private void updateSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int index = listPref.findIndexOfValue(value);
            if (index >= 0) {
                listPref.setSummary(listPref.getEntries()[index]);
            }
        }
    }
}
