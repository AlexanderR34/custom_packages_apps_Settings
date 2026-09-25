package com.android.settings.biometrics;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

import java.io.File;

@SearchIndexable
public class UdfpsCustomIconSettings extends DashboardFragment {

    private static final String TAG = "UdfpsCustomIconSettings";
    private static final String PREF_PREVIEW = "udfps_custom_icon_preview_layout";

    private ImageView mPreviewWallpaper;
    private ImageView mPreviewUdfpsIcon;
    private ActivityResultLauncher<String> mImagePickerLauncher;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onCustomImagePicked);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        updatePreview();
    }

    private void setupViews() {
        LayoutPreference layoutPref = findPreference(PREF_PREVIEW);
        if (layoutPref == null) return;

        View root = layoutPref.findViewById(R.id.preview_card_frame);
        if (root == null) return;

        mPreviewWallpaper = layoutPref.findViewById(R.id.preview_wallpaper);
        mPreviewUdfpsIcon = layoutPref.findViewById(R.id.preview_udfps_icon);

        View cardPickImage = layoutPref.findViewById(R.id.card_pick_custom_image);
        View btnReset = layoutPref.findViewById(R.id.btn_reset_udfps);

        if (cardPickImage != null) {
            cardPickImage.setOnClickListener(v -> mImagePickerLauncher.launch("image/*"));
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                UdfpsCustomIconManager.resetToAosp(requireContext());
                updatePreview();
                Toast.makeText(getContext(), "Restablecido a diseño predeterminado", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void onCustomImagePicked(Uri uri) {
        if (uri == null) return;
        boolean success = UdfpsCustomIconManager.saveCustomImage(requireContext(), uri);
        if (success) {
            updatePreview();
            Toast.makeText(getContext(), "Icono de huella personalizado aplicado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePreview() {
        Context context = getContext();
        if (context == null || mPreviewUdfpsIcon == null) return;

        Drawable wp = UdfpsCustomIconManager.getLockscreenWallpaper(context);
        if (wp != null && mPreviewWallpaper != null) {
            mPreviewWallpaper.setImageDrawable(wp);
        }

        int style = UdfpsCustomIconManager.getIconStyle(context);
        TextView tvBadge = null;
        LayoutPreference layoutPref = findPreference(PREF_PREVIEW);
        if (layoutPref != null) {
            tvBadge = layoutPref.findViewById(R.id.tv_current_style_badge);
        }

        String styleLabel = "Estilo: Predeterminado AOSP";
        if (style == UdfpsCustomIconManager.STYLE_CUSTOM_IMAGE) {
            Bitmap bmp = UdfpsCustomIconManager.loadCustomImageBitmap(context);
            if (bmp != null) {
                mPreviewUdfpsIcon.setImageBitmap(bmp);
                styleLabel = "Estilo: Imagen personalizada";
            } else {
                mPreviewUdfpsIcon.setImageResource(R.drawable.ic_fingerprint_24dp);
            }
        } else {
            mPreviewUdfpsIcon.setImageResource(R.drawable.ic_fingerprint_24dp);
        }

        if (tvBadge != null) {
            tvBadge.setText(styleLabel);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreview();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.udfps_custom_icon_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DISPLAY;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.udfps_custom_icon_settings);
}
