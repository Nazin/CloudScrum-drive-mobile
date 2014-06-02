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

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.model.File;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.common.base.StringUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.agh.masters.cloudscrum.adapter.TaskListAdapter;
import pl.edu.agh.masters.cloudscrum.exception.Authorization;
import pl.edu.agh.masters.cloudscrum.model.ReleaseFile;
import pl.edu.agh.masters.cloudscrum.model.Task;

public class AllTasksActivity extends BaseActivity {

    private List<Task> tasksData = new ArrayList<Task>();
    private TaskListAdapter tasksAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.title)).setText(R.string.all_tasks);

        setList();
        loadTasks();
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

        final GoogleAccountCredential credential = getGoogleAccountCredential();
        SharedPreferences pref = getAppSharedPreferences();

        String accountName = pref.getString(ACCOUNT_NAME, "");
        credential.setSelectedAccountName(accountName);

        new AsyncTask<Void, Void, Void>() {

            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = new ProgressDialog(AllTasksActivity.this);
                dialog.setMessage(AllTasksActivity.this.getString(R.string.loading_tasks));
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {

                List<ReleaseFile> files = getFiles(credential);

                try {

                    SharedPreferences pref = getAppSharedPreferences();
                    String accountName = pref.getString(ACCOUNT_NAME, "");
                    String password = pref.getString(PASSWORD, "");

                    SpreadsheetService service = new SpreadsheetService(APPLICATION_NAME);
                    service.setProtocolVersion(SpreadsheetService.Versions.V3);
                    service.setUserCredentials(accountName, password);

                    List<Task> tempTasks = new ArrayList<Task>();

                    for (ReleaseFile file : files) {

                        WorksheetFeed feed = service.getFeed(new URL("https://spreadsheets.google.com/feeds/worksheets/" + file.getReleaseId() + "/private/full"), WorksheetFeed.class);
                        List<WorksheetEntry> worksheets = feed.getEntries();

                        int selectedWorksheet = 0;

                        for (int i=0; i<worksheets.size(); i++) {
                            if (!worksheets.get(i).getTitle().getPlainText().contains(CLOSED_ITERATION_IN_TITLE)) {
                                selectedWorksheet = i;
                                break;
                            }
                        }

                        URL cellFeedUrl = worksheets.get(selectedWorksheet).getCellFeedUrl();
                        CellFeed cells = service.getFeed(cellFeedUrl, CellFeed.class);

                        Task task = null;
                        int storyRow = 0;

                        for (CellEntry cell : cells.getEntries()) {

                            if (cell.getCell().getRow() >= STORIES_START_ROW) {

                                if (cell.getCell().getCol() == STORIES_ID_COLUMN && !cell.getCell().getValue().equals("")) {
                                    storyRow = cell.getCell().getRow();
                                }

                                if (cell.getCell().getCol() == TASKS_TITLE_COLUMN && storyRow != cell.getCell().getRow()) {

                                    task = new Task(cell.getCell().getValue(), cell.getCell().getRow(), selectedWorksheet);

                                    task.setReleaseId(file.getReleaseId());
                                    task.setReleaseTitle(file.getReleaseTitle());
                                    task.setProjectId(file.getProjectId());
                                    task.setProjectTitle(file.getProjectTitle());
                                    task.setCompanyId(file.getCompanyId());
                                    task.setCompanyTitle(file.getCompanyTitle());

                                    tempTasks.add(task);
                                } else if (cell.getCell().getCol() == TASKS_OWNER_COLUMN) {
                                    if (task != null && task.getRowNo() == cell.getCell().getRow()) {
                                        task.setOwner(cell.getCell().getValue());
                                    }
                                } else if (cell.getCell().getCol() == TASKS_EFFORT_COLUMN) {
                                    if (task != null && task.getRowNo() == cell.getCell().getRow()) {
                                        task.setTime(Long.valueOf(cell.getCell().getValue()));
                                    }
                                } else if (cell.getCell().getCol() == TASKS_DETAILS_COLUMN) {
                                    if (task != null && task.getRowNo() == cell.getCell().getRow()) {
                                        task.setDetails(cell.getCell().getValue());
                                    }
                                }
                            }
                        }
                    }

                    tasksData.clear();
                    tasksData.addAll(filterTasks(tempTasks));
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

    private List<ReleaseFile> getFiles(GoogleAccountCredential credential) {

        List<ReleaseFile> result = new ArrayList<ReleaseFile>();

        try {

            Map<String, File> companiesData = new HashMap<String, File>();
            Map<String, File> projectsData = new HashMap<String, File>();

            List<String> companiesIds = new ArrayList<String>();
            List<String> projectsIds = new ArrayList<String>();

            List<File> companies = searchFiles(credential, COMPANIES_QUERY);

            for (File file : companies) {
                if (file.getTitle().startsWith(COMPANY_DIRECTORY)) {
                    file.setTitle(file.getTitle().substring(COMPANY_DIRECTORY.length()));
                    companiesData.put(file.getId(), file);
                    companiesIds.add(file.getId());
                }
            }

            if (companiesIds.size() == 0) {
                return result;
            }

            List<File> projects = searchFiles(credential, "('" + StringUtil.join(companiesIds, "' in parents or '") + "' in parents) and trashed = false and mimeType = 'application/vnd.google-apps.folder'");

            for (File file : projects) {
                projectsData.put(file.getId(), file);
                projectsIds.add(file.getId());
            }

            if (projectsIds.size() == 0) {
                return result;
            }

            List<File> releases = searchFiles(credential, "title contains '" + RELEASE_FILE + "' and ('" + StringUtil.join(projectsIds, "' in parents or '") + "' in parents) and trashed = false and mimeType = 'application/vnd.google-apps.spreadsheet'");

            for (File file : releases) {

                if (file.getTitle().startsWith(RELEASE_FILE)) {

                    ReleaseFile release = new ReleaseFile();

                    release.setReleaseId(file.getId());
                    release.setReleaseTitle(file.getTitle().substring(RELEASE_FILE.length()));

                    File project = projectsData.get(file.getParents().get(0).getId());

                    release.setProjectId(project.getId());
                    release.setProjectTitle(project.getTitle());

                    File company = companiesData.get(project.getParents().get(0).getId());

                    release.setCompanyId(company.getId());
                    release.setCompanyTitle(company.getTitle());

                    result.add(release);
                }
            }

        } catch (Authorization e) {
            e.printStackTrace();
        }

        return result;
    }

    private void startNextActivity(Task task) {
        Intent intent = new Intent(AllTasksActivity.this, TaskActivity.class);
        intent.putExtra(COMPANY_TITLE, task.getCompanyTitle());
        intent.putExtra(COMPANY_ID, task.getCompanyId());
        intent.putExtra(PROJECT_TITLE, task.getProjectTitle());
        intent.putExtra(PROJECT_ID, task.getProjectId());
        intent.putExtra(RELEASE_TITLE, task.getReleaseTitle());
        intent.putExtra(RELEASE_ID, task.getReleaseId());
        intent.putExtra(TASK_DATA, task);
        startActivityForResult(intent, TasksActivity.STARTED_FROM_FLOW);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == TasksActivity.STARTED_FROM_FLOW) {
            loadTasks();
        }
    }
}
