/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.mms;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.BitmapDecodingException;

import java.io.IOException;

public class AttachmentManager {

  private final ConversationActivity conversation;
  private final View                 attachmentView;
  private final ImageView            thumbnail;
  private final Button               removeButton;
  private final SlideDeck            slideDeck;

  public AttachmentManager(ConversationActivity conversation) {
    this.attachmentView =             conversation.findViewById(R.id.attachment_editor);
    this.thumbnail      = (ImageView) conversation.findViewById(R.id.attachment_thumbnail);
    this.removeButton   = (Button)    conversation.findViewById(R.id.remove_image_button);
    this.slideDeck      = new SlideDeck();
    this.conversation   = conversation;

    this.removeButton.setOnClickListener(new RemoveButtonListener());
  }

  public void clear() {
    slideDeck.clear();
    attachmentView.setVisibility(View.GONE);
    conversation.updateSendButtonState();
  }

  private void show() {
    attachmentView.setVisibility(View.VISIBLE);
    conversation.updateSendButtonState();
  }

  public void setImage(Uri image) throws IOException, BitmapDecodingException {
    ImageSlide slide = new ImageSlide(conversation, image);
    slideDeck.addSlide(slide);
    thumbnail.setImageDrawable(slide.getThumbnail(345, 261));
    show();
  }

  public void setVideo(Uri video) throws IOException, MediaTooLargeException {
    VideoSlide slide = new VideoSlide(conversation, video);
    slideDeck.addSlide(slide);
    thumbnail.setImageDrawable(slide.getThumbnail(thumbnail.getWidth(), thumbnail.getHeight()));
    show();
  }

  public void setAudio(Uri audio)throws IOException, MediaTooLargeException {
    AudioSlide slide = new AudioSlide(conversation, audio);
    slideDeck.addSlide(slide);
    thumbnail.setImageDrawable(slide.getThumbnail(thumbnail.getWidth(), thumbnail.getHeight()));
    show();
  }

  public boolean isAttachmentPresent() {
    return attachmentView.getVisibility() == View.VISIBLE;
  }

  public SlideDeck getSlideDeck() {
    return slideDeck;
  }

  public static void selectVideo(Activity activity, int requestCode) {
    selectMediaType(activity, "video/*", requestCode);
  }

  public static void selectImage(Activity activity, int requestCode) {
    selectMediaType(activity, "image/*", requestCode);
  }

  public static void selectAudio(Activity activity, int requestCode) {
    selectMediaType(activity, "audio/*", requestCode);
  }

  private static void selectMediaType(Activity activity, String type, int requestCode) {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType(type);
    activity.startActivityForResult(intent, requestCode);
  }

  private class RemoveButtonListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      clear();
    }
  }

}
