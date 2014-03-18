package org.thoughtcrime.securesms.contacts;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.BitmapWorkerTask;

import java.util.HashMap;
import java.util.Map;

public class ContactSelectionListAdapter extends BaseAdapter {

  private final static int STYLE_ATTRIBUTES[] = new int[]{R.attr.contact_selection_push_user,
                                                          R.attr.contact_selection_lay_user,
                                                          R.attr.contact_selection_push_label,
                                                          R.attr.contact_selection_lay_label};

  private final Context        context;
  private       Cursor         cursor;
  private final boolean        multiSelect;
  private final LayoutInflater li;
  private final TypedArray     drawables;

  private final HashMap<Long, ContactAccessor.ContactData> selectedContacts = new HashMap<Long, ContactAccessor.ContactData>();

  public static class ViewHolder {
    public CheckBox  checkBox;
    public TextView  name;
    public TextView  number;
    public ImageView contactPhoto;
    public int       position;
  }

  public ContactSelectionListAdapter(Context context, Cursor cursor, boolean multiSelect) {
    super();
    this.context     = context;
    this.cursor      = cursor;
    this.li          = LayoutInflater.from(context);
    this.drawables   = context.obtainStyledAttributes(STYLE_ATTRIBUTES);
    this.multiSelect = multiSelect;
  }

  public View newView(ViewGroup parent) {
    final View v            = li.inflate(R.layout.push_contact_selection_list_item, parent, false);
    final ViewHolder holder = new ViewHolder();

    holder.name         = (TextView) v.findViewById(R.id.name);
    holder.number       = (TextView) v.findViewById(R.id.number);
    holder.checkBox     = (CheckBox) v.findViewById(R.id.check_box);
    holder.contactPhoto = (ImageView) v.findViewById(R.id.contact_photo_image);

    if (!multiSelect) holder.checkBox.setVisibility(View.GONE);

    v.setTag(R.id.holder_tag, holder);
    return v;
  }

  public void bindView(View view, Context context, Cursor cursor, int i) {
    Log.d("PushList", "bindView() for view " + view.getId() + " with cursor pos " + cursor.getPosition());

    boolean isPushUser;
    try {
      isPushUser = (cursor.getInt(cursor.getColumnIndexOrThrow(ContactAccessor.PUSH_COLUMN)) > 0);
    } catch (IllegalArgumentException iae) {
      isPushUser = false;
    }

    ContactAccessor.ContactData contactData = ContactAccessor.getInstance().getContactData(context, cursor);
    view.setTag(R.id.contact_info_tag, contactData);

    ViewHolder holder = (ViewHolder) view.getTag(R.id.holder_tag);

    if (!isPushUser) {
      holder.contactPhoto.setBackgroundDrawable(null);
      holder.name.setTextColor(drawables.getColor(1, 0xff000000));
      holder.number.setTextColor(drawables.getColor(1, 0xff000000));
    } else {
      holder.contactPhoto.setBackgroundResource(R.drawable.push_user_highlight);
      holder.name.setTextColor(drawables.getColor(0, 0xa0000000));
      holder.number.setTextColor(drawables.getColor(0, 0xa0000000));
    }

    if (selectedContacts.containsKey(contactData.id)) {
      holder.checkBox.setChecked(true);
    } else {
      holder.checkBox.setChecked(false);
    }

    holder.name.setText(contactData.name);

    if (contactData.numbers.isEmpty()) {
      holder.name.setEnabled(false);
      holder.number.setText("");
    } else {
      holder.number.setText(contactData.numbers.get(0).number);
    }
    loadBitmap(contactData.numbers.get(0).number, holder.contactPhoto);
  }

  public boolean cancelPotentialWork(String number, ImageView imageView) {
    final BitmapWorkerTask bitmapWorkerTask = BitmapWorkerTask.getBitmapWorkerTask(imageView);

    if (bitmapWorkerTask != null) {
      final String bitmapData = bitmapWorkerTask.number;
      if (bitmapData != null && !bitmapData.equals(number)) {
        bitmapWorkerTask.cancel(true);
      } else {
        return false;
      }
    }
    return true;
  }

  public void loadBitmap(String number, ImageView imageView) {
    if (cancelPotentialWork(number, imageView)) {
      final BitmapWorkerTask task = new BitmapWorkerTask(context, imageView);
      final BitmapWorkerTask.AsyncDrawable asyncDrawable = new BitmapWorkerTask.AsyncDrawable(context.getResources(),
                                                                                              null, task);
      imageView.setImageDrawable(asyncDrawable);
      task.execute(number);
    }
  }

  @Override
  public int getCount() {
    if (cursor != null) return cursor.getCount();
    else                return 0;
  }

  @Override
  public Object getItem(int i) {
    if (cursor == null) return null;

    cursor.moveToPosition(i);
    return cursor;
  }

  @Override
  public long getItemId(int i) {
    cursor.moveToPosition(i);
    if (cursor != null) return cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
    else                return 0;
  }

  @Override
  public View getView(int i, View convertView, ViewGroup viewGroup) {
    if (cursor != null && cursor.moveToPosition(i)) {
      final View v;
      if (convertView == null) v = newView(viewGroup);
      else                     v = convertView;

      bindView(v, context, cursor, i);
      return v;
    } else {
      return null;
    }
  }

  public void changeCursor(Cursor cursor) {
    if (this.cursor != null && !this.cursor.isClosed()) this.cursor.close();

    this.cursor = cursor;
    if (cursor != null) {
      notifyDataSetChanged();
    } else {
      notifyDataSetInvalidated();
    }
  }

  public Map<Long,ContactAccessor.ContactData> getSelectedContacts() {
    return selectedContacts;
  }
}
