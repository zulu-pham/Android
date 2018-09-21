package com.gvs.secure.util;

import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

public class KeyStoreUtil {

    private final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private String KEYSTORE_ALIAS;

    private static KeyStoreUtil INSTANCE;

    /**
     * Returns the singleton instance.
     */
    public static synchronized KeyStoreUtil getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new KeyStoreUtil();
        }
        return INSTANCE;
    }

    private KeyStoreUtil() {

    }

    public void init(Context context) {
        KEYSTORE_ALIAS = context.getPackageName();
    }

    public void test(Context context) {
        try {
            generateKey(context);

            String inputString = "secure-storage-in-android";

            String cipher = RSAEncrypt(inputString);
            String plain = RSADecrypt(cipher);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String RSAEncrypt(String plainText) throws GeneralSecurityException, IOException {
        KeyStore.Entry entry = getKeyStore().getEntry(KEYSTORE_ALIAS, null);
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            PublicKey publicKey = ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();

            byte[] bytes = plainText.getBytes("UTF-8");
            byte[] encryptedBytes = RSAEncrypt(publicKey, bytes);
            byte[] base64encryptedBytes = Base64.encode(encryptedBytes, Base64.DEFAULT);
            return new String(base64encryptedBytes);
        }

        return null;
    }

    public String RSADecrypt(String cipherText) throws GeneralSecurityException, IOException {
        KeyStore.Entry entry = getKeyStore().getEntry(KEYSTORE_ALIAS, null);
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();

            byte[] bytes = cipherText.getBytes("UTF-8");
            byte[] base64encryptedBytes = Base64.decode(bytes, Base64.DEFAULT);
            byte[] decryptedBytes = RSADecrypt(privateKey, base64encryptedBytes);
            return new String(decryptedBytes);
        }

        return null;
    }

    private byte[] RSAEncrypt(PublicKey publicKey, byte[] bytes) throws GeneralSecurityException, IOException {
        Cipher cipher = getCipher();
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
        cipherOutputStream.write(bytes);
        cipherOutputStream.close();

        return outputStream.toByteArray();
    }

    private byte[] RSADecrypt(PrivateKey privateKey, byte[] bytes) throws GeneralSecurityException, IOException {
        Cipher cipher = getCipher();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(bytes), cipher);

        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte) nextByte);
        }

        byte[] dbytes = new byte[values.size()];
        for (int i = 0; i < dbytes.length; i++) {
            dbytes[i] = values.get(i).byteValue();
        }

        cipherInputStream.close();
        return dbytes;
    }

    private Cipher getCipher() throws GeneralSecurityException {
        String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Cipher.getInstance(CIPHER_TRANSFORMATION, "AndroidKeyStoreBCWorkaround");
        } else {
            return Cipher.getInstance(CIPHER_TRANSFORMATION, "AndroidOpenSSL");
        }
    }

    private KeyStore getKeyStore() throws GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        try {
            keyStore.load(null);
        } catch (IOException e) {
            throw new GeneralSecurityException("unable to load keystore", e);
        }
        return keyStore;
    }

    /**
     * RSA key pair is generated and stored securely.
     *
     * @return
     * @throws GeneralSecurityException
     */
    private void generateKey(Context context) throws Exception {
        KeyStore keyStore = getKeyStore();
        String packageName = context.getPackageName();
        boolean containsAlias = keyStore.containsAlias(packageName);

        if (!containsAlias) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                generateRSAKey_AboveApi23();
            } else {
                generateRSAKey_BelowApi23(context);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void generateRSAKey_AboveApi23() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);

        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec
                .Builder(KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .setUserAuthenticationRequired(true)
                .build();

        keyPairGenerator.initialize(keyGenParameterSpec);
        keyPairGenerator.generateKeyPair();
    }

    private void generateRSAKey_BelowApi23(Context context) throws Exception {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 100);

        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEYSTORE_ALIAS)
                .setSubject(new X500Principal("CN=" + KEYSTORE_ALIAS))
                .setSerialNumber(BigInteger.TEN)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);
        keyPairGenerator.initialize(spec);
        keyPairGenerator.generateKeyPair();
    }
}
