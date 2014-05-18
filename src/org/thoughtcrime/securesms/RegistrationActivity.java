package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.thoughtcrime.securesms.components.StepPagerStrip;
import org.thoughtcrime.securesms.registration.RegistrationPageFragment;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.textsecure.crypto.MasterSecret;


public class RegistrationActivity extends SherlockFragmentActivity {
  private static final String TAG = RegistrationActivity.class.getSimpleName();

  private static final int STATE_CREATE_PASSPHRASE = 0;
  private static final int STATE_IMPORT_MESSAGES   = 1;
  private static final int STATE_PUSH_REGISTER     = 2;

  private MasterSecret masterSecret;

  private StepPagerStrip strip;
  private Button         nextButton;
  private Button         skipButton;

  private int state = 0;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.registration_activity);

    getSupportActionBar().setTitle(getString(R.string.RegistrationActivity_connect_with_textsecure));
    initializeResources();
  }

  private void initializeResources() {
    strip = (StepPagerStrip) findViewById(R.id.step_pager_strip);
    nextButton = (Button) findViewById(R.id.button_next);
    skipButton = (Button) findViewById(R.id.button_previous);

    state = TextSecurePreferences.getRegistrationState(this);
    syncRegistrationState();

    strip.setPageCount(4);

    skipButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        RegistrationPageFragment currentPage = (RegistrationPageFragment) getSupportFragmentManager().findFragmentByTag("current");
        nextButton.setEnabled(false);
        currentPage.onSkipPage(new RegistrationPageFragment.CompletionListener() {
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
    });

    nextButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        RegistrationPageFragment currentPage = (RegistrationPageFragment)getSupportFragmentManager().findFragmentByTag("current");
        nextButton.setEnabled(false);
        currentPage.onFinishPage(new RegistrationPageFragment.CompletionListener() {
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
    });
  }

  private void stepState() {
    state += 1;
    syncRegistrationState();
  }

  private void syncRegistrationState() {
    switch (state) {
    case STATE_CREATE_PASSPHRASE: setFragment(new PassphraseCreateActivity()); break; // XXX
    case STATE_IMPORT_MESSAGES:   setFragment(new PushRegistrationFragment()); break; // XXX
    case STATE_PUSH_REGISTER:     setFragment(new PushRegistrationFragment()); break;
    }
    strip.setCurrentPage(state);
    TextSecurePreferences.setRegistrationState(this, state);
  }

  private void setFragment(final Fragment fragment) {
    final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.fragment_view, fragment, "current");
    transaction.commit();
  }
}
