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
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.List;

public class ProjectsActivity extends BaseActivity {

    private String companyId;
    private String companyTitle;

    private List<File> projectsData = new ArrayList<File>();
    FolderListAdapter projectsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        companyTitle = getIntent().getStringExtra(COMPANY_TITLE);
        companyId = getIntent().getStringExtra(COMPANY_ID);

        ((TextView)findViewById(R.id.title)).setText(companyTitle);

        setList();
        loadProjects();
    }

    @Override
    public void onBackPressed() {

        if (isBack) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(IS_BACK, true);
            startActivity(intent);
        }

        finish();
    }

    private void setList() {

        projectsAdapter = new FolderListAdapter(this, projectsData);
        ListView projectsListView = prepareListView(projectsAdapter);

        projectsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                startNextActivity(projectsData.get(i));
            }
        });
    }

    private void loadProjects() {

        final GoogleAccountCredential credential = getGoogleAccountCredential();
        SharedPreferences pref = getPreferences();

        String accountName = pref.getString(ACCOUNT_NAME, "");
        credential.setSelectedAccountName(accountName);

        new AsyncTask<Void, Void, List<File>>() {

            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = new ProgressDialog(ProjectsActivity.this);
                dialog.setMessage(ProjectsActivity.this.getString(R.string.loading_projects));
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected List<File> doInBackground(Void... params) {
                try {
                    return searchFiles(credential, "'" + companyId + "' in parents  and trashed = false and mimeType = 'application/vnd.google-apps.folder'");
                } catch (UserRecoverableAuthIOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected void onPostExecute(List<File> result) {

                if (dialog != null) {
                    dialog.dismiss();
                }

                if (result != null) {

                    projectsData.clear();

                    for (File file : result) {
                        projectsData.add(file);
                    }

                    if (projectsData.size() == 1 && (!isBack)) {
                        startNextActivity(projectsData.get(0));
                    }
                }

                projectsAdapter.notifyDataSetChanged();
            }

            protected void onCancelled() {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        }.execute();
    }

    private void startNextActivity(File file) {
        Intent intent = new Intent(ProjectsActivity.this, ReleasesActivity.class);
        intent.putExtra(COMPANY_TITLE, companyTitle);
        intent.putExtra(COMPANY_ID, companyId);
        intent.putExtra(PROJECT_TITLE, file.getTitle());
        intent.putExtra(PROJECT_ID, file.getId());
        startActivity(intent);
    }
}
