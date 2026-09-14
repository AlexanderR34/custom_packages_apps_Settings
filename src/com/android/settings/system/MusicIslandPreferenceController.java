package com.android.settings.system;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.Preference;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controller for Status Bar Music Island toggle in Settings
 */
public class MusicIslandPreferenceController extends TogglePreferenceController {

    public static final String SETTING_KEY = "status_bar_music_island";

    public MusicIslandPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(), SETTING_KEY, 0, UserHandle.myUserId()) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(), SETTING_KEY, isChecked ? 1 : 0, UserHandle.myUserId());
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
