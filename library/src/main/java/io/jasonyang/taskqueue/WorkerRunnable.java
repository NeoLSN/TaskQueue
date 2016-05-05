package io.jasonyang.taskqueue;

import android.os.Process;

/**
 * Created by JasonYang on 2015/7/31.
 */
final class WorkerRunnable implements Runnable {

    private final Task<?, ?> mTask;
    private final ResultDelivery mDelivery;

    public WorkerRunnable(Task task, ResultDelivery delivery) {
        mTask = task;
        mDelivery = delivery;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        try {
            if (mTask.isCanceled() || mTask.isTimeout() || mTask.hasHadResultDelivered()) {
                mTask.finish();
                return;
            }
            Result<?> result = mTask.execute();
            mDelivery.postResult(mTask, result);
        } catch (Throwable error) {
            mDelivery.postError(mTask, error);
        }
    }
}
