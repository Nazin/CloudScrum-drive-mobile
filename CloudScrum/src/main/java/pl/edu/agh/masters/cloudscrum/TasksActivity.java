package pl.edu.agh.masters.cloudscrum;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class TasksActivity extends BaseActivity {

    private String companyId;
    private String companyTitle;
    private String projectId;
    private String projectTitle;
    private String releaseId;
    private String releaseTitle;

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

        //setList();
        //loadTasks();
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
}
