package pl.edu.agh.masters.cloudscrum.model;

import java.io.Serializable;

public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String details = "";
    private String owner = "";
    private String status = "";
    private int rowNo;
    private int selectedWorksheet;
    private long time = 0;

    private String companyId;
    private String companyTitle;
    private String projectId;
    private String projectTitle;
    private String releaseId;
    private String releaseTitle;

    public Task(String title, int rowNo, int selectedWorksheet) {
        this.title = title;
        this.rowNo = rowNo;
        this.selectedWorksheet = selectedWorksheet;
    }

    public String getReleaseTitle() {
        return releaseTitle;
    }

    public void setReleaseTitle(String releaseTitle) {
        this.releaseTitle = releaseTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRowNo() {
        return rowNo;
    }

    public int getSelectedWorksheet() {
        return selectedWorksheet;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyTitle() {
        return companyTitle;
    }

    public void setCompanyTitle(String companyTitle) {
        this.companyTitle = companyTitle;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
    }
}
