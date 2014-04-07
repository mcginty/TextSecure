package org.thoughtcrime.securesms.database.loaders;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import org.thoughtcrime.securesms.database.Database;
import org.thoughtcrime.securesms.database.DatabaseFactory;

public class ConversationLoader extends CursorLoader {
  private final ForceLoadContentObserver mObserver = new ForceLoadContentObserver();
  private final Context context;
  private final long threadId;

  public ConversationLoader(Context context, long threadId) {
    super(context);
    this.context  = context.getApplicationContext();
    this.threadId = threadId;
  }

  @Override
  public Cursor loadInBackground() {
    final Cursor cursor = DatabaseFactory.getMmsSmsDatabase(context).getConversation(threadId);
    if (cursor != null) {
      cursor.getCount();
      cursor.registerContentObserver(mObserver);
    }
    return cursor;
  }
}
