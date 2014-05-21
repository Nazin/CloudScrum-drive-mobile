package pl.edu.agh.masters.cloudscrum;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;

import java.net.URL;
import java.util.List;

public class TaskActivity extends BaseActivity {

    static final int TIMER_HANDLER_DELAY = 250;

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
    private TextView taskDetails;

    private View startTimerButton;
    private View stopTimerButton;

    private boolean taskDetailsVisible = false;
    private boolean isTimerStarted = false;
    private boolean isTaskSaved = false;

    private long timerStartTime = 0;
    private long timerRunTime = 0;

    private Handler timerHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            long delay = System.currentTimeMillis() - timerStartTime;
            timerRunTime = (int) (delay / 1000);
            showTaskTime(task.getTime() + timerRunTime);
            timerHandler.sendEmptyMessageDelayed(0, TIMER_HANDLER_DELAY);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.task);

        init(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        init(intent);
    }

    @Override
    public void onBackPressed() {

        if (isBack) {
            Intent intent = new Intent(this, TasksActivity.class);
            intent.putExtra(IS_BACK, true);
            intent.putExtra(COMPANY_TITLE, companyTitle);
            intent.putExtra(COMPANY_ID, companyId);
            intent.putExtra(PROJECT_TITLE, projectTitle);
            intent.putExtra(PROJECT_ID, projectId);
            intent.putExtra(RELEASE_TITLE, releaseTitle);
            intent.putExtra(RELEASE_ID, releaseId);
            startActivity(intent);
        } else {
            if (isTaskSaved) {
                setResult(RESULT_OK);
            }
        }

        finish();
    }

    private void init(Intent intent) {

        companyTitle = intent.getStringExtra(COMPANY_TITLE);
        companyId = intent.getStringExtra(COMPANY_ID);
        projectTitle = intent.getStringExtra(PROJECT_TITLE);
        projectId = intent.getStringExtra(PROJECT_ID);
        releaseTitle = intent.getStringExtra(RELEASE_TITLE);
        releaseId = intent.getStringExtra(RELEASE_ID);
        task = (Task)intent.getSerializableExtra(TASK_DATA);

        ((TextView)findViewById(R.id.title)).setText(companyTitle + " > " + projectTitle + " > " + releaseTitle);

        service = new SpreadsheetService(APPLICATION_NAME);
        service.setProtocolVersion(SpreadsheetService.Versions.V3);

        setTaskData();
        setTimerButtonsListeners();
    }

    private void setTaskData() {

        taskDetails = (TextView)findViewById(R.id.taskDetails);
        toggleDetails = (TextView)findViewById(R.id.toggleDetails);

        taskDetails.setText(task.getDetails());
        ((TextView)findViewById(R.id.taskTitle)).setText(task.getTitle());

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

    private void setTimerButtonsListeners() {

        startTimerButton = findViewById(R.id.startTimerButton);
        stopTimerButton = findViewById(R.id.stopTimerButton);

        startTimerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                isTimerStarted = true;
                setTimerButtonsStates();
                timerStartTime = System.currentTimeMillis();
                timerHandler.sendEmptyMessageDelayed(0, TIMER_HANDLER_DELAY);
            }
        });

        stopTimerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                isTimerStarted = false;
                setTimerButtonsStates();
                timerHandler.removeMessages(0);
                saveTaskTime();
            }
        });

        setTimerButtonsStates();//TODO temp
    }

    private void saveTaskTime() {

        task.setTime(task.getTime() + timerRunTime);
        isTaskSaved = true;

        new AsyncTask<Void, Void, Void>() {

            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = new ProgressDialog(TaskActivity.this);
                dialog.setMessage(TaskActivity.this.getString(R.string.saving_effort));
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {

                try {

                    SharedPreferences pref = getPreferences();
                    String accountName = pref.getString(ACCOUNT_NAME, "");
                    String password = pref.getString(PASSWORD, "");

                    service.setUserCredentials(accountName, password);

                    WorksheetFeed feed = service.getFeed(new URL("https://spreadsheets.google.com/feeds/worksheets/" + releaseId + "/private/full"), WorksheetFeed.class);
                    List<WorksheetEntry> worksheets = feed.getEntries();

                    URL cellFeedUrl = worksheets.get(task.getSelectedWorksheet()).getCellFeedUrl();
                    CellFeed cells = service.getFeed(cellFeedUrl, CellFeed.class);

                    for (CellEntry cell : cells.getEntries()) {
                        if (cell.getCell().getRow() == task.getRowNo() && cell.getCell().getCol() == TASKS_EFFORT_COLUMN) {
                            cell.changeInputValueLocal(String.valueOf(task.getTime()));
                            cell.update();
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }

            protected void onPostExecute(Void result) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }

            protected void onCancelled() {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        }.execute();
    }

    private void setTimerButtonsStates() {
        startTimerButton.setEnabled(!isTimerStarted);
        stopTimerButton.setEnabled(isTimerStarted);
    }

    private void toggleTaskDetails() {
        if (taskDetailsVisible) {
            taskDetails.setVisibility(View.VISIBLE);
            toggleDetails.setText(R.string.hide_details);
        } else {
            taskDetails.setVisibility(View.GONE);
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
