Task Queue
========

[![Release](https://jitpack.io/v/NeoLSN/TaskQueue.svg?style=flat)](https://jitpack.io/#NeoLSN/TaskQueue)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Task%20Queue-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/3579)

Description
--------
An Android task queue library. Support priority, timeout, multiple execution, exclusive task and retry.

Setup
--------

```gradle
repositories {
    ...
    maven { url "https://jitpack.io" }
}
dependencies {
    ...
    compile 'com.github.NeoLSN:TaskQueue:1.0.2'
}
```

Usage
--------

##### Step 1
Establish a task pool for app.
```Java
public class TaskQueueApplication extends Application {

    private TaskPool taskPool;

    @Override public void onCreate() {
        super.onCreate();
        ...
        taskPool = new TaskPool(new ExecutorDelivery(new Handler(Looper.getMainLooper())));
        taskPool.start();
    }

    public TaskPool getTaskPool() {
        return taskPool;
    }
}
```

##### Step 2
Add task to pool
```Java
TaskQueueApplication app = (TaskQueueApplication) getApplication();
TaskPool taskPool = app.resetTaskPool();
Task<?, ?> task = ... //create you own task
taskPool.add(task);
```

API
--------
**Task**
```Java
public abstract class Task<I, O> {

    public void cancel();

    // task status
    public boolean isPending();
    public boolean isExecuting();
    public boolean isDone();
    public boolean isCanceled();
    public boolean isTimeout();
    public State getState();

    public Object getTag();
    public void setTag(Object tag);

    public long getTimeout();
    public void setTimeout(long timeout, TimeUnit unit);
    public void resetTimeout();

    public Priority getPriority();
    // Set priority before it is add to pool.
    public void setPriority(Priority priority);
    public int getSequence();

    public RetryPolicy getRetryPolicy();
    public void setRetryPolicy(RetryPolicy retryPolicy);

    // Only one task be executed at one time in the pool if the tasks' key are the same.
    public abstract String getExclusiveKey();
    public abstract void setData(I data);
    public abstract I getData();
    protected abstract void deliverResult(O result);
    protected abstract O onExecute() throws Exception;
    protected void onFinish();
}
```
**TaskPool**
```Java
public class TaskPool {

    // Default using - Executors.newCachedThreadPool()
    protected ExecutorService createExecutor();
    public void start();
    public void stop();

    // Return null when pool is stopped.
    public <I, O> Task<I, O> add(Task<I, O> task);
    public int size();

    public List<Task<?, ?>> getTasks(RequestFilter filter);
    public List<Task<?, ?>> getTasks();
    public List<Task<?, ?>> getTasksByTag(final Object tag);
    public List<Task<?, ?>> getTasksByExclusiveKey(final String key);

    public void cancel(RequestFilter filter);
    public void cancelAll();
    public void cancelByTag(final Object tag);
    public void cancelByExclusiveKey(final String key);

    public interface RequestFilter {
        boolean apply(Task<?, ?> task);
    }

    public <I, O> void addTaskListener(TaskPoolListener<I, O> listener);
    public <I, O> void removeTaskListener(TaskPoolListener<I, O> listener);

    public interface TaskPoolListener<I, O> {
        void onAdd(Task<I, O> task, int size);
        void onRemove(Task<I, O> task, int size);
    }
}
```
**ResultDelivery**

For task pool to deliver the result to specified thread
```Java
public interface ResultDelivery {

    public void postResult(Task<?, ?> task, Result<?> result);
    public void postResult(Task<?, ?> task, Result<?> result, Runnable runnable);
    public void postError(Task<?, ?> task, Throwable error);
}
```
**RetryPolicy**

Do nothing if it needs to retry. Throw an error if it can\'t retry.
```Java
public interface RetryPolicy {

    public void retryOrNot(Throwable error) throws Throwable;
}
```





