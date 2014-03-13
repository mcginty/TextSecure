package org.thoughtcrime.securesms.components;

import java.io.InputStream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.os.SystemClock;
import android.view.View;

/*
 * from https://code.google.com/p/animated-gifs-in-android/source/browse/trunk/AnimatedGifs/src/eu/andlabs/tutorial/animatedgifs/views/GifView.java
 */
public class GifView extends View {

  private Movie movie;

  private long movieStart;

  public GifView(Context context, InputStream stream) {
    super(context);

    movie = Movie.decodeStream(stream);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    canvas.drawColor(Color.TRANSPARENT);
    super.onDraw(canvas);
    final long now = SystemClock.uptimeMillis();

    if (movieStart == 0) {
      movieStart = now;
    }

    final int relTime = (int)((now - movieStart) % movie.duration());
    movie.setTime(relTime);
    movie.draw(canvas, 10, 10);
    this.invalidate();
  }
}