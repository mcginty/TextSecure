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
package org.thoughtcrime.securesms.mms;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.database.MmsDatabase;
import org.thoughtcrime.securesms.util.BitmapDecodingException;
import org.thoughtcrime.securesms.util.LRUCache;
import org.thoughtcrime.securesms.util.ListenableFutureTask;
import org.thoughtcrime.securesms.util.SmilUtil;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILMediaElement;
import org.w3c.dom.smil.SMILRegionElement;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import pl.droidsonroids.gif.GifDrawable;
import ws.com.google.android.mms.ContentType;
import ws.com.google.android.mms.pdu.PduPart;

public class GifSlide extends Slide {
  private static final String TAG = GifSlide.class.getSimpleName();

  private static final int MAX_CACHE_SIZE = 10;
  private static final Map<Uri, SoftReference<Drawable>> thumbnailCache =
      Collections.synchronizedMap(new LRUCache<Uri, SoftReference<Drawable>>(MAX_CACHE_SIZE));

  public GifSlide(Context context, MasterSecret masterSecret, PduPart part) {
    super(context, masterSecret, part);
  }

  public GifSlide(Context context, Uri uri) throws IOException, BitmapDecodingException {
    super(context, constructPartFromUri(uri));
  }

  @Override
  public ListenableFutureTask<Pair<Drawable,Boolean>> getThumbnail(Context context) {
    if (part.isPendingPush()) {
      return new ListenableFutureTask<>(new Pair<>(context.getResources().getDrawable(R.drawable.stat_sys_download), true));
    }

    Drawable thumbnail = getCachedThumbnail();

    if (thumbnail != null) {
      return new ListenableFutureTask<>(new Pair<>(thumbnail, true));
    }

    return resolveThumbnail(context);
  }

  private ListenableFutureTask<Pair<Drawable,Boolean>> resolveThumbnail(Context context) {
    final WeakReference<Context> weakContext = new WeakReference<>(context);

    Callable<Pair<Drawable,Boolean>> slideCallable = new Callable<Pair<Drawable, Boolean>>() {
      @Override
      public Pair<Drawable, Boolean> call() throws Exception {
        final Context context = weakContext.get();
        if (context == null) {
          Log.w(TAG, "context SoftReference was null, leaving");
          return null;
        }

        try {
          Drawable gif;
          long startDecode = System.currentTimeMillis();

          if (part.getDataUri() != null) {
            gif = new GifDrawable(new BufferedInputStream(PartAuthority.getPartStream(context, masterSecret, part.getDataUri())));
          } else {
            throw new FileNotFoundException("couldn't find gif to load");
          }

          Log.w(TAG, "thumbnail decode/generate time: " + (System.currentTimeMillis() - startDecode) + "ms");

          thumbnailCache.put(part.getDataUri(), new SoftReference<>(gif));

          return new Pair<>(gif, false);
        } catch (IOException e) {
          Log.w(TAG, e);
          return new Pair<>(context.getResources().getDrawable(R.drawable.ic_missing_thumbnail_picture), false);
        }
      }
    };
    ListenableFutureTask<Pair<Drawable,Boolean>> futureTask = new ListenableFutureTask<>(slideCallable);
    MmsDatabase.slideResolver.execute(futureTask);
    return futureTask;
  }


  private Drawable getCachedThumbnail() {
    synchronized (thumbnailCache) {
      SoftReference<Drawable> bitmapReference = thumbnailCache.get(part.getDataUri());
      Log.w(TAG, "Got soft reference: " + bitmapReference);

      if (bitmapReference != null) {
        Drawable bitmap = bitmapReference.get();
        Log.w(TAG, "Got cached bitmap: " + bitmap);
        if (bitmap != null) return bitmap;
        else                thumbnailCache.remove(part.getDataUri());
      }
    }

    return null;
  }

  @Override
  public boolean hasImage() {
    return true;
  }

  @Override
  public SMILRegionElement getSmilRegion(SMILDocument document) {
    SMILRegionElement region = (SMILRegionElement) document.createElement("region");
    region.setId("Image");
    region.setLeft(0);
    region.setTop(0);
    region.setWidth(SmilUtil.ROOT_WIDTH);
    region.setHeight(SmilUtil.ROOT_HEIGHT);
    region.setFit("meet");
    return region;
  }

  @Override
  public SMILMediaElement getMediaElement(SMILDocument document) {
    return SmilUtil.createMediaElement("img", document, new String(getPart().getName()));
  }

  private static PduPart constructPartFromUri(Uri uri)
      throws IOException, BitmapDecodingException
  {
    PduPart part = new PduPart();

    part.setDataUri(uri);
    part.setContentType(ContentType.IMAGE_GIF.getBytes());
    part.setContentId((System.currentTimeMillis()+"").getBytes());
    part.setName(("Image" + System.currentTimeMillis()).getBytes());

    return part;
  }
}
