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

import java.util.ArrayList;
import java.util.List;

import pl.edu.agh.masters.cloudscrum.adapter.FolderListAdapter;
import pl.edu.agh.masters.cloudscrum.exception.Authorization;

public class ReleasesActivity extends BaseActivity {

    static final String RELEASE_FILE = "release-";

    private String companyId;
    private String companyTitle;
    private String projectId;
    private String projectTitle;

    private List<File> releasesData = new ArrayList<File>();
    private FolderListAdapter releasesAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        companyTitle = getIntent().getStringExtra(COMPANY_TITLE);
        companyId = getIntent().getStringExtra(COMPANY_ID);
        projectTitle = getIntent().getStringExtra(PROJECT_TITLE);
        projectId = getIntent().getStringExtra(PROJECT_ID);

        ((TextView)findViewById(R.id.title)).setText(companyTitle + " > " + projectTitle);

        setList();
        loadReleases();
    }

    @Override
    public void onBackPressed() {

        if (isBack) {
            Intent intent = new Intent(this, ProjectsActivity.class);
            intent.putExtra(IS_BACK, true);
            intent.putExtra(COMPANY_TITLE, companyTitle);
            intent.putExtra(COMPANY_ID, companyId);
            startActivity(intent);
        }

        finish();
    }

    private void setList() {

        releasesAdapter = new FolderListAdapter(this, releasesData);
        ListView releasesListView = prepareListView(releasesAdapter);

        releasesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                startNextActivity(releasesData.get(i));
            }
        });
    }

    private void loadReleases() {

        final GoogleAccountCredential credential = getGoogleAccountCredential();
        SharedPreferences pref = getPreferences();

        String accountName = pref.getString(ACCOUNT_NAME, "");
        credential.setSelectedAccountName(accountName);

        new AsyncTask<Void, Void, List<File>>() {

            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = new ProgressDialog(ReleasesActivity.this);
                dialog.setMessage(ReleasesActivity.this.getString(R.string.loading_releases));
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected List<File> doInBackground(Void... params) {
                try {
                    return searchFiles(credential, "title contains '" + RELEASE_FILE + "' and '" + projectId + "' in parents and trashed = false and mimeType = 'application/vnd.google-apps.spreadsheet'");
                } catch (Authorization e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected void onPostExecute(List<File> result) {

                if (dialog != null) {
                    dialog.dismiss();
                }

                if (result != null) {

                    releasesData.clear();

                    for (File file : result) {
                        if (file.getTitle().startsWith(RELEASE_FILE)){
                            file.setTitle(file.getTitle().substring(RELEASE_FILE.length()));
                            releasesData.add(file);
                        }
                    }

                    if (releasesData.size() == 1 && (!isBack)) {
                        startNextActivity(releasesData.get(0));
                    }
                }

                releasesAdapter.notifyDataSetChanged();
            }

            protected void onCancelled() {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        }.execute();
    }

    private void startNextActivity(File file) {
        Intent intent = new Intent(ReleasesActivity.this, TasksActivity.class);
        intent.putExtra(COMPANY_TITLE, companyTitle);
        intent.putExtra(COMPANY_ID, companyId);
        intent.putExtra(PROJECT_TITLE, projectTitle);
        intent.putExtra(PROJECT_ID, projectId);
        intent.putExtra(RELEASE_TITLE, file.getTitle());
        intent.putExtra(RELEASE_ID, file.getId());
        startActivity(intent);
    }
}
