package io.jasonyang.taskqueue.example;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.List;
import java.util.Map;

import io.jasonyang.taskqueue.Task;
import io.jasonyang.taskqueue.TaskPool;

public class MainActivity extends AppCompatActivity {

    private TaskPool taskPool;
    private Map<String, List<Task<?, ?>>> store;

    private ListView taskListGeneral;
    private ListView taskList1;
    private ListView taskList2;

    private TaskAdapter generalTaskAdapter;
    private TaskAdapter taskList1Adapter;
    private TaskAdapter taskList2Adapter;

    private TaskPool.TaskPoolListener listener = new TaskPool.TaskPoolListener<Object, Object>() {
        @Override
        public void onAdd(Task<Object, Object> task, int size) {
            updateUI();
        }

        @Override
        public void onRemove(Task<Object, Object> task, int size) {
            updateUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Task<?, ?> task = TaskGenerator.generateTask();
                taskPool.add(task);
            }
        });

        TaskQueueApplication app = (TaskQueueApplication) getApplication();
        taskPool = app.getTaskPool();
        taskPool.addTaskListener(listener);
        store = app.getStore();

        taskListGeneral = (ListView) findViewById(R.id.queue_list_general);
        taskList1 = (ListView) findViewById(R.id.queue_list_1);
        taskList2 = (ListView) findViewById(R.id.queue_list_2);

        taskListGeneral.setAdapter(generalTaskAdapter = new TaskAdapter(store.get(null)));
        taskList1.setAdapter(taskList1Adapter = new TaskAdapter(store.get("1")));
        taskList2.setAdapter(taskList2Adapter = new TaskAdapter(store.get("2")));
    }


    private void updateUI() {
        generalTaskAdapter.notifyDataSetChanged();
        taskList1Adapter.notifyDataSetChanged();
        taskList2Adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_reset) {
            store.get(null).clear();
            store.get("1").clear();
            store.get("2").clear();

            taskPool.removeTaskListener(listener);
            TaskQueueApplication app = (TaskQueueApplication) getApplication();
            taskPool = app.resetTaskPool();
            taskPool.addTaskListener(listener);

            updateUI();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
