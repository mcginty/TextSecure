package org.thoughtcrime.securesms.registration;

import com.actionbarsherlock.app.SherlockFragment;

import org.whispersystems.textsecure.crypto.MasterSecret;

public abstract class RegistrationPageFragment extends SherlockFragment {

  public abstract void onFinishPage(CompletionListener listener);
  public abstract void onSkipPage(CompletionListener listener);
  public void setMasterSecret(MasterSecret masterSecret) { }

  public interface CompletionListener {
    public void onComplete();
    public void onCancel();
  }
}
