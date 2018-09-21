package com.gvs.secure;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

public class ScreenLockUtils {
    private static ScreenLockUtils instance;

    private ScreenLockUtils() {
    }

    /**
     * Returns the singleton instance.
     */
    static synchronized ScreenLockUtils getInstance() {
        if (instance == null) {
            instance = new ScreenLockUtils();
        }
        return instance;
    }

    /**
     * Checks if the device screen lock is enabled. Returns the status as a boolean.
     */
    public boolean isScreenLockEnabled(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        assert keyguardManager != null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return keyguardManager.isDeviceSecure();
        }
        return keyguardManager.isKeyguardSecure();
    }

    /**
     * Checks if the device is locked. Returns the status as a boolean.
     */
    boolean isScreenLocked(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        assert keyguardManager != null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return keyguardManager.isDeviceLocked();
        }
        return keyguardManager.isKeyguardLocked();
    }
}
