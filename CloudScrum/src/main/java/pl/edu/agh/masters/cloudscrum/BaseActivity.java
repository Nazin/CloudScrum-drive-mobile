package pl.edu.agh.masters.cloudscrum;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import pl.edu.agh.masters.cloudscrum.exception.Authorization;
import pl.edu.agh.masters.cloudscrum.model.Task;

public class BaseActivity extends Activity {

    static final String APPLICATION_NAME = "CloudScrum";

    static final String ACCOUNT_NAME = "accountName";
    static final String PASSWORD = "password";

    static final String COMPANY_TITLE = "companyTitle";
    static final String COMPANY_ID = "companyId";

    static final String PROJECT_TITLE = "projectTitle";
    static final String PROJECT_ID = "projectId";

    static final String RELEASE_TITLE = "releaseTitle";
    static final String RELEASE_ID = "releaseId";

    static final String TASK_DATA = "taskData";

    static final String IS_BACK = "isBack";

    static final String CLOSED_ITERATION_IN_TITLE = "(Closed)";
    static final int STORIES_START_ROW = 11;
    static final int STORIES_ID_COLUMN = 2;
    static final int TASKS_TITLE_COLUMN = 4;
    static final int TASKS_OWNER_COLUMN = 5;
    static final int TASKS_STATUS_COLUMN = 6;
    static final int TASKS_EFFORT_COLUMN = 8;
    static final int TASKS_DETAILS_COLUMN = 9;

    static final String COMPANY_DIRECTORY = "CloudScrum-";
    static final String RELEASE_FILE = "release-";

    static final String COMPANIES_QUERY = "title contains '" + COMPANY_DIRECTORY + "' and ('root' in parents or sharedWithMe) and trashed = false and mimeType = 'application/vnd.google-apps.folder'";

    protected boolean isBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isBack = getIntent().getBooleanExtra(IS_BACK, false);
    }

    protected SharedPreferences getAppSharedPreferences() {
        return getAppSharedPreferences(Activity.MODE_PRIVATE);
    }

    protected SharedPreferences getAppSharedPreferences(int mode) {
        return getSharedPreferences(this.getClass().getPackage().toString(), mode);
    }

    protected GoogleAccountCredential getGoogleAccountCredential() {
        return GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE));
    }

    protected Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
    }

    protected ListView prepareListView(BaseAdapter adapter) {

        ListView listView = (ListView)findViewById(R.id.elements);
        listView.setItemsCanFocus(false);
        listView.setCacheColorHint(00000000);

        listView.setAdapter(adapter);
        listView.setEmptyView(findViewById(R.id.no_elements));

        return listView;
    }

    protected List<File> searchFiles(GoogleAccountCredential credential, String query) throws Authorization {

        List<File> result = null;
        Drive service = getDriveService(credential);

        try {
            result = new ArrayList<File>();
            Drive.Files.List request = service.files().list().setQ(query);
            FileList files = request.execute();
            result.addAll(files.getItems());
        } catch (UserRecoverableAuthIOException e) {
            throw new Authorization(e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    protected List<Task> filterTasks(List<Task> tasks) {

        SharedPreferences pref = getAppSharedPreferences();
        String accountName = pref.getString(ACCOUNT_NAME, "");

        for (Iterator<Task> taskIterator = tasks.iterator(); taskIterator.hasNext(); ) {
            if (!taskIterator.next().getOwner().equals(accountName)) {
                taskIterator.remove();
            }
        }

        return tasks;
    }
}
