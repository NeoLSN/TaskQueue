package io.jasonyang.taskqueue.example;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.jasonyang.taskqueue.ExecutorDelivery;
import io.jasonyang.taskqueue.Task;
import io.jasonyang.taskqueue.TaskPool;

/**
 * Created by JasonYang on 2016/4/13.
 */
public class TaskQueueApplication extends Application {

    private Map<String, List<Task<?, ?>>> historyStore;
    private TaskPool taskPool;

    private Comparator<Task<?, ?>> comparator = new Comparator<Task<?, ?>>() {
        @Override
        public int compare(Task<?, ?> lhs, Task<?, ?> rhs) {
            if (!lhs.isPending() || !rhs.isPending()) return 0;
            return lhs.compareTo(rhs);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        historyStore = new HashMap<String, List<Task<?, ?>>>(3);
        historyStore.put(null, new LinkedList<Task<?, ?>>());
        historyStore.put("1", new LinkedList<Task<?, ?>>());
        historyStore.put("2", new LinkedList<Task<?, ?>>());
    }

    public TaskPool getTaskPool() {
        if (taskPool == null) {
            taskPool = new TaskPool(new ExecutorDelivery(new Handler(Looper.getMainLooper())));
            taskPool.start();

            taskPool.addTaskListener(new TaskPool.TaskPoolListener<Object, Object>() {
                @Override
                public void onAdd(Task<Object, Object> task, int size) {
                    addTaskToHistory(task);
                }

                @Override
                public void onRemove(Task<Object, Object> task, int size) {
                }
            });
        }
        return taskPool;
    }

    public TaskPool resetTaskPool() {
        if (taskPool != null) {
            taskPool.stop();
            taskPool.start();
        }
        return taskPool;
    }

    public Map<String, List<Task<?, ?>>> getStore() {
        return historyStore;
    }

    private void addTaskToHistory(Task<?, ?> task) {
        List<Task<?, ?>> taskList = historyStore.get(task.getExclusiveKey());
        taskList.add(task);
        Collections.sort(taskList, comparator);
    }
}
