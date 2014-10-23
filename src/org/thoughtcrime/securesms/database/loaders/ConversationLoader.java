package org.thoughtcrime.securesms.database.loaders;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;

import org.thoughtcrime.securesms.database.DatabaseFactory;

public class ConversationLoader extends CursorLoader {

  private final Context context;
  private final long threadId;
  private final Bundle bundle;

  public ConversationLoader(Context context, long threadId, Bundle bundle) {
    super(context);
    this.context  = context.getApplicationContext();
    this.threadId = threadId;
    this.bundle   = bundle;
  }

  public Bundle getBundle() {
    return bundle;
  }

  @Override
  public Cursor loadInBackground() {
    return DatabaseFactory.getMmsSmsDatabase(context).getConversation(threadId);
  }
}
