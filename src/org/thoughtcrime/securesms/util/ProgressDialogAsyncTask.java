package org.thoughtcrime.securesms.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public abstract class ProgressDialogAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
  private   final Context        context;
  protected       ProgressDialog progress;
  private   final String         title;
  private   final String         message;

  public ProgressDialogAsyncTask(Context context, String title, String message, boolean indeterminate) {
    super();
    this.context       = context;
    this.title         = title;
    this.message       = message;

    progress = new ProgressDialog(context);
    progress.setTitle(title);
    progress.setMessage(message);
    progress.setIndeterminate(indeterminate);
    if (!indeterminate) {
      progress.setMax(100);
      progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }
    progress.setCancelable(false);
  }

  public ProgressDialogAsyncTask(Context context, int title, int message, boolean indeterminate) {
    this(context, context.getString(title), context.getString(message), indeterminate);
  }

  public ProgressDialogAsyncTask(Context context, String title, String message) {
    this(context, title, message, false);
  }

  public ProgressDialogAsyncTask(Context context, int title, int message) {
    this(context, title, message, false);
  }

  @Override
  protected void onPreExecute() {
    progress.show();
  }

  @Override
  protected void onPostExecute(Result result) {
    if (progress != null) progress.dismiss();
  }
}

