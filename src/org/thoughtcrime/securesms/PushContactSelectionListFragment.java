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


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import org.thoughtcrime.securesms.contacts.ContactAccessor;
import org.thoughtcrime.securesms.contacts.ContactAccessor.ContactData;
import org.thoughtcrime.securesms.contacts.ContactAccessor.NumberData;
import org.thoughtcrime.securesms.contacts.ContactSelectionListAdapter;
import org.thoughtcrime.securesms.contacts.ContactSelectionListAdapter.ViewHolder;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;
import org.whispersystems.textsecure.util.InvalidNumberException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Activity for selecting a list of contacts.
 *
 * @author Moxie Marlinspike
 *
 */

public class PushContactSelectionListFragment extends ListFragment {

  private Map<Long, ContactData>    selectedContacts;
  private OnContactSelectedListener onContactSelectedListener;
  private boolean                   multi;

  @Override
  public void onActivityCreated(Bundle icicle) {
    super.onCreate(icicle);
    initializeResources();
  }

  @Override
  public void onResume() {
    super.onResume();
    multi = getActivity().getIntent().getBooleanExtra(PushContactSelectionActivity.MULTI_SELECT_EXTRA, false);
    initializeCursor();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.push_contact_selection_list_activity, container, false);
  }

  public List<ContactData> getSelectedContacts() {
    if (selectedContacts == null) return null;

    List<ContactData> selected = new LinkedList<ContactData>();
    selected.addAll(selectedContacts.values());

    return selected;
  }

  private void addSingleNumberContact(ContactData contactData) {
    if (multi) {
      selectedContacts.put(contactData.id, contactData);
    }
    if (onContactSelectedListener != null) {
      onContactSelectedListener.onContactSelected(contactData);
    }
  }

  private void removeContact(ContactData contactData) {
    selectedContacts.remove(contactData.id);
  }

  private void addMultipleNumberContact(ContactData contactData, TextView textView, CheckBox checkBox) {
    String[] options = new String[contactData.numbers.size()];
    int i = 0;

    for (NumberData option : dedupeNumbers(contactData.numbers)) {
      options[i++] = option.type + " " + option.number;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(getString(R.string.ContactSelectionlistFragment_select_for) + " " + contactData.name);

    if (multi) builder.setMultiChoiceItems(options, null, new MultiDiscriminatorClickedListener(contactData));
    else       builder.setSingleChoiceItems(options, -1, new SingleDiscriminatorClickedListener(contactData));

    builder.setPositiveButton(android.R.string.ok, new DiscriminatorFinishedListener(contactData, textView, checkBox));
    builder.setOnCancelListener(new DiscriminatorFinishedListener(contactData, textView, checkBox));
    builder.show();
  }

  private void initializeCursor() {
    update();
    ContactSelectionListAdapter adapter = new ContactSelectionListAdapter(getActivity(), null, multi);
    selectedContacts = adapter.getSelectedContacts();
    setListAdapter(adapter);
  }

  private void initializeResources() {
    this.getListView().setFocusable(true);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    final ContactData contactData = (ContactData) v.getTag(R.id.contact_info_tag);
    final ViewHolder holder = (ViewHolder) v.getTag(R.id.holder_tag);
    holder.checkBox.toggle();

    if (holder.checkBox.isChecked()) {
      if (contactData.numbers.size() == 1) addSingleNumberContact(contactData);
      else {
        final List<NumberData> dedupedNumbers = dedupeNumbers(contactData.numbers);
        contactData.numbers.clear();
        contactData.numbers.addAll(dedupedNumbers);
        if (dedupedNumbers.size() == 1) addSingleNumberContact(contactData);
        else                            addMultipleNumberContact(contactData, holder.name, holder.checkBox);
      }
    } else {
      removeContact(contactData);
    }
  }

  private List<NumberData> dedupeNumbers(List<NumberData> numbers) {
    final Map<String,NumberData> map = new HashMap<String,NumberData>();
    for (NumberData numberData : numbers) {
      try {
        map.put(Util.canonicalizeNumber(getActivity(), numberData.number), numberData);
      } catch (InvalidNumberException ine) {
        map.put(numberData.number, numberData);
      }
    }
    return new ArrayList<NumberData>(map.values());
  }

  public void update() {
    new LoadCursorAsyncTask().execute();
  }

  private class DiscriminatorFinishedListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private final ContactData contactData;
    private final TextView    textView;
    private final CheckBox    checkBox;

    public DiscriminatorFinishedListener(ContactData contactData, TextView textView, CheckBox checkBox) {
      this.contactData = contactData;
      this.textView = textView;
      this.checkBox = checkBox;
    }

    public void onClick(DialogInterface dialog, int which) {
      if (!multi) return;

      ContactData selected = selectedContacts.get(contactData.id);

      if (selected == null && textView != null) {
        checkBox.setChecked(false);
      } else if (selected != null && selected.numbers.size() == 0) {
        selectedContacts.remove(selected.id);
        if (textView != null) checkBox.setChecked(false);
      }

      if (textView == null) ((ContactSelectionListAdapter) getListView().getAdapter()).notifyDataSetChanged();
    }

    public void onCancel(DialogInterface dialog) {
      onClick(dialog, 0);
    }
  }

  private class SingleDiscriminatorClickedListener implements DialogInterface.OnClickListener {
    private final ContactData contactData;

    public SingleDiscriminatorClickedListener(ContactData contactData) {
      this.contactData = contactData;
    }

    public void onClick(DialogInterface dialog, int which) {
      ContactData singlePhoneContact = new ContactData(contactData.id,
                                                       contactData.name,
                                                       Collections.singletonList(contactData.numbers.get(which)));
      addSingleNumberContact(singlePhoneContact);
    }
  }

  private class MultiDiscriminatorClickedListener implements DialogInterface.OnMultiChoiceClickListener {
    private final ContactData contactData;

    public MultiDiscriminatorClickedListener(ContactData contactData) {
      this.contactData = contactData;
    }

    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
      Log.w("ContactSelectionListActivity", "Got checked: " + isChecked);

      ContactData existing = selectedContacts.get(contactData.id);

      if (existing == null) {
        Log.w("ContactSelectionListActivity", "No existing contact data, creating...");

        if (!isChecked)
          throw new AssertionError("We shouldn't be unchecking data that doesn't exist.");

        existing = new ContactData(contactData.id, contactData.name);
        selectedContacts.put(existing.id, existing);
      }

      NumberData selectedData = contactData.numbers.get(which);

      if (!isChecked) existing.numbers.remove(selectedData);
      else existing.numbers.add(selectedData);
    }
  }

  private class LoadCursorAsyncTask extends AsyncTask<Void, Void, Cursor> {

    @Override
    protected Cursor doInBackground(Void... voids) {
      final Cursor pushCursor;
      final Cursor contactsWithNumbers;

      if (TextSecurePreferences.isSmsNonDataOutEnabled(getActivity())) {
        contactsWithNumbers = ContactAccessor.getInstance().getCursorForContactsWithNumbers(getActivity());
      } else {
        contactsWithNumbers = null;
      }

      if (TextSecurePreferences.isPushRegistered(getActivity())) {
        pushCursor = ContactAccessor.getInstance().getCursorForContactsWithPush(getActivity());
      } else {
        pushCursor = null;
      }

      if (pushCursor != null && contactsWithNumbers != null) return new MergeCursor(new Cursor[]{pushCursor, contactsWithNumbers});
      else if (pushCursor != null)                           return pushCursor;
      else if (contactsWithNumbers != null)                  return contactsWithNumbers;
      else                                                   return null;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      ((TextView)getView().findViewById(android.R.id.empty)).setText(R.string.contact_selection_group_activity__finding_contacts);
    }

    @Override
    protected void onPostExecute(Cursor cursor) {
      super.onPostExecute(cursor);
      ((TextView)getView().findViewById(android.R.id.empty)).setText(R.string.contact_selection_group_activity__no_contacts);
      ((ContactSelectionListAdapter) getListAdapter()).changeCursor(cursor);
    }
  }

  public void setOnContactSelectedListener(OnContactSelectedListener onContactSelectedListener) {
    this.onContactSelectedListener = onContactSelectedListener;
  }

  public interface OnContactSelectedListener {
    public void onContactSelected(ContactData contactData);
  }
}
