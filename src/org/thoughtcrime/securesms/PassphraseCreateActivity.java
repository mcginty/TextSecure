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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import org.thoughtcrime.securesms.crypto.IdentityKeyUtil;
import org.thoughtcrime.securesms.registration.RegistrationPageFragment;
import org.thoughtcrime.securesms.util.PassphraseUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.util.MemoryCleaner;
import org.thoughtcrime.securesms.util.VersionTracker;
import org.whispersystems.textsecure.util.Util;

/**
 * Activity for creating a user's local encryption passphrase.
 *
 * @author Moxie Marlinspike
 */

public class PassphraseCreateActivity extends RegistrationPageFragment {

  private LinearLayout createLayout;
  private LinearLayout progressLayout;

  private EditText passphraseEdit;
  private EditText passphraseRepeatEdit;
  private Button   okButton;
  private Button   skipButton;

  public PassphraseCreateActivity() { }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.create_passphrase_activity, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    initializeResources();
  }

  private void initializeResources() {
    this.createLayout         = (LinearLayout)getView().findViewById(R.id.create_layout);
    this.progressLayout       = (LinearLayout)getView().findViewById(R.id.progress_layout);
    this.passphraseEdit       = (EditText)    getView().findViewById(R.id.passphrase_edit);
    this.passphraseRepeatEdit = (EditText)    getView().findViewById(R.id.passphrase_edit_repeat);
    this.okButton             = (Button)      getView().findViewById(R.id.ok_button);
    this.skipButton           = (Button)      getView().findViewById(R.id.skip_button);
  }

  private void verifyAndSavePassphrases(CompletionListener listener) {
    if (Util.isEmpty(this.passphraseEdit) || Util.isEmpty(this.passphraseRepeatEdit)) {
      Toast.makeText(getActivity(), R.string.PassphraseCreateActivity_you_must_specify_a_password, Toast.LENGTH_SHORT).show();
      return;
    }

    String passphrase       = this.passphraseEdit.getText().toString();
    String passphraseRepeat = this.passphraseRepeatEdit.getText().toString();

    if (!passphrase.equals(passphraseRepeat)) {
      Toast.makeText(getActivity(), R.string.PassphraseCreateActivity_passphrases_dont_match, Toast.LENGTH_SHORT).show();
      this.passphraseEdit.setText("");
      this.passphraseRepeatEdit.setText("");
      return;
    }

    // We do this, but the edit boxes are basically impossible to clean up.
    MemoryCleaner.clean(passphraseRepeat);
    new SecretGenerator(getActivity(), listener).execute(passphrase);
  }

  private void disablePassphrase(CompletionListener listener) {
    TextSecurePreferences.setPasswordDisabled(getActivity(), true);
    new SecretGenerator(getActivity(), listener).execute(MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
  }

  @Override
  public void onFinishPage(CompletionListener listener) {
    verifyAndSavePassphrases(listener);
  }

  @Override
  public void onSkipPage(CompletionListener listener) {
    disablePassphrase(listener);
  }

  private class SecretGenerator extends AsyncTask<String, Void, Void> {
    private       MasterSecret       masterSecret;
    private final Context            context;
    private final CompletionListener listener;

    public SecretGenerator(Context context, CompletionListener listener) {
      this.context  = context;
      this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
      createLayout.setVisibility(View.GONE);
      progressLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected Void doInBackground(String... params) {
      String passphrase = params[0];
      masterSecret      = MasterSecretUtil.generateMasterSecret(context,
                                                                passphrase);

      // We do this, but the edit boxes are basically impossible to clean up.
      MemoryCleaner.clean(passphrase);

      MasterSecretUtil.generateAsymmetricMasterSecret(context, masterSecret);
      IdentityKeyUtil.generateIdentityKeys(context, masterSecret);
      VersionTracker.updateLastSeenVersion(context);

      return null;
    }

    @Override
    protected void onPostExecute(Void param) {
      PassphraseUtil.setMasterSecret(context, masterSecret, new PassphraseUtil.SetMasterSecretListener() {
        @Override
        public void onSuccess() {
          cleanup();
        }
      });
      listener.onComplete();
    }
  }

  private void cleanup() {
    this.passphraseEdit       = null;
    this.passphraseRepeatEdit = null;
    System.gc();
  }
}
