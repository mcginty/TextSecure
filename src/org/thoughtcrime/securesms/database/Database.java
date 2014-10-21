/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.Set;

import de.greenrobot.event.EventBus;

public abstract class Database {

  protected static final String ID_WHERE            = "_id = ?";
  private static final String CONVERSATION_URI      = "content://textsecure/thread/";
  private static final String CONVERSATION_LIST_URI = "content://textsecure/conversation-list";

  protected       SQLiteOpenHelper databaseHelper;
  protected final Context context;

  public Database(Context context, SQLiteOpenHelper databaseHelper) {
    this.context        = context;
    this.databaseHelper = databaseHelper;
  }

  protected void notifyConversationListeners(Set<Long> threadIds, ThreadEvent event) {
    for (long threadId : threadIds)
      notifyConversationListeners(threadId, event);
  }

  protected void notifyConversationListeners(long threadId, ThreadEvent event) {
    context.getContentResolver().notifyChange(Uri.parse(CONVERSATION_URI + threadId), null);
    EventBus.getDefault().post(event);
  }

  protected void notifyConversationListListeners() {
    context.getContentResolver().notifyChange(Uri.parse(CONVERSATION_LIST_URI), null);
  }

  protected void setNotifyConverationListeners(Cursor cursor, long threadId) {
    cursor.setNotificationUri(context.getContentResolver(), Uri.parse(CONVERSATION_URI + threadId));
  }

  protected void setNotifyConverationListListeners(Cursor cursor) {
    cursor.setNotificationUri(context.getContentResolver(), Uri.parse(CONVERSATION_LIST_URI));
  }

  public void reset(SQLiteOpenHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
  }

  public static class ThreadEvent {
    public long threadId;
    public ThreadEvent(long threadId) {
      this.threadId = threadId;
    }
  }

  public static class MessageEvent extends ThreadEvent {
    private long messageId;
    public MessageEvent(long threadId, long messageId) {
      super(threadId);
      this.messageId = messageId;
    }
  }

  public static class InsertEvent extends MessageEvent {
    public InsertEvent(long threadId, long messageId) {
      super(threadId, messageId);
    }
  }

  public static class DeleteEvent extends MessageEvent {
    public DeleteEvent(long threadId, long messageId) {
      super(threadId, messageId);
    }
  }
  public static class UpdateEvent extends MessageEvent {
    public UpdateEvent(long threadId, long messageId) {
      super(threadId, messageId);
    }
  }
}
