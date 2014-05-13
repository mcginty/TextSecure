package org.thoughtcrime.securesms.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.IOException;
import java.util.Collection;

public class AttachmentTransferDatabase {
  private static final String TAG = AttachmentTransferDatabase.class.getSimpleName();
  public  static final String URI = "content://textsecure/transfers/";

  private static final String DATABASE_NAME     = "transfers.db";
  private static final int    DATABASE_VERSION  = 1;

  public static final String TABLE_NAME         = "transfers";
  public static final String ID_COLUMN          = "_id";
  public static final String MESSAGE_ID_COLUMN  = "message_id";
  public static final String PART_ID_COLUMN     = "part_id";
  public static final String TRANSFERRED_COLUMN = "transferred";
  public static final String TOTAL_COLUMN       = "total";

  private final SQLiteOpenHelper databaseHelper;
  private final Context          context;

  private static AttachmentTransferDatabase instance = null;

  public synchronized static AttachmentTransferDatabase getInstance(Context context) {
    if (instance == null) instance = new AttachmentTransferDatabase(context);
    return instance;
  }

  public AttachmentTransferDatabase(Context context) {
    this.context = context;
    this.databaseHelper = new DatabaseOpenHelper(context, DATABASE_NAME, DATABASE_VERSION);
  }

  public long add(final long messageId, final long partId, final long transferred, final long total) {
    final SQLiteDatabase db = databaseHelper.getWritableDatabase();
    if (db == null) return -1;
    final ContentValues values = new ContentValues();
    values.put(MESSAGE_ID_COLUMN, messageId);
    values.put(PART_ID_COLUMN, partId);
    values.put(TRANSFERRED_COLUMN, transferred);
    values.put(TOTAL_COLUMN, total);
    final long transferId = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    notifyConversationListeners(messageId);
    return transferId;
  }

  public void update(final long messageId, final long partId, final long transferred, final long total) {
    final SQLiteDatabase db = databaseHelper.getWritableDatabase();
    if (db == null) return;
    final ContentValues values = new ContentValues();
    values.put(TRANSFERRED_COLUMN, transferred);
    values.put(TOTAL_COLUMN, total);
    db.update(TABLE_NAME, values, PART_ID_COLUMN + " = ?", new String[]{""+partId});
    notifyConversationListeners(messageId);
  }

  public TransferEntry get(final long messageId) {
    final SQLiteDatabase db = databaseHelper.getReadableDatabase();
    if (db == null) return null;
    final Cursor cursor = db.query(TABLE_NAME, null, MESSAGE_ID_COLUMN + " = ?", new String[]{""+messageId}, null, null, null);
    if (cursor == null || !cursor.moveToNext()) return null;
    return new TransferEntry(cursor.getLong(cursor.getColumnIndexOrThrow(ID_COLUMN)),
                             cursor.getLong(cursor.getColumnIndexOrThrow(MESSAGE_ID_COLUMN)),
                             cursor.getLong(cursor.getColumnIndexOrThrow(PART_ID_COLUMN)),
                             cursor.getLong(cursor.getColumnIndexOrThrow(TRANSFERRED_COLUMN)),
                             cursor.getLong(cursor.getColumnIndexOrThrow(TOTAL_COLUMN)));
  }

  private void notifyConversationListeners(long messageId) {
    context.getContentResolver().notifyChange(Uri.parse(URI + messageId), null);
  }

  private static class DatabaseOpenHelper extends SQLiteOpenHelper {

    private final Context context;

    private static final String TABLE_CREATE =
        "CREATE TABLE " + TABLE_NAME + " (" +
            ID_COLUMN + " INTEGER PRIMARY KEY, " +
            MESSAGE_ID_COLUMN + " INTEGER, " +
            PART_ID_COLUMN + " INTEGER, " +
            TRANSFERRED_COLUMN + " INTEGER, " +
            TOTAL_COLUMN + " INTEGER);";

    DatabaseOpenHelper(Context context, String name, int version) {
      super(context, name, null, version);
      this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
  }

  public static class TransferEntry {
    public long id;
    public long messageId;
    public long partId;
    public long transferred;
    public long total;

    public TransferEntry(long id, long messageId, long partId, long transferred, long total) {
      this.id = id;
      this.messageId = messageId;
      this.partId = partId;
      this.transferred = transferred;
      this.total = total;
    }
  }
}
