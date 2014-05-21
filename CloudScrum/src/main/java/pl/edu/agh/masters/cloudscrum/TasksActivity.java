package pl.edu.agh.masters.cloudscrum;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import pl.edu.agh.masters.cloudscrum.adapter.TaskListAdapter;

public class TasksActivity extends BaseActivity {

    private String companyId;
    private String companyTitle;
    private String projectId;
    private String projectTitle;
    private String releaseId;
    private String releaseTitle;

    private List<Task> tasksData = new ArrayList<Task>();
    private TaskListAdapter tasksAdapter;

    private SpreadsheetService service;

    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        companyTitle = getIntent().getStringExtra(COMPANY_TITLE);
        companyId = getIntent().getStringExtra(COMPANY_ID);
        projectTitle = getIntent().getStringExtra(PROJECT_TITLE);
        projectId = getIntent().getStringExtra(PROJECT_ID);
        releaseTitle = getIntent().getStringExtra(RELEASE_TITLE);
        releaseId = getIntent().getStringExtra(RELEASE_ID);

        ((TextView)findViewById(R.id.title)).setText(companyTitle + " > " + projectTitle + " > " + releaseTitle);

        service = new SpreadsheetService(APPLICATION_NAME);
        service.setProtocolVersion(SpreadsheetService.Versions.V3);

        setList();
        loadTasks();
    }

    @Override
    public void onBackPressed() {

        if (isBack) {
            Intent intent = new Intent(this, ProjectsActivity.class);
            intent.putExtra(IS_BACK, true);
            intent.putExtra(COMPANY_TITLE, companyTitle);
            intent.putExtra(COMPANY_ID, companyId);
            intent.putExtra(PROJECT_TITLE, projectTitle);
            intent.putExtra(PROJECT_ID, projectId);
            startActivity(intent);
        }

        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == 3) {
            loadTasks();
        }
    }

    private void setList() {

        tasksAdapter = new TaskListAdapter(this, tasksData);
        ListView tasksListView = prepareListView(tasksAdapter);

        tasksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                startNextActivity(tasksData.get(i));
            }
        });
    }

    private void loadTasks() {

        new AsyncTask<Void, Void, Void>() {

            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = new ProgressDialog(TasksActivity.this);
                dialog.setMessage(TasksActivity.this.getString(R.string.loading_tasks));
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

                    int selectedWorksheet = 0;

                    for (int i=0; i<worksheets.size(); i++) {
                        if (!worksheets.get(i).getTitle().toString().contains(CLOSED_ITERATION_IN_TITLE)) {
                            selectedWorksheet = i;
                            break;
                        }
                    }

                    URL cellFeedUrl = worksheets.get(selectedWorksheet).getCellFeedUrl();
                    CellFeed cells = service.getFeed(cellFeedUrl, CellFeed.class);
                    tasksData.clear();

                    Task task = null;
                    int storyRow = 0;

                    //TODO filter by owner
                    for (CellEntry cell : cells.getEntries()) {

                        if (cell.getCell().getRow() >= STORIES_START_ROW) {

                            if (cell.getCell().getCol() == STORIES_ID_COLUMN && !cell.getCell().getValue().equals("")) {
                                storyRow = cell.getCell().getRow();
                            }

                            if (cell.getCell().getCol() == TASKS_TITLE_COLUMN && storyRow != cell.getCell().getRow()) {
                                task = new Task(cell.getCell().getValue(), cell.getCell().getRow(), selectedWorksheet);
                                tasksData.add(task);
                            } else if (cell.getCell().getCol() == TASKS_EFFORT_COLUMN) {
                                if (task != null && task.getRowNo() == cell.getCell().getRow()) {
                                    task.setTime(Long.valueOf(cell.getCell().getValue()));
                                }
                            } else if (cell.getCell().getCol() == TASKS_DETAILS_COLUMN){
                                if (task != null && task.getRowNo() == cell.getCell().getRow()) {
                                    task.setDetails(cell.getCell().getValue());
                                }
                            }
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
                tasksAdapter.notifyDataSetChanged();
            }

            protected void onCancelled() {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        }.execute();
    }

    private void startNextActivity(Task task) {
        Intent intent = new Intent(TasksActivity.this, TaskActivity.class);
        intent.putExtra(COMPANY_TITLE, companyTitle);
        intent.putExtra(COMPANY_ID, companyId);
        intent.putExtra(PROJECT_TITLE, projectTitle);
        intent.putExtra(PROJECT_ID, projectId);
        intent.putExtra(RELEASE_TITLE, releaseTitle);
        intent.putExtra(RELEASE_ID, releaseId);
        intent.putExtra(TASK_DATA, task);
        startActivityForResult(intent, 3);
    }
}
