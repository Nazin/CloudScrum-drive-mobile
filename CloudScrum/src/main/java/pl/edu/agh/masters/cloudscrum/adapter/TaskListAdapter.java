package pl.edu.agh.masters.cloudscrum.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import pl.edu.agh.masters.cloudscrum.R;
import pl.edu.agh.masters.cloudscrum.model.Task;

public class TaskListAdapter extends BaseAdapter {


    private List<Task> tasksList;
    private LayoutInflater inflater = null;

    protected Context context;

    public TaskListAdapter(Context context, List<Task> tasksList) {

        this.context = context;
        this.tasksList = tasksList;

        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return tasksList.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        Task data = tasksList.get(position);

        if (convertView == null)
            view = inflater.inflate(R.layout.task_row, null);

        ((TextView)view.findViewById(R.id.element_title)).setText(data.getTitle());
        ((TextView)view.findViewById(R.id.element_effort)).setText("Effort: " + formatTime(data.getTime()));

        return view;
    }

    private String formatTime(long time) {

        int hours = (int) (time / 3600);
        int minutes = (int) ((time % 3600) / 60);
        int seconds = (int) (time % 60);

        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }
}
