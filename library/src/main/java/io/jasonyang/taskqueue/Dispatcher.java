package io.jasonyang.taskqueue;

import android.os.Process;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * Created by JasonYang on 2015/7/27.
 */
final class Dispatcher extends Thread {

    private final BlockingQueue<Task<?, ?>> mPendingQueue;
    private final ExecutorService mExecutor;
    private final ResultDelivery mDelivery;

    private volatile boolean mQuit = false;

    public Dispatcher(BlockingQueue<Task<?, ?>> pendingQueue, ExecutorService executor,
                      ResultDelivery delivery) {
        mPendingQueue = pendingQueue;
        mExecutor = executor;
        mDelivery = delivery;
    }

    public void quit() {
        mQuit = true;
        interrupt();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            try {
                // Take a job from the queue.
                final Task<?, ?> task = mPendingQueue.take();
                if (task.isCanceled()) {
                    task.finish();
                    continue;
                }

                if (!mExecutor.isShutdown()) {
                    mExecutor.execute(new WorkerRunnable(task, mDelivery));
                }
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    return;
                }
            }
        }
    }
}
