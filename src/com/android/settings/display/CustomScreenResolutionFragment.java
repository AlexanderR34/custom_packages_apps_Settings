package com.android.settings.display;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManagerGlobal;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Screen Resolution Changer Fragment - Material Expressive 3 / Monet Dynamic Theming (Android 17)
 */
public class CustomScreenResolutionFragment extends SettingsPreferenceFragment {

    private static final String TAG = "CustomScreenResolutionFragment";
    private static final String KEY_LAYOUT_PREF = "screen_resolution_custom_layout";
    private static final String SETTINGS_KEY = "custom_screen_resolution_key";

    public static final String KEY_1440P = "res_1440p";
    public static final String KEY_1220P = "res_1220p";
    public static final String KEY_1080P = "res_1080p";
    public static final String KEY_720P  = "res_720p";

    private static final int BASE_WIDTH = 1220;
    private static final int BASE_HEIGHT = 2712;
    private static final int BASE_DENSITY = 446;

    private String mSelectedKey;
    private String mAppliedKey;

    private LinearLayout mCard720p;
    private LinearLayout mCard1080p;
    private LinearLayout mCard1220p;
    private LinearLayout mCard1440p;

    private ImageView mRadio720p;
    private ImageView mRadio1080p;
    private ImageView mRadio1220p;
    private ImageView mRadio1440p;

    private FrameLayout mFrameHd;
    private FrameLayout mFrameFhd;
    private FrameLayout mFrameStock;
    private FrameLayout mFrameQhd;

