/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.widget;

import static android.view.View.TEXT_DIRECTION_LOCALE;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.CustomEditTextPreferenceCompat;

/**
 * {@code EditTextPreference} that supports input validation.
 */
public class ValidatedEditTextPreference extends CustomEditTextPreferenceCompat {

    public interface Validator {
        boolean isTextValid(String value);
    }

    private final EditTextWatcher mTextWatcher = new EditTextWatcher();
    private Validator mValidator;
    private boolean mIsPassword;
    private boolean mIsSummaryPassword;
    private boolean mAllowRandomPassword;

    public ValidatedEditTextPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ValidatedEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ValidatedEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ValidatedEditTextPreference(Context context) {
        super(context);
    }

    public void setAllowRandomPassword(boolean allow) {
        mAllowRandomPassword = allow;
    }

    public boolean isAllowRandomPassword() {
        return mAllowRandomPassword;
    }

    /**
     * Generates a strong random password containing uppercase letters, lowercase letters,
     * numbers 0-9, and special characters.
     */
    public static String generateStrongRandomPassword(int length) {
        if (length < 8) length = 12;
        final String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String lower = "abcdefghijkmnopqrstuvwxyz";
        final String numbers = "0123456789";
        final String special = "!@#$%&*-_+=~?";
        final String allChars = upper + lower + numbers + special;

        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        // Guarantee at least 1 uppercase, 1 lowercase, 1 digit (0-9), and 1 special symbol
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(numbers.charAt(random.nextInt(numbers.length())));
        sb.append(special.charAt(random.nextInt(special.length())));

        for (int i = 4; i < length; i++) {
            sb.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle characters
        char[] array = sb.toString().toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }

        return new String(array);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            android.content.DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        if (mAllowRandomPassword) {
            builder.setNeutralButton(R.string.wifi_hotspot_generate_password, null);
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        final EditText editText = view.findViewById(android.R.id.edit);
        if (editText != null) {
            editText.setHint(getDialogTitle());
            editText.setTextDirection(TEXT_DIRECTION_LOCALE);
        }
        if (editText != null && !TextUtils.isEmpty(editText.getText())) {
            editText.setSelection(editText.getText().length());
        }
        if (mValidator != null && editText != null) {
            editText.removeTextChangedListener(mTextWatcher);
            if (mIsPassword) {
                editText.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                editText.setMaxLines(1);
            }
            editText.addTextChangedListener(mTextWatcher);
        }
        if (mAllowRandomPassword) {
            view.post(() -> {
                final AlertDialog dialog = (AlertDialog) getDialog();
                if (dialog != null) {
                    final android.widget.Button neutralButton =
                            dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    if (neutralButton != null) {
                        neutralButton.setOnClickListener(v -> {
                            String newPassword = generateStrongRandomPassword(12);
                            if (editText != null) {
                                editText.setText(newPassword);
                                editText.setSelection(newPassword.length());
                            }
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);
                            if (mValidator != null && editText != null) {
                                boolean valid = mValidator.isTextValid(newPassword);
                                final android.widget.Button positiveButton =
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                if (positiveButton != null) {
                                    positiveButton.setEnabled(valid);
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView textView = (TextView) holder.findViewById(android.R.id.summary);
        if (textView == null) {
            return;
        }
        if (mIsSummaryPassword) {
            textView.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            textView.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        }
    }

    public void setIsPassword(boolean isPassword) {
        mIsPassword = isPassword;
    }

    public void setIsSummaryPassword(boolean isPassword) {
        mIsSummaryPassword = isPassword;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public boolean isPassword() {
        return mIsPassword;
    }

    public void setValidator(Validator validator) {
        mValidator = validator;
    }

    private class EditTextWatcher implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            final EditText editText = getEditText();
            if (mValidator != null && editText != null) {
                final AlertDialog dialog = (AlertDialog) getDialog();
                final boolean valid = mValidator.isTextValid(editText.getText().toString());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(valid);
            }
        }
    }

}
