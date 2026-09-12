package com.android.settings.gestures;

import android.content.Context;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller for Navigation Bar Buttons Style in Button Navigation Settings
 */
public class NavBarButtonsStylePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String SETTING_KEY = "nav_bar_buttons_style";
    public static final int STYLE_DEFAULT = 0;
    public static final int STYLE_HYPEROS = 1;

    private ListPreference mPreference;

    public NavBarButtonsStylePreferenceController(Context context, String preferenceKey) {
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
            int currentVal = Settings.Secure.getInt(mContext.getContentResolver(), SETTING_KEY, STYLE_DEFAULT);
            mPreference.setValue(String.valueOf(currentVal));
            updateSummary(mPreference, String.valueOf(currentVal));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String stringValue = (String) newValue;
        int value = Integer.parseInt(stringValue);
        Settings.Secure.putInt(mContext.getContentResolver(), SETTING_KEY, value);
        Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, value);
        updateSummary(preference, stringValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mPreference != null) {
            int currentVal = Settings.Secure.getInt(mContext.getContentResolver(), SETTING_KEY, STYLE_DEFAULT);
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
