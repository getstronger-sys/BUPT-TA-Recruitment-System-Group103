package bupt.ta.service;

import bupt.ta.model.AdminSettings;
import bupt.ta.model.Application;
import bupt.ta.model.Job;
import bupt.ta.model.SiteNotification;
import bupt.ta.model.TAProfile;
import bupt.ta.model.User;
import bupt.ta.storage.DataStorage;
import bupt.ta.util.JobActivity;
import bupt.ta.util.JobWorkloadEstimator;

import java.io.IOException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared admin analytics and workload-enforcement logic.
 */
public class AdminService {

    /** Application status set when workload rules auto-close excess selections. */
    public static final String STATUS_AUTO_CLOSED = "AUTO_CLOSED";
    private static final String AUTO_CLOSE_NOTE_PREFIX = "[System] Closed automatically — workload cap reached (";

    /** Aggregate counts for the admin dashboard. */
    public static class DashboardSummary {
        private final int totalJobs;
        private final int openJobs;
        private final int inactiveJobs;
        private final int totalApplications;
        private final int totalTas;
        private final int totalMos;
        private final int totalAdmins;
        private final int tasAtOrOverLimit;
        private final int jobsAtCapacity;
        private final Map<String, Integer> applicationCounts;

        public DashboardSummary(int totalJobs, int openJobs, int inactiveJobs, int totalApplications,
                                int totalTas, int totalMos, int totalAdmins, int tasAtOrOverLimit,
                                int jobsAtCapacity, Map<String, Integer> applicationCounts) {
            this.totalJobs = totalJobs;
            this.openJobs = openJobs;
            this.inactiveJobs = inactiveJobs;
            this.totalApplications = totalApplications;
            this.totalTas = totalTas;
            this.totalMos = totalMos;
            this.totalAdmins = totalAdmins;
            this.tasAtOrOverLimit = tasAtOrOverLimit;
            this.jobsAtCapacity = jobsAtCapacity;
            this.applicationCounts = applicationCounts != null ? applicationCounts : Collections.emptyMap();
        }

        public int getTotalJobs() { return totalJobs; }
        public int getOpenJobs() { return openJobs; }
        public int getInactiveJobs() { return inactiveJobs; }
        public int getTotalApplications() { return totalApplications; }
        public int getTotalTas() { return totalTas; }
        public int getTotalMos() { return totalMos; }
        public int getTotalAdmins() { return totalAdmins; }
        public int getTasAtOrOverLimit() { return tasAtOrOverLimit; }
        public int getJobsAtCapacity() { return jobsAtCapacity; }
        public Map<String, Integer> getApplicationCounts() { return applicationCounts; }
        public int getCount(String status) { return applicationCounts.getOrDefault(status, 0); }
    }

    /** Filtered user list with per-role activity metrics. */
    public static class UserDirectoryReport {
        private final int totalUsers;
        private final int totalTas;
        private final int totalMos;
        private final int totalAdmins;
        private final String roleFilter;
        private final String query;
        private final List<UserListRow> rows;

        public UserDirectoryReport(int totalUsers, int totalTas, int totalMos, int totalAdmins,
                                   String roleFilter, String query, List<UserListRow> rows) {
            this.totalUsers = totalUsers;
            this.totalTas = totalTas;
            this.totalMos = totalMos;
            this.totalAdmins = totalAdmins;
            this.roleFilter = roleFilter != null ? roleFilter : "ALL";
            this.query = query != null ? query : "";
            this.rows = rows != null ? rows : Collections.emptyList();
        }

        public int getTotalUsers() { return totalUsers; }
        public int getTotalTas() { return totalTas; }
        public int getTotalMos() { return totalMos; }
        public int getTotalAdmins() { return totalAdmins; }
        public String getRoleFilter() { return roleFilter; }
        public String getQuery() { return query; }
        public List<UserListRow> getRows() { return rows; }
        public int getVisibleCount() { return rows.size(); }
    }

    /** One row in the admin user directory. */
    public static class UserListRow {
        private final User user;
        private final TAProfile profile;
        private final int selectedCount;
        private final int pendingCount;
        private final int interviewCount;
        private final int waitlistCount;
        private final int autoClosedCount;
        private final int unreadNotificationCount;
        private final int savedJobsCount;
        private final int postedJobsCount;
        private final int activeJobsCount;
        private final int totalApplicationsReceived;
        private final int totalSelectedForMo;

        public UserListRow(User user, TAProfile profile, int selectedCount, int pendingCount, int interviewCount,
                           int waitlistCount, int autoClosedCount, int unreadNotificationCount, int savedJobsCount,
                           int postedJobsCount, int activeJobsCount, int totalApplicationsReceived,
                           int totalSelectedForMo) {
            this.user = user;
            this.profile = profile;
            this.selectedCount = selectedCount;
            this.pendingCount = pendingCount;
            this.interviewCount = interviewCount;
            this.waitlistCount = waitlistCount;
            this.autoClosedCount = autoClosedCount;
            this.unreadNotificationCount = unreadNotificationCount;
            this.savedJobsCount = savedJobsCount;
            this.postedJobsCount = postedJobsCount;
            this.activeJobsCount = activeJobsCount;
            this.totalApplicationsReceived = totalApplicationsReceived;
            this.totalSelectedForMo = totalSelectedForMo;
        }

        public User getUser() { return user; }
        public TAProfile getProfile() { return profile; }
        public int getSelectedCount() { return selectedCount; }
        public int getPendingCount() { return pendingCount; }
        public int getInterviewCount() { return interviewCount; }
        public int getWaitlistCount() { return waitlistCount; }
        public int getAutoClosedCount() { return autoClosedCount; }
        public int getUnreadNotificationCount() { return unreadNotificationCount; }
        public int getSavedJobsCount() { return savedJobsCount; }
        public int getPostedJobsCount() { return postedJobsCount; }
        public int getActiveJobsCount() { return activeJobsCount; }
        public int getTotalApplicationsReceived() { return totalApplicationsReceived; }
        public int getTotalSelectedForMo() { return totalSelectedForMo; }
        public String getDisplayName() { return resolveUserName(user, user != null ? user.getId() : "-"); }
        public String getEmail() {
            if (profile != null && !isBlank(profile.getEmail())) {
                return profile.getEmail();
            }
            return user != null ? user.getEmail() : "";
        }
        public String getStudentId() {
            if (profile != null && !isBlank(profile.getStudentId())) {
                return profile.getStudentId();
            }
            return user != null ? user.getStudentId() : "";
        }
    }

