package pl.edu.agh.masters.cloudscrum;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import com.google.api.services.drive.model.File;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import pl.edu.agh.masters.cloudscrum.adapter.FolderListAdapter;
import pl.edu.agh.masters.cloudscrum.exception.Authorization;

public class MainActivity extends BaseActivity {

    static final int REQUEST_ACCOUNT_PICKER = 1;
    static final int REQUEST_AUTHORIZATION = 2;

    private GoogleAccountCredential credential;
    private String accountName;
    private String password;

    private List<File> companiesData = new ArrayList<File>();
    private FolderListAdapter companiesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        credential = getGoogleAccountCredential();

        SharedPreferences pref = getAppSharedPreferences();
        accountName = pref.getString(ACCOUNT_NAME, "");
        password = pref.getString(PASSWORD, "");

        if (accountName.equals("")) {
            startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        } else if (password.equals("")) {
            inputPassword();
        } else {
            loadData();
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
                        if (password.equals("")) {
                            inputPassword();
                        } else {
                            loadData();
                        }
                    }
                }

                break;

            case REQUEST_AUTHORIZATION:

                if (resultCode == Activity.RESULT_OK) {

                    data.getExtras();

                    new AsyncTask<Void, Void, Void>() {

                        boolean hasToken = false;

                        @Override
                        protected Void doInBackground(Void... params) {
                            hasToken = hasAccessToken();
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void v) {
                            if (hasToken) {
                                loadData();
                            }
                        }
                    }.execute();
                } else {
                    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                }

                break;
        }
    }

    public boolean hasAccessToken() {

        try {
            credential.getToken();
            return true;
        } catch (GooglePlayServicesAvailabilityException e) {
            e.printStackTrace();
        } catch (UserRecoverableAuthException e) {
            e.printStackTrace();
            startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GoogleAuthException e) {
            e.printStackTrace();
        }

        return false;
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

    private void loadData() {

        SharedPreferences pref = getAppSharedPreferences();
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
                        if (file.getTitle().startsWith(COMPANY_DIRECTORY)) {
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
            result = searchFiles(credential, COMPANIES_QUERY);
        } catch (Authorization e) {
            startActivityForResult(e.getOriginalException().getIntent(), REQUEST_AUTHORIZATION);
        }

        return result;
    }

    private void inputPassword() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.please_provide_password);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                password = input.getText().toString();
                verifyPassword();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();
            }
        });

        builder.show();
    }

    private void verifyPassword() {

        new AsyncTask<Void, Void, Void>() {

            ProgressDialog dialog;
            SpreadsheetService service;
            boolean isPasswordCorrect;

            protected void onPreExecute() {
                dialog = new ProgressDialog(MainActivity.this);
                dialog.setMessage(MainActivity.this.getString(R.string.verifying_password));
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {

                try {

                    service = new SpreadsheetService(APPLICATION_NAME);
                    service.setProtocolVersion(SpreadsheetService.Versions.V3);
                    service.setUserCredentials(accountName, password);

                    service.getFeed(new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full"), SpreadsheetFeed.class);

                    isPasswordCorrect = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    isPasswordCorrect = false;
                }

                return null;
            }

            protected void onPostExecute(Void result) {

                if (dialog != null) {
                    dialog.dismiss();
                }

                if (isPasswordCorrect) {
                    SharedPreferences pref = getAppSharedPreferences();
                    final SharedPreferences.Editor editor = pref.edit();
                    editor.putString(PASSWORD, password);
                    editor.commit();
                    loadData();
                } else {
                    Toast.makeText(MainActivity.this, R.string.incorrect_password, Toast.LENGTH_SHORT).show();
                    inputPassword();
                }
            }

            protected void onCancelled() {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        }.execute();
    }

    private void startNextActivity(File file) {
        Intent intent = new Intent(MainActivity.this, ProjectsActivity.class);
        intent.putExtra(COMPANY_TITLE, file.getTitle());
        intent.putExtra(COMPANY_ID, file.getId());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_all_tasks:
                Intent intent = new Intent(this, AllTasksActivity.class);
                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
