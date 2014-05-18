package org.thoughtcrime.securesms.registration;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragment;

public abstract class RegistrationPageFragment extends SherlockFragment {
  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
  }

  public abstract void onFinishPage(CompletionListener listener);
  public abstract void onSkipPage(CompletionListener listener);

  public interface CompletionListener {
    public void onComplete();
    public void onCancel();
  }
}