    /** TA workload summary for the admin workload page. */
    public static class WorkloadRow {
        private final String applicantId;
        private final String applicantName;
        private final int selectedCount;
        private final int pendingCount;
        private final double estimatedSelectedHours;
        private final boolean aboveAverage;
        private final boolean atOrOverLimit;
        private final boolean aboveLimit;
        private final List<String> selectedJobTitles;

        public WorkloadRow(String applicantId, String applicantName, int selectedCount, int pendingCount,
                           double estimatedSelectedHours,
                           boolean aboveAverage, boolean atOrOverLimit, boolean aboveLimit,
                           List<String> selectedJobTitles) {
            this.applicantId = applicantId;
            this.applicantName = applicantName;
            this.selectedCount = selectedCount;
            this.pendingCount = pendingCount;
            this.estimatedSelectedHours = estimatedSelectedHours;
            this.aboveAverage = aboveAverage;
            this.atOrOverLimit = atOrOverLimit;
            this.aboveLimit = aboveLimit;
            this.selectedJobTitles = selectedJobTitles != null ? selectedJobTitles : Collections.emptyList();
        }

        public String getApplicantId() { return applicantId; }
        public String getApplicantName() { return applicantName; }
        public int getSelectedCount() { return selectedCount; }
        public int getPendingCount() { return pendingCount; }
        public double getEstimatedSelectedHours() { return estimatedSelectedHours; }
        public boolean isAboveAverage() { return aboveAverage; }
        public boolean isAtOrOverLimit() { return atOrOverLimit; }
        public boolean isAboveLimit() { return aboveLimit; }
        public List<String> getSelectedJobTitles() { return selectedJobTitles; }
    }

    /** TA who is at or over the configured workload cap. */
    public static class LimitAlert {
        private final String applicantId;
        private final String applicantName;
        private final int selectedCount;
        private final int pendingCount;
        /** Human-readable load vs cap, e.g. "3 jobs / cap 2" or "58.0 h / cap 40.0 h". */
        private final String loadVsCap;

        public LimitAlert(String applicantId, String applicantName, int selectedCount, int pendingCount, String loadVsCap) {
            this.applicantId = applicantId;
            this.applicantName = applicantName;
            this.selectedCount = selectedCount;
            this.pendingCount = pendingCount;
            this.loadVsCap = loadVsCap != null ? loadVsCap : "";
        }

        public String getApplicantId() { return applicantId; }
        public String getApplicantName() { return applicantName; }
        public int getSelectedCount() { return selectedCount; }
        public int getPendingCount() { return pendingCount; }
        public String getLoadVsCap() { return loadVsCap; }
    }

    /** Application in interview stage missing expected notice fields. */
    public static class InterviewNoticeAlert {
        private final String applicationId;
        private final String applicantId;
        private final String applicantName;
        private final String jobTitle;
        private final String moduleCode;
        private final boolean missingTime;
        private final boolean missingLocation;

        public InterviewNoticeAlert(String applicationId, String applicantId, String applicantName,
                                    String jobTitle, String moduleCode,
                                    boolean missingTime, boolean missingLocation) {
            this.applicationId = applicationId;
            this.applicantId = applicantId;
            this.applicantName = applicantName;
            this.jobTitle = jobTitle;
            this.moduleCode = moduleCode;
            this.missingTime = missingTime;
            this.missingLocation = missingLocation;
        }

        public String getApplicationId() { return applicationId; }
        public String getApplicantId() { return applicantId; }
        public String getApplicantName() { return applicantName; }
        public String getJobTitle() { return jobTitle; }
        public String getModuleCode() { return moduleCode; }
        public boolean isMissingTime() { return missingTime; }
        public boolean isMissingLocation() { return missingLocation; }
    }

    /** Application data anomaly flagged for admin review. */
    public static class ApplicationAlert {
        private final String applicationId;
        private final String applicantId;
        private final String applicantName;
        private final String jobTitle;
        private final String moduleCode;
        private final String status;
        private final String issue;

        public ApplicationAlert(String applicationId, String applicantId, String applicantName, String jobTitle,
                                String moduleCode, String status, String issue) {
            this.applicationId = applicationId;
            this.applicantId = applicantId;
            this.applicantName = applicantName;
            this.jobTitle = jobTitle;
            this.moduleCode = moduleCode;
            this.status = status;
            this.issue = issue;
        }

        public String getApplicationId() { return applicationId; }
        public String getApplicantId() { return applicantId; }
        public String getApplicantName() { return applicantName; }
        public String getJobTitle() { return jobTitle; }
        public String getModuleCode() { return moduleCode; }
        public String getStatus() { return status; }
        public String getIssue() { return issue; }
    }

    /** Job posting that has reached its selection capacity. */
    public static class CapacityAlert {
        private final String jobId;
        private final String jobTitle;
        private final String moduleCode;
        private final int selectedCount;
        private final int maxApplicants;

        public CapacityAlert(String jobId, String jobTitle, String moduleCode, int selectedCount, int maxApplicants) {
            this.jobId = jobId;
            this.jobTitle = jobTitle;
            this.moduleCode = moduleCode;
            this.selectedCount = selectedCount;
            this.maxApplicants = maxApplicants;
        }

        public String getJobId() { return jobId; }
        public String getJobTitle() { return jobTitle; }
        public String getModuleCode() { return moduleCode; }
        public int getSelectedCount() { return selectedCount; }
        public int getMaxApplicants() { return maxApplicants; }
    }

    /** Aggregated alerts for the admin monitoring dashboard. */
    public static class MonitoringReport {
        private final List<LimitAlert> limitAlerts;
        private final List<InterviewNoticeAlert> interviewNoticeAlerts;
        private final List<ApplicationAlert> inactiveJobAlerts;
        private final List<ApplicationAlert> missingJobAlerts;
        private final List<CapacityAlert> capacityAlerts;

        public MonitoringReport(List<LimitAlert> limitAlerts,
                                List<InterviewNoticeAlert> interviewNoticeAlerts,
                                List<ApplicationAlert> inactiveJobAlerts,
                                List<ApplicationAlert> missingJobAlerts,
                                List<CapacityAlert> capacityAlerts) {
            this.limitAlerts = limitAlerts != null ? limitAlerts : Collections.emptyList();
            this.interviewNoticeAlerts = interviewNoticeAlerts != null ? interviewNoticeAlerts : Collections.emptyList();
            this.inactiveJobAlerts = inactiveJobAlerts != null ? inactiveJobAlerts : Collections.emptyList();
            this.missingJobAlerts = missingJobAlerts != null ? missingJobAlerts : Collections.emptyList();
            this.capacityAlerts = capacityAlerts != null ? capacityAlerts : Collections.emptyList();
        }

