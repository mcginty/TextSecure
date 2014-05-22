package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Button;

import org.thoughtcrime.securesms.components.StepPagerStrip;
import org.thoughtcrime.securesms.registration.RegistrationPageFragment;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.textsecure.crypto.MasterSecret;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class RegistrationActivity extends PassphraseAwareSherlockFragmentActivity {
  private static final String TAG = RegistrationActivity.class.getSimpleName();

  private static final int TOTAL_STATES            = 3;
  private static final int STATE_CREATE_PASSPHRASE = 0;
  private static final int STATE_IMPORT_MESSAGES   = 1;
  private static final int STATE_PUSH_REGISTER     = 2;

  @InjectView(R.id.step_pager_strip) StepPagerStrip strip;
  @InjectView(R.id.button_next)      Button         nextButton;

  private int state = 0;
  private MasterSecret masterSecret;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.registration_activity);

    getSupportActionBar().setTitle(getString(R.string.RegistrationActivity_connect_with_textsecure));
    initializeResources();
  }

  @Override
  public void onMasterSecretCleared() {
    super.onMasterSecretCleared();
    this.masterSecret = null;
    getCurrentPage().setMasterSecret(null);
    if (state != STATE_CREATE_PASSPHRASE) {
      finish();
    }
  }

  @Override
  public void onNewMasterSecret(MasterSecret masterSecret) {
    super.onNewMasterSecret(masterSecret);
    this.masterSecret = masterSecret;
    getCurrentPage().setMasterSecret(masterSecret);
  }

  @OnClick(R.id.button_next)
  public void nextPage(Button button) {

    nextButton.setEnabled(false);

    getCurrentPage().onFinishPage(new RegistrationPageFragment.CompletionListener() {
      @Override
      public void onComplete() {
        Log.w(TAG, "got onComplete, stepping state.");
        stepState();
        nextButton.setEnabled(true);
      }

      @Override
      public void onCancel() {
        nextButton.setEnabled(true);
      }
    });
  }

  @OnClick(R.id.button_previous)
  public void skipPage(Button button) {
    nextButton.setEnabled(false);

    getCurrentPage().onSkipPage(new RegistrationPageFragment.CompletionListener() {
      @Override
      public void onComplete() {
        stepState();
        nextButton.setEnabled(true);
      }

      @Override
      public void onCancel() {
        nextButton.setEnabled(true);
      }
    });
  }

  private RegistrationPageFragment getCurrentPage() {
    return (RegistrationPageFragment) getSupportFragmentManager().findFragmentByTag("current");
  }
  private void initializeResources() {
    ButterKnife.inject(this);
    state = TextSecurePreferences.getRegistrationState(this);
    syncRegistrationState();

    strip.setPageCount(TOTAL_STATES);
  }

  private void stepState() {
    state += 1;
    syncRegistrationState();
  }

  private void syncRegistrationState() {
    if (state == TOTAL_STATES) {
      TextSecurePreferences.setRegistrationComplete(this, true);
      startActivity(new Intent(this, RoutingActivity.class));
      finish();
      return;
    }
    switch (state) {
    case STATE_CREATE_PASSPHRASE: setFragment(new PassphraseCreateActivity());  break;
    case STATE_IMPORT_MESSAGES:   setFragment(new DatabaseMigrationActivity()); break;
    case STATE_PUSH_REGISTER:     setFragment(new PushRegistrationFragment());  break;
    }
    strip.setCurrentPage(state);
    TextSecurePreferences.setRegistrationState(this, state);
  }

  private void setFragment(final RegistrationPageFragment fragment) {
    final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.fragment_view, fragment, "current");
    transaction.commit();
    fragment.setMasterSecret(masterSecret);
  }
}
