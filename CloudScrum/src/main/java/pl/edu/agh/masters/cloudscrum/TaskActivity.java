package pl.edu.agh.masters.cloudscrum;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;

import java.net.URL;
import java.util.List;

import pl.edu.agh.masters.cloudscrum.model.Task;

public class TaskActivity extends BaseActivity {

    static final int TIMER_HANDLER_DELAY = 250;
    static final int NOTIFICATION_TIMER_ID = 2019;

    static final String NOTIFICATION_STARTED = "notificationStarted";
    static final String TIMER_STARTED = "timerStarted";
    static final String TIMER_START_TIME = "timerStartTime";
    static final String ACTIVE_TIMER_TASK_ROW = "activeTimerTaskRow";
    static final String ACTIVE_TIMER_RELEASE_ID = "activeTimerReleaseId";

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
    private boolean firstTimeStatusChanged = false;

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        timerHandler.removeMessages(0);
    }

    @Override
    public void onPause() {

        if (isTimerStarted) {
            SharedPreferences pref = getAppSharedPreferences();
            final SharedPreferences.Editor editor = pref.edit();
            editor.putString(ACTIVE_TIMER_RELEASE_ID, releaseId);
            editor.putInt(ACTIVE_TIMER_TASK_ROW, task.getRowNo());
            editor.putLong(TIMER_START_TIME, timerStartTime);
            editor.commit();
            showTimerNotification();
        }

        super.onPause();
    }

    private void init(Intent intent) {

        companyTitle = intent.getStringExtra(COMPANY_TITLE);
        companyId = intent.getStringExtra(COMPANY_ID);
        projectTitle = intent.getStringExtra(PROJECT_TITLE);
        projectId = intent.getStringExtra(PROJECT_ID);
        releaseTitle = intent.getStringExtra(RELEASE_TITLE);
        releaseId = intent.getStringExtra(RELEASE_ID);
        task = (Task)intent.getSerializableExtra(TASK_DATA);
        isTimerStarted = intent.getBooleanExtra(TIMER_STARTED, false);
        timerStartTime = intent.getLongExtra(TIMER_START_TIME, 0);

        ((TextView)findViewById(R.id.title)).setText(companyTitle + " > " + projectTitle + " > " + releaseTitle);

        service = new SpreadsheetService(APPLICATION_NAME);
        service.setProtocolVersion(SpreadsheetService.Versions.V3);

        setTaskData();
        setTimerButtonsListeners();
        handleNotification();
        setStatusSpinnerListener();
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
                SharedPreferences pref = getAppSharedPreferences();
                final SharedPreferences.Editor editor = pref.edit();
                editor.putString(ACTIVE_TIMER_RELEASE_ID, "");
                editor.commit();
                isTimerStarted = false;
                setTimerButtonsStates();
                timerHandler.removeMessages(0);
                saveTaskTime();
            }
        });
    }

    private void setStatusSpinnerListener() {

        final Spinner status = (Spinner)findViewById(R.id.status);

        status.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (firstTimeStatusChanged) {
                    task.setStatus(status.getSelectedItem().toString());
                    saveTaskStatus();
                } else {
                    firstTimeStatusChanged = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        ArrayAdapter statusAdapter = (ArrayAdapter) status.getAdapter();
        status.setSelection(statusAdapter.getPosition(task.getStatus()));
    }

    private void handleNotification() {

        if (isTimerStarted) {
            isBack = true;
            setTimerButtonsStates();
            hideTimerNotification();
        } else {

            SharedPreferences pref = getAppSharedPreferences();
            int activeRow = pref.getInt(ACTIVE_TIMER_TASK_ROW, 0);
            String activeReleaseId = pref.getString(ACTIVE_TIMER_RELEASE_ID, "");

            if (!activeReleaseId.equals("")) {
                if (activeRow == task.getRowNo() && releaseId.equals(activeReleaseId)) {
                    isTimerStarted = true;
                    timerStartTime = pref.getLong(TIMER_START_TIME, timerStartTime);
                    setTimerButtonsStates();
                    hideTimerNotification();
                } else {
                    startTimerButton.setEnabled(false);
                    stopTimerButton.setEnabled(false);
                }
            } else {
                setTimerButtonsStates();
            }
        }
    }

    private void hideTimerNotification() {

        timerHandler.removeMessages(0);
        timerHandler.sendEmptyMessageDelayed(0, TIMER_HANDLER_DELAY);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        SharedPreferences pref = getAppSharedPreferences();
        final SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(NOTIFICATION_STARTED, false);
        editor.commit();
    }

    private void showTimerNotification() {

        new Thread(

            new Runnable() {

                @Override
                public void run() {

                    SharedPreferences pref = getAppSharedPreferences(Activity.MODE_MULTI_PROCESS);
                    final SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean(NOTIFICATION_STARTED, true);
                    editor.commit();

                    boolean run = true;

                    while (run) {

                        long delay = System.currentTimeMillis() - timerStartTime;
                        timerRunTime = (int) (delay / 1000);
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(TaskActivity.this)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.timer))
                            .setSmallIcon(R.drawable.timer_small)
                            .setContentTitle(task.getTitle())
                            .setContentText(formatTime(task.getTime() + timerRunTime));

                        notificationBuilder.setAutoCancel(true);
                        notificationBuilder.setOngoing(true);

                        Intent resultIntent = new Intent(TaskActivity.this, TaskActivity.class);
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(TaskActivity.this);
                        stackBuilder.addParentStack(TaskActivity.class);
                        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        resultIntent.putExtra(COMPANY_TITLE, companyTitle);
                        resultIntent.putExtra(COMPANY_ID, companyId);
                        resultIntent.putExtra(PROJECT_TITLE, projectTitle);
                        resultIntent.putExtra(PROJECT_ID, projectId);
                        resultIntent.putExtra(RELEASE_TITLE, releaseTitle);
                        resultIntent.putExtra(RELEASE_ID, releaseId);
                        resultIntent.putExtra(TASK_DATA, task);

                        resultIntent.putExtra(TIMER_STARTED, isTimerStarted);
                        resultIntent.putExtra(TIMER_START_TIME, timerStartTime);

                        stackBuilder.addNextIntent(resultIntent);

                        notificationBuilder.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));
                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                        try {
                            Thread.sleep(TIMER_HANDLER_DELAY);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        notificationManager.notify(NOTIFICATION_TIMER_ID, notificationBuilder.build());
                        run = pref.getBoolean(NOTIFICATION_STARTED, false);
                    }

                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancel(NOTIFICATION_TIMER_ID);
                }
            }
        ).start();
    }

    private void saveTaskTime() {
        task.setTime(task.getTime() + timerRunTime);
        saveTask(TASKS_EFFORT_COLUMN, String.valueOf(task.getTime()), TaskActivity.this.getString(R.string.saving_effort));
    }

    private void saveTaskStatus() {
        saveTask(TASKS_STATUS_COLUMN, task.getStatus(), TaskActivity.this.getString(R.string.saving_status));
    }

    private void saveTask(final int column, final String value, final String message) {

        isTaskSaved = true;

        new AsyncTask<Void, Void, Void>() {

            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = new ProgressDialog(TaskActivity.this);
                dialog.setMessage(message);
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {

                try {

                    SharedPreferences pref = getAppSharedPreferences();
                    String accountName = pref.getString(ACCOUNT_NAME, "");
                    String password = pref.getString(PASSWORD, "");

                    service.setUserCredentials(accountName, password);

                    WorksheetFeed feed = service.getFeed(new URL("https://spreadsheets.google.com/feeds/worksheets/" + releaseId + "/private/full"), WorksheetFeed.class);
                    List<WorksheetEntry> worksheets = feed.getEntries();

                    URL cellFeedUrl = worksheets.get(task.getSelectedWorksheet()).getCellFeedUrl();
                    CellFeed cells = service.getFeed(cellFeedUrl, CellFeed.class);

                    for (CellEntry cell : cells.getEntries()) {
                        if (cell.getCell().getRow() == task.getRowNo() && cell.getCell().getCol() == column) {
                            cell.changeInputValueLocal(value);
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
