package org.thoughtcrime.securesms.components;

/***
 Copyright (c) 2013 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import android.annotation.TargetApi;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.commonsware.cwac.camera.CameraHost;
import com.commonsware.cwac.camera.SimpleCameraHost;

/**
 * Primary class for using `CameraView` as a fragment. Just
 * add this as a fragment, no different than any other
 * fragment that you might use, and it will handle the
 * camera preview, plus give you controls to take pictures,
 * etc.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class CameraFragment extends Fragment {
  private CameraView cameraView = null;
  private CameraHost host       = null;

  /*
   * (non-Javadoc)
   *
   * @see android.app.Fragment#onCreateView(android.view.
   * LayoutInflater, android.view.ViewGroup,
   * android.os.Bundle)
   */
  @Override
  public View onCreateView(LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState)
  {
    cameraView = new CameraView(getActivity());
    cameraView.setHost(getHost());

    return (cameraView);
  }

  /*
   * (non-Javadoc)
   *
   * @see android.app.Fragment#onResume()
   */
  @Override
  public void onResume() {
    super.onResume();

    cameraView.onResume();
  }

  /*
   * (non-Javadoc)
   *
   * @see android.app.Fragment#onPause()
   */
  @Override
  public void onPause() {
    cameraView.setPreviewCallback(null);
    cameraView.onPause();

    super.onPause();
  }

  /**
   * @return the CameraHost instance you want to use for
   *         this fragment, where the default is an instance
   *         of the stock SimpleCameraHost.
   */
  public CameraHost getHost() {
    if (host == null) {
      host=new SimpleCameraHost(getActivity());
    }

    return(host);
  }

  /**
   * Call this (or override getHost()) to supply the
   * CameraHost used for most of the detailed interaction
   * with the camera.
   *
   * @param host
   *          a CameraHost instance, such as a subclass of
   *          SimpleCameraHost
   */
  public void setHost(CameraHost host) {
    this.host=host;
  }

  /**
   * @return the orientation of the screen, in degrees
   *         (0-360)
   */
  public int getDisplayOrientation() {
    return(cameraView.getDisplayOrientation());
  }

  /**
   * Call this to lock the camera to landscape mode (with a
   * parameter of true), regardless of what the actual
   * screen orientation is.
   *
   * @param enable
   *          true to lock the camera to landscape, false to
   *          allow normal rotation
   */
  public void lockToLandscape(boolean enable) {
    cameraView.lockToLandscape(enable);
  }

  /**
   * Call this to begin an auto-focus operation (e.g., in
   * response to the user tapping something to focus the
   * camera).
   */
  public void autoFocus() {
    cameraView.autoFocus();
  }

  /**
   * Call this to cancel an auto-focus operation that had
   * been started via a call to autoFocus().
   */
  public void cancelAutoFocus() {
    cameraView.cancelAutoFocus();
  }

  /**
   * @return true if auto-focus is an option on this device,
   *         false otherwise
   */
  public boolean isAutoFocusAvailable() {
    return(cameraView.isAutoFocusAvailable());
  }

  /**
   * If you are in single-shot mode and are done processing
   * a previous picture, call this to restart the camera
   * preview.
   */
  public void restartPreview() {
    cameraView.restartPreview();
  }

  public void setVisibility(int visibility) {
    if (getView() != null) getView().setVisibility(visibility);
  }

  /**
   * @return the name of the current flash mode, as reported
   *         by Camera.Parameters
   */
  public String getFlashMode() {
    return(cameraView.getFlashMode());
  }

  public void setFlashMode(String mode) {
    cameraView.setFlashMode(mode);
  }

  public void setPreviewCallback(PreviewCallback callback) {
    cameraView.setPreviewCallback(callback);
  }
}
