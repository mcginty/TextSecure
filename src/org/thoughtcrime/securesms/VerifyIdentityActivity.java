/**
 * Copyright (C) 2011 Whisper Systems
 * Copyright (C) 2013 Open Whisper Systems
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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.commonsware.cwac.camera.PictureTransaction;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import org.thoughtcrime.securesms.components.CameraFragment;
import org.thoughtcrime.securesms.crypto.IdentityKeyParcelable;
import org.thoughtcrime.securesms.crypto.IdentityKeyUtil;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.storage.TextSecureSessionStore;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.util.Base64;
import org.thoughtcrime.securesms.util.Util;
import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.state.SessionRecord;
import org.whispersystems.libaxolotl.state.SessionStore;
import org.whispersystems.textsecure.api.push.TextSecureAddress;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for verifying identity keys.
 *
 * @author Moxie Marlinspike
 */
public class VerifyIdentityActivity extends KeyScanningActivity {
  private static final String TAG = VerifyIdentityActivity.class.getSimpleName();

  private Recipient    recipient;
  private MasterSecret masterSecret;

  private ImageView         success;
  private ImageView         qrView;
  private CameraFragment    cameraFragment;
  private MultiFormatReader qrReader;

  private ExecutorService decoderPool;

  @Override
  protected void onCreate(Bundle state, @NonNull MasterSecret masterSecret) {
    this.masterSecret = masterSecret;
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    setContentView(R.layout.verify_identity_activity);

    qrReader = new MultiFormatReader();
    qrReader.setHints(new HashMap<DecodeHintType, Object>() {{
      put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));
    }});
    initializeResources();
    initializeFingerprints();
  }

  @Override
  public void onResume() {
    super.onResume();
    getSupportActionBar().setTitle(R.string.AndroidManifest__verify_identity);
  }

  @Override
  public void onPostResume() {
    super.onPostResume();
    decoderPool = Executors.newSingleThreadExecutor();
    cameraFragment.autoFocus();
    cameraFragment.setPreviewCallback(new PreviewCallback() {
      @Override public void onPreviewFrame(final byte[] data, final Camera camera) {
        Log.w(TAG, "decoding!");
        byte[] receivedBytes = decode(data, camera.getParameters().getPreviewSize());
        if (receivedBytes != null) {
          Log.w(TAG, "got identity key QR!");
          success.setVisibility(View.VISIBLE);
          cameraFragment.setVisibility(View.INVISIBLE);
        }
      }
    });
  }

  @Override
  public void onPause() {
    super.onPause();
    decoderPool.shutdownNow();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  private @Nullable byte[] decode(final byte[] data, final Size size) {
    PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data,
                                                                   size.width, size.height,
                                                                   0, 0,
                                                                   size.width, size.height,
                                                                   false);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    Result rawResult = null;
    try {
      rawResult = qrReader.decodeWithState(bitmap);
    } catch (NotFoundException nfe) {
      Log.w(TAG, "NotFoundException");
    } finally {
      qrReader.reset();
    }
    if (rawResult != null && rawResult.getBarcodeFormat() == BarcodeFormat.QR_CODE) {
      try {
        return Base64.decode(rawResult.getText());
      } catch (IOException ioe) {
        Log.w(TAG, ioe);
        return new byte[]{};
      }
    }
    return null;
  }

  private @Nullable Bitmap encode(final byte[] data, int size) {
    BitMatrix result;
    try {
      result = new MultiFormatWriter().encode(Base64.encodeBytes(data),
                                              BarcodeFormat.QR_CODE,
                                              size, size, null);
      int width = result.getWidth();
      int height = result.getHeight();
      int[] pixels = new int[width * height];
      for (int y = 0; y < height; y++) {
        int offset = y * width;
        for (int x = 0; x < width; x++) {
          pixels[offset + x] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
        }
      }
      Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
      bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
      return bitmap;

    } catch (IllegalArgumentException | WriterException iae) {
      Log.w(TAG, iae);
      return null;
    }

  }

  private void initializeLocalIdentityKey() {
    if (!IdentityKeyUtil.hasIdentityKey(this)) {
      finish(); // XXX
      return;
    }

    qrView.setImageBitmap(encode(IdentityKeyUtil.getIdentityKey(this).getPublicKey().serialize(), 500));
  }

  private void initializeRemoteIdentityKey() {
    IdentityKeyParcelable identityKeyParcelable = getIntent().getParcelableExtra("remote_identity");
    IdentityKey identityKey           = null;

    if (identityKeyParcelable != null) {
      identityKey = identityKeyParcelable.get();
    }

    if (identityKey == null) {
      identityKey = getRemoteIdentityKey(masterSecret, recipient);
    }

//    if (identityKey == null) {
//      remoteIdentityFingerprint.setText(R.string.VerifyIdentityActivity_recipient_has_no_identity_key);
//    } else {
//      remoteIdentityFingerprint.setText(identityKey.getFingerprint());
//    }
  }

  private void initializeFingerprints() {
    initializeLocalIdentityKey();
    initializeRemoteIdentityKey();
  }

  private void initializeResources() {
//    this.localIdentityFingerprint  = (TextView)findViewById(R.id.you_read);
//    this.remoteIdentityFingerprint = (TextView)findViewById(R.id.friend_reads);
    this.recipient                 = RecipientFactory.getRecipientForId(this, this.getIntent().getLongExtra("recipient", -1), true);
    this.cameraFragment            = (CameraFragment)getSupportFragmentManager().findFragmentById(R.id.camera_fragment);
    this.success                   = (ImageView)findViewById(R.id.success);
    this.qrView                    = (ImageView)findViewById(R.id.qr_view);
    success.setImageDrawable(TextDrawable.builder().buildRound("\u2713", Color.GREEN));
  }

  @Override
  protected void initiateDisplay() {
    if (!IdentityKeyUtil.hasIdentityKey(this)) {
      Toast.makeText(this,
                     R.string.VerifyIdentityActivity_you_don_t_have_an_identity_key_exclamation,
                     Toast.LENGTH_LONG).show();
      return;
    }

    super.initiateDisplay();
  }

  @Override
  protected void initiateScan() {
    IdentityKey identityKey = getRemoteIdentityKey(masterSecret, recipient);

    if (identityKey == null) {
      Toast.makeText(this, R.string.VerifyIdentityActivity_recipient_has_no_identity_key_exclamation,
                     Toast.LENGTH_LONG).show();
    } else {
      super.initiateScan();
    }
  }

  @Override
  protected String getScanString() {
    return getString(R.string.VerifyIdentityActivity_scan_their_key_to_compare);
  }

  @Override
  protected String getDisplayString() {
    return getString(R.string.VerifyIdentityActivity_get_my_key_scanned);
  }

  @Override
  protected IdentityKey getIdentityKeyToCompare() {
    return getRemoteIdentityKey(masterSecret, recipient);
  }

  @Override
  protected IdentityKey getIdentityKeyToDisplay() {
    return IdentityKeyUtil.getIdentityKey(this);
  }

  @Override
  protected String getNotVerifiedMessage() {
    return getString(R.string.VerifyIdentityActivity_warning_the_scanned_key_does_not_match_please_check_the_fingerprint_text_carefully);
  }

  @Override
  protected String getNotVerifiedTitle() {
    return getString(R.string.VerifyIdentityActivity_not_verified_exclamation);
  }

  @Override
  protected String getVerifiedMessage() {
    return getString(R.string.VerifyIdentityActivity_their_key_is_correct_it_is_also_necessary_to_verify_your_key_with_them_as_well);
  }

  @Override
  protected String getVerifiedTitle() {
    return getString(R.string.VerifyIdentityActivity_verified_exclamation);
  }

  private IdentityKey getRemoteIdentityKey(MasterSecret masterSecret, Recipient recipient) {
    SessionStore   sessionStore   = new TextSecureSessionStore(this, masterSecret);
    AxolotlAddress axolotlAddress = new AxolotlAddress(recipient.getNumber(), TextSecureAddress.DEFAULT_DEVICE_ID);
    SessionRecord  record         = sessionStore.loadSession(axolotlAddress);

    if (record == null) {
      return null;
    }

    return record.getSessionState().getRemoteIdentityKey();
  }
}
