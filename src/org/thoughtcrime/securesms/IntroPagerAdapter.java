package org.thoughtcrime.securesms;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class IntroPagerAdapter extends FragmentStatePagerAdapter {

  private static final int[] drawables = new int[] {
      R.drawable.splash_logo,
      R.drawable.message_splash,
      R.drawable.splash_logo
  };

  private static final int[] texts = new int[] {
      R.string.IntroFragment_got_with_it,
      R.string.IntroFragment_security_by_coconuts,
      R.string.IntroFragment_lorem_mango
  };

  public IntroPagerAdapter(FragmentManager fm) {
    super(fm);
  }

  @Override
  public Fragment getItem(int i) {
    return IntroFragment.newInstance(drawables[i], texts[i]);
  }

  @Override
  public int getCount() {
    return 3;
  }
}
