package com.buynme.buynme.utilities;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by phamdinhan on 28/09/2016.
 */

public class KeyHash {

    /**
     * Generate Key Hash, and put it onCreate method of Activity
     *
     * @param context
     */
    void GenerateKeyHash(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("com.buynme.buynme",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
    }
}
