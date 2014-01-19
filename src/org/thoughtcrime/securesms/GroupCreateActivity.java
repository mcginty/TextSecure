package org.thoughtcrime.securesms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import org.thoughtcrime.securesms.recipients.Recipients;
import org.thoughtcrime.securesms.util.ActionBarUtil;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;


public class GroupCreateActivity extends PassphraseRequiredSherlockFragmentActivity {

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private static final int PICK_CONTACT          = 1;

  @Override
  public void onCreate(Bundle state) {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
    super.onCreate(state);

    setContentView(R.layout.group_create_activity);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    ActionBarUtil.initializeDefaultActionBar(this, getSupportActionBar(), "New Group");

    initializeResources();
  }

  private void initializeResources() {
    ((ImageButton)findViewById(R.id.add_people_button)).setOnClickListener(new AddRecipientButtonListener());
  }

  @Override
  public void onActivityResult(int reqCode, int resultCode, Intent data) {
    Log.w("ComposeMessageActivity", "onActivityResult called: " + resultCode + " , " + data);
    super.onActivityResult(reqCode, resultCode, data);

    if (data == null || resultCode != Activity.RESULT_OK)
      return;

    switch (reqCode) {
      case PICK_CONTACT:
        Recipients recipients = data.getParcelableExtra("recipients");

        if (recipients != null)
          Log.w("poop", "recipient chosen: " + recipients.getRecipientsList().get(0).getName());

        break;
    }
  }

  private class AddRecipientButtonListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent(GroupCreateActivity.this, ContactSelectionActivity.class);
      startActivityForResult(intent, PICK_CONTACT);
    }
  }
}
