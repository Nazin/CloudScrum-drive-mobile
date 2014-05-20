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
import java.util.List;

import pl.edu.agh.masters.cloudscrum.exception.Authorization;

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

    static final String IS_BACK = "isBack";

    protected boolean isBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isBack = getIntent().getBooleanExtra(IS_BACK, false);
    }

    protected SharedPreferences getPreferences() {
        return getSharedPreferences(this.getClass().getPackage().toString(), Activity.MODE_PRIVATE);
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
}
