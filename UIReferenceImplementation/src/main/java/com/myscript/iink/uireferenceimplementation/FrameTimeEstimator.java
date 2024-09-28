//<!--AI STATEMENT: AI was not used in this. This file was imported from https://github.com/MyScript/interactive-ink-examples-android
//into this Android SDK and then edited by our team -->

// Adapted from:
// https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:input/input-motionprediction/

package com.myscript.iink.uireferenceimplementation;

import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Get screen fastest refresh rate (in ms)
 */
@SuppressWarnings("deprecation")
public class FrameTimeEstimator {
  private static final float LEGACY_FRAME_TIME_MS = 16f;
  private static final float MS_IN_A_SECOND = 1000f;

  static public float getFrameTime(@NonNull Context context)
  {
    return getFastestFrameTimeMs(context);
  }

  private static Display getDisplayForContext(Context context)
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
    {
      return Api30Impl.getDisplayForContext(context);
    }
    return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
  }

  private static float getFastestFrameTimeMs(Context context)
  {
    Display defaultDisplay = getDisplayForContext(context);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    {
      return Api23Impl.getFastestFrameTimeMs(defaultDisplay);
    }
    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      return Api21Impl.getFastestFrameTimeMs(defaultDisplay);
    }
    else
    {
      return LEGACY_FRAME_TIME_MS;
    }
  }

  @SuppressWarnings("deprecation")
  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  static class Api21Impl
  {
    private Api21Impl()
    {
      // Not instantiable
    }

    @DoNotInline
    static float getFastestFrameTimeMs(Display display)
    {
      float[] refreshRates = display.getSupportedRefreshRates();
      float largestRefreshRate = refreshRates[0];

      for (int c = 1; c < refreshRates.length; c++)
      {
        if (refreshRates[c] > largestRefreshRate)
          largestRefreshRate = refreshRates[c];
      }

      return MS_IN_A_SECOND / largestRefreshRate;
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  static class Api23Impl
  {
    private Api23Impl()
    {
      // Not instantiable
    }

    @DoNotInline
    static float getFastestFrameTimeMs(Display display)
    {
      Display.Mode[] displayModes = display.getSupportedModes();
      float largestRefreshRate = displayModes[0].getRefreshRate();

      for (int c = 1; c < displayModes.length; c++)
      {
        float currentRefreshRate = displayModes[c].getRefreshRate();
        if (currentRefreshRate > largestRefreshRate)
          largestRefreshRate = currentRefreshRate;
      }

      return MS_IN_A_SECOND / largestRefreshRate;
    }
  }

  @RequiresApi(Build.VERSION_CODES.R)
  static class Api30Impl
  {
    private Api30Impl()
    {
      // Not instantiable
    }

    @DoNotInline
    static Display getDisplayForContext(Context context)
    {
      return context.getDisplay();
    }
  }
}
