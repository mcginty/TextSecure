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

import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import org.apache.http.conn.util.InetAddressUtils;
import org.thoughtcrime.securesms.components.TouchImageView;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.MediaServer;
import org.whispersystems.textsecure.crypto.MasterSecret;
import org.whispersystems.textsecure.util.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Activity for initiating/receiving key QR code scans.
 *
 * @author Moxie Marlinspike
 */
public class MediaPreviewActivity extends PassphraseRequiredSherlockActivity {
  private final static String TAG = MediaPreviewActivity.class.getSimpleName();

  public final static String MASTER_SECRET_EXTRA = "master_secret";
  public final static String MEDIA_URI_EXTRA     = "media_uri";
  public final static String MEDIA_TYPE_EXTRA    = "media_type";

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private MasterSecret   masterSecret;
  private TouchImageView image;
  private VideoView      video;
  private MediaServer    mediaServer;

  @Override
  protected void onCreate(Bundle bundle) {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                         WindowManager.LayoutParams.FLAG_FULLSCREEN);
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
    super.onCreate(bundle);
    setContentView(R.layout.media_preview_activity);
    initializeResources();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
    final Uri uri = getIntent().getData();
    final String type = getIntent().getType();
    masterSecret = getIntent().getParcelableExtra(MASTER_SECRET_EXTRA);

    try {
      final InputStream is = PartAuthority.getPartStream(this, masterSecret, uri);
      if (type != null) {
        if (type.startsWith("image/")) displayImage(is);
        else if (type.startsWith("video/")) displayVideo(is, type);
        else throw new UnsupportedOperationException("Type not supported.");
      }
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      Toast.makeText(getApplicationContext(), "Could not read the media", Toast.LENGTH_LONG).show();
      finish();
    } catch (UnsupportedOperationException uoe) {
      Log.w(TAG, uoe);
      Toast.makeText(getApplicationContext(), "Unsupported media type", Toast.LENGTH_LONG).show();
      finish();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    stopMediaServer();
  }

  final protected static char[] hexArray = "0123456789abcdef".toCharArray();
  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  private void initializeResources() {
    image = (TouchImageView) findViewById(R.id.image);
    video = (VideoView)      findViewById(R.id.video);
  }

  private void displayImage(final InputStream is) {
    image.setImageBitmap(BitmapFactory.decodeStream(is));
    image.setVisibility(View.VISIBLE);
  }

  private void displayVideo(final InputStream is, final String type) {
    final byte[] nonceBytes = new byte[16];
    final String nonce;
    try {
      SecureRandom.getInstance("SHA1PRNG").nextBytes(nonceBytes);
      nonce = Hex.toStringCondensed(nonceBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }

    startMediaServer(is, type, nonce);
    video.setVisibility(View.VISIBLE);
    video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
      }
    });
    video.setMediaController(null);
    video.setVideoPath("http://127.0.0.1:" + MediaServer.PORT + "/" + nonce);
  }

  private void saveToDisk() {
    //TODO
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);

    MenuInflater inflater = this.getSupportMenuInflater();
    inflater.inflate(R.menu.media_preview, menu);
    menu.clear();

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
    case R.id.save:        saveToDisk();    return true;
    case android.R.id.home:
      finish();
      return true;
    }

    return false;
  }

  private void startMediaServer(final InputStream is, final String type, final String nonce) {
    if (mediaServer == null) {
      mediaServer = new MediaServer("127.0.0.1", is, type, nonce);
    }
    if (!mediaServer.isAlive()) {
      try {
        mediaServer.start();
      } catch (IOException ioe) {
        Log.w(TAG, "couldn't start web server...");
      }
    }
  }

  private void stopMediaServer() {
    if (mediaServer != null && mediaServer.isAlive()) {
      mediaServer.stop();
    }
  }

  public String getLocalIpAddress() {
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress())) {
            String ipAddr = inetAddress.getHostAddress();
            return ipAddr;
          }
        }
      }
    } catch (SocketException ex) {
      Log.d(TAG, ex.toString());
    }
    return null;
  }

  public static boolean isContentTypeSupported(final String contentType) {
    return contentType.startsWith("image/") ||
           contentType.startsWith("video/");
  }
}
