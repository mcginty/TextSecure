/**
 * Copyright (C) 2014 Open Whisper Systems
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
package org.whispersystems.textsecure.api.messages;

import org.whispersystems.textsecure.api.util.TransferObserver;

import java.io.InputStream;

public class TextSecureAttachmentStream extends TextSecureAttachment {

  private final InputStream      inputStream;
  private final long             length;
  private final TransferObserver transferObserver;

  public TextSecureAttachmentStream(InputStream inputStream, String contentType, long length) {
    this(inputStream, contentType, length, null);
  }

  public TextSecureAttachmentStream(InputStream inputStream, String contentType, long length, TransferObserver transferObserver) {
    super(contentType);
    this.inputStream      = inputStream;
    this.length           = length;
    this.transferObserver = transferObserver;
  }

  @Override
  public boolean isStream() {
    return true;
  }

  @Override
  public boolean isPointer() {
    return false;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public long getLength() {
    return length;
  }

  public TransferObserver getTransferObserver() {
    return transferObserver;
  }
}
