/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.wifi;

import static com.android.settingslib.wifi.WifiUtils.getHotspotIconResource;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.R;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.wifi.WifiUtils;
import com.android.wifitrackerlib.HotspotNetworkEntry;
import com.android.wifitrackerlib.WifiEntry;

/**
 * Preference to display a WifiEntry in a wifi picker.
 */
public class WifiEntryPreference extends RestrictedPreference implements
        WifiEntry.WifiEntryCallback,
        View.OnClickListener {

    // These values must be kept within [WifiEntry.WIFI_LEVEL_MIN, WifiEntry.WIFI_LEVEL_MAX]
    private static final int[] WIFI_CONNECTION_STRENGTH = {
            R.string.accessibility_no_wifi,
            R.string.accessibility_wifi_one_bar,
            R.string.accessibility_wifi_two_bars,
            R.string.accessibility_wifi_three_bars,
            R.string.accessibility_wifi_signal_full
    };

    private final WifiUtils.InternetIconInjector mIconInjector;
    private WifiEntry mWifiEntry;
    private int mLevel = -1;
    private boolean mShowX; // Shows the Wi-Fi signl icon of Pie+x when it's true.
    private CharSequence mContentDescription;
    private OnButtonClickListener mOnButtonClickListener;

    public WifiEntryPreference(@NonNull Context context, @NonNull WifiEntry wifiEntry) {
        this(context, wifiEntry, new WifiUtils.InternetIconInjector(context));
    }

    @VisibleForTesting
    WifiEntryPreference(@NonNull Context context, @NonNull WifiEntry wifiEntry,
            @NonNull WifiUtils.InternetIconInjector iconInjector) {
        super(context);
        int layoutResId = SettingsThemeHelper.isExpressiveTheme(getContext())
                ? R.layout.preference_access_point_expressive : R.layout.preference_access_point;
        setLayoutResource(layoutResId);
        mIconInjector = iconInjector;
        setWifiEntry(wifiEntry);
    }

    /**
     * Set updated {@link WifiEntry} to refresh the preference
     *
     * @param wifiEntry An instance of {@link WifiEntry}
     */
    public void setWifiEntry(@NonNull WifiEntry wifiEntry) {
        mWifiEntry = wifiEntry;
        mWifiEntry.setListener(this);
        refresh();
    }

    public WifiEntry getWifiEntry() {
        return mWifiEntry;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (mWifiEntry.isVerboseSummaryEnabled()) {
            TextView summary = (TextView) view.findViewById(android.R.id.summary);
            if (summary != null) {
                summary.setMaxLines(100);
            }
        }
        final Drawable drawable = getIcon();
        if (drawable != null) {
            drawable.setLevel(mLevel);
        }

        view.itemView.setContentDescription(mContentDescription);

        // Turn off divider
        view.findViewById(com.android.settingslib.widget.preference.twotarget.R.id.two_target_divider)
                .setVisibility(View.INVISIBLE);

        final LinearLayout endIcons = (LinearLayout) view.findViewById(
                com.android.settings.R.id.wifi_end_icons);

        // Enable the icon button when the help string in this WifiEntry is not null.
        final ImageButton imageButton = (ImageButton) view.findViewById(R.id.icon_button);
        if (mWifiEntry.getHelpUriString() != null
                && mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_DISCONNECTED) {
            final Drawable drawablehelp = getDrawable(R.drawable.ic_help);
            drawablehelp.setTintList(
                    Utils.getColorAttr(getContext(), android.R.attr.colorControlNormal));
            ((ImageView) imageButton).setImageDrawable(drawablehelp);
            imageButton.setVisibility(View.VISIBLE);
            imageButton.setOnClickListener(this);
            imageButton.setContentDescription(
                    getContext().getText(R.string.help_label));
        } else if (endIcons != null) {
            updateEndIcons(endIcons);
        }
    }

    @VisibleForTesting
    void updateEndIcons(LinearLayout endIcons) {
        endIcons.removeAllViews();
        // The shared icon should precede the lock icon to match the mocks.
        if (displaySharedIcon()) {
            addIcon(endIcons,
                    com.android.settings.R.drawable.ic_group_24dp);
        }
        if ((mWifiEntry.getSecurity() != WifiEntry.SECURITY_NONE)
                && (mWifiEntry.getSecurity() != WifiEntry.SECURITY_OWE)) {
            addIcon(endIcons,
                    com.android.settings.R.drawable.ic_friction_lock_closed);
        }
    }

    private void addIcon(LinearLayout endIcons, @DrawableRes int drawableId) {
        ImageView icon = new ImageView(getContext());
        icon.setImageDrawable(getDrawable(drawableId));
        icon.setImageTintList(Utils.getColorAttr(getContext(),
                android.R.attr.colorControlNormal));
        ((View) icon).setMinimumWidth(
                getContext().getResources().getDimensionPixelSize(
                        com.android.settings.R.dimen.wifi_end_icon_min_width)
        );

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        endIcons.addView(icon, layoutParams);
    }

    private boolean displaySharedIcon() {
        if (!com.android.settings.wifi.WifiUtils.isWifiMultiuserEnabled()) {
            return false;
        }

        UserManager userManager = getContext().getSystemService(UserManager.class);
        if (userManager.getUserCount() <= 1) {
            return false;
        }

        return (mWifiEntry.getWifiConfiguration() == null)
                ? false : mWifiEntry.getWifiConfiguration().shared;
    }

    /**
     * Updates the title and summary; may indirectly call notifyChanged().
     */
    public void refresh() {
        CharSequence rawTitle = mWifiEntry.getTitle();
        String titleStr = rawTitle != null ? rawTitle.toString() : "";
        int pos = titleStr.indexOf(" [(");
        if (pos != -1) {
            titleStr = titleStr.substring(0, pos);
        }
        pos = titleStr.indexOf(" [");
        if (pos != -1) {
            titleStr = titleStr.substring(0, pos);
        }
        setTitle(titleStr);
        if (mWifiEntry instanceof HotspotNetworkEntry) {
            updateHotspotIcon(((HotspotNetworkEntry) mWifiEntry).getDeviceType());
        } else {
            mLevel = mWifiEntry.getLevel();
            mShowX = mWifiEntry.shouldShowXLevelIcon();
            updateIcon(mShowX, mLevel);
        }

        CharSequence summary = mWifiEntry.getSummary(false /* concise */);
        if (summary != null) {
            String summaryStr = summary.toString();
            int debugPos = summaryStr.indexOf(" / f = ");
            if (debugPos != -1) {
                summaryStr = summaryStr.substring(0, debugPos);
            }
            debugPos = summaryStr.indexOf(" [;(");
            if (debugPos != -1) {
                summaryStr = summaryStr.substring(0, debugPos);
            }
            debugPos = summaryStr.indexOf(" [{");
            if (debugPos != -1) {
                summaryStr = summaryStr.substring(0, debugPos);
            }
            summary = summaryStr;
        }
        setSummary(summary);
        mContentDescription = buildContentDescription();
    }

    /**
     * Indicates the state of the WifiEntry has changed and clients may retrieve updates through
     * the WifiEntry getter methods.
     */
    public void onUpdated() {
        // TODO(b/70983952): Fill this method in
        refresh();
    }

    /**
     * Result of the connect request indicated by the WifiEntry.CONNECT_STATUS constants.
     */
    public void onConnectResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    /**
     * Result of the disconnect request indicated by the WifiEntry.DISCONNECT_STATUS constants.
     */
    public void onDisconnectResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    /**
     * Result of the forget request indicated by the WifiEntry.FORGET_STATUS constants.
     */
    public void onForgetResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    /**
     * Result of the sign-in request indecated by the WifiEntry.SIGNIN_STATUS constants
     */
    public void onSignInResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    protected int getIconColorAttr() {
        final boolean accent = (mWifiEntry.hasInternetAccess()
                && mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED);
        return accent ? android.R.attr.colorAccent : android.R.attr.colorControlNormal;
    }

    private void setIconWithTint(Drawable drawable) {
        if (drawable != null) {
            drawable.setTintList(Utils.getColorAttr(getContext(), getIconColorAttr()));
            String standard = mWifiEntry != null ? mWifiEntry.getStandardString() : null;
            if (!TextUtils.isEmpty(standard) && (standard.contains("4") || standard.contains("5") || standard.contains("6") || standard.contains("7"))) {
                drawable = new WifiStandardDrawable(getContext(), drawable, standard);
            }
            setIcon(drawable);
        } else {
            setIcon(null);
        }
    }

    @VisibleForTesting
    void updateIcon(boolean showX, int level) {
        if (level == -1) {
            setIcon(null);
            return;
        }
        setIconWithTint(mIconInjector.getIcon(showX, level));
    }

    @VisibleForTesting
    void updateHotspotIcon(int deviceType) {
        setIconWithTint(getContext().getDrawable(getHotspotIconResource(deviceType)));
    }

    /**
     * Helper method to generate content description string.
     */
    @VisibleForTesting
    CharSequence buildContentDescription() {
        final Context context = getContext();

        CharSequence contentDescription = getTitle();
        final CharSequence summary = getSummary();
        if (!TextUtils.isEmpty(summary)) {
            contentDescription = TextUtils.concat(contentDescription, ",", summary);
        }
        int level = mWifiEntry.getLevel();
        if (level >= 0 && level < WIFI_CONNECTION_STRENGTH.length) {
            contentDescription = TextUtils.concat(contentDescription, ",",
                    context.getString(WIFI_CONNECTION_STRENGTH[level]));
        }
        if (displaySharedIcon()) {
            contentDescription = TextUtils.concat(contentDescription, ",",
                    context.getString(
                            R.string.accessibility_wifi_shared_network_icon_message));
        }

        return TextUtils.concat(contentDescription, ",",
                mWifiEntry.getSecurity() == WifiEntry.SECURITY_NONE
                        ? context.getString(R.string.accessibility_wifi_security_type_none)
                        : context.getString(R.string.accessibility_wifi_security_type_secured));
    }

    /**
     * Set listeners, who want to listen the button client event.
     */
    public void setOnButtonClickListener(OnButtonClickListener listener) {
        mOnButtonClickListener = listener;
        notifyChanged();
    }

    @Override
    protected int getSecondTargetResId() {
        return com.android.settings.R.layout.preference_end_icons_container;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.icon_button) {
            if (mOnButtonClickListener != null) {
                mOnButtonClickListener.onButtonClick(this);
            }
        }
    }

    /**
     * Callback to inform the caller that the icon button is clicked.
     */
    public interface OnButtonClickListener {

        /**
         * Register to listen the button click event.
         */
        void onButtonClick(WifiEntryPreference preference);
    }

    private Drawable getDrawable(@DrawableRes int iconResId) {
        Drawable buttonIcon = null;

        try {
            buttonIcon = getContext().getDrawable(iconResId);
        } catch (Resources.NotFoundException exception) {
            // Do nothing
        }
        return buttonIcon;
    }

    private static class WifiStandardDrawable extends Drawable {
        private final Drawable mBaseIcon;
        private final String mText;
        private final Paint mTextPaint;

        WifiStandardDrawable(Context context, Drawable baseIcon, String standard) {
            mBaseIcon = baseIcon;
            if (standard.contains("4")) mText = "4";
            else if (standard.contains("5")) mText = "5";
            else if (standard.contains("6")) mText = "6";
            else if (standard.contains("7")) mText = "7";
            else mText = null;

            float density = context.getResources().getDisplayMetrics().density;

            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mTextPaint.setColor(Utils.getColorAttrDefaultColor(context, android.R.attr.colorAccent));
            mTextPaint.setTextSize(10 * density);
            mTextPaint.setTypeface(Typeface.create("google-sans", Typeface.BOLD));
            mTextPaint.setTextAlign(Paint.Align.RIGHT);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            mBaseIcon.setBounds(bounds);
            mBaseIcon.draw(canvas);

            if (mText != null) {
                float x = bounds.right;
                float y = bounds.top + (bounds.height() * 0.35f);
                canvas.drawText(mText, x, y, mTextPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            mBaseIcon.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mBaseIcon.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return mBaseIcon.getOpacity();
        }

        @Override
        public int getIntrinsicWidth() {
            return mBaseIcon.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return mBaseIcon.getIntrinsicHeight();
        }
    }
}
