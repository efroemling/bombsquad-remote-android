package net.froemling.bsremote;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;

public class WorkerThread extends HandlerThread implements Callback {

  private Handler mHandler;

  WorkerThread() {
    super("Worker");
  }

  void doRunnable(Runnable runnable) {
    if (mHandler == null) {
      mHandler = new Handler(getLooper(), this);
    }
    Message msg = mHandler.obtainMessage(0, runnable);
    mHandler.sendMessage(msg);
  }

  @Override
  public boolean handleMessage(Message msg) {
    Runnable runnable = (Runnable) msg.obj;
    runnable.run();
    return true;
  }

}
