package io.jasonyang.taskqueue.example;

import android.util.Log;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.jasonyang.taskqueue.Result;
import io.jasonyang.taskqueue.Task;

/**
 * Created by JasonYang on 2016/4/16.
 */
public final class TaskGenerator {

    private static final Random r = new Random();

    private TaskGenerator() {
    }

    public static Task<?, ?> generateTask() {
        return new ExampleTask(new Result.ErrorListener() {
            @Override
            public void onError(Throwable error) {
                Log.w(getClass().getSimpleName(), "error => " + error);
            }
        });
    }

    private static class ExampleTask extends Task<Void, Void> {

        final String exclusiveKey;

        public ExampleTask(Result.ErrorListener listener) {
            super(listener);

            final Task.Priority p;
            switch (r.nextInt(4)) {
                case 0:
                    p = Task.Priority.IMMEDIATE;
                    break;
                case 1:
                    p = Task.Priority.HIGH;
                    break;
                case 2:
                    p = Task.Priority.NORMAL;
                    break;
                case 3:
                default:
                    p = Task.Priority.LOW;
            }
            setPriority(p);

            switch (r.nextInt(3)) {
                case 0:
                    exclusiveKey = null;
                    break;
                case 1:
                    exclusiveKey = "1";
                    break;
                case 2:
                default:
                    exclusiveKey = "2";
            }

            setTimeout(r.nextInt(8), TimeUnit.SECONDS);
        }

        @Override
        public Void getData() {
            return null;
        }

        @Override
        public void setData(Void data) {
        }

        @Override
        protected Void onExecute() throws Exception {
            Thread.sleep(r.nextInt(11) * 1000);
            return null;
        }

        @Override
        public String getExclusiveKey() {
            return exclusiveKey;
        }

        @Override
        protected void deliverResult(Void result) {
        }
    }
}
