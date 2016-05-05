package io.jasonyang.taskqueue;

/**
 * Created by JasonYang on 2015/7/28.
 */
public interface RetryPolicy {

    public void retryOrNot(Throwable error) throws Throwable;
}
