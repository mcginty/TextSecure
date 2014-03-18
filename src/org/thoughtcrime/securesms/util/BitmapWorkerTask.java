package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.recipients.RecipientFormattingException;

import java.lang.ref.WeakReference;

public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
  private final WeakReference<ImageView> imageViewReference;
  private final Context                  context;
  public        String                   number;

  public BitmapWorkerTask(Context context, ImageView imageView) {
    // Use a WeakReference to ensure the ImageView can be garbage collected
    imageViewReference = new WeakReference<ImageView>(imageView);
    this.context = context;
  }

  // Decode image in background.
  @Override
  protected Bitmap doInBackground(String... params) {
    number = params[0];
    try {
      return RecipientFactory.getRecipientsFromString(context, number, false).getPrimaryRecipient().getContactPhoto();
    } catch (RecipientFormattingException rfe) {
      return null;
    }
  }

  @Override
  protected void onPostExecute(Bitmap bitmap) {
    if (isCancelled()) {
      bitmap = null;
    }

    if (bitmap != null) {
      final ImageView imageView = imageViewReference.get();
      final BitmapWorkerTask bitmapWorkerTask =
          getBitmapWorkerTask(imageView);
      if (this == bitmapWorkerTask && imageView != null) {
        TransitionDrawable transition = new TransitionDrawable(new Drawable[]{new ColorDrawable(Color.TRANSPARENT), new BitmapDrawable(bitmap)});
        imageView.setImageDrawable(transition);
        transition.startTransition(200);
      }
    }
  }

  public static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
    if (imageView != null) {
      final Drawable drawable = imageView.getDrawable();
      if (drawable instanceof AsyncDrawable) {
        final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
        return asyncDrawable.getBitmapWorkerTask();
      }
    }
    return null;
  }

  public static class AsyncDrawable extends BitmapDrawable {
    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

    public AsyncDrawable(Resources res, Bitmap bitmap,
                         BitmapWorkerTask bitmapWorkerTask) {
      super(res, bitmap);
      bitmapWorkerTaskReference =
          new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
    }

    public BitmapWorkerTask getBitmapWorkerTask() {
      return bitmapWorkerTaskReference.get();
    }
  }

}
