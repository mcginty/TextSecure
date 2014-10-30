package org.thoughtcrime.securesms.events;

/**
 * Created by kaonashi on 11/26/14.
 */
public class TransferProgressEvent {
  public long messageId;
  public long totalBytes;
  public long transferredBytes;

  public TransferProgressEvent(long messageId, long totalBytes, long transferredBytes) {
    this.messageId = messageId;
    this.totalBytes = totalBytes;
    this.transferredBytes = transferredBytes;
  }
}