    private TextView mTextDescription;
    private Button mButtonApply;

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_resolution_settings;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncCurrentSetting();
    }

    private void syncCurrentSetting() {
        Context context = getContext();
        if (context == null) return;

        mAppliedKey = Settings.System.getString(context.getContentResolver(), SETTINGS_KEY);
        if (mAppliedKey == null || mAppliedKey.isEmpty()) {
            mAppliedKey = KEY_1220P;
        }
        mSelectedKey = mAppliedKey;
        updateSelectionUi(mSelectedKey);
    }

    private void initViews() {
        LayoutPreference layoutPref = findPreference(KEY_LAYOUT_PREF);
        if (layoutPref == null) {
            Log.w(TAG, "LayoutPreference " + KEY_LAYOUT_PREF + " not found");
            return;
        }

        mCard720p = layoutPref.findViewById(R.id.card_res_720p);
        mCard1080p = layoutPref.findViewById(R.id.card_res_1080p);
        mCard1220p = layoutPref.findViewById(R.id.card_res_1220p);
        mCard1440p = layoutPref.findViewById(R.id.card_res_1440p);

        mRadio720p = layoutPref.findViewById(R.id.radio_res_720p);
        mRadio1080p = layoutPref.findViewById(R.id.radio_res_1080p);
        mRadio1220p = layoutPref.findViewById(R.id.radio_res_1220p);
        mRadio1440p = layoutPref.findViewById(R.id.radio_res_1440p);

        mFrameHd = layoutPref.findViewById(R.id.frame_preview_hd);
        mFrameFhd = layoutPref.findViewById(R.id.frame_preview_fhd);
        mFrameStock = layoutPref.findViewById(R.id.frame_preview_stock);
        mFrameQhd = layoutPref.findViewById(R.id.frame_preview_qhd);

        mTextDescription = layoutPref.findViewById(R.id.text_resolution_description);
        mButtonApply = layoutPref.findViewById(R.id.button_apply_resolution);

        setupListeners();
        syncCurrentSetting();
    }

    private void setupListeners() {
        if (mCard720p != null) {
            mCard720p.setOnClickListener(v -> selectResolution(KEY_720P));
        }
        if (mCard1080p != null) {
            mCard1080p.setOnClickListener(v -> selectResolution(KEY_1080P));
        }
        if (mCard1220p != null) {
            mCard1220p.setOnClickListener(v -> selectResolution(KEY_1220P));
        }
        if (mCard1440p != null) {
            mCard1440p.setOnClickListener(v -> selectResolution(KEY_1440P));
        }

        if (mButtonApply != null) {
            mButtonApply.setOnClickListener(v -> applyResolution());
        }
    }

    private void selectResolution(String key) {
        mSelectedKey = key;
        updateSelectionUi(key);
    }

    private void updateSelectionUi(String activeKey) {
        if (activeKey == null) {
            activeKey = KEY_1220P;
        }

        // Reset all radio icons (only the radio point indicates the selection)
        if (mRadio720p != null) mRadio720p.setImageResource(R.drawable.m3_radio_unchecked);
        if (mRadio1080p != null) mRadio1080p.setImageResource(R.drawable.m3_radio_unchecked);
        if (mRadio1220p != null) mRadio1220p.setImageResource(R.drawable.m3_radio_unchecked);
        if (mRadio1440p != null) mRadio1440p.setImageResource(R.drawable.m3_radio_unchecked);

        // Reset zoom circle frames
        if (mFrameHd != null) mFrameHd.setBackgroundResource(R.drawable.m3_zoom_circle_bg);
        if (mFrameFhd != null) mFrameFhd.setBackgroundResource(R.drawable.m3_zoom_circle_bg);
        if (mFrameStock != null) mFrameStock.setBackgroundResource(R.drawable.m3_zoom_circle_bg);
        if (mFrameQhd != null) mFrameQhd.setBackgroundResource(R.drawable.m3_zoom_circle_bg);

        // Activate only the selected radio point and zoom frame
        switch (activeKey) {
            case KEY_720P:
                if (mRadio720p != null) mRadio720p.setImageResource(R.drawable.m3_radio_checked);
                if (mFrameHd != null) mFrameHd.setBackgroundResource(R.drawable.m3_zoom_circle_active_bg);
                if (mTextDescription != null) mTextDescription.setText(R.string.custom_screen_resolution_desc_720p);
                break;
            case KEY_1080P:
                if (mRadio1080p != null) mRadio1080p.setImageResource(R.drawable.m3_radio_checked);
                if (mFrameFhd != null) mFrameFhd.setBackgroundResource(R.drawable.m3_zoom_circle_active_bg);
                if (mTextDescription != null) mTextDescription.setText(R.string.custom_screen_resolution_desc_1080p);
                break;
            case KEY_1440P:
                if (mRadio1440p != null) mRadio1440p.setImageResource(R.drawable.m3_radio_checked);
                if (mFrameQhd != null) mFrameQhd.setBackgroundResource(R.drawable.m3_zoom_circle_active_bg);
                if (mTextDescription != null) mTextDescription.setText(R.string.custom_screen_resolution_desc_1440p);
                break;
            case KEY_1220P:
            default:
                if (mRadio1220p != null) mRadio1220p.setImageResource(R.drawable.m3_radio_checked);
                if (mFrameStock != null) mFrameStock.setBackgroundResource(R.drawable.m3_zoom_circle_active_bg);
                if (mTextDescription != null) mTextDescription.setText(R.string.custom_screen_resolution_desc_stock);
                break;
        }

        // Check if there are changes relative to the applied system state
        boolean hasPendingChanges = !activeKey.equals(mAppliedKey);
        if (mButtonApply != null) {
            mButtonApply.setEnabled(hasPendingChanges);
            mButtonApply.setAlpha(hasPendingChanges ? 1.0f : 0.45f);
        }
    }

    private void applyResolution() {
        Context context = getContext();
        if (context == null || mSelectedKey == null) return;

        int width = BASE_WIDTH;
        int height = BASE_HEIGHT;

        switch (mSelectedKey) {
            case KEY_1440P: width = 1440; height = 3200; break;
            case KEY_1220P: width = BASE_WIDTH; height = BASE_HEIGHT; break;
            case KEY_1080P: width = 1080; height = 2400; break;
            case KEY_720P:  width = 720;  height = 1600; break;
        }

        applyResolutionAndUniformDpi(context, width, height);

        Settings.System.putString(context.getContentResolver(), SETTINGS_KEY, mSelectedKey);
        mAppliedKey = mSelectedKey;

        if (mButtonApply != null) {
            mButtonApply.setEnabled(false);
            mButtonApply.setAlpha(0.45f);
        }

        Toast.makeText(context, R.string.custom_screen_resolution_applied_toast, Toast.LENGTH_SHORT).show();
    }

    private void applyResolutionAndUniformDpi(Context context, int newWidth, int newHeight) {
        int userId = UserHandle.USER_CURRENT;
        try {
            userId = ActivityManager.getCurrentUser();
        } catch (Exception ignored) {}

        try {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            if (wm != null) {
                if (newWidth == BASE_WIDTH) {
                    wm.clearForcedDisplaySize(Display.DEFAULT_DISPLAY);
                    wm.clearForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY, userId);
                } else {
                    int newDensity = (newWidth * BASE_DENSITY) / BASE_WIDTH;
                    wm.setForcedDisplaySize(Display.DEFAULT_DISPLAY, newWidth, newHeight);
                    wm.setForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY, newDensity, userId);
                }
            }
        } catch (RemoteException | SecurityException e) {
            Log.e(TAG, "Failed to apply resolution via WindowManagerGlobal", e);
        }

        // Optional Android 14/15/16/17 hardware display mode fallback
        try {
            DisplayManager dm = context.getSystemService(DisplayManager.class);
            if (dm != null) {
                Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);
                if (display != null) {
                    Display.Mode currentMode = display.getMode();
                    for (Display.Mode mode : display.getSupportedModes()) {
                        if (mode.getPhysicalWidth() == newWidth && mode.getPhysicalHeight() == newHeight) {
                            if (currentMode != null && mode.getRefreshRate() == currentMode.getRefreshRate()) {
                                display.setUserPreferredDisplayMode(mode);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "DisplayManager setUserPreferredDisplayMode not available or failed", e);
        }
    }
}
