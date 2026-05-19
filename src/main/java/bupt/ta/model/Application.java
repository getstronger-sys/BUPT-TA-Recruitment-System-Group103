package bupt.ta.model;

/**
 * Job application - TA applies for a job.
 */
public class Application {
    private String id;
    private String jobId;
    private String applicantId;  // TA user id
    private String applicantName;
    private String status;  // PENDING, INTERVIEW, WAITLIST, SELECTED, REJECTED, AUTO_CLOSED, WITHDRAWN
    private String appliedAt;
    private String notes;  // MO's notes when selecting
    /** Interview notice (in-app, not email) — set when MO sends batch/single notice */
    private String interviewTime;
    private String interviewLocation;
    private String interviewAssessment;
    private String interviewSlotId;
    private String interviewBookedAt;
    /** Legacy field kept for old JSON data; new applications do not choose numbered TA roles. */
    private String preferredRole;
    /** MO decision reason captured when selecting/rejecting after interview. */
    private String decisionReason;
    /** Optional feedback visible to the applicant on their application page. */
    private String applicantFeedback;

    public Application() {
        this.status = "PENDING";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAppliedAt() { return appliedAt; }
    public void setAppliedAt(String appliedAt) { this.appliedAt = appliedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getInterviewTime() { return interviewTime; }
    public void setInterviewTime(String interviewTime) { this.interviewTime = interviewTime; }
    public String getInterviewLocation() { return interviewLocation; }
    public void setInterviewLocation(String interviewLocation) { this.interviewLocation = interviewLocation; }
    public String getInterviewAssessment() { return interviewAssessment; }
    public void setInterviewAssessment(String interviewAssessment) { this.interviewAssessment = interviewAssessment; }
    public String getInterviewSlotId() { return interviewSlotId; }
    public void setInterviewSlotId(String interviewSlotId) { this.interviewSlotId = interviewSlotId; }
    public String getInterviewBookedAt() { return interviewBookedAt; }
    public void setInterviewBookedAt(String interviewBookedAt) { this.interviewBookedAt = interviewBookedAt; }
    public String getPreferredRole() { return preferredRole; }
    public void setPreferredRole(String preferredRole) { this.preferredRole = preferredRole; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public String getApplicantFeedback() { return applicantFeedback; }
    public void setApplicantFeedback(String applicantFeedback) { this.applicantFeedback = applicantFeedback; }
}
