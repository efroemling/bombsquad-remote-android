package net.froemling.bsremote;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

//public class WorkerThread {
public class WorkerThread extends HandlerThread implements Callback {

    private Handler mHandler;

    public WorkerThread() {
        super("Worker");
    }

    public void doRunnable(Runnable runnable) {
        if (mHandler == null) {
            mHandler = new Handler(getLooper(), this);
        }
        Message msg = mHandler.obtainMessage(0, runnable);
        // Log.v("WTF","WTF SENDING MESSAGE");
        mHandler.sendMessage(msg);
    }

    @Override
    public boolean handleMessage(Message msg) {
        // Log.v("WTF","WTF HANDLER BEGIN");
        Runnable runnable = (Runnable) msg.obj;
        runnable.run();
        // Log.v("WTF","WTF HANDLER END");
        return true;
    }

}
