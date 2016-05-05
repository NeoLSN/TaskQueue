package io.jasonyang.taskqueue.example;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import io.jasonyang.taskqueue.Task;
import kale.adapter.item.AdapterItem;

/**
 * Created by JasonYang on 2016/4/16.
 */
public class TaskItem implements AdapterItem<Task<?, ?>> {

    private View root;
    private TextView taskNameText;
    private TextView taskPriorityText;
    private ImageView taskPriorityIndicator;
    private TextView taskStateText;

    @Override
    public int getLayoutResId() {
        return R.layout.item_task;
    }

    @Override
    public void bindViews(View view) {
        root = view;
        taskNameText = (TextView) root.findViewById(R.id.task_name);
        taskPriorityText = (TextView) root.findViewById(R.id.task_priority);
        taskPriorityIndicator = (ImageView) root.findViewById(R.id.task_priority_indicator);
        taskStateText = (TextView) root.findViewById(R.id.task_state);
    }

    @Override
    public void setViews() {
    }

    @Override
    public void handleData(Task<?, ?> task, int i) {
        final Context context = root.getContext();
        taskNameText.setText(context.getString(R.string.task_name, task.getSequence()));
        final int color;
        final String priority;
        switch (task.getPriority()) {
            case IMMEDIATE:
                color = Color.RED;
                priority = "immediate";
                break;
            case HIGH:
                color = Color.YELLOW;
                priority = "high";
                break;
            case NORMAL:
                color = Color.GREEN;
                priority = "normal";
                break;
            case LOW:
            default:
                color = Color.GRAY;
                priority = "low";
        }
        taskPriorityIndicator.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        taskPriorityText.setText(priority);

        String state = task.getState() +
                (task.isCanceled() ? ", CANCELED" : "") +
                (task.isTimeout() ? ", TIMEOUT" : "");
        taskStateText.setText(state);

        final int bgColor;
        switch (task.getState()) {
            case PENDING:
                bgColor = Color.WHITE;
                break;
            case EXECUTING:
                bgColor = context.getResources().getColor(android.R.color.holo_orange_light);
                break;
            case FINISHED:
            default:
                bgColor = Color.LTGRAY;
        }
        root.setBackgroundColor(bgColor);
    }
}
