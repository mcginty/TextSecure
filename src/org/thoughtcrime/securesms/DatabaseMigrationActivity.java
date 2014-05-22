package org.thoughtcrime.securesms;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.thoughtcrime.securesms.database.SmsMigrator.ProgressDescription;
import org.thoughtcrime.securesms.registration.RegistrationPageFragment;
import org.thoughtcrime.securesms.service.ApplicationMigrationService;
import org.thoughtcrime.securesms.service.ApplicationMigrationService.ImportState;
import org.whispersystems.textsecure.crypto.MasterSecret;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class DatabaseMigrationActivity extends RegistrationPageFragment {

  private final ImportServiceConnection serviceConnection  = new ImportServiceConnection();
  private final ImportStateHandler      importStateHandler = new ImportStateHandler();
  private final BroadcastReceiver       completedReceiver  = new NullReceiver();
  private MasterSecret masterSecret;
  private CompletionListener pendingCompletionListener = null;

  @InjectView(R.id.prompt_layout)   LinearLayout promptLayout;
  @InjectView(R.id.progress_layout) LinearLayout progressLayout;
  @InjectView(R.id.import_progress) ProgressBar  progress;
  @InjectView(R.id.import_status)   TextView     progressLabel;

  private ApplicationMigrationService importService;
  private boolean isVisible = false;

  public DatabaseMigrationActivity() { }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    final View view = inflater.inflate(R.layout.database_migration_activity, container, false);
    ButterKnife.inject(this, view);
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    initializeResources();
    initializeServiceBinding();
  }

  @Override
  public void onResume() {
    super.onResume();
    isVisible = true;
    registerForCompletedNotification();
  }

  @Override
  public void onPause() {
    super.onPause();
    isVisible = false;
    unregisterForCompletedNotification();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    shutdownServiceBinding();
  }

  private void initializeServiceBinding() {
    Intent intent = new Intent(getActivity(), ApplicationMigrationService.class);
    getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
  }

  private void initializeResources() {
    this.progressLayout.setVisibility(View.GONE);
    this.promptLayout.setVisibility(View.GONE);
  }

  private void registerForCompletedNotification() {
    IntentFilter filter = new IntentFilter();
    filter.addAction(ApplicationMigrationService.COMPLETED_ACTION);
    filter.setPriority(1000);

    getActivity().registerReceiver(completedReceiver, filter);
  }

  private void unregisterForCompletedNotification() {
    getActivity().unregisterReceiver(completedReceiver);
  }

  private void shutdownServiceBinding() {
    getActivity().unbindService(serviceConnection);
  }

  private void handleStateIdle() {
    this.promptLayout.setVisibility(View.VISIBLE);
    this.progressLayout.setVisibility(View.GONE);
  }

  private void handleStateProgress(ProgressDescription update) {
    this.promptLayout.setVisibility(View.GONE);
    this.progressLayout.setVisibility(View.VISIBLE);
    this.progressLabel.setText(update.primaryComplete + "/" + update.primaryTotal);

    double max               = this.progress.getMax();
    double primaryTotal      = update.primaryTotal;
    double primaryComplete   = update.primaryComplete;
    double secondaryTotal    = update.secondaryTotal;
    double secondaryComplete = update.secondaryComplete;

    this.progress.setProgress((int)Math.round((primaryComplete / primaryTotal) * max));
    this.progress.setSecondaryProgress((int)Math.round((secondaryComplete / secondaryTotal) * max));
  }

  @Override
  public void onFinishPage(CompletionListener listener) {
    pendingCompletionListener = listener;
    Intent intent = new Intent(getActivity(), ApplicationMigrationService.class);
    intent.setAction(ApplicationMigrationService.MIGRATE_DATABASE);
    intent.putExtra("master_secret", masterSecret);
    getActivity().startService(intent);

    promptLayout.setVisibility(View.GONE);
    progressLayout.setVisibility(View.VISIBLE);
  }

  private void handleImportComplete() {
    if (pendingCompletionListener != null) {
      pendingCompletionListener.onComplete();
    }
    pendingCompletionListener = null;
  }

  @Override
  public void onSkipPage(CompletionListener listener) {
    ApplicationMigrationService.setDatabaseImported(getActivity());
    listener.onComplete();
  }

  @Override
  public void setMasterSecret(MasterSecret masterSecret) {
    super.setMasterSecret(masterSecret);
    this.masterSecret = masterSecret;
  }

  private class ImportStateHandler extends Handler {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
      case ImportState.STATE_IDLE:                   handleStateIdle();                                     break;
      case ImportState.STATE_MIGRATING_IN_PROGRESS:  handleStateProgress((ProgressDescription)message.obj); break;
      case ImportState.STATE_MIGRATING_COMPLETE:     handleImportComplete();                                break;
      }
    }
  }

  private class ImportServiceConnection implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      importService  = ((ApplicationMigrationService.ApplicationMigrationBinder)service).getService();
      importService.setImportStateHandler(importStateHandler);

      ImportState state = importService.getState();
      importStateHandler.obtainMessage(state.state, state.progress).sendToTarget();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      importService.setImportStateHandler(null);
    }
  }

  private class NullReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      abortBroadcast();
    }
  }


}
