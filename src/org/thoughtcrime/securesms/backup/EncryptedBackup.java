package org.thoughtcrime.securesms.backup;

import android.content.Context;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.util.Log;

import org.thoughtcrime.securesms.EncryptedBackupImportActivity;
import org.thoughtcrime.securesms.crypto.DecryptingPartInputStream;
import org.thoughtcrime.securesms.crypto.EncryptingPartOutputStream;
import org.thoughtcrime.securesms.crypto.InvalidPassphraseException;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.service.KeyCachingService;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class EncryptedBackup {
  private final static String TAG     = "EncryptedBackup";
  private final static int    VERSION = 1;

  private static File getCryptoSharedPreferences(Context context) {
    File sharedPrefs = new File(context.getApplicationInfo().dataDir + File.separator + "shared_prefs");
    if (!sharedPrefs.exists()) {
      sharedPrefs.mkdir();
    }
    return new File(sharedPrefs, MasterSecretUtil.PREFERENCES_NAME + ".xml");
  }

  public static OutputStream getOutputStream(Context context, File outFile, MasterSecret masterSecret)
      throws IOException
  {
    FileOutputStream plaintextStream = new FileOutputStream(outFile);

    InputStream is = new FileInputStream(getCryptoSharedPreferences(context));

    Log.w(TAG, "writing header of size " + is.available());
    byte[] headerSize = ByteBuffer.allocate(4).putInt(is.available()).array();

    plaintextStream.write(VERSION);
    plaintextStream.write(headerSize);
    Util.copy(is, plaintextStream);

    return new EncryptingPartOutputStream(outFile, masterSecret, true);
  }

  public static void initializeImport(Context context, File inFile)
      throws IOException, UnsupportedVersionException
  {
    FileInputStream inputStream = new FileInputStream(inFile);

    byte[] headerSizeBytes = new byte[4];
    int    version         = inputStream.read();

    if (version != VERSION) {
      throw new UnsupportedVersionException("backup file version " + version + " unsupported");
    }

    if (inputStream.read(headerSizeBytes) != headerSizeBytes.length) {
      throw new IOException("incomplete header length, malformed encrypted backup");
    }

    int headerSize = ByteBuffer.wrap(headerSizeBytes).getInt();
    Log.w(TAG, "read header size of " + headerSize);

    OutputStream os = new FileOutputStream(getCryptoSharedPreferences(context));

    Util.copy(inputStream, os, headerSize);

    EncryptedBackupImportActivity.setPendingImport(context, inFile);

    boolean passwordDisabled = false;
    try {
      MasterSecretUtil.getMasterSecret(context, MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
      passwordDisabled = true;
    } catch (InvalidPassphraseException ipe) {
      // nothing
    }
    TextSecurePreferences.setPasswordDisabled(context, passwordDisabled);

    if (passwordDisabled) {
      Log.w(TAG, "disabling key caching service");
      Intent intent = new Intent(context, KeyCachingService.class);
      intent.setAction(KeyCachingService.DISABLE_ACTION);
      context.startService(intent);
    }
  }

  public static InputStream getInputStream(Context context, File inFile, MasterSecret masterSecret)
      throws IOException, UnsupportedVersionException
  {
    FileInputStream inputStream = new FileInputStream(inFile);

    byte[] headerSizeBytes = new byte[4];
    int    version         = inputStream.read();

    if (version != VERSION) {
      throw new UnsupportedVersionException("backup file version " + version + " unsupported");
    }
    if (inputStream.read(headerSizeBytes) != headerSizeBytes.length) {
      throw new IOException("incomplete header length, malformed encrypted backup");
    }
    int headerSize = ByteBuffer.wrap(headerSizeBytes).getInt();
    Log.w(TAG, "read header size of " + headerSize);


    return new DecryptingPartInputStream(inFile, masterSecret, 1 + headerSizeBytes.length + headerSize);
  }
}