        public List<LimitAlert> getLimitAlerts() { return limitAlerts; }
        public List<InterviewNoticeAlert> getInterviewNoticeAlerts() { return interviewNoticeAlerts; }
        public List<ApplicationAlert> getInactiveJobAlerts() { return inactiveJobAlerts; }
        public List<ApplicationAlert> getMissingJobAlerts() { return missingJobAlerts; }
        public List<CapacityAlert> getCapacityAlerts() { return capacityAlerts; }
        public int getTotalIssues() {
            return limitAlerts.size() + interviewNoticeAlerts.size() + inactiveJobAlerts.size()
                    + missingJobAlerts.size() + capacityAlerts.size();
        }
    }

    /** Application row on an admin detail view. */
    public static class AdminApplicationRow {
        private final Application application;
        private final Job job;

        public AdminApplicationRow(Application application, Job job) {
            this.application = application;
            this.job = job;
        }

        public Application getApplication() { return application; }
        public Job getJob() { return job; }
    }

    /** Full admin read-only report for one TA. */
    public static class TADetailReport {
        private final User user;
        private final TAProfile profile;
        private final List<AdminApplicationRow> applicationRows;
        private final List<Job> savedJobs;
        private final List<SiteNotification> notifications;
        private final int selectedCount;
        private final int pendingCount;
        private final int interviewCount;
        private final int waitlistCount;
        private final int rejectedCount;
        private final int autoClosedCount;
        private final int withdrawnCount;

        public TADetailReport(User user, TAProfile profile, List<AdminApplicationRow> applicationRows,
                              List<Job> savedJobs, List<SiteNotification> notifications,
                              int selectedCount, int pendingCount, int interviewCount, int waitlistCount,
                              int rejectedCount, int autoClosedCount, int withdrawnCount) {
            this.user = user;
            this.profile = profile;
            this.applicationRows = applicationRows != null ? applicationRows : Collections.emptyList();
            this.savedJobs = savedJobs != null ? savedJobs : Collections.emptyList();
            this.notifications = notifications != null ? notifications : Collections.emptyList();
            this.selectedCount = selectedCount;
            this.pendingCount = pendingCount;
            this.interviewCount = interviewCount;
            this.waitlistCount = waitlistCount;
            this.rejectedCount = rejectedCount;
            this.autoClosedCount = autoClosedCount;
            this.withdrawnCount = withdrawnCount;
        }

        public User getUser() { return user; }
        public TAProfile getProfile() { return profile; }
        public List<AdminApplicationRow> getApplicationRows() { return applicationRows; }
        public List<Job> getSavedJobs() { return savedJobs; }
        public List<SiteNotification> getNotifications() { return notifications; }
        public int getSelectedCount() { return selectedCount; }
        public int getPendingCount() { return pendingCount; }
        public int getInterviewCount() { return interviewCount; }
        public int getWaitlistCount() { return waitlistCount; }
        public int getRejectedCount() { return rejectedCount; }
        public int getAutoClosedCount() { return autoClosedCount; }
        public int getWithdrawnCount() { return withdrawnCount; }
        public int getTotalApplications() { return applicationRows.size(); }
        public int getSavedJobsCount() { return savedJobs.size(); }
        public int getNotificationCount() { return notifications.size(); }
        public long getUnreadNotificationCount() { return notifications.stream().filter(n -> !n.isRead()).count(); }
        public long getReadNotificationCount() { return notifications.stream().filter(SiteNotification::isRead).count(); }
    }

    /** One job row on an MO admin detail view. */
    public static class MOJobDetailRow {
        private final Job job;
        private final int totalApplications;
        private final int pendingCount;
        private final int interviewCount;
        private final int waitlistCount;
        private final int selectedCount;
        private final int rejectedCount;
        private final int withdrawnCount;
        private final int autoClosedCount;
        private final boolean atOrOverCapacity;
        private final boolean overCapacity;
        private final boolean inactiveWithActiveApplications;

        public MOJobDetailRow(Job job, int totalApplications, int pendingCount, int interviewCount, int waitlistCount,
                              int selectedCount, int rejectedCount, int withdrawnCount, int autoClosedCount,
                              boolean atOrOverCapacity, boolean overCapacity, boolean inactiveWithActiveApplications) {
            this.job = job;
            this.totalApplications = totalApplications;
            this.pendingCount = pendingCount;
            this.interviewCount = interviewCount;
            this.waitlistCount = waitlistCount;
            this.selectedCount = selectedCount;
            this.rejectedCount = rejectedCount;
            this.withdrawnCount = withdrawnCount;
            this.autoClosedCount = autoClosedCount;
            this.atOrOverCapacity = atOrOverCapacity;
            this.overCapacity = overCapacity;
            this.inactiveWithActiveApplications = inactiveWithActiveApplications;
        }

        public Job getJob() { return job; }
        public int getTotalApplications() { return totalApplications; }
        public int getPendingCount() { return pendingCount; }
        public int getInterviewCount() { return interviewCount; }
        public int getWaitlistCount() { return waitlistCount; }
        public int getSelectedCount() { return selectedCount; }
        public int getRejectedCount() { return rejectedCount; }
        public int getWithdrawnCount() { return withdrawnCount; }
        public int getAutoClosedCount() { return autoClosedCount; }
        public boolean isAtOrOverCapacity() { return atOrOverCapacity; }
        public boolean isOverCapacity() { return overCapacity; }
        public boolean isInactiveWithActiveApplications() { return inactiveWithActiveApplications; }
    }

    /** Full admin read-only report for one module organiser. */
    public static class MODetailReport {
        private final User user;
        private final List<MOJobDetailRow> jobRows;
        private final List<AdminApplicationRow> applicationRows;
        private final int totalJobs;
        private final int activeJobs;
        private final int inactiveJobs;
        private final int totalApplications;
        private final int distinctApplicants;
        private final int pendingCount;
        private final int interviewCount;
        private final int waitlistCount;
        private final int selectedCount;
        private final int rejectedCount;
        private final int withdrawnCount;
        private final int autoClosedCount;

