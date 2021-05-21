package com.medyear.idvalidation.ext.perm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.listener.OnDialogButtonClickListener;
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener;

/**
 * Utility listener that shows a {@link Dialog} with a minimum configuration when the user rejects
 * any of the requested permissions
 */
public class CDialogOnAnyDeniedMultiplePermissionsListener extends BaseMultiplePermissionsListener {

    private final Context context;
    private final PermissionCallback callback;
    private final String title;
    private final String message;
    private final String positiveButtonText;
    private final Drawable icon;
    private final OnDialogButtonClickListener onDialogButtonClickListener;

    private CDialogOnAnyDeniedMultiplePermissionsListener(Context context, PermissionCallback callback, String title,
                                                          String message, String positiveButtonText, Drawable icon,
                                                          OnDialogButtonClickListener onDialogButtonClickListener) {
        this.context = context;
        this.callback = callback;
        this.title = title;
        this.message = message;
        this.positiveButtonText = positiveButtonText;
        this.icon = icon;
        this.onDialogButtonClickListener = onDialogButtonClickListener;
    }

    @Override
    public void onPermissionsChecked(MultiplePermissionsReport report) {
        super.onPermissionsChecked(report);

        if (!report.areAllPermissionsGranted()) {
            showDialog();
        } else {
            callback.onPermissionRequest(true);
        }
    }

    private void showDialog() {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    dialog.dismiss();
                    onDialogButtonClickListener.onClick();
                })
                .setIcon(icon)
                .show();
    }

    /**
     * Builder class to configure the displayed dialog.
     * Non set fields will be initialized to an empty string.
     */
    public static class Builder {
        private final Context context;
        private PermissionCallback callback;
        private String title;
        private String message;
        private String buttonText;
        private Drawable icon;
        private OnDialogButtonClickListener onDialogButtonClickListener;

        private Builder(Context context) {
            this.context = context;
        }

        public static Builder withContext(Context context) {
            return new Builder(context);
        }

        public Builder withCallback(PermissionCallback callback) {
            this.callback = callback;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withTitle(@StringRes int resId) {
            this.title = context.getString(resId);
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder withMessage(@StringRes int resId) {
            this.message = context.getString(resId);
            return this;
        }

        public Builder withButtonText(String buttonText) {
            this.buttonText = buttonText;
            return this;
        }

        public Builder withButtonText(@StringRes int resId) {
            this.buttonText = context.getString(resId);
            return this;
        }

        public Builder withButtonText(String buttonText, OnDialogButtonClickListener onDialogButtonClickListener) {
            this.buttonText = buttonText;
            this.onDialogButtonClickListener = onDialogButtonClickListener;
            return this;
        }

        public Builder withButtonText(@StringRes int resId, OnDialogButtonClickListener onDialogButtonClickListener) {
            this.buttonText = context.getString(resId);
            this.onDialogButtonClickListener = onDialogButtonClickListener;
            return this;
        }

        public Builder withIcon(Drawable icon) {
            this.icon = icon;
            return this;
        }

        public Builder withIcon(@DrawableRes int resId) {
            this.icon = ContextCompat.getDrawable(context, resId);
            return this;
        }

        public CDialogOnAnyDeniedMultiplePermissionsListener build() {
            String title = this.title == null ? "" : this.title;
            String message = this.message == null ? "" : this.message;
            String buttonText = this.buttonText == null ? "" : this.buttonText;
            OnDialogButtonClickListener onDialogButtonClickListener =
                    this.onDialogButtonClickListener != null
                            ? this.onDialogButtonClickListener
                            : () -> {
                    };
            return new CDialogOnAnyDeniedMultiplePermissionsListener(context, callback, title, message, buttonText, icon,
                    onDialogButtonClickListener);
        }
    }
}
