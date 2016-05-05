package io.jasonyang.taskqueue;

/**
 * Created by JasonYang on 2015/7/27.
 */
public interface ResultDelivery {

    public void postResult(Task<?, ?> task, Result<?> result);

    public void postResult(Task<?, ?> task, Result<?> result, Runnable runnable);

    public void postError(Task<?, ?> task, Throwable error);
}
