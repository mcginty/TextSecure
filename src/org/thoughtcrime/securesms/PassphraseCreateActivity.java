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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.thoughtcrime.securesms.crypto.IdentityKeyUtil;
import org.thoughtcrime.securesms.registration.RegistrationPageFragment;
import org.thoughtcrime.securesms.util.PassphraseUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.util.MemoryCleaner;
import org.thoughtcrime.securesms.util.VersionTracker;
import org.whispersystems.textsecure.util.Util;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.ButterKnife.Action;
import butterknife.InjectView;
import butterknife.InjectViews;

/**
 * Activity for creating a user's local encryption passphrase.
 *
 * @author Moxie Marlinspike
 */

public class PassphraseCreateActivity extends RegistrationPageFragment {

  @InjectView(R.id.create_layout)   LinearLayout createLayout;
  @InjectView(R.id.progress_layout) LinearLayout progressLayout;

  @InjectViews({R.id.passphrase_edit, R.id.passphrase_edit_repeat}) List<EditText> passphrases;

  static final ButterKnife.Action<View> CLEAR = new Action<View>() {
    @Override
    public void apply(View view, int index) {
      view.setEnabled(false);
    }
  };

  static final ButterKnife.Action<View> ERASE = new Action<View>() {
    @Override
    public void apply(View view, int index) {
      if (view instanceof EditText) {
        ((EditText)view).setText(null);
      }
      view = null;
    }
  };

  public PassphraseCreateActivity() { }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.create_passphrase_activity, container, false);
    ButterKnife.inject(this, view);
    return view;
  }

  private void verifyAndSavePassphrases(CompletionListener listener) {
    if (Util.isEmpty(passphrases.get(0)) || Util.isEmpty(passphrases.get(1))) {
      Toast.makeText(getActivity(), R.string.PassphraseCreateActivity_you_must_specify_a_password, Toast.LENGTH_SHORT).show();
      return;
    }

    String passphrase       = passphrases.get(0).getText().toString();
    String passphraseRepeat = passphrases.get(1).getText().toString();

    if (!passphrase.equals(passphraseRepeat)) {
      Toast.makeText(getActivity(), R.string.PassphraseCreateActivity_passphrases_dont_match, Toast.LENGTH_SHORT).show();
      ButterKnife.apply(passphrases, CLEAR);
      return;
    }

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
    ButterKnife.apply(passphrases, ERASE);
    System.gc();
  }
}
