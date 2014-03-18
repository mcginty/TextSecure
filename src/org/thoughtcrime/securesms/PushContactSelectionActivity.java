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

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.thoughtcrime.securesms.components.SingleRecipientPanel;
import org.thoughtcrime.securesms.contacts.ContactAccessor;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.recipients.RecipientFormattingException;
import org.thoughtcrime.securesms.recipients.Recipients;
import org.thoughtcrime.securesms.util.ActionBarUtil;
import org.thoughtcrime.securesms.util.DirectoryHelper;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.NumberUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.textsecure.crypto.MasterSecret;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import static org.thoughtcrime.securesms.contacts.ContactAccessor.ContactData;
import static org.thoughtcrime.securesms.contacts.ContactAccessor.getInstance;

/**
 * Activity container for selecting a list of contacts.
 *
 * @author Moxie Marlinspike
 *
 */
public class PushContactSelectionActivity extends PassphraseRequiredSherlockFragmentActivity {
  private final static String TAG                 = "PushContactSelectionActivity";
  public  final static  String MASTER_SECRET_EXTRA = "master_secret";
  public  final static  String MULTI_SELECT_EXTRA  = "multi_select";

  private final DynamicTheme dynamicTheme = new DynamicTheme();
  private       MasterSecret masterSecret;
  private       boolean      multi;

  private SingleRecipientPanel             recipientsPanel;
  private PushContactSelectionListFragment contactsFragment;

  @Override
  protected void onCreate(Bundle icicle) {
    dynamicTheme.onCreate(this);
    super.onCreate(icicle);

    final ActionBar actionBar = this.getSupportActionBar();
    ActionBarUtil.initializeDefaultActionBar(this, actionBar);
    actionBar.setDisplayHomeAsUpEnabled(true);

    setContentView(R.layout.push_contact_selection_activity);
    initializeResources();
  }

  private void initializeResources() {
    recipientsPanel  = (SingleRecipientPanel)             findViewById(R.id.recipients);
    contactsFragment = (PushContactSelectionListFragment) getSupportFragmentManager().findFragmentById(R.id.contact_selection_list_fragment);
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);

    multi        = getIntent().getBooleanExtra(MULTI_SELECT_EXTRA, false);
    masterSecret = getIntent().getParcelableExtra(MASTER_SECRET_EXTRA);

    if (multi) {
      findViewById(R.id.recipients).setVisibility(View.GONE);
    } else {
      findViewById(R.id.recipients).setVisibility(View.VISIBLE);
      contactsFragment.setOnContactSelectedListener(new PushContactSelectionListFragment.OnContactSelectedListener() {
        @Override
        public void onContactSelected(ContactData contactData) {
          Log.i(TAG, "Choosing contact from list.");
          Recipients recipients = contactDataToRecipients(contactData);
          openNewConversation(recipients);
        }
      });

      recipientsPanel.setPanelChangeListener(new SingleRecipientPanel.RecipientsPanelChangedListener() {
        @Override
        public void onRecipientsPanelUpdate(Recipients recipients) {
          Log.i(TAG, "Choosing contact from autocompletion.");
          openNewConversation(recipients);
        }
      });
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getSupportMenuInflater();
    menu.clear();

    if (TextSecurePreferences.isPushRegistered(this))  inflater.inflate(R.menu.push_directory, menu);
    if (multi)                                         inflater.inflate(R.menu.contact_selection, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
    case R.id.menu_refresh_directory:  handleDirectoryRefresh();  return true;
    case R.id.menu_selection_finished: handleSelectionFinished(); return true;
    case android.R.id.home:            finish();                  return true;
    }
    return false;
  }

  private void handleSelectionFinished() {

    Intent resultIntent = getIntent();
    resultIntent.putParcelableArrayListExtra("contacts", new ArrayList<ContactData>(contactsFragment.getSelectedContacts()));

    setResult(RESULT_OK, resultIntent);

    finish();
  }

  private void handleDirectoryRefresh() {
    if (!TextSecurePreferences.isPushRegistered(this)) {
      Toast.makeText(getApplicationContext(),
                     getString(R.string.SingleContactSelectionActivity_you_are_not_registered_with_the_push_service),
                     Toast.LENGTH_LONG).show();
      return;
    }

    new AsyncTask<Void, Void, Void>() {
      private ProgressDialog progress;

      @Override
      protected void onPreExecute() {
        progress = ProgressDialog.show(PushContactSelectionActivity.this,
                                       getString(R.string.SingleContactSelectionActivity_updating_directory),
                                       getString(R.string.SingleContactSelectionActivity_updating_push_directory),
                                       true);
      }

      @Override
      protected Void doInBackground(Void... params) {
        DirectoryHelper.refreshDirectory(getApplicationContext());
        return null;
      }

      @Override
      protected void onPostExecute(Void result) {
        contactsFragment.update();
        if (progress != null)
          progress.dismiss();
      }
    }.execute();

  }


  private Recipients contactDataToRecipients(ContactData contactData) {
    if (contactData == null || contactData.numbers == null) return null;
    Recipients recipients = new Recipients(new ArrayList<Recipient>());
    for (ContactAccessor.NumberData numberData : contactData.numbers) {
      if (NumberUtil.isValidSmsOrEmailOrGroup(numberData.number)) {
        try {
          Recipients recipientsForNumber = RecipientFactory.getRecipientsFromString(PushContactSelectionActivity.this,
                                                                                    numberData.number,
                                                                                    false);
          recipients.getRecipientsList().addAll(recipientsForNumber.getRecipientsList());
        } catch (RecipientFormattingException rfe) {
          Log.w(TAG, "Caught RecipientFormattingException when trying to convert a selected number to a Recipient.", rfe);
        }
      }
    }
    return recipients;
  }

  private void openNewConversation(Recipients recipients) {
    if (recipients != null) {
      Intent intent = new Intent(this, ConversationActivity.class);
      intent.putExtra(ConversationActivity.RECIPIENTS_EXTRA, recipients.toIdString());
      intent.putExtra(ConversationActivity.MASTER_SECRET_EXTRA, masterSecret);
      long existingThread = DatabaseFactory.getThreadDatabase(this).getThreadIdIfExistsFor(recipients);
      intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, existingThread);
      intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
      startActivity(intent);
      finish();
    }
  }
}
