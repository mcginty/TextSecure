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
package org.thoughtcrime.securesms;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.thoughtcrime.securesms.database.Database.InsertMessageEvent;
import org.thoughtcrime.securesms.database.Database.ThreadEvent;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.MmsSmsColumns;
import org.thoughtcrime.securesms.database.MmsSmsDatabase;
import org.thoughtcrime.securesms.database.SmsDatabase;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.util.LRUCache;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * A cursor adapter for a conversation thread.  Ultimately
 * used by ComposeMessageActivity to display a conversation
 * thread in a ListActivity.
 *
 * @author Moxie Marlinspike
 *
 */
public class ConversationAdapter extends Adapter<ConversationAdapter.ViewHolder> {
  private static final String TAG = ConversationAdapter.class.getSimpleName();

  private static final int MAX_CACHE_SIZE = 40;
  private final Map<String,SoftReference<MessageRecord>> messageRecordCache =
      Collections.synchronizedMap(new LRUCache<String, SoftReference<MessageRecord>>(MAX_CACHE_SIZE));

  private final Handler failedIconClickHandler;
  private final Context context;
  private final MasterSecret masterSecret;
  private final boolean groupThread;
  private final boolean pushDestination;
  private final LayoutInflater inflater;
  private       Cursor cursor;

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public ConversationItem conversationItem;
    public ViewHolder(ConversationItem conversationItem) {
      super(conversationItem);
      this.conversationItem = conversationItem;
    }
  }

  public ConversationAdapter(Context context, MasterSecret masterSecret,
                             Handler failedIconClickHandler, boolean groupThread, boolean pushDestination)
  {
    super();
    this.context                = context;
    this.masterSecret           = masterSecret;
    this.failedIconClickHandler = failedIconClickHandler;
    this.groupThread            = groupThread;
    this.pushDestination        = pushDestination;
    this.inflater               = LayoutInflater.from(context);
  }

  public void updateCursor(Cursor newCursor, ThreadEvent event) {
    Log.w(TAG, "changeCursor()");
    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }
    cursor = newCursor;
    if (event == null) {
      notifyDataSetChanged();
    } else {
      notifyItemInserted(0);
    }
  }

  public void bindData(ConversationItem item, Cursor cursor) {
    long id                     = cursor.getLong(cursor.getColumnIndexOrThrow(SmsDatabase.ID));
    String type                 = cursor.getString(cursor.getColumnIndexOrThrow(MmsSmsDatabase.TRANSPORT));
    MessageRecord messageRecord = getMessageRecord(id, cursor, type);

    item.set(masterSecret, messageRecord, failedIconClickHandler, groupThread, pushDestination);
  }

  public ConversationItem newView(Cursor cursor, ViewGroup parent, int type) {

    int layout;
    switch (type) {
      case R.id.message_type_outgoing:     layout = R.layout.conversation_item_sent;     break;
      case R.id.message_type_incoming:     layout = R.layout.conversation_item_received; break;
      case R.id.message_type_group_action: layout = R.layout.conversation_item_activity; break;
      default: throw new IllegalArgumentException("unsupported item view type given to ConversationAdapter");
    }
    return (ConversationItem) inflater.inflate(layout, parent, false);
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
    Log.w(TAG, "onCreateViewHolder()");
    ConversationItem item = newView(cursor, parent, type);
    return new ViewHolder(item);
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, int i) {
    Log.w(TAG, "onBindViewHolder(" + i + ")");
    if (cursor == null || !cursor.moveToPosition(i)) return;
    bindData(viewHolder.conversationItem, cursor);
  }

  public int getItemViewType(int i) {
    Log.w(TAG, "getItemViewType(" + i + ")");
    if (cursor == null || !cursor.moveToPosition(i)) {
      Log.w(TAG, "cursor null or out of bounds, returning default item view type");
      return 0;
    }
    return getItemViewType(cursor);
  }

  @Override
  public int getItemCount() {
    if (cursor == null) return 0;
    return cursor.getCount();
  }

  private int getItemViewType(Cursor cursor) {
    for (String name:cursor.getColumnNames()) {
      Log.w(TAG, "column: " + name);
    }
    long id                     = cursor.getLong(cursor.getColumnIndexOrThrow(MmsSmsColumns.ID));
    String type                 = cursor.getString(cursor.getColumnIndexOrThrow(MmsSmsDatabase.TRANSPORT));
    MessageRecord messageRecord = getMessageRecord(id, cursor, type);

    if      (messageRecord.isGroupAction()) return R.id.message_type_group_action;
    else if (messageRecord.isOutgoing())    return R.id.message_type_outgoing;
    else                                    return R.id.message_type_incoming;
  }

  private MessageRecord getMessageRecord(long messageId, Cursor cursor, String type) {
    SoftReference<MessageRecord> reference = messageRecordCache.get(type + messageId);

    if (reference != null) {
      MessageRecord record = reference.get();

      if (record != null)
        return record;
    }

    MmsSmsDatabase.Reader reader = DatabaseFactory.getMmsSmsDatabase(context)
                                                  .readerFor(cursor, masterSecret);

    MessageRecord messageRecord = reader.getCurrent();

    messageRecordCache.put(type + messageId, new SoftReference<MessageRecord>(messageRecord));

    return messageRecord;
  }
}
