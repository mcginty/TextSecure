package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.util.Log;

import org.thoughtcrime.securesms.R;
import org.whispersystems.textsecure.util.FutureTaskListener;
import org.whispersystems.textsecure.util.ListenableFutureTask;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by kaonashi on 1/5/14.
 *
 * Assumptions: The dictionary is 2048 entries large, so 2^11. At this size, the converted wordlength
 * of a number of bits num_bits would be ceil(num_bits/11)
 */
public class Mnemonic {
  private final Context context;
  private List<String> mnemonicDict;
  private Object mnemonicDictLock = new Object();
  private final static String TAG = "Mnemonic";
  private static final ExecutorService threadPool = Executors.newSingleThreadExecutor();

  public Mnemonic(Context context) {
    this.context = context;
  }

  /**
   * Blocking disk reads, do off main thread
   */
  private void buildDictionary() throws IOException {
    synchronized(mnemonicDictLock) {
      if (mnemonicDict != null) return;

      mnemonicDict = new ArrayList<String>(2048);

      BufferedReader reader = new BufferedReader(
          new InputStreamReader(
              context.getApplicationContext()
                  .getResources()
                  .openRawResource(R.raw.mnemonic_wordlist_en)));

      String line = reader.readLine();
      while (line != null) {
        mnemonicDict.add(line);
        line = reader.readLine();
      }
    }
  }

  public void fromBytes(final byte[] bytes, final FutureTaskListener<String> callback) {

    Callable<String> bytesReturnedCallable = new Callable<String>() {
      @Override
      public String call() throws Exception {
        if (mnemonicDict == null) {
          buildDictionary();
        }

        BitInputStream bin = new BitInputStream(new ByteArrayInputStream(bytes));
        StringBuilder mnemonicBuilder = new StringBuilder();
        while (bin.available() > 0) {
          try {
            final int bits = bin.readBits((short)(bin.available() < 11 ? bin.available() : 11));
            mnemonicBuilder
                .append(mnemonicDict.get(bits))
                .append(" ");
          } catch (IOException e) {
            Log.e(TAG, "hit IOException when creating the mnemonic sentence");
            throw e;
          }
        }
        return mnemonicBuilder.deleteCharAt(mnemonicBuilder.length()-1).toString();
      }
    };

    threadPool.submit(new ListenableFutureTask<String>(bytesReturnedCallable, callback));

  }
}
