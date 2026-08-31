package com.android.settings.display;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import android.util.Log;

import androidx.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.TopIntroPreference;

import java.util.ArrayList;
import java.util.List;

public class CustomScreenResolutionFragment extends RadioButtonPickerFragment {

    private static final String TAG = "CustomScreenResolutionFragment";

    private static final String KEY_1440P = "res_1440p";
    private static final String KEY_1220P = "res_1220p"; 
    private static final String KEY_1080P = "res_1080p";
    private static final String KEY_720P  = "res_720p";

    private static final int BASE_WIDTH = 1220;
    private static final int BASE_DENSITY = 446; 

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_resolution_settings;
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        TopIntroPreference introPreference = new TopIntroPreference(screen.getContext());
        introPreference.setTitle(R.string.custom_screen_resolution_summary);
        screen.addPreference(introPreference);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        List<CandidateInfo> candidates = new ArrayList<>();
        Context context = getContext();
        
        candidates.add(new ResolutionCandidateInfo(KEY_1440P, context.getString(R.string.custom_screen_resolution_1440p)));
        candidates.add(new ResolutionCandidateInfo(KEY_1220P, context.getString(R.string.custom_screen_resolution_1220p)));
        candidates.add(new ResolutionCandidateInfo(KEY_1080P, context.getString(R.string.custom_screen_resolution_1080p)));
        candidates.add(new ResolutionCandidateInfo(KEY_720P, context.getString(R.string.custom_screen_resolution_720p)));
        
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        String currentKey = Settings.System.getString(getContext().getContentResolver(), "custom_screen_resolution_key");
        if (currentKey == null) {
            return KEY_1220P;
        }
        return currentKey;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        int width = 1220;
        int height = 2712;

        switch (key) {
            case KEY_1440P: width = 1440; height = 3200; break;
            case KEY_1220P: width = 1220; height = 2712; break;
            case KEY_1080P: width = 1080; height = 2400; break;
            case KEY_720P:  width = 720;  height = 1600; break;
        }

        applyResolutionAndUniformDpi(width, height);

        Settings.System.putString(getContext().getContentResolver(), "custom_screen_resolution_key", key);
        return true;
    }

    private void applyResolutionAndUniformDpi(int newWidth, int newHeight) {
        try {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            
            if (newWidth == BASE_WIDTH) {
                wm.clearForcedDisplaySize(Display.DEFAULT_DISPLAY);
                wm.clearForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY, UserHandle.USER_CURRENT);
            } else {
                int newDensity = (newWidth * BASE_DENSITY) / BASE_WIDTH;
                
                wm.setForcedDisplaySize(Display.DEFAULT_DISPLAY, newWidth, newHeight);
                wm.setForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY, newDensity, UserHandle.USER_CURRENT);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set custom resolution", e);
        }
    }

    @Override
    public int getMetricsCategory() {
        return 0; 
    }

    class ResolutionCandidateInfo extends CandidateInfo {
        private final String mKey;
        private final String mLabel;

        ResolutionCandidateInfo(String key, String label) {
            super(true);
            mKey = key;
            mLabel = label;
        }

        @Override public CharSequence loadLabel() { return mLabel; }
        @Override public Drawable loadIcon() { return null; }
        @Override public String getKey() { return mKey; }
    }
}