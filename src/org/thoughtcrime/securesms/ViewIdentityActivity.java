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
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.thoughtcrime.securesms.util.BasicEnglish;
import org.whispersystems.textsecure.crypto.IdentityKey;
import org.thoughtcrime.securesms.util.Mnemonic;
import org.whispersystems.textsecure.util.FutureTaskListener;
import org.whispersystems.textsecure.util.Hex;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Activity for displaying an identity key.
 *
 * @author Moxie Marlinspike
 */
public class ViewIdentityActivity extends KeyScanningActivity {

  private TextView    identityFingerprintMnemonic;
  private TextView    identityFingerprint;
  private IdentityKey identityKey;
  private ImageButton mnemonicHelpButton;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    setContentView(R.layout.view_identity_activity);

    initialize();
  }

  protected void initialize() {
    initializeResources();
    initializeFingerprint();
  }

  private void initializeFingerprint() {
    try {
      identityFingerprintMnemonic.setText("");

      if (identityKey == null) {
        identityFingerprintMnemonic.setText(R.string.ViewIdentityActivity_you_do_not_have_an_identity_key);
      } else {
        byte[] identityBytes = identityKey.serialize();
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] fingerprintBytes = md.digest(identityBytes);

        String mnemomic;
        try {
          mnemomic = new BasicEnglish(this).fromBytes(fingerprintBytes, 10);
        } catch (IOException ioe) {
          mnemomic = "Oops, some shit went down.";
        }

        identityFingerprintMnemonic.setText(mnemomic);

        identityFingerprint.setText(Hex.toString(fingerprintBytes));
      }
    } catch (NoSuchAlgorithmException nsae) {
      Log.w("ViewIdentityActivity", nsae);
    }
  }

  private void initializeResources() {
    this.identityKey         = getIntent().getParcelableExtra("identity_key");
    this.identityFingerprintMnemonic = (TextView)findViewById(R.id.identity_fingerprint_mnemonic);
    this.identityFingerprint = (TextView)findViewById(R.id.identity_fingerprint);
    this.mnemonicHelpButton  = (ImageButton)findViewById(R.id.mnemonic_help_button);
    String title             = getIntent().getStringExtra("title");

    final Context appContext = this;
    mnemonicHelpButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(appContext);
            builder.setTitle("Mnemonic Fingerprints");
            builder.setMessage("Mnemonic fingerprints are a representation of your unique fingerprint using specifically chosen words that make it easier to read and verify fingerprints without error.");
            builder.setPositiveButton("OK", null);
            builder.show();
        }
    });

    if (title != null) {
      getSupportActionBar().setTitle(getIntent().getStringExtra("title"));
    }
  }

  @Override
  protected String getScanString() {
    return getString(R.string.ViewIdentityActivity_scan_to_compare);
  }

  @Override
  protected String getDisplayString() {
    return getString(R.string.ViewIdentityActivity_get_scanned_to_compare);
  }

  @Override
  protected IdentityKey getIdentityKeyToCompare() {
    return identityKey;
  }

  @Override
  protected IdentityKey getIdentityKeyToDisplay() {
    return identityKey;
  }

  @Override
  protected String getNotVerifiedMessage() {
    return  getString(R.string.ViewIdentityActivity_warning_the_scanned_key_does_not_match_exclamation);
  }

  @Override
  protected String getNotVerifiedTitle() {
    return getString(R.string.ViewIdentityActivity_not_verified_exclamation);
  }

  @Override
  protected String getVerifiedMessage() {
    return getString(R.string.ViewIdentityActivity_the_scanned_key_matches_exclamation);
  }

  @Override
  protected String getVerifiedTitle() {
    return getString(R.string.ViewIdentityActivity_verified_exclamation);
  }
}
