package io.jasonyang.taskqueue;

/**
 * Created by JasonYang on 2015/7/28.
 */
public class DefaultRetryPolicy implements RetryPolicy {

    public static final int DEFAULT_MAX_RETRIES = 0;

    private final int mMaxNumRetries;

    private int mCurrentRetryCount;

    public DefaultRetryPolicy() {
        this(DEFAULT_MAX_RETRIES);
    }

    public DefaultRetryPolicy(int maxNumRetries) {
        mMaxNumRetries = maxNumRetries;
    }

    public int getCurrentRetryCount() {
        return mCurrentRetryCount;
    }

    @Override
    public void retryOrNot(Throwable error) throws Throwable {
        mCurrentRetryCount++;
        if (!hasAttemptRemaining()) {
            throw error;
        }
    }

    protected boolean hasAttemptRemaining() {
        return mCurrentRetryCount <= mMaxNumRetries;
    }
}
