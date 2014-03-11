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
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.BitmapDecodingException;
import org.thoughtcrime.securesms.util.BitmapUtil;
import org.thoughtcrime.securesms.util.SmilUtil;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILMediaElement;
import org.w3c.dom.smil.SMILRegionElement;
import org.thoughtcrime.securesms.crypto.MasterSecret;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;

import ws.com.google.android.mms.ContentType;
import ws.com.google.android.mms.pdu.PduPart;

public class ImageSlide extends Slide {

  public ImageSlide(Context context, MasterSecret masterSecret, PduPart part) {
    super(context, masterSecret, part);
  }

  public ImageSlide(Context context, Uri uri) throws IOException, BitmapDecodingException {
    super(context, constructPartFromUri(uri));
  }

  @Override
  public Drawable getThumbnail(int maxWidth, int maxHeight) {
    Drawable thumbnail = getCachedThumbnail();

    if (thumbnail != null) {
      return thumbnail;
    }

    if (part.isPendingPush()) {
      return context.getResources().getDrawable(R.drawable.stat_sys_download);
    }

    if (part.getThumbnailUri() != null) {
      try {
        Log.w("ImageSlide", "Getting database cached thumbnail...");
        return new BitmapDrawable(context.getResources(),
                                  BitmapFactory.decodeStream(PartAuthority.getPartStream(context, masterSecret,
                                                                                         part.getThumbnailUri())));
      } catch (FileNotFoundException e) {
        Log.w("ImageSlide", e);
      }
    }

    try {
      InputStream measureStream = getPartDataInputStream();
      InputStream dataStream    = getPartDataInputStream();

      thumbnail = new BitmapDrawable(context.getResources(), BitmapUtil.createScaledBitmap(measureStream, dataStream, maxWidth, maxHeight));
      thumbnailCache.put(part.getDataUri(), new SoftReference<Drawable>(thumbnail));

      return thumbnail;
    } catch (FileNotFoundException e) {
      Log.w("ImageSlide", e);
      return context.getResources().getDrawable(R.drawable.ic_missing_thumbnail_picture);
    } catch (BitmapDecodingException e) {
      Log.w("ImageSlide", e);
      return context.getResources().getDrawable(R.drawable.ic_missing_thumbnail_picture);
    }
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
    part.setContentType(ContentType.IMAGE_JPEG.getBytes());
    part.setContentId((System.currentTimeMillis()+"").getBytes());
    part.setName(("Image" + System.currentTimeMillis()).getBytes());

    return part;
  }
}
