package bupt.ta.service;

import bupt.ta.model.AdminSettings;
import bupt.ta.model.Application;
import bupt.ta.model.Job;
import bupt.ta.model.SiteNotification;
import bupt.ta.model.TAProfile;
import bupt.ta.model.User;
import bupt.ta.storage.DataStorage;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Creates in-app notifications for TA applicants when application state changes.
 * Comments describe intent for each {@link SiteNotification#getKind()} value.
 */
public class StudentNotificationService {

    private static final EmailNotificationService EMAIL_SERVICE = new EmailNotificationService();
    private static final Logger LOGGER = Logger.getLogger(StudentNotificationService.class.getName());

    public static final String KIND_APPLICATION_SUBMITTED = "APPLICATION_SUBMITTED";
    public static final String KIND_STATUS_INTERVIEW = "STATUS_INTERVIEW";
    public static final String KIND_STATUS_WAITLIST = "STATUS_WAITLIST";
    public static final String KIND_STATUS_SELECTED = "STATUS_SELECTED";
    public static final String KIND_STATUS_REJECTED = "STATUS_REJECTED";
    public static final String KIND_INTERVIEW_DETAILS = "INTERVIEW_DETAILS";
    public static final String KIND_AUTO_CLOSED = "AUTO_CLOSED";
    public static final String KIND_WITHDRAWN = "WITHDRAWN";
    public static final String KIND_AUTO_PROMOTED = "AUTO_PROMOTED";

    private static String jobLabel(Job job, String jobId) {
        if (job != null && job.getTitle() != null && !job.getTitle().trim().isEmpty()) {
            String code = job.getModuleCode() != null && !job.getModuleCode().trim().isEmpty()
                    ? " (" + job.getModuleCode().trim() + ")" : "";
            return job.getTitle().trim() + code;
        }
        return jobId != null ? jobId : "Unknown job";
    }

    /**
     * Notifies the applicant that an application was submitted.
     *
     * @param storage persistence
     * @param app     application record
     * @param job     job posting (may be null)
     * @throws IOException if notification persistence fails
     */
    public static void notifyApplicationSubmitted(DataStorage storage, Application app, Job job) throws IOException {
        if (app == null || app.getApplicantId() == null) {
            return;
        }
        SiteNotification n = new SiteNotification();
        n.setRecipientUserId(app.getApplicantId());
        n.setApplicationId(app.getId());
        n.setJobId(app.getJobId());
        n.setKind(KIND_APPLICATION_SUBMITTED);
        n.setTitle("Application received");
        n.setBody("Your application for " + jobLabel(job, app.getJobId()) + " was submitted successfully. "
                + "You will receive in-app updates when the module organiser reviews your application.");
        n.setRead(false);
        storage.addSiteNotification(n);
        AdminSettings adminSettings = storage.loadAdminSettings();
        sendNotificationEmail(storage, app.getApplicantId(),
                "[TA Recruitment] Application received",
                EMAIL_SERVICE.maybeAppendPortalLink(n.getBody(), "/ta/applications", adminSettings),
                adminSettings);
    }

    /**
     * Notifies the applicant that their application moved to the interview stage.
     *
     * @param storage persistence
     * @param app     application record
     * @param job     job posting (may be null)
     * @throws IOException if notification persistence fails
     */
    public static void notifyInterviewInvite(DataStorage storage, Application app, Job job) throws IOException {
        if (app == null || app.getApplicantId() == null) {
            return;
        }
        SiteNotification n = new SiteNotification();
        n.setRecipientUserId(app.getApplicantId());
        n.setApplicationId(app.getId());
        n.setJobId(app.getJobId());
        n.setKind(KIND_STATUS_INTERVIEW);
        n.setTitle("Interview stage");
        n.setBody("Your application for " + jobLabel(job, app.getJobId())
                + " has moved to the interview stage. Check My Applications for details; "
                + "the organiser may post time and location in-app.");
        n.setRead(false);
        storage.addSiteNotification(n);
        AdminSettings adminSettings = storage.loadAdminSettings();
        sendNotificationEmail(storage, app.getApplicantId(),
                "[TA Recruitment] Interview stage update",
                EMAIL_SERVICE.maybeAppendPortalLink(n.getBody(), "/ta/applications", adminSettings),
                adminSettings);
    }

    /**
     * Notifies the applicant that they are on the waitlist.
     *
     * @param storage persistence
     * @param app     application record
     * @param job     job posting (may be null)
     * @throws IOException if notification persistence fails
     */
    public static void notifyWaitlist(DataStorage storage, Application app, Job job) throws IOException {
        if (app == null || app.getApplicantId() == null) {
            return;
        }
        SiteNotification n = new SiteNotification();
        n.setRecipientUserId(app.getApplicantId());
        n.setApplicationId(app.getId());
        n.setJobId(app.getJobId());
        n.setKind(KIND_STATUS_WAITLIST);
        n.setTitle("Waitlist");
        n.setBody("Your application for " + jobLabel(job, app.getJobId())
                + " is now on the waitlist. You will be notified if the status changes.");
        n.setRead(false);
        storage.addSiteNotification(n);
        AdminSettings adminSettings = storage.loadAdminSettings();
        sendNotificationEmail(storage, app.getApplicantId(),
                "[TA Recruitment] Waitlist update",
                EMAIL_SERVICE.maybeAppendPortalLink(n.getBody(), "/ta/applications", adminSettings),
                adminSettings);
    }

    /**
     * Notifies the applicant that they were selected for the role.
     *
     * @param storage persistence
     * @param app     application record
     * @param job     job posting (may be null)
     * @throws IOException if notification persistence fails
     */
    public static void notifySelected(DataStorage storage, Application app, Job job) throws IOException {
        if (app == null || app.getApplicantId() == null) {
            return;
        }
        SiteNotification n = new SiteNotification();
        n.setRecipientUserId(app.getApplicantId());
        n.setApplicationId(app.getId());
        n.setJobId(app.getJobId());
        n.setKind(KIND_STATUS_SELECTED);
        n.setTitle("Application successful");
        n.setBody("Congratulations — you were selected for " + jobLabel(job, app.getJobId()) + ".");
        n.setRead(false);
        storage.addSiteNotification(n);
        AdminSettings adminSettings = storage.loadAdminSettings();
        sendNotificationEmail(storage, app.getApplicantId(),
                "[TA Recruitment] Application successful",
                EMAIL_SERVICE.maybeAppendPortalLink("Congratulations - you were selected for " + jobLabel(job, app.getJobId()) + ".", "/ta/applications", adminSettings),
                adminSettings);
    }

    /**
     * Notifies the applicant that they were not selected.
     *
     * @param storage persistence
     * @param app     application record
     * @param job     job posting (may be null)
     * @param moNotes optional organiser message included in the body
     * @throws IOException if notification persistence fails
     */
    public static void notifyRejected(DataStorage storage, Application app, Job job, String moNotes) throws IOException {
        if (app == null || app.getApplicantId() == null) {
            return;
        }
        SiteNotification n = new SiteNotification();
        n.setRecipientUserId(app.getApplicantId());
        n.setApplicationId(app.getId());
        n.setJobId(app.getJobId());
        n.setKind(KIND_STATUS_REJECTED);
        n.setTitle("Application update");
        StringBuilder body = new StringBuilder();
        body.append("Your application for ").append(jobLabel(job, app.getJobId()))
                .append(" was not successful.");
        if (moNotes != null && !moNotes.trim().isEmpty()) {
            body.append(" Message from organiser: ").append(moNotes.trim());
        }
        n.setBody(body.toString());
        n.setRead(false);
        storage.addSiteNotification(n);
        AdminSettings adminSettings = storage.loadAdminSettings();
        sendNotificationEmail(storage, app.getApplicantId(),
                "[TA Recruitment] Application update",
                EMAIL_SERVICE.maybeAppendPortalLink(n.getBody(), "/ta/applications", adminSettings),
                adminSettings);
    }

    /**
     * Notifies the applicant of interview time, location, and assessment notes.
     *
     * @param storage persistence
     * @param app     application record (carries interview fields)
     * @param job     job posting (may be null)
     * @throws IOException if notification persistence fails
     */
    public static void notifyInterviewDetails(DataStorage storage, Application app, Job job) throws IOException {
        if (app == null || app.getApplicantId() == null) {
            return;
        }
        String time = app.getInterviewTime() != null ? app.getInterviewTime().trim() : "";
        String loc = app.getInterviewLocation() != null ? app.getInterviewLocation().trim() : "";
        String assess = app.getInterviewAssessment() != null ? app.getInterviewAssessment().trim() : "";
        SiteNotification n = new SiteNotification();
        n.setRecipientUserId(app.getApplicantId());
        n.setApplicationId(app.getId());
        n.setJobId(app.getJobId());
        n.setKind(KIND_INTERVIEW_DETAILS);
        n.setTitle("Interview information");
        StringBuilder body = new StringBuilder();
        body.append("Interview details for ").append(jobLabel(job, app.getJobId())).append(":\n");
        body.append("Time: ").append(time.isEmpty() ? "—" : time).append("\n");
        body.append("Location: ").append(loc.isEmpty() ? "—" : loc);
        if (!assess.isEmpty()) {
            body.append("\nAssessment / notes: ").append(assess);
        }
        n.setBody(body.toString());
        n.setRead(false);
        storage.addSiteNotification(n);
        AdminSettings adminSettings = storage.loadAdminSettings();
        sendNotificationEmail(storage, app.getApplicantId(),
                "[TA Recruitment] Interview information",
                EMAIL_SERVICE.maybeAppendPortalLink(
                        n.getBody() + "\n\nYou can also download an interview calendar file from My Applications.",
                        "/ta/applications",
                        adminSettings),
                adminSettings);
    }

    /**
     * Notifies the applicant that a pending application was auto-closed (workload cap).
     *
     * @param storage persistence
     * @param app     application record
     * @param job     job posting (may be null)
     * @param note    optional system note appended to the body
     * @throws IOException if notification persistence fails
     */
    public static void notifyAutoClosed(DataStorage storage, Application app, Job job, String note) throws IOException {
        if (app == null || app.getApplicantId() == null) {
            return;
        }
        SiteNotification n = new SiteNotification();
        n.setRecipientUserId(app.getApplicantId());
        n.setApplicationId(app.getId());
        n.setJobId(app.getJobId());
        n.setKind(KIND_AUTO_CLOSED);
        n.setTitle("Application closed automatically");
        String extra = note != null && !note.trim().isEmpty() ? " " + note.trim() : "";
        n.setBody("Your pending application for " + jobLabel(job, app.getJobId())
                + " was closed automatically because you reached the maximum number of selected positions." + extra);
        n.setRead(false);
        storage.addSiteNotification(n);
        AdminSettings adminSettings = storage.loadAdminSettings();
        sendNotificationEmail(storage, app.getApplicantId(),
                "[TA Recruitment] Application closed automatically",
                EMAIL_SERVICE.maybeAppendPortalLink(n.getBody(), "/ta/applications", adminSettings),
                adminSettings);
    }

    /**
     * Notifies the applicant that they withdrew an application.
     *
     * @param storage persistence
     * @param app     application record
     * @param job     job posting (may be null)
     * @throws IOException if notification persistence fails
     */
    public static void notifyWithdrawn(DataStorage storage, Application app, Job job) throws IOException {
        if (app == null || app.getApplicantId() == null) {
            return;
        }
        SiteNotification n = new SiteNotification();
        n.setRecipientUserId(app.getApplicantId());
        n.setApplicationId(app.getId());
        n.setJobId(app.getJobId());
        n.setKind(KIND_WITHDRAWN);
        n.setTitle("Application withdrawn");
        n.setBody("You withdrew your application for " + jobLabel(job, app.getJobId()) + ".");
        n.setRead(false);
        storage.addSiteNotification(n);
        AdminSettings adminSettings = storage.loadAdminSettings();
        sendNotificationEmail(storage, app.getApplicantId(),
                "[TA Recruitment] Application withdrawn",
                EMAIL_SERVICE.maybeAppendPortalLink(n.getBody(), "/ta/applications", adminSettings),
                adminSettings);
    }

    /**
     * Notifies the applicant that they were promoted from the waitlist to selected.
     *
     * @param storage persistence
     * @param app     application record
     * @param job     job posting (may be null)
     * @throws IOException if notification persistence fails
     */
    public static void notifyAutoPromotedFromWaitlist(DataStorage storage, Application app, Job job) throws IOException {
        if (app == null || app.getApplicantId() == null) {
            return;
        }
        SiteNotification n = new SiteNotification();
        n.setRecipientUserId(app.getApplicantId());
        n.setApplicationId(app.getId());
        n.setJobId(app.getJobId());
        n.setKind(KIND_AUTO_PROMOTED);
        n.setTitle("Promoted from waitlist");
        n.setBody("You were automatically selected for " + jobLabel(job, app.getJobId())
                + " from the waitlist after a vacancy opened.");
        n.setRead(false);
        storage.addSiteNotification(n);
        AdminSettings adminSettings = storage.loadAdminSettings();
        sendNotificationEmail(storage, app.getApplicantId(),
                "[TA Recruitment] Promoted from waitlist",
                EMAIL_SERVICE.maybeAppendPortalLink(n.getBody(), "/ta/applications", adminSettings),
                adminSettings);
    }

    private static void sendNotificationEmail(DataStorage storage, String userId, String subject, String body, AdminSettings adminSettings) {
        if (storage == null || userId == null || userId.trim().isEmpty()) {
            return;
        }
        try {
            // When email channel is disabled / not configured, do nothing (no send attempt, no warnings).
            if (adminSettings != null) {
                if (!EMAIL_SERVICE.isConfigured(adminSettings)) {
                    return;
                }
            } else {
                if (!EMAIL_SERVICE.isConfigured()) {
                    return;
                }
            }

            String recipientEmail = resolveRecipientEmail(storage, userId);
            if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
                LOGGER.info("Skip email notification because recipient email is missing for userId=" + userId);
                return;
            }
            User u = storage.findUserById(userId);
            String displayName = u != null && u.getRealName() != null && !u.getRealName().trim().isEmpty()
                    ? u.getRealName().trim()
                    : (u != null && u.getUsername() != null && !u.getUsername().trim().isEmpty()
                        ? u.getUsername().trim()
                        : userId);
            String portalUrl = EMAIL_SERVICE.resolvePortalUrl("/ta/applications", adminSettings);
            String html = EMAIL_SERVICE.renderHtmlTemplate(
                    "Recruitment update",
                    displayName,
                    body,
                    portalUrl != null ? "Open portal" : null,
                    portalUrl);
            EmailNotificationService.SendResult result = EMAIL_SERVICE.sendHtml(recipientEmail, subject, body, html, adminSettings);
            if (result.isSuccess()) {
                LOGGER.info("Email notification sent to " + recipientEmail + " for userId=" + userId
                        + " subject=" + subject);
            } else {
                LOGGER.warning("Email notification failed for " + recipientEmail + " userId=" + userId
                        + " subject=" + subject + " detail=" + result.getDetail());
            }
        } catch (IOException e) {
            LOGGER.warning("Email notification lookup failed for userId=" + userId + " detail=" + e.getMessage());
        }
    }

    private static String resolveRecipientEmail(DataStorage storage, String userId) throws IOException {
        TAProfile profile = storage.getProfileByUserId(userId);
        if (profile != null && profile.getEmail() != null && !profile.getEmail().trim().isEmpty()) {
            return profile.getEmail().trim();
        }
        User user = storage.findUserById(userId);
        if (user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail().trim();
        }
        return null;
    }
}
