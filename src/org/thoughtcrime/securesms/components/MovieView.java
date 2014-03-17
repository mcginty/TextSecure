package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.os.SystemClock;
import android.view.View;

import java.io.InputStream;

public class MovieView extends View {

  private Movie movie;

  private long movieStart;

  public MovieView(Context context) {
    super(context);
  }

  public void setMovie(InputStream stream) {
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