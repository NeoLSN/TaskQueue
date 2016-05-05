package io.jasonyang.taskqueue;

/**
 * Created by JasonYang on 2015/7/28.
 */
public class Result<T> {

    public final T result;
    public final Throwable error;

    public static <T> Result<T> success(T result) {
        return new Result<T>(result);
    }

    public static <T> Result<T> error(Throwable error) {
        return new Result<T>(error);
    }

    private Result(T result) {
        this.result = result;
        this.error = null;
    }

    private Result(Throwable error) {
        this.result = null;
        this.error = error;
    }

    public boolean isSuccess() {
        return error == null;
    }

    public interface Listener<T> {

        public void onResult(T result);
    }

    public interface ErrorListener {

        public void onError(Throwable error);
    }
}
