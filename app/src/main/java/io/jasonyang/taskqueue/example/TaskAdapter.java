package io.jasonyang.taskqueue.example;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import io.jasonyang.taskqueue.Task;
import kale.adapter.CommonAdapter;
import kale.adapter.item.AdapterItem;

/**
 * Created by JasonYang on 2016/4/16.
 */
public class TaskAdapter extends CommonAdapter<Task<?, ?>> {

    public TaskAdapter(@Nullable List<Task<?, ?>> data) {
        super(data, 1);
    }

    @NonNull
    @Override
    public AdapterItem createItem(Object o) {
        return new TaskItem();
    }
}
