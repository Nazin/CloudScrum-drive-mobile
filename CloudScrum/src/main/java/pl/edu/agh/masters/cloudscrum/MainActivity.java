package pl.edu.agh.masters.cloudscrum;

import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    static final int REQUEST_ACCOUNT_PICKER = 1;
    static final String COMPANY_DIRECTORY = "CloudScrum-";

    private GoogleAccountCredential credential;
    private String accountName;

    private List<File> companiesData = new ArrayList<File>();
    FolderListAdapter companiesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        credential = getGoogleAccountCredential();

        SharedPreferences pref = getPreferences();
        String accountName = pref.getString(ACCOUNT_NAME, "");

        if (accountName.equals("")) {
            startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        } else {
            loadData(accountName);
        }

        setList();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        switch (requestCode) {

            case REQUEST_ACCOUNT_PICKER:

                if (resultCode == RESULT_OK && data != null && data.getExtras() != null){

                    accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                    if (accountName != null) {
                        loadData(accountName);
                    }
                }

                break;
        }
    }

    private void setList() {

        companiesAdapter = new FolderListAdapter(this, companiesData);
        ListView companiesListView = prepareListView(companiesAdapter);

        companiesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                startNextActivity(companiesData.get(i));
            }
        });
    }

    private void loadData(String accountName) {

        SharedPreferences pref = getPreferences();
        final SharedPreferences.Editor editor = pref.edit();
        editor.putString(ACCOUNT_NAME, accountName);
        editor.commit();

        credential.setSelectedAccountName(accountName);

        new AsyncTask<Void, Void, List<File>>() {

            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = new ProgressDialog(MainActivity.this);
                dialog.setMessage(MainActivity.this.getString(R.string.loading_companies));
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected List<File> doInBackground(Void... params) {
                return getFiles();
            }

            protected void onPostExecute(List<File> result) {

                if (dialog != null) {
                    dialog.dismiss();
                }

                if (result != null) {

                    companiesData.clear();

                    for (File file : result) {
                        if (file.getTitle().startsWith(COMPANY_DIRECTORY)){
                            file.setTitle(file.getTitle().substring(COMPANY_DIRECTORY.length()));
                            companiesData.add(file);
                        }
                    }

                    if (companiesData.size() == 1 && (!isBack)) {
                        startNextActivity(companiesData.get(0));
                    }
                }

                companiesAdapter.notifyDataSetChanged();
            }

            protected void onCancelled() {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        }.execute();
    }

    private List<File> getFiles() {

        List<File> result = null;

        try {
            result = searchFiles(credential, "title contains '" + COMPANY_DIRECTORY + "' and 'root' in parents and trashed = false and mimeType = 'application/vnd.google-apps.folder'");
        } catch (UserRecoverableAuthIOException e) {
            Log.e("AUTH", "AUTH PROBLEM");
        }

        return result;
    }

    private void startNextActivity(File file) {
        Intent intent = new Intent(MainActivity.this, ProjectsActivity.class);
        intent.putExtra(COMPANY_TITLE, file.getTitle());
        intent.putExtra(COMPANY_ID, file.getId());
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
