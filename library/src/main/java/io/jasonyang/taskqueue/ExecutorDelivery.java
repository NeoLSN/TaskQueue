package io.jasonyang.taskqueue;

import android.os.Handler;

import java.util.concurrent.Executor;

/**
 * Created by JasonYang on 2015/7/28.
 */
public class ExecutorDelivery implements ResultDelivery {

    private final Executor mResponsePoster;

    public ExecutorDelivery(final Handler handler) {
        // Make an Executor that just wraps the handler.
        mResponsePoster = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };
    }

    public ExecutorDelivery(Executor executor) {
        mResponsePoster = executor;
    }

    @Override
    public void postResult(Task<?, ?> task, Result<?> result) {
        postResult(task, result, null);
    }

    @Override
    public void postResult(Task<?, ?> task, Result<?> result, Runnable runnable) {
        mResponsePoster.execute(new ResponseDeliveryRunnable(task, result, runnable));
    }

    @Override
    public void postError(Task<?, ?> task, Throwable error) {
        Result<?> result = Result.error(error);
        mResponsePoster.execute(new ResponseDeliveryRunnable(task, result, null));
    }

    @SuppressWarnings("rawtypes")
    private class ResponseDeliveryRunnable implements Runnable {

        private final Task mTask;
        private final Result mResult;
        private final Runnable mRunnable;

        public ResponseDeliveryRunnable(Task task, Result result, Runnable runnable) {
            mTask = task;
            mResult = result;
            mRunnable = runnable;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            // If this request has canceled, finish it and don't deliver.
            if (mTask.isCanceled()) {
                mTask.finish();
                return;
            }

            if (mResult.isSuccess()) {
                mTask.markDelivered();
                mTask.deliverResult(mResult.result);
            } else {
                mTask.deliverError(mResult.error);
            }

            mTask.finish();

            if (mRunnable != null) {
                mRunnable.run();
            }
        }
    }

}
