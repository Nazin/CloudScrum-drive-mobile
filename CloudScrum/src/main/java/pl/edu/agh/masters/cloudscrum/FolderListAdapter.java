package pl.edu.agh.masters.cloudscrum;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.api.services.drive.model.File;

import java.util.List;

public class FolderListAdapter extends BaseAdapter {

    private List<File> listData;
    private LayoutInflater inflater = null;

    protected Context context;

    public FolderListAdapter(Context context, List<File> listData) {

        this.context = context;
        this.listData = listData;

        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return listData.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        File data = listData.get(position);

        if (convertView == null)
            view = inflater.inflate(R.layout.row, null);

        ((TextView)view.findViewById(R.id.element_title)).setText(data.getTitle());

        return view;
    }
}
