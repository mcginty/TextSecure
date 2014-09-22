package org.thoughtcrime.securesms;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.widget.ProgressBar;
import android.widget.Toast;

import junit.framework.Assert;

import org.thoughtcrime.securesms.backup.UnsupportedVersionException;
import org.thoughtcrime.securesms.crypto.DecryptingQueue;
import org.thoughtcrime.securesms.database.EncryptedBackupExporter;
import org.thoughtcrime.securesms.database.NoExternalStorageException;
import org.thoughtcrime.securesms.database.ThreadDatabase.ProgressListener;
import org.thoughtcrime.securesms.notifications.MessageNotifier;
import org.thoughtcrime.securesms.util.ProgressDialogAsyncTask;
import org.thoughtcrime.securesms.util.VersionTracker;
import org.whispersystems.textsecure.crypto.MasterSecret;

import java.io.File;
import java.io.IOException;

public class EncryptedBackupImportActivity extends Activity {
  private static final String TAG = EncryptedBackupImportActivity.class.getSimpleName();
  private static final String PREFERENCE = "encrypted_backup_import";
  private MasterSecret masterSecret;

  private static final int SUCCESS     = 0;
  private static final int NO_SD_CARD  = 1;
  private static final int IO_ERROR    = 2;
  private static final int BAD_VERSION = 3;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    this.masterSecret = getIntent().getParcelableExtra("master_secret");
    if (!isPendingImport(this)) {
      throw new AssertionError(TAG + " should never be called when there's no pending encrypted import");
    }

    Log.w(TAG, "beginning import");
    new ImportEncryptedTask().execute();
  }

  public static boolean isPendingImport(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).contains(PREFERENCE);
  }

  public static void setPendingImport(Context context, File file) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().putString(PREFERENCE, file.getAbsolutePath()).apply();
  }

  private class ImportEncryptedTask extends ProgressDialogAsyncTask<Void, Pair<Integer,Integer>, Integer> {

    public ImportEncryptedTask() {
      super(EncryptedBackupImportActivity.this,
            R.string.ExportFragment_exporting,
            R.string.ExportFragment_exporting_keys_settings_and_messages,
            false);
    }

    @Override
    protected void onPostExecute(Integer result) {
      super.onPostExecute(result);
      Context context = EncryptedBackupImportActivity.this;

      if (context == null) return;

      switch (result) {
        case NO_SD_CARD:
          Toast.makeText(context,
                         context.getString(R.string.ExportFragment_error_unable_to_write_to_sd_card),
                         Toast.LENGTH_LONG).show();
          break;
        case IO_ERROR:
          Toast.makeText(context,
                         context.getString(R.string.ExportFragment_error_while_writing_to_sd_card),
                         Toast.LENGTH_LONG).show();
          break;
        case BAD_VERSION:
          Toast.makeText(context,
                         context.getString(R.string.ImportFragment_error_importing_backup_version),
                         Toast.LENGTH_LONG).show();
          break;
        case SUCCESS:
          Toast.makeText(context,
                         context.getString(R.string.ExportFragment_success),
                         Toast.LENGTH_LONG).show();
          break;
      }
      PreferenceManager.getDefaultSharedPreferences(context).edit().remove(PREFERENCE).apply();
      Intent intent = new Intent(context, RegistrationActivity.class);
      intent.putExtra("master_secret", masterSecret);
      context.startActivity(intent);
      finish();
    }

    @Override
    protected void onProgressUpdate(Pair<Integer,Integer>... values) {
      super.onProgressUpdate(values);
      if (values == null || values.length < 1) return;
      Pair<Integer, Integer> progressUpdate = values[0];
      progress.setMax(progressUpdate.second);
      progress.setProgress(progressUpdate.first);
    }

    @Override
    protected Integer doInBackground(Void... params) {
      try {
        EncryptedBackupExporter.importFromSd(EncryptedBackupImportActivity.this, masterSecret, new ProgressListener() {
          @Override
          public void onProgress(int complete, int total) {
            publishProgress(new Pair<Integer, Integer>(complete, total));
          }
        });
        return SUCCESS;
      } catch (UnsupportedVersionException e) {
        Log.w("ExportFragment", e);
        return BAD_VERSION;
      } catch (NoExternalStorageException e) {
        Log.w("ExportFragment", e);
        return NO_SD_CARD;
      } catch (IOException e) {
        Log.w("ExportFragment", e);
        return IO_ERROR;
      }
    }
  }

}
