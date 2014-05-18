package org.thoughtcrime.securesms.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.thoughtcrime.securesms.service.KeyCachingService;
import org.whispersystems.textsecure.crypto.MasterSecret;

/**
 * Created by kaonashi on 5/18/14.
 */
public class PassphraseUtil {

  public interface SetMasterSecretListener {
    public void onSuccess();
  }

  public static void setMasterSecret(final Context context,
                                        final MasterSecret masterSecret,
                                        final SetMasterSecretListener listener)
  {
    final ServiceConnection serviceConnection = new ServiceConnection() {
      private KeyCachingService keyCachingService;
      @Override
      public void onServiceConnected(ComponentName className, IBinder service) {
        keyCachingService = ((KeyCachingService.KeyCachingBinder) service).getService();
        keyCachingService.setMasterSecret(masterSecret);

        context.unbindService(this);

        MemoryCleaner.clean(masterSecret);
        if (listener != null) listener.onSuccess();
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        keyCachingService = null;
      }
    };

    Intent bindIntent = new Intent(context, KeyCachingService.class);
    context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
  }
}
