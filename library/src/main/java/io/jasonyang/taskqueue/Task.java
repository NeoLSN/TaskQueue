package io.jasonyang.taskqueue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by jasonyang on 2015/4/20.
 */
public abstract class Task<I, O> implements Comparable<Task<I, O>> {

    private static final ExecutorService sExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            1L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    private transient TaskPool mTaskPool;
    private Result.ErrorListener mErrorListener;
    private Integer mSequence;
    private boolean mResultDelivered = false;
    private boolean mIsCanceled = false;
    private boolean mIsTimeout = false;
    private RetryPolicy mRetryPolicy;
    private Object mTag;
    private long mTimeout = 0;
    private Future<O> mFuture;
    private Priority mPriority = Priority.NORMAL;
    private State mState = State.PENDING;

    public Task(Result.ErrorListener listener) {
        mErrorListener = listener;
        setRetryPolicy(new DefaultRetryPolicy());
    }

    public abstract I getData();

    public abstract void setData(I data);

    public boolean isPending() {
        return State.PENDING.equals(mState);
    }

    public boolean isExecuting() {
        return State.EXECUTING.equals(mState);
    }

    public boolean isDone() {
        return State.FINISHED.equals(mState);
    }

    public boolean isCanceled() {
        return mIsCanceled;
    }

    public boolean isTimeout() {
        return mIsTimeout;
    }

    public void markDelivered() {
        mResultDelivered = true;
    }

    public boolean hasHadResultDelivered() {
        return mResultDelivered;
    }

    public Object getTag() {
        return mTag;
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    public void setTaskPool(TaskPool pool) {
        this.mTaskPool = pool;
    }

    public long getTimeout() {
        return mTimeout;
    }

    public void setTimeout(long timeout, TimeUnit unit) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }
        if (unit == null) {
            throw new IllegalArgumentException("unit == null");
        }
        long millis = unit.toMillis(timeout);
        if (millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Timeout too large.");
        }
        if (millis == 0 && timeout > 0) {
            throw new IllegalArgumentException("Timeout too small.");
        }
        mTimeout = millis;
    }

    public void resetTimeout() {
        mTimeout = 0;
    }

    public State getState() {
        return mState;
    }

    public Priority getPriority() {
        return mPriority;
    }

    public void setPriority(Priority priority) {
        mPriority = priority;
    }

    public final int getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        }
        return mSequence;
    }

    final void setSequence(int sequence) {
        mSequence = sequence;
    }

    public RetryPolicy getRetryPolicy() {
        return mRetryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        mRetryPolicy = retryPolicy;
    }

    @Override
    public int compareTo(Task another) {
        Priority left = this.getPriority();
        Priority right = another.getPriority();

        // High-priority requests are "lesser" so they are sorted to the front.
        // Equal priorities are sorted by sequence number to provide FIFO ordering.
        return left == right ? this.mSequence - another.mSequence
                : right.ordinal() - left.ordinal();
    }

    final Result<O> execute() throws Throwable {
        mState = State.EXECUTING;
        for (; ; ) {
            mIsTimeout = false;
            try {
                mFuture = sExecutor.submit(new Callable<O>() {
                    @Override
                    public O call() throws Exception {
                        return onExecute();
                    }
                });

                O res = mTimeout > 0 ? mFuture.get(getTimeout(), TimeUnit.MILLISECONDS)
                        : mFuture.get();

                return Result.success(res);
            } catch (TimeoutException e) {
                mIsTimeout = true;
                retryOrNot(e);
            } catch (ExecutionException e) {
                retryOrNot(e.getCause());
            } finally {
                cancelFuture();
            }
        }
    }

    private void retryOrNot(Throwable e) throws Throwable {
        if (mRetryPolicy != null) {
            mRetryPolicy.retryOrNot(e);
        } else {
            throw e;
        }
    }

    private void cancelFuture() {
        if (mFuture != null && !mFuture.isCancelled()) {
            mFuture.cancel(true);
        }
    }

    void finish() {
        mState = State.FINISHED;
        if (mTaskPool != null) {
            mTaskPool.finish(this);
            onFinish();
        }
    }

    protected void onFinish() {
        mErrorListener = null;
    }

    protected abstract O onExecute() throws Exception;

    public void cancel() {
        mIsCanceled = true;
        cancelFuture();
    }

    public abstract String getExclusiveKey();

    protected abstract void deliverResult(O result);

    public void deliverError(Throwable error) {
        if (mErrorListener != null) {
            mErrorListener.onError(error);
        }
    }

    public enum State {
        PENDING,
        EXECUTING,
        FINISHED,
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }
}
