package dev.li.widget;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    if (State.ownIds(c).length > 0 && State.connected(c)) State.start(c);
  }
}
