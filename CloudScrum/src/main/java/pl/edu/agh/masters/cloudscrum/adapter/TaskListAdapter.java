package pl.edu.agh.masters.cloudscrum;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

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
            view = inflater.inflate(R.layout.row, null);

        ((TextView)view.findViewById(R.id.element_title)).setText(data.getTitle());

        return view;
    }
}
