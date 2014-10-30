package org.whispersystems.textsecure.api.util;

/**
 * Interface for an observer to check progress updates
 */
public interface TransferObserver {
  public void onUpdate(long current, long total);
}
