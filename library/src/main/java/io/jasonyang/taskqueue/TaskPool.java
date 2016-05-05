package io.jasonyang.taskqueue;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jasonyang on 2015/4/17.
 */
public class TaskPool {

    private final AtomicInteger mSequenceGenerator = new AtomicInteger();
    private final Set<Task<?, ?>> mCurrentTasks = new HashSet<Task<?, ?>>();
    private final PriorityBlockingQueue<Task<?, ?>> mPendingQueue
            = new PriorityBlockingQueue<Task<?, ?>>();
    private final Map<String, Queue<Task<?, ?>>> mWaitingTasks
            = new HashMap<String, Queue<Task<?, ?>>>();
    private final ResultDelivery mDelivery;
    private final List<TaskPoolListener> mTaskPoolListeners = new ArrayList<TaskPoolListener>();
    private Dispatcher mDispatcher;
    private ExecutorService mTaskExecutor;
    private boolean isRunning = false;

    public TaskPool(@NonNull ResultDelivery delivery) {
        mDelivery = delivery;
    }

    public synchronized void start() {
        stop();

        try {
            mTaskExecutor = createExecutor();

            mDispatcher = new Dispatcher(mPendingQueue, mTaskExecutor, mDelivery);
            mDispatcher.start();
            isRunning = true;
        } catch (Exception e) {
            stop();
        }
    }

    protected ExecutorService createExecutor() {
        return Executors.newCachedThreadPool();
    }

    public synchronized void stop() {
        isRunning = false;
        if (mDispatcher != null) {
            mDispatcher.quit();
        }
        if (mTaskExecutor != null) {
            mTaskExecutor.shutdownNow();
        }

        synchronized (mCurrentTasks) {
            mCurrentTasks.clear();
            mPendingQueue.clear();
            mWaitingTasks.clear();
            mSequenceGenerator.set(0);
        }
    }

    public int size() {
        return mCurrentTasks.size();
    }

    private int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }

    public <I, O> Task<I, O> add(@NonNull Task<I, O> task) {
        if (!isRunning) return null;

        // Tag the task as belonging to this pool and add it to the set of current tasks.
        task.setTaskPool(this);
        task.setSequence(getSequenceNumber());
        synchronized (mCurrentTasks) {
            mCurrentTasks.add(task);
        }
        synchronized (mTaskPoolListeners) {
            for (TaskPoolListener<I, O> listener : mTaskPoolListeners) {
                listener.onAdd(task, size());
            }
        }

        return dispatch(task);
    }

    private <I, O> Task<I, O> dispatch(Task<I, O> task) {

        if (TextUtils.isEmpty(task.getExclusiveKey())) {
            mPendingQueue.add(task);
            return task;
        }

        // Insert request into stage if there's already a request with the same queue name in flight.
        synchronized (mWaitingTasks) {
            String exclusiveKey = task.getExclusiveKey();
            if (mWaitingTasks.containsKey(exclusiveKey)) {
                // There is already a request in flight. Queue up.
                Queue<Task<?, ?>> stagedRequests = mWaitingTasks.get(exclusiveKey);
                if (stagedRequests == null) {
                    stagedRequests = new PriorityQueue<Task<?, ?>>();
                }
                stagedRequests.add(task);
                mWaitingTasks.put(exclusiveKey, stagedRequests);
            } else {
                // Insert 'null' queue for this queue name, indicating there is now a request in flight.
                mWaitingTasks.put(exclusiveKey, null);
                mPendingQueue.add(task);
            }
            return task;
        }
    }

    <I, O> void finish(Task<I, O> task) {
        // Remove from the set of requests currently being processed.
        synchronized (mCurrentTasks) {
            mCurrentTasks.remove(task);
        }
        synchronized (mTaskPoolListeners) {
            for (TaskPoolListener<I, O> listener : mTaskPoolListeners) {
                listener.onRemove(task, size());
            }
        }

        if (TextUtils.isEmpty(task.getExclusiveKey())) {
            return;
        }

        synchronized (mWaitingTasks) {
            String exclusiveKey = task.getExclusiveKey();
            Queue<Task<?, ?>> waitingRequests = mWaitingTasks.get(exclusiveKey);
            if (waitingRequests != null && !waitingRequests.isEmpty()) {
                Task<?, ?> queueTask = waitingRequests.poll();
                mPendingQueue.add(queueTask);
            } else {
                mWaitingTasks.remove(exclusiveKey);
            }
        }
    }

    public List<Task<?, ?>> getTasks() {
        return getTasks(new RequestFilter() {
            @Override
            public boolean apply(Task<?, ?> task) {
                return true;
            }
        });
    }

    public List<Task<?, ?>> getTasks(RequestFilter filter) {
        List<Task<?, ?>> tasks = new LinkedList<Task<?, ?>>();
        synchronized (mCurrentTasks) {
            for (Task<?, ?> task : mCurrentTasks) {
                if (filter.apply(task)) {
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    public List<Task<?, ?>> getTasksByTag(final Object tag) {
        return getTasks(new RequestFilter() {
            @Override
            public boolean apply(Task<?, ?> task) {
                return task.getTag() == tag;
            }
        });
    }

    public List<Task<?, ?>> getTasksByExclusiveKey(final String key) {
        return getTasks(new RequestFilter() {
            @Override
            public boolean apply(Task<?, ?> task) {
                return TextUtils.equals(task.getExclusiveKey(), key);
            }
        });
    }

    public interface RequestFilter {

        boolean apply(Task<?, ?> task);
    }

    public void cancelAll() {
        cancel(new RequestFilter() {
            @Override
            public boolean apply(Task<?, ?> task) {
                return true;
            }
        });
    }

    public void cancel(RequestFilter filter) {
        synchronized (mCurrentTasks) {
            for (Task<?, ?> task : mCurrentTasks) {
                if (filter.apply(task)) {
                    task.cancel();
                }
            }
        }
    }

    public void cancelByTag(final Object tag) {
        cancel(new RequestFilter() {
            @Override
            public boolean apply(Task<?, ?> task) {
                return task.getTag() == tag;
            }
        });
    }

    public void cancelByExclusiveKey(final String key) {
        cancel(new RequestFilter() {
            @Override
            public boolean apply(Task<?, ?> task) {
                return TextUtils.equals(task.getExclusiveKey(), key);
            }
        });
    }

    public interface TaskPoolListener<I, O> {

        void onAdd(Task<I, O> task, int size);

        void onRemove(Task<I, O> task, int size);
    }

    public <I, O> void addTaskListener(TaskPoolListener<I, O> listener) {
        synchronized (mTaskPoolListeners) {
            mTaskPoolListeners.add(listener);
        }
    }

    public <I, O> void removeTaskListener(TaskPoolListener<I, O> listener) {
        synchronized (mTaskPoolListeners) {
            mTaskPoolListeners.remove(listener);
        }
    }
}