        public MODetailReport(User user, List<MOJobDetailRow> jobRows, List<AdminApplicationRow> applicationRows,
                              int totalJobs, int activeJobs, int inactiveJobs, int totalApplications,
                              int distinctApplicants, int pendingCount, int interviewCount, int waitlistCount,
                              int selectedCount, int rejectedCount, int withdrawnCount, int autoClosedCount) {
            this.user = user;
            this.jobRows = jobRows != null ? jobRows : Collections.emptyList();
            this.applicationRows = applicationRows != null ? applicationRows : Collections.emptyList();
            this.totalJobs = totalJobs;
            this.activeJobs = activeJobs;
            this.inactiveJobs = inactiveJobs;
            this.totalApplications = totalApplications;
            this.distinctApplicants = distinctApplicants;
            this.pendingCount = pendingCount;
            this.interviewCount = interviewCount;
            this.waitlistCount = waitlistCount;
            this.selectedCount = selectedCount;
            this.rejectedCount = rejectedCount;
            this.withdrawnCount = withdrawnCount;
            this.autoClosedCount = autoClosedCount;
        }

        public User getUser() { return user; }
        public List<MOJobDetailRow> getJobRows() { return jobRows; }
        public List<AdminApplicationRow> getApplicationRows() { return applicationRows; }
        public int getTotalJobs() { return totalJobs; }
        public int getActiveJobs() { return activeJobs; }
        public int getInactiveJobs() { return inactiveJobs; }
        public int getTotalApplications() { return totalApplications; }
        public int getDistinctApplicants() { return distinctApplicants; }
        public int getPendingCount() { return pendingCount; }
        public int getInterviewCount() { return interviewCount; }
        public int getWaitlistCount() { return waitlistCount; }
        public int getSelectedCount() { return selectedCount; }
        public int getRejectedCount() { return rejectedCount; }
        public int getWithdrawnCount() { return withdrawnCount; }
        public int getAutoClosedCount() { return autoClosedCount; }
        public long getCapacityRiskCount() { return jobRows.stream().filter(MOJobDetailRow::isAtOrOverCapacity).count(); }
        public long getInactiveActiveRiskCount() { return jobRows.stream().filter(MOJobDetailRow::isInactiveWithActiveApplications).count(); }
    }

    /** Builds a filterable user directory with per-role activity metrics. */
    public UserDirectoryReport buildUserDirectoryReport(DataStorage storage, String roleFilter, String query) throws IOException {
        List<User> users = storage.loadUsers();
        List<TAProfile> profiles = storage.loadProfiles();
        List<Application> apps = storage.loadApplications();
        List<Job> jobs = storage.loadJobs();
        List<SiteNotification> notifications = storage.loadSiteNotifications();

        Map<String, TAProfile> profileByUser = profiles.stream()
                .collect(Collectors.toMap(TAProfile::getUserId, p -> p, (a, b) -> a));
        Map<String, List<Application>> appsByApplicant = apps.stream()
                .collect(Collectors.groupingBy(Application::getApplicantId));
        Map<String, List<Job>> jobsByMo = jobs.stream()
                .collect(Collectors.groupingBy(Job::getPostedBy));
        Map<String, List<Application>> appsByJob = apps.stream()
                .collect(Collectors.groupingBy(Application::getJobId));
        Map<String, Long> unreadByUser = notifications.stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.groupingBy(SiteNotification::getRecipientUserId, Collectors.counting()));

        String normalizedRole = normalizeRoleFilter(roleFilter);
        String cleanQuery = normalizeQuery(query);

        List<UserListRow> rows = new ArrayList<>();
        for (User user : users) {
            if (!"ALL".equals(normalizedRole) && !normalizedRole.equalsIgnoreCase(user.getRole())) {
                continue;
            }
            TAProfile profile = profileByUser.get(user.getId());
            if (!matchesUserQuery(user, profile, cleanQuery)) {
                continue;
            }

            int selected = 0;
            int pending = 0;
            int interview = 0;
            int waitlist = 0;
            int autoClosed = 0;
            int postedJobsCount = 0;
            int activeJobsCount = 0;
            int totalApplicationsReceived = 0;
            int totalSelectedForMo = 0;
            int savedJobsCount = profile != null ? profile.getSavedJobIds().size() : 0;
            int unreadNotificationCount = unreadByUser.getOrDefault(user.getId(), 0L).intValue();

            if ("TA".equalsIgnoreCase(user.getRole())) {
                List<Application> ownedApps = appsByApplicant.getOrDefault(user.getId(), Collections.emptyList());
                for (Application app : ownedApps) {
                    String status = normalizeStatus(app.getStatus());
                    if ("SELECTED".equals(status)) selected++;
                    else if ("PENDING".equals(status)) pending++;
                    else if ("INTERVIEW".equals(status)) interview++;
                    else if ("WAITLIST".equals(status)) waitlist++;
                    else if (STATUS_AUTO_CLOSED.equals(status)) autoClosed++;
                }
            } else if ("MO".equalsIgnoreCase(user.getRole())) {
                List<Job> moJobs = jobsByMo.getOrDefault(user.getId(), Collections.emptyList());
                postedJobsCount = moJobs.size();
                activeJobsCount = (int) moJobs.stream().filter(JobActivity::isActive).count();
                for (Job job : moJobs) {
                    List<Application> jobApps = appsByJob.getOrDefault(job.getId(), Collections.emptyList());
                    totalApplicationsReceived += jobApps.size();
                    totalSelectedForMo += (int) jobApps.stream().filter(a -> "SELECTED".equals(normalizeStatus(a.getStatus()))).count();
                }
            }

            rows.add(new UserListRow(
                    user,
                    profile,
                    selected,
                    pending,
                    interview,
                    waitlist,
                    autoClosed,
                    unreadNotificationCount,
                    savedJobsCount,
                    postedJobsCount,
                    activeJobsCount,
                    totalApplicationsReceived,
                    totalSelectedForMo
            ));
        }

