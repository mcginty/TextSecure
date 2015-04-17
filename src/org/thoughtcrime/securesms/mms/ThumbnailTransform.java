package org.thoughtcrime.securesms.mms;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

public class ThumbnailTransform extends BitmapTransformation {
  private static final String TAG = ThumbnailTransform.class.getSimpleName();

  public ThumbnailTransform(Context context) {
    super(context);
  }

  @SuppressWarnings("unused")
  public ThumbnailTransform(BitmapPool bitmapPool) {
    super(bitmapPool);
  }

  @Override
  protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
    Log.w(TAG, String.format("transform(%dx%d -> %dx%d)",
                             toTransform.getWidth(), toTransform.getHeight(),
                             outWidth, outHeight));

    if (toTransform.getWidth() < (outWidth / 2) && toTransform.getHeight() < (outHeight / 2)) {
      return toTransform;
    }

    final float inAspectRatio  = (float) toTransform.getWidth() / toTransform.getHeight();
    final float outAspectRatio = (float) outWidth / outHeight;
    Log.w(TAG, String.format("inAR: %.03f, outAR: %.03f", inAspectRatio, outAspectRatio));
    if (inAspectRatio < outAspectRatio) {
      outWidth = (int)(outHeight * inAspectRatio);
    }

    final Bitmap toReuse = pool.get(outWidth, outHeight, toTransform.getConfig() != null
                                                         ? toTransform.getConfig()
                                                         : Bitmap.Config.ARGB_8888);
    Bitmap transformed = TransformationUtils.centerCrop(toReuse, toTransform, outWidth, outHeight);
    if (toReuse != null && toReuse != transformed && !pool.put(toReuse)) {
      toReuse.recycle();
    }
    return transformed;
  }

  @Override
  public String getId() {
    return ThumbnailTransform.class.getCanonicalName();
  }
}
