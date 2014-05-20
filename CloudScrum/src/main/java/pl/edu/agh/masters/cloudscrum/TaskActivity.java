package pl.edu.agh.masters.cloudscrum;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.gdata.client.spreadsheet.SpreadsheetService;

public class TaskActivity extends BaseActivity {

    private String companyId;
    private String companyTitle;
    private String projectId;
    private String projectTitle;
    private String releaseId;
    private String releaseTitle;

    private Task task;

    private SpreadsheetService service;

    private TextView toggleDetails;
    private TextView taskTime;

    private boolean taskDetailsVisible = false;

    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.task);

        companyTitle = getIntent().getStringExtra(COMPANY_TITLE);
        companyId = getIntent().getStringExtra(COMPANY_ID);
        projectTitle = getIntent().getStringExtra(PROJECT_TITLE);
        projectId = getIntent().getStringExtra(PROJECT_ID);
        releaseTitle = getIntent().getStringExtra(RELEASE_TITLE);
        releaseId = getIntent().getStringExtra(RELEASE_ID);
        task = (Task)getIntent().getSerializableExtra(TASK_DATA);

        ((TextView)findViewById(R.id.title)).setText(companyTitle + " > " + projectTitle + " > " + releaseTitle);

        service = new SpreadsheetService(APPLICATION_NAME);
        service.setProtocolVersion(SpreadsheetService.Versions.V3);

        setData();
    }

    private void setData() {

        ((TextView)findViewById(R.id.taskTitle)).setText(task.getTitle());
        ((TextView)findViewById(R.id.taskDetails)).setText(task.getDetails());

        toggleDetails = (TextView)findViewById(R.id.toggleDetails);
        toggleDetails.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                taskDetailsVisible = !taskDetailsVisible;
                toggleTaskDetails();
            }
        });
        toggleTaskDetails();

        taskTime = (TextView)findViewById(R.id.taskTime);
        showTaskTime(task.getTime());
    }

    private void toggleTaskDetails() {
        if (taskDetailsVisible) {
            findViewById(R.id.taskDetails).setVisibility(View.VISIBLE);
            toggleDetails.setText(R.string.hide_details);
        } else {
            findViewById(R.id.taskDetails).setVisibility(View.GONE);
            toggleDetails.setText(R.string.show_details);
        }
    }

    private void showTaskTime(long time) {
        taskTime.setText(formatTime(time));
    }

    private String formatTime(long time) {

        int hours = (int) (time / 3600);
        int minutes = (int) ((time % 3600) / 60);
        int seconds = (int) (time % 60);

        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }
}