        rows.sort(Comparator
                .comparing((UserListRow row) -> roleSortOrder(row.getUser() != null ? row.getUser().getRole() : ""))
                .thenComparing(UserListRow::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        int totalTas = (int) users.stream().filter(u -> "TA".equalsIgnoreCase(u.getRole())).count();
        int totalMos = (int) users.stream().filter(u -> "MO".equalsIgnoreCase(u.getRole())).count();
        int totalAdmins = (int) users.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();
        return new UserDirectoryReport(users.size(), totalTas, totalMos, totalAdmins, normalizedRole, cleanQuery, rows);
    }

    /** Builds the admin read-only report for one TA. */
    public TADetailReport buildTADetailReport(DataStorage storage, String userId) throws IOException {
        User user = storage.findUserById(userId);
        if (user == null || !"TA".equalsIgnoreCase(user.getRole())) {
            return null;
        }

        TAProfile profile = storage.getProfileByUserId(userId);
        if (profile == null) {
            profile = new TAProfile(userId);
        }

        List<Job> jobs = storage.loadJobs();
        Map<String, Job> jobMap = jobs.stream().collect(Collectors.toMap(Job::getId, j -> j, (a, b) -> a));
        List<Application> apps = storage.getApplicationsByApplicantId(userId);
        apps.sort(Comparator.comparing(Application::getAppliedAt, Comparator.nullsLast(String::compareTo)).reversed());

        List<AdminApplicationRow> applicationRows = new ArrayList<>();
        int selected = 0;
        int pending = 0;
        int interview = 0;
        int waitlist = 0;
        int rejected = 0;
        int autoClosed = 0;
        int withdrawn = 0;
        for (Application app : apps) {
            applicationRows.add(new AdminApplicationRow(app, jobMap.get(app.getJobId())));
            String status = normalizeStatus(app.getStatus());
            if ("SELECTED".equals(status)) selected++;
            else if ("PENDING".equals(status)) pending++;
            else if ("INTERVIEW".equals(status)) interview++;
            else if ("WAITLIST".equals(status)) waitlist++;
            else if ("REJECTED".equals(status)) rejected++;
            else if (STATUS_AUTO_CLOSED.equals(status)) autoClosed++;
            else if ("WITHDRAWN".equals(status)) withdrawn++;
        }

        List<Job> savedJobs = new ArrayList<>();
        for (String jobId : profile.getSavedJobIds()) {
            Job job = jobMap.get(jobId);
            if (job != null) {
                savedJobs.add(job);
            }
        }
        savedJobs.sort(Comparator.comparing(Job::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        List<SiteNotification> notifications = storage.getSiteNotificationsForUser(userId);

        return new TADetailReport(user, profile, applicationRows, savedJobs, notifications,
                selected, pending, interview, waitlist, rejected, autoClosed, withdrawn);
    }

    /** Builds the admin read-only report for one module organiser. */
    public MODetailReport buildMODetailReport(DataStorage storage, String userId) throws IOException {
        User user = storage.findUserById(userId);
        if (user == null || !"MO".equalsIgnoreCase(user.getRole())) {
            return null;
        }

        List<Job> allJobs = storage.loadJobs();
        List<Application> allApps = storage.loadApplications();
        List<Job> moJobs = allJobs.stream()
                .filter(j -> userId.equals(j.getPostedBy()))
                .sorted(Comparator.comparing(Job::getCreatedAt, Comparator.nullsLast(String::compareTo)).reversed())
                .collect(Collectors.toList());
        Map<String, List<Application>> appsByJob = allApps.stream()
                .collect(Collectors.groupingBy(Application::getJobId));

        List<MOJobDetailRow> jobRows = new ArrayList<>();
        List<AdminApplicationRow> applicationRows = new ArrayList<>();
        int totalApplications = 0;
        int pending = 0;
        int interview = 0;
        int waitlist = 0;
        int selected = 0;
        int rejected = 0;
        int withdrawn = 0;
        int autoClosed = 0;

        for (Job job : moJobs) {
            List<Application> jobApps = new ArrayList<>(appsByJob.getOrDefault(job.getId(), Collections.emptyList()));
            jobApps.sort(Comparator.comparing(Application::getAppliedAt, Comparator.nullsLast(String::compareTo)).reversed());
            totalApplications += jobApps.size();

            int jobPending = 0;
            int jobInterview = 0;
            int jobWaitlist = 0;
            int jobSelected = 0;
            int jobRejected = 0;
            int jobWithdrawn = 0;
            int jobAutoClosed = 0;
            boolean inactiveWithActive = false;
            for (Application app : jobApps) {
                applicationRows.add(new AdminApplicationRow(app, job));
                String status = normalizeStatus(app.getStatus());
                if ("PENDING".equals(status)) {
                    jobPending++;
                    pending++;
                } else if ("INTERVIEW".equals(status)) {
                    jobInterview++;
                    interview++;
                } else if ("WAITLIST".equals(status)) {
                    jobWaitlist++;
                    waitlist++;
                } else if ("SELECTED".equals(status)) {
                    jobSelected++;
                    selected++;
                } else if ("REJECTED".equals(status)) {
                    jobRejected++;
                    rejected++;
                } else if ("WITHDRAWN".equals(status)) {
                    jobWithdrawn++;
                    withdrawn++;
                } else if (STATUS_AUTO_CLOSED.equals(status)) {
                    jobAutoClosed++;
                    autoClosed++;
                }
                if (JobActivity.isInactive(job) && ("PENDING".equals(status) || "INTERVIEW".equals(status))) {
                    inactiveWithActive = true;
                }
            }

            boolean atOrOverCapacity = job.getMaxApplicants() > 0 && jobSelected >= job.getMaxApplicants();
            boolean overCapacity = job.getMaxApplicants() > 0 && jobSelected > job.getMaxApplicants();
            jobRows.add(new MOJobDetailRow(
                    job,
                    jobApps.size(),
                    jobPending,
                    jobInterview,
                    jobWaitlist,
                    jobSelected,
                    jobRejected,
                    jobWithdrawn,
                    jobAutoClosed,
                    atOrOverCapacity,
                    overCapacity,
                    inactiveWithActive
            ));
        }

        applicationRows.sort((left, right) -> {
            String l = left.getApplication() != null ? left.getApplication().getAppliedAt() : null;
            String r = right.getApplication() != null ? right.getApplication().getAppliedAt() : null;
            return Comparator.nullsLast(String::compareTo).reversed().compare(l, r);
        });

        int distinctApplicants = (int) applicationRows.stream()
                .map(row -> row.getApplication().getApplicantId())
                .distinct()
                .count();

        return new MODetailReport(
                user,
                jobRows,
                applicationRows,
                moJobs.size(),
                (int) moJobs.stream().filter(JobActivity::isActive).count(),
                (int) moJobs.stream().filter(JobActivity::isInactive).count(),
                totalApplications,
                distinctApplicants,
                pending,
                interview,
                waitlist,
                selected,
                rejected,
                withdrawn,
                autoClosed
        );
    }

    /** Computes dashboard totals and workload-cap indicators. */
    public DashboardSummary buildDashboardSummary(DataStorage storage, AdminSettings settings) throws IOException {
        List<User> users = storage.loadUsers();
        List<Job> jobs = storage.loadJobs();
        List<Application> apps = storage.loadApplications();

        int totalTas = (int) users.stream().filter(u -> "TA".equals(u.getRole())).count();
        int totalMos = (int) users.stream().filter(u -> "MO".equals(u.getRole())).count();
        int totalAdmins = (int) users.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
        int openJobs = (int) jobs.stream().filter(JobActivity::isActive).count();
        int inactiveJobs = jobs.size() - openJobs;

        Map<String, Integer> applicationCounts = new LinkedHashMap<>();
        applicationCounts.put("PENDING", 0);
        applicationCounts.put("INTERVIEW", 0);
        applicationCounts.put("WAITLIST", 0);
        applicationCounts.put("SELECTED", 0);
        applicationCounts.put("REJECTED", 0);
        applicationCounts.put(STATUS_AUTO_CLOSED, 0);
        applicationCounts.put("WITHDRAWN", 0);
        for (Application app : apps) {
            String status = normalizeStatus(app.getStatus());
            applicationCounts.put(status, applicationCounts.getOrDefault(status, 0) + 1);
        }

        Map<String, Long> selectedByApplicant = apps.stream()
                .filter(a -> "SELECTED".equals(a.getStatus()))
                .collect(Collectors.groupingBy(Application::getApplicantId, Collectors.counting()));
        Map<String, Job> jobMapForHours = jobs.stream().collect(Collectors.toMap(Job::getId, j -> j, (a, b) -> a));
        Map<String, Double> selectedHoursByApplicant = new HashMap<>();
        for (Application a : apps) {
            if (!"SELECTED".equals(a.getStatus())) {
                continue;
            }
            Job j = jobMapForHours.get(a.getJobId());
            selectedHoursByApplicant.merge(a.getApplicantId(), JobWorkloadEstimator.estimatedHoursPerSelectedTa(j), Double::sum);
        }
        int tasAtOrOverLimit = 0;
        if (settings != null && settings.hasWorkloadLimit()) {
            if (settings.usesHourWorkloadLimit()) {
                double cap = settings.getMaxWorkloadHoursPerTa();
                tasAtOrOverLimit = (int) selectedHoursByApplicant.values().stream()
                        .filter(h -> h >= cap - 1e-9)
                        .count();
            } else {
                int limit = settings.getMaxSelectedJobsPerTa();
                tasAtOrOverLimit = (int) selectedByApplicant.values().stream().filter(v -> v >= limit).count();
            }
        }

        Map<String, Long> selectedByJob = apps.stream()
                .filter(a -> "SELECTED".equals(a.getStatus()))
                .collect(Collectors.groupingBy(Application::getJobId, Collectors.counting()));
        int jobsAtCapacity = 0;
        for (Job job : jobs) {
            if (job.getMaxApplicants() > 0 && selectedByJob.getOrDefault(job.getId(), 0L) >= job.getMaxApplicants()) {
                jobsAtCapacity++;
            }
        }

        return new DashboardSummary(
                jobs.size(),
                openJobs,
                inactiveJobs,
                apps.size(),
                totalTas,
                totalMos,
                totalAdmins,
                tasAtOrOverLimit,
                jobsAtCapacity,
                applicationCounts
        );
    }

    /** Lists each TA's selected workload for the admin workload page. */
    public List<WorkloadRow> buildWorkloadRows(DataStorage storage, AdminSettings settings) throws IOException {
        List<Application> apps = storage.loadApplications();
        List<Job> jobs = storage.loadJobs();
        List<User> users = storage.loadUsers();

        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<String, Job> jobMap = jobs.stream().collect(Collectors.toMap(Job::getId, j -> j, (a, b) -> a));

        Map<String, Integer> selectedByApplicant = new HashMap<>();
        Map<String, Integer> pendingByApplicant = new HashMap<>();
        Map<String, List<String>> selectedTitles = new HashMap<>();
        Map<String, Double> selectedHoursByApplicant = new HashMap<>();
        for (Application app : apps) {
            String applicantId = app.getApplicantId();
            if ("SELECTED".equals(app.getStatus())) {
                selectedByApplicant.merge(applicantId, 1, Integer::sum);
                Job job = jobMap.get(app.getJobId());
                selectedHoursByApplicant.merge(applicantId, JobWorkloadEstimator.estimatedHoursPerSelectedTa(job), Double::sum);
                selectedTitles.computeIfAbsent(applicantId, key -> new ArrayList<>())
                        .add(job != null ? job.getTitle() : app.getJobId());
            } else if ("PENDING".equals(app.getStatus())) {
                pendingByApplicant.merge(applicantId, 1, Integer::sum);
            }
        }

        double avgSelected = selectedByApplicant.isEmpty()
                ? 0
                : selectedByApplicant.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        double avgHours = selectedHoursByApplicant.isEmpty()
                ? 0
                : selectedHoursByApplicant.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);

        boolean hourMode = settings != null && settings.usesHourWorkloadLimit();
        List<WorkloadRow> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : selectedByApplicant.entrySet()) {
            String applicantId = entry.getKey();
            int selectedCount = entry.getValue();
            int pendingCount = pendingByApplicant.getOrDefault(applicantId, 0);
            double estHours = selectedHoursByApplicant.getOrDefault(applicantId, 0.0);
            User user = userMap.get(applicantId);
            boolean atOrOverLimit;
            boolean aboveLimit;
            if (hourMode) {
                double cap = settings.getMaxWorkloadHoursPerTa();
                atOrOverLimit = estHours >= cap - 1e-9;
                aboveLimit = estHours > cap + 1e-9;
            } else if (settings != null && settings.getMaxSelectedJobsPerTa() > 0) {
                int cap = settings.getMaxSelectedJobsPerTa();
                atOrOverLimit = selectedCount >= cap;
                aboveLimit = selectedCount > cap;
            } else {
                atOrOverLimit = false;
                aboveLimit = false;
            }
            boolean aboveAverage = hourMode ? estHours > avgHours + 1e-9 : selectedCount > avgSelected;
            rows.add(new WorkloadRow(
                    applicantId,
                    resolveUserName(user, applicantId),
                    selectedCount,
                    pendingCount,
                    estHours,
                    aboveAverage,
                    atOrOverLimit,
                    aboveLimit,
                    selectedTitles.getOrDefault(applicantId, Collections.emptyList())
            ));
        }

        rows.sort(Comparator
                .comparing(WorkloadRow::isAboveLimit).reversed()
                .thenComparing(WorkloadRow::isAtOrOverLimit).reversed()
                .thenComparing(WorkloadRow::getEstimatedSelectedHours, Comparator.reverseOrder())
                .thenComparing(WorkloadRow::getSelectedCount, Comparator.reverseOrder())
                .thenComparing(WorkloadRow::getApplicantName, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    /**
     * Sums estimated hours ({@link JobWorkloadEstimator}) for each SELECTED application for this applicant.
     */
    /** Sums estimated hours across a TA's SELECTED applications. */
    public double sumSelectedWorkloadHours(DataStorage storage, String applicantId) throws IOException {
        if (applicantId == null || applicantId.trim().isEmpty()) {
            return 0.0;
        }
        List<Application> apps = storage.loadApplications();
        List<Job> jobs = storage.loadJobs();
        Map<String, Job> jobMap = jobs.stream().collect(Collectors.toMap(Job::getId, j -> j, (a, b) -> a));
        double sum = 0.0;
        for (Application a : apps) {
            if (!applicantId.equals(a.getApplicantId()) || !"SELECTED".equals(a.getStatus())) {
                continue;
            }
            sum += JobWorkloadEstimator.estimatedHoursPerSelectedTa(jobMap.get(a.getJobId()));
        }
        return sum;
    }

    /**
     * Auto-closes excess SELECTED applications for one TA down to the configured cap.
     *
     * @return number of applications auto-closed
     */
    public int enforceWorkloadLimitForApplicant(DataStorage storage, String applicantId, String keepApplicationId,
                                                AdminSettings settings) throws IOException {
        if (applicantId == null || applicantId.trim().isEmpty()) {
            return 0;
        }
        if (settings == null || !settings.hasWorkloadLimit() || !settings.isAutoClosePendingWhenLimitReached()) {
            return 0;
        }

        boolean useHours = settings.usesHourWorkloadLimit();
        if (useHours) {
            double cap = settings.getMaxWorkloadHoursPerTa();
            double totalH = sumSelectedWorkloadHours(storage, applicantId);
            if (totalH < cap - 1e-9) {
                return 0;
            }
        } else {
            if (settings.getMaxSelectedJobsPerTa() <= 0) {
                return 0;
            }
            List<Application> appsCheck = storage.loadApplications();
            long selectedCount = appsCheck.stream()
                    .filter(a -> applicantId.equals(a.getApplicantId()))
                    .filter(a -> "SELECTED".equals(a.getStatus()))
                    .count();
            if (selectedCount < settings.getMaxSelectedJobsPerTa()) {
                return 0;
            }
        }

        List<Application> apps = storage.loadApplications();
        int closed = 0;
        for (Application app : apps) {
            if (!applicantId.equals(app.getApplicantId())) {
                continue;
            }
            if (keepApplicationId != null && keepApplicationId.equals(app.getId())) {
                continue;
            }
            if (!"PENDING".equals(app.getStatus())) {
                continue;
            }
            app.setStatus(STATUS_AUTO_CLOSED);
            String detail;
            if (useHours) {
                double totalH = sumSelectedWorkloadHours(storage, applicantId);
                detail = String.format(Locale.US, "estimated %.1f h on selected posts, cap %.1f h)",
                        totalH, settings.getMaxWorkloadHoursPerTa());
            } else {
                detail = String.format(Locale.US, "max %d selected posts)", settings.getMaxSelectedJobsPerTa());
            }
            app.setNotes(appendAutoCloseNote(app.getNotes(), detail));
            storage.saveApplication(app);
            Job closedJob = storage.getJobById(app.getJobId());
            StudentNotificationService.notifyAutoClosed(storage, app, closedJob, app.getNotes());
            closed++;
        }
        return closed;
    }

    /** Applies workload-cap enforcement for every TA; returns total auto-closed count. */
    public int enforceWorkloadLimitGlobally(DataStorage storage, AdminSettings settings) throws IOException {
        if (settings == null || !settings.hasWorkloadLimit() || !settings.isAutoClosePendingWhenLimitReached()) {
            return 0;
        }
        List<Application> apps = storage.loadApplications();
        int totalClosed = 0;
        List<String> applicantIds = apps.stream()
                .map(Application::getApplicantId)
                .distinct()
                .collect(Collectors.toList());
        for (String applicantId : applicantIds) {
            totalClosed += enforceWorkloadLimitForApplicant(storage, applicantId, null, settings);
        }
        return totalClosed;
    }

    /** Builds anomaly and capacity alerts for the admin monitoring page. */
    public MonitoringReport buildMonitoringReport(DataStorage storage, AdminSettings settings) throws IOException {
        List<User> users = storage.loadUsers();
        List<Job> jobs = storage.loadJobs();
        List<Application> apps = storage.loadApplications();

        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<String, Job> jobMap = jobs.stream().collect(Collectors.toMap(Job::getId, j -> j, (a, b) -> a));

        List<LimitAlert> limitAlerts = new ArrayList<>();
        if (settings != null && settings.hasWorkloadLimit()) {
            Map<String, Long> selectedByApplicant = apps.stream()
                    .filter(a -> "SELECTED".equals(a.getStatus()))
                    .collect(Collectors.groupingBy(Application::getApplicantId, Collectors.counting()));
            Map<String, Long> pendingByApplicant = apps.stream()
                    .filter(a -> "PENDING".equals(a.getStatus()))
                    .collect(Collectors.groupingBy(Application::getApplicantId, Collectors.counting()));
            Map<String, Double> selectedHoursByApplicant = new HashMap<>();
            for (Application a : apps) {
                if (!"SELECTED".equals(a.getStatus())) {
                    continue;
                }
                Job j = jobMap.get(a.getJobId());
                selectedHoursByApplicant.merge(a.getApplicantId(), JobWorkloadEstimator.estimatedHoursPerSelectedTa(j), Double::sum);
            }
            boolean hourMode = settings.usesHourWorkloadLimit();
            for (Map.Entry<String, Long> entry : selectedByApplicant.entrySet()) {
                long pendingCount = pendingByApplicant.getOrDefault(entry.getKey(), 0L);
                boolean conflict;
                String loadVsCap;
                if (hourMode) {
                    double h = selectedHoursByApplicant.getOrDefault(entry.getKey(), 0.0);
                    double cap = settings.getMaxWorkloadHoursPerTa();
                    conflict = pendingCount > 0 && h >= cap - 1e-9;
                    loadVsCap = String.format(Locale.US, "%.1f h / cap %.1f h", h, cap);
                } else {
                    int cap = settings.getMaxSelectedJobsPerTa();
                    conflict = pendingCount > 0 && entry.getValue() >= cap;
                    loadVsCap = entry.getValue() + " jobs / cap " + cap;
                }
                if (conflict) {
                    User user = userMap.get(entry.getKey());
                    limitAlerts.add(new LimitAlert(
                            entry.getKey(),
                            resolveUserName(user, entry.getKey()),
                            entry.getValue().intValue(),
                            (int) pendingCount,
                            loadVsCap
                    ));
                }
            }
            limitAlerts.sort(Comparator.comparing(LimitAlert::getSelectedCount, Comparator.reverseOrder()));
        }

        List<InterviewNoticeAlert> interviewNoticeAlerts = new ArrayList<>();
        List<ApplicationAlert> inactiveJobAlerts = new ArrayList<>();
        List<ApplicationAlert> missingJobAlerts = new ArrayList<>();
        for (Application app : apps) {
            Job job = jobMap.get(app.getJobId());
            User user = userMap.get(app.getApplicantId());
            String applicantName = resolveUserName(user, app.getApplicantId());

            if ("INTERVIEW".equals(app.getStatus())) {
                boolean missingTime = isBlank(app.getInterviewTime());
                boolean missingLocation = isBlank(app.getInterviewLocation());
                if (missingTime || missingLocation) {
                    interviewNoticeAlerts.add(new InterviewNoticeAlert(
                            app.getId(),
                            app.getApplicantId(),
                            applicantName,
                            job != null ? job.getTitle() : app.getJobId(),
                            job != null ? job.getModuleCode() : "-",
                            missingTime,
                            missingLocation
                    ));
                }
            }

            if (job == null) {
                missingJobAlerts.add(new ApplicationAlert(
                        app.getId(),
                        app.getApplicantId(),
                        applicantName,
                        app.getJobId(),
                        "-",
                        normalizeStatus(app.getStatus()),
                        "Application references a job that does not exist."
                ));
                continue;
            }

            if (("PENDING".equals(app.getStatus()) || "INTERVIEW".equals(app.getStatus())) && JobActivity.isInactive(job)) {
                inactiveJobAlerts.add(new ApplicationAlert(
                        app.getId(),
                        app.getApplicantId(),
                        applicantName,
                        job.getTitle(),
                        job.getModuleCode(),
                        normalizeStatus(app.getStatus()),
                        "Application is still active although the job is closed or past deadline."
                ));
            }
        }

        Map<String, Long> selectedByJob = apps.stream()
                .filter(a -> "SELECTED".equals(a.getStatus()))
                .collect(Collectors.groupingBy(Application::getJobId, Collectors.counting()));
        List<CapacityAlert> capacityAlerts = new ArrayList<>();
        for (Job job : jobs) {
            if (job.getMaxApplicants() <= 0) {
                continue;
            }
            long selectedCount = selectedByJob.getOrDefault(job.getId(), 0L);
            if (selectedCount > job.getMaxApplicants()) {
                capacityAlerts.add(new CapacityAlert(
                        job.getId(),
                        job.getTitle(),
                        job.getModuleCode(),
                        (int) selectedCount,
                        job.getMaxApplicants()
                ));
            }
        }
        capacityAlerts.sort(Comparator.comparing(CapacityAlert::getSelectedCount, Comparator.reverseOrder()));

        return new MonitoringReport(limitAlerts, interviewNoticeAlerts, inactiveJobAlerts, missingJobAlerts, capacityAlerts);
    }

    /** Normalizes application status strings for comparisons and reporting. */
    public static String normalizeStatus(String status) {
        return status == null || status.trim().isEmpty() ? "UNKNOWN" : status.trim().toUpperCase();
    }

    private static String resolveUserName(User user, String fallback) {
        if (user == null) {
            return fallback != null ? fallback : "-";
        }
        if (!isBlank(user.getRealName())) {
            return user.getRealName().trim();
        }
        if (!isBlank(user.getUsername())) {
            return user.getUsername().trim();
        }
        return fallback != null ? fallback : "-";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalizeRoleFilter(String roleFilter) {
        if (isBlank(roleFilter)) {
            return "ALL";
        }
        String normalized = roleFilter.trim().toUpperCase();
        if ("TA".equals(normalized) || "MO".equals(normalized) || "ADMIN".equals(normalized)) {
            return normalized;
        }
        return "ALL";
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private static boolean matchesUserQuery(User user, TAProfile profile, String query) {
        if (isBlank(query)) {
            return true;
        }
        String needle = query.trim().toLowerCase();
        return containsIgnoreCase(user != null ? user.getUsername() : null, needle)
                || containsIgnoreCase(user != null ? user.getRealName() : null, needle)
                || containsIgnoreCase(user != null ? user.getEmail() : null, needle)
                || containsIgnoreCase(user != null ? user.getStudentId() : null, needle)
                || containsIgnoreCase(user != null ? user.getId() : null, needle)
                || containsIgnoreCase(user != null ? user.getRole() : null, needle)
                || containsIgnoreCase(profile != null ? profile.getEmail() : null, needle)
                || containsIgnoreCase(profile != null ? profile.getStudentId() : null, needle)
                || containsIgnoreCase(profile != null ? profile.getProgramme() : null, needle);
    }

    private static boolean containsIgnoreCase(String value, String normalizedNeedle) {
        return value != null && value.toLowerCase().contains(normalizedNeedle);
    }

    private static int roleSortOrder(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if ("TA".equals(normalized)) {
            return 0;
        }
        if ("MO".equals(normalized)) {
            return 1;
        }
        if ("ADMIN".equals(normalized)) {
            return 2;
        }
        return 3;
    }

    private static String appendAutoCloseNote(String existing, String detailInsideParens) {
        String autoNote = AUTO_CLOSE_NOTE_PREFIX + detailInsideParens;
        if (isBlank(existing)) {
            return autoNote;
        }
        String trimmed = existing.trim();
        if (trimmed.contains(autoNote)) {
            return trimmed;
        }
        return trimmed + " | " + autoNote;
    }
}
