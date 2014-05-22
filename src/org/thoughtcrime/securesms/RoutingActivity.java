package org.thoughtcrime.securesms;

import android.content.Intent;
import android.net.Uri;

import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.recipients.RecipientFormattingException;
import org.thoughtcrime.securesms.recipients.Recipients;
import org.thoughtcrime.securesms.service.ApplicationMigrationService;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.textsecure.crypto.MasterSecret;

public class RoutingActivity extends PassphraseRequiredSherlockActivity {

  private static final int STATE_REGISTER             = 1;
  private static final int STATE_PROMPT_PASSPHRASE    = 2;
  private static final int STATE_IMPORT_DATABASE      = 3;
  private static final int STATE_CONVERSATION_OR_LIST = 4;
  private static final int STATE_UPGRADE_DATABASE     = 5;

  private MasterSecret masterSecret   = null;
  private boolean      isVisible      = false;
  private boolean      canceledResult = false;
  private boolean      newIntent      = false;

  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    this.newIntent = true;
  }

  @Override
  public void onResume() {
    if (this.canceledResult && !this.newIntent) {
      finish();
    }

    this.newIntent      = false;
    this.canceledResult = false;
    this.isVisible      = true;
    super.onResume();
  }

  @Override
  public void onPause() {
    this.isVisible = false;
    super.onPause();
  }

  @Override
  public void onNewMasterSecret(MasterSecret masterSecret) {
    this.masterSecret = masterSecret;

    if (isVisible) {
      routeApplicationState();
    }
  }

  @Override
  public void onMasterSecretCleared() {
    this.masterSecret = null;

    if (isVisible) {
      routeApplicationState();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED) {
      canceledResult = true;
    }
  }

  private void routeApplicationState() {
    int state = getApplicationState();

    switch (state) {
    case STATE_PROMPT_PASSPHRASE:        handlePromptPassphrase();          break;
    case STATE_IMPORT_DATABASE:          handleImportDatabase();            break;
    case STATE_CONVERSATION_OR_LIST:     handleDisplayConversationOrList(); break;
    case STATE_UPGRADE_DATABASE:         handleUpgradeDatabase();           break;
    case STATE_REGISTER:                 handleRegistration();              break;
    }
  }

  private void handleCreatePassphrase() {
    Intent intent = new Intent(this, PassphraseCreateActivity.class);
    startActivityForResult(intent, 1);
  }

  private void handlePromptPassphrase() {
    Intent intent = new Intent(this, PassphrasePromptActivity.class);
    startActivityForResult(intent, 2);
  }

  private void handleImportDatabase() {
    Intent intent = new Intent(this, DatabaseMigrationActivity.class);
    intent.putExtra("master_secret", masterSecret);
    intent.putExtra("next_intent", getPushRegistrationIntent());

    startActivity(intent);
    finish();
  }

  private void handleUpgradeDatabase() {
    Intent intent = new Intent(this, DatabaseUpgradeActivity.class);
    intent.putExtra("master_secret", masterSecret);
    intent.putExtra("next_intent", TextSecurePreferences.hasPromptedPushRegistration(this) ?
                                   getConversationListIntent() : getPushRegistrationIntent());

    startActivity(intent);
    finish();
  }

  private void handleRegistration() {
    Intent intent = getPushRegistrationIntent();
    intent.putExtra("next_intent", getConversationListIntent());
    startActivity(intent);
    finish();
  }

  private void handleDisplayConversationOrList() {
    final ConversationParameters parameters = getConversationParameters();

    final Intent intent;
    if (isShareAction()) {
      intent = getShareIntent(parameters);
    } else if (parameters.recipients != null) {
      intent = getConversationIntent(parameters);
    } else {
      intent = getConversationListIntent();
    }
    startActivity(intent);
    finish();
  }

  private Intent getConversationIntent(ConversationParameters parameters) {
    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(ConversationActivity.RECIPIENTS_EXTRA, parameters.recipients != null ? parameters.recipients.toIdString() : "");
    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, parameters.thread);
    intent.putExtra(ConversationActivity.MASTER_SECRET_EXTRA, masterSecret);
    intent.putExtra(ConversationActivity.DRAFT_TEXT_EXTRA, parameters.draftText);
    intent.putExtra(ConversationActivity.DRAFT_IMAGE_EXTRA, parameters.draftImage);
    intent.putExtra(ConversationActivity.DRAFT_AUDIO_EXTRA, parameters.draftAudio);
    intent.putExtra(ConversationActivity.DRAFT_VIDEO_EXTRA, parameters.draftVideo);

    return intent;
  }

  private Intent getShareIntent(ConversationParameters parameters) {
    Intent intent = new Intent(this, ShareActivity.class);
    intent.putExtra("master_secret", masterSecret);

    if (parameters != null) {
      intent.putExtra(ConversationActivity.DRAFT_TEXT_EXTRA, parameters.draftText);
      intent.putExtra(ConversationActivity.DRAFT_IMAGE_EXTRA, parameters.draftImage);
      intent.putExtra(ConversationActivity.DRAFT_AUDIO_EXTRA, parameters.draftAudio);
      intent.putExtra(ConversationActivity.DRAFT_VIDEO_EXTRA, parameters.draftVideo);
    }

    return intent;
  }

  private Intent getConversationListIntent() {
    Intent intent = new Intent(this, ConversationListActivity.class);
    intent.putExtra("master_secret", masterSecret);

    return intent;
  }

  private Intent getPushRegistrationIntent() {
    Intent intent = new Intent(this, RegistrationActivity.class);
    intent.putExtra("master_secret", masterSecret);

    return intent;
  }

  private int getApplicationState() {
    if (!MasterSecretUtil.isPassphraseInitialized(this))
      return STATE_REGISTER;

    if (masterSecret == null)
      return STATE_PROMPT_PASSPHRASE;

//    if (!ApplicationMigrationService.isDatabaseImported(this))
//      return STATE_IMPORT_DATABASE;

    if (DatabaseUpgradeActivity.isUpdate(this))
      return STATE_UPGRADE_DATABASE;

    if (!TextSecurePreferences.isRegistrationComplete(this))
      return STATE_REGISTER;

    return STATE_CONVERSATION_OR_LIST;
  }

  private ConversationParameters getConversationParameters() {
    if (isSendAction()) {
      return getConversationParametersForSendAction();
    } else if (isShareAction()) {
      return getConversationParametersForShareAction();
    } else {
      return getConversationParametersForInternalAction();
    }
  }

  private ConversationParameters getConversationParametersForSendAction() {
    Recipients recipients;
    long       threadId = getIntent().getLongExtra("thread_id", -1);

    try {
      String data = getIntent().getData().getSchemeSpecificPart();
      recipients = RecipientFactory.getRecipientsFromString(this, data, false);
      threadId   = DatabaseFactory.getThreadDatabase(this).getThreadIdIfExistsFor(recipients);
    } catch (RecipientFormattingException rfe) {
      recipients = null;
    }

    return new ConversationParameters(threadId, recipients, null, null, null, null);
  }

  private ConversationParameters getConversationParametersForShareAction() {
    String type      = getIntent().getType();
    String draftText = null;
    Uri draftImage   = null;
    Uri draftAudio   = null;
    Uri draftVideo   = null;

    if ("text/plain".equals(type)) {
      draftText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
    } else if (type != null && type.startsWith("image/")) {
      draftImage = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
    } else if (type != null && type.startsWith("audio/")) {
      draftAudio = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
    } else if (type != null && type.startsWith("video/")) {
      draftVideo = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
    }

    return new ConversationParameters(-1, null, draftText, draftImage, draftAudio, draftVideo);
  }

  private ConversationParameters getConversationParametersForInternalAction() {
    long threadId         = getIntent().getLongExtra("thread_id", -1);
    Recipients recipients = getIntent().getParcelableExtra("recipients");

    return new ConversationParameters(threadId, recipients, null, null, null, null);
  }

  private boolean isShareAction() {
    return Intent.ACTION_SEND.equals(getIntent().getAction());
  }

  private boolean isSendAction() {
    return Intent.ACTION_SENDTO.equals(getIntent().getAction());
  }

  private static class ConversationParameters {
    public final long       thread;
    public final Recipients recipients;
    public final String     draftText;
    public final Uri        draftImage;
    public final Uri        draftAudio;
    public final Uri        draftVideo;

    public ConversationParameters(long thread, Recipients recipients,
                                  String draftText, Uri draftImage, Uri draftAudio, Uri draftVideo)
    {
     this.thread     = thread;
     this.recipients = recipients;
     this.draftText  = draftText;
     this.draftImage = draftImage;
     this.draftAudio = draftAudio;
     this.draftVideo = draftVideo;
    }
  }

}
