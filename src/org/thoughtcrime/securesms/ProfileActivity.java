/**
 * Copyright (C) 2014 Open Whisper Systems
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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.protobuf.ByteString;

import org.thoughtcrime.securesms.components.PushRecipientsPanel;
import org.thoughtcrime.securesms.contacts.ContactAccessor;
import org.thoughtcrime.securesms.contacts.RecipientsEditor;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.GroupDatabase;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.mms.OutgoingGroupMediaMessage;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.recipients.RecipientFormattingException;
import org.thoughtcrime.securesms.recipients.Recipients;
import org.thoughtcrime.securesms.sms.MessageSender;
import org.thoughtcrime.securesms.util.BasicEnglish;
import org.thoughtcrime.securesms.util.BitmapUtil;
import org.thoughtcrime.securesms.util.Dialogs;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.GroupUtil;
import org.thoughtcrime.securesms.util.SelectedRecipientsAdapter;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;
import org.whispersystems.textsecure.crypto.IdentityKey;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.directory.Directory;
import org.whispersystems.textsecure.directory.NotInDirectoryException;
import org.whispersystems.textsecure.storage.Session;
import org.whispersystems.textsecure.util.Base64;
import org.whispersystems.textsecure.util.Hex;
import org.whispersystems.textsecure.util.InvalidNumberException;
import org.whispersystems.textsecure.zxing.integration.IntentIntegrator;
import org.whispersystems.textsecure.zxing.integration.IntentResult;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import ws.com.google.android.mms.MmsException;

import static org.thoughtcrime.securesms.contacts.ContactAccessor.ContactData;
import static org.whispersystems.textsecure.push.PushMessageProtos.PushMessageContent.GroupContext;

/**
 * Activity to create and update groups
 *
 * @author Jake McGinty
 */
public class ProfileActivity extends PassphraseRequiredSherlockActivity {

  private final static String TAG = ProfileActivity.class.getSimpleName();

  public static final String RECIPIENT_EXTRA     = "recipient_id";
  public static final String MASTER_SECRET_EXTRA = "master_secret";

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private Recipient recipient = null;

  private MasterSecret masterSecret;

  @InjectView(R.id.avatar)       ImageView avatar;
  @InjectView(R.id.name)         TextView  name;
  @InjectView(R.id.number)       TextView  number;
  @InjectView(R.id.fingerprint)  TextView  fingerprint;
  @InjectView(R.id.verify_scan)  Button    verifyScan;
  @InjectView(R.id.verify_call)  Button    verifyCall;
  @InjectView(R.id.verify_other) Button    verifyOther;

  @Override
  public void onCreate(Bundle state) {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
    super.onCreate(state);

    setContentView(R.layout.profile);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    initializeResources();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
    getSupportActionBar().setTitle("Profile");
  }

  private void initializeResources() {
    recipient = getIntent().getParcelableExtra(RECIPIENT_EXTRA);
    masterSecret = getIntent().getParcelableExtra(MASTER_SECRET_EXTRA);
    ButterKnife.inject(this);

    avatar.setImageBitmap(recipient.getContactPhoto());
    name.setText(recipient.getName());
    number.setText(recipient.getNumber());
    initializeFingerprint();
//    fingerprint.setText(Session.getRemoteIdentityKey(this, masterSecret, recipient).getFingerprint());
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

    if ((scanResult != null) && (scanResult.getContents() != null)) {
      String data = scanResult.getContents();

      if (data.equals(Base64.encodeBytes(Session.getRemoteIdentityKey(this, masterSecret, recipient).serialize()))) {
        Dialogs.showInfoDialog(this, "VERIFIED!", "VERIFIED!");
      } else {
        Dialogs.showAlertDialog(this, "NOT VERIFIED", "NOT VERIFIED");
      }
    } else {
      Toast.makeText(this, R.string.KeyScanningActivity_no_scanned_key_found_exclamation,
                     Toast.LENGTH_LONG).show();
    }
  }

  @OnClick(R.id.verify_scan)
  void handleVerifyScan() {
    IntentIntegrator.initiateScan(this);
  }

  private void initializeFingerprint() {
    try {
      fingerprint.setText("");
      IdentityKey identityKey = Session.getRemoteIdentityKey(this, masterSecret, recipient);
      if (identityKey == null) {
        fingerprint.setText(R.string.ViewIdentityActivity_you_do_not_have_an_identity_key);
      } else {
        byte[] identityBytes = identityKey.serialize();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] fingerprintBytes = md.digest(identityBytes);
        Log.w(TAG, "got fingerprint of " + fingerprintBytes.length + " bytes");
        String mnemomic;
        try {
          mnemomic = new BasicEnglish(this).fromBytes(fingerprintBytes, 16);
        } catch (IOException ioe) {
          mnemomic = "Oops, some shit went down.";
        }

        fingerprint.setText(mnemomic);
      }
    } catch (NoSuchAlgorithmException nsae) {
      Log.w("ViewIdentityActivity", nsae);
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getSupportMenuInflater();
    menu.clear();

    inflater.inflate(R.menu.group_create, menu);
    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
    case android.R.id.home:
      finish();
      return true;
    default:
      return false;
    }
  }
}
