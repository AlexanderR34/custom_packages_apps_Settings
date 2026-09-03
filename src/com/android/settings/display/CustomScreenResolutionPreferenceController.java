package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import androidx.preference.Preference;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.R;

public class CustomScreenResolutionPreferenceController extends BasePreferenceController {

    public CustomScreenResolutionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        String currentKey = Settings.System.getString(mContext.getContentResolver(), "custom_screen_resolution_key");
        if (currentKey == null || currentKey.isEmpty()) {
            currentKey = "res_1220p";
        }

        switch (currentKey) {
            case "res_1440p": return mContext.getString(R.string.custom_screen_resolution_1440p);
            case "res_1080p": return mContext.getString(R.string.custom_screen_resolution_1080p);
            case "res_720p":  return mContext.getString(R.string.custom_screen_resolution_720p);
            default:          return mContext.getString(R.string.custom_screen_resolution_1220p);
        }
    }
}
