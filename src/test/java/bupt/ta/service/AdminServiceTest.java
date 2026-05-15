package bupt.ta.service;

import bupt.ta.model.AdminSettings;
import bupt.ta.model.Application;
import bupt.ta.model.Job;
import bupt.ta.model.WorkArrangementItem;
import bupt.ta.model.SiteNotification;
import bupt.ta.model.TAProfile;
import bupt.ta.model.User;
import bupt.ta.storage.DataStorage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link AdminService} workload and reporting logic. */
public class AdminServiceTest {

    private final AdminService adminService = new AdminService();

    @Before
    public void disableMailForTests() {
        System.setProperty("ta.mail.enabled", "false");
    }

    @After
    public void clearMailOverride() {
        System.clearProperty("ta.mail.enabled");
    }

    @Test
    public void testAdminSettingsPersistence() throws Exception {
        Path tmp = Files.createTempDirectory("ta-admin-test");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            AdminSettings settings = storage.loadAdminSettings();
            assertNotNull(settings);
            assertEquals(2, settings.getMaxSelectedJobsPerTa());
            assertTrue(settings.isAutoClosePendingWhenLimitReached());

            settings.setMaxSelectedJobsPerTa(3);
            settings.setMaxWorkloadHoursPerTa(40.0);
            settings.setAutoClosePendingWhenLimitReached(false);
            storage.saveAdminSettings(settings);

            AdminSettings reloaded = storage.loadAdminSettings();
            assertEquals(3, reloaded.getMaxSelectedJobsPerTa());
            assertEquals(40.0, reloaded.getMaxWorkloadHoursPerTa(), 1e-9);
            assertFalse(reloaded.isAutoClosePendingWhenLimitReached());
        } finally {
            deleteRecursive(tmp);
        }
    }

    @Test
    public void testAutoClosePendingApplicationsWhenLimitReached() throws Exception {
        Path tmp = Files.createTempDirectory("ta-admin-test");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            User applicant = createUser(storage, "ta-extra", "Applicant One");

            Job selectedJob = createJob(storage, "Selected Job", "EBU7001", 3);
            Job pendingJob = createJob(storage, "Pending Job", "EBU7002", 3);
            Job interviewJob = createJob(storage, "Interview Job", "EBU7003", 3);

            Application selected = createApplication(storage, selectedJob.getId(), applicant.getId(), applicant.getRealName(), "SELECTED");
            Application pending = createApplication(storage, pendingJob.getId(), applicant.getId(), applicant.getRealName(), "PENDING");
            Application interview = createApplication(storage, interviewJob.getId(), applicant.getId(), applicant.getRealName(), "INTERVIEW");

            AdminSettings settings = new AdminSettings();
            settings.setMaxSelectedJobsPerTa(1);
            settings.setAutoClosePendingWhenLimitReached(true);

            int closed = adminService.enforceWorkloadLimitForApplicant(storage, applicant.getId(), selected.getId(), settings);
            assertEquals(1, closed);

            Application pendingReloaded = storage.loadApplications().stream()
                    .filter(a -> a.getId().equals(pending.getId()))
                    .findFirst().orElse(null);
            Application interviewReloaded = storage.loadApplications().stream()
                    .filter(a -> a.getId().equals(interview.getId()))
                    .findFirst().orElse(null);

            assertNotNull(pendingReloaded);
            assertEquals(AdminService.STATUS_AUTO_CLOSED, pendingReloaded.getStatus());
            assertTrue(pendingReloaded.getNotes().contains("workload"));
            assertNotNull(interviewReloaded);
            assertEquals("INTERVIEW", interviewReloaded.getStatus());
        } finally {
            deleteRecursive(tmp);
        }
    }

    @Test
    public void testAutoClosePendingWhenHourCapReached() throws Exception {
        Path tmp = Files.createTempDirectory("ta-admin-test");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            User applicant = createUser(storage, "ta-hour-cap", "Hour Cap TA");

            Job heavy = createJobWithEstimatedHours(storage, "Heavy module", "EBU8001", 5, 45.0);
            Job other = createJobWithEstimatedHours(storage, "Other module", "EBU8002", 5, 10.0);

            Application selected = createApplication(storage, heavy.getId(), applicant.getId(), applicant.getRealName(), "SELECTED");
            Application pending = createApplication(storage, other.getId(), applicant.getId(), applicant.getRealName(), "PENDING");

            AdminSettings settings = new AdminSettings();
            settings.setMaxWorkloadHoursPerTa(40.0);
            settings.setMaxSelectedJobsPerTa(0);
            settings.setAutoClosePendingWhenLimitReached(true);

            int closed = adminService.enforceWorkloadLimitForApplicant(storage, applicant.getId(), selected.getId(), settings);
            assertEquals(1, closed);

            Application pendingReloaded = storage.loadApplications().stream()
                    .filter(a -> a.getId().equals(pending.getId()))
                    .findFirst().orElse(null);
            assertNotNull(pendingReloaded);
            assertEquals(AdminService.STATUS_AUTO_CLOSED, pendingReloaded.getStatus());
            assertTrue(pendingReloaded.getNotes().contains("workload"));
        } finally {
            deleteRecursive(tmp);
        }
    }

    @Test
    public void testMonitoringReportFindsExpectedIssues() throws Exception {
        Path tmp = Files.createTempDirectory("ta-admin-test");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            User applicant = createUser(storage, "ta-monitor", "Applicant Two");
            User applicant2 = createUser(storage, "ta-monitor-2", "Applicant Three");

            Job openJob = createJob(storage, "Open Job", "EBU7100", 3);
            Job closedJob = createJob(storage, "Closed Job", "EBU7101", 3);
            closedJob.setStatus("CLOSED");
            storage.saveJob(closedJob);

            Job cappedJob = createJob(storage, "Capped Job", "EBU7102", 1);

            createApplication(storage, openJob.getId(), applicant.getId(), applicant.getRealName(), "SELECTED");
            createApplication(storage, openJob.getId(), applicant.getId(), applicant.getRealName(), "PENDING");
            createApplication(storage, openJob.getId(), applicant.getId(), applicant.getRealName(), "INTERVIEW");
            createApplication(storage, closedJob.getId(), applicant.getId(), applicant.getRealName(), "PENDING");
            createApplication(storage, "J9999", applicant.getId(), applicant.getRealName(), "PENDING");

            createApplication(storage, cappedJob.getId(), applicant.getId(), applicant.getRealName(), "SELECTED");
            createApplication(storage, cappedJob.getId(), applicant2.getId(), applicant2.getRealName(), "SELECTED");

            AdminSettings settings = new AdminSettings();
            settings.setMaxSelectedJobsPerTa(1);
            settings.setAutoClosePendingWhenLimitReached(true);

            AdminService.MonitoringReport report = adminService.buildMonitoringReport(storage, settings);
            assertEquals(1, report.getLimitAlerts().size());
            assertEquals(1, report.getInterviewNoticeAlerts().size());
            assertEquals(1, report.getInactiveJobAlerts().size());
            assertEquals(1, report.getMissingJobAlerts().size());
            assertEquals(1, report.getCapacityAlerts().size());
        } finally {
            deleteRecursive(tmp);
        }
    }

    @Test
    public void testUserDirectoryReportSummarizesTaAndMoMetrics() throws Exception {
        Path tmp = Files.createTempDirectory("ta-admin-test");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            User ta = createUser(storage, "ta-directory", "Directory TA");
            User mo = createUser(storage, "mo-directory", "Directory MO", "MO");
            createUser(storage, "admin-directory", "Directory Admin", "ADMIN");

            TAProfile profile = new TAProfile(ta.getId());
            profile.setStudentId("20260001");
            profile.setEmail("directory.ta@example.com");
            profile.setProgramme("Computer Science");
            profile.getSavedJobIds().add("J9999");
            storage.saveProfile(profile);

            SiteNotification unread = new SiteNotification();
            unread.setRecipientUserId(ta.getId());
            unread.setTitle("Unread");
            unread.setKind("STATUS_SELECTED");
            unread.setRead(false);
            storage.addSiteNotification(unread);

            SiteNotification read = new SiteNotification();
            read.setRecipientUserId(ta.getId());
            read.setTitle("Read");
            read.setKind("STATUS_INTERVIEW");
            read.setRead(true);
            storage.addSiteNotification(read);

            Job openJob = createJob(storage, "Directory Open", "EBU7200", 3, mo.getId(), mo.getRealName());
            Job closedJob = createJob(storage, "Directory Closed", "EBU7201", 2, mo.getId(), mo.getRealName());
            closedJob.setStatus("CLOSED");
            storage.saveJob(closedJob);

            createApplication(storage, openJob.getId(), ta.getId(), ta.getRealName(), "SELECTED");
            createApplication(storage, openJob.getId(), ta.getId(), ta.getRealName(), "PENDING");
            createApplication(storage, closedJob.getId(), ta.getId(), ta.getRealName(), "INTERVIEW");
            createApplication(storage, closedJob.getId(), ta.getId(), ta.getRealName(), "WAITLIST");
            createApplication(storage, closedJob.getId(), ta.getId(), ta.getRealName(), AdminService.STATUS_AUTO_CLOSED);

            AdminService.UserDirectoryReport taReport = adminService.buildUserDirectoryReport(storage, "TA", "computer");
            assertEquals(1, taReport.getRows().size());
            AdminService.UserListRow taRow = taReport.getRows().get(0);
            assertEquals(1, taRow.getSelectedCount());
            assertEquals(1, taRow.getPendingCount());
            assertEquals(1, taRow.getInterviewCount());
            assertEquals(1, taRow.getWaitlistCount());
            assertEquals(1, taRow.getAutoClosedCount());
            assertEquals(1, taRow.getUnreadNotificationCount());
            assertEquals(1, taRow.getSavedJobsCount());

            AdminService.UserDirectoryReport moReport = adminService.buildUserDirectoryReport(storage, "MO", "directory mo");
            assertEquals(1, moReport.getRows().size());
            AdminService.UserListRow moRow = moReport.getRows().get(0);
            assertEquals(2, moRow.getPostedJobsCount());
            assertEquals(1, moRow.getActiveJobsCount());
            assertEquals(5, moRow.getTotalApplicationsReceived());
            assertEquals(1, moRow.getTotalSelectedForMo());
        } finally {
            deleteRecursive(tmp);
        }
    }

    @Test
    public void testBuildTADetailReportIncludesSavedJobsAndNotifications() throws Exception {
        Path tmp = Files.createTempDirectory("ta-admin-test");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            User ta = createUser(storage, "ta-detail", "Detail TA");
            User mo = createUser(storage, "mo-detail-owner", "Detail MO", "MO");

            Job job1 = createJob(storage, "Detail Job One", "EBU7300", 3, mo.getId(), mo.getRealName());
            Job job2 = createJob(storage, "Detail Job Two", "EBU7301", 2, mo.getId(), mo.getRealName());

            TAProfile profile = new TAProfile(ta.getId());
            profile.setProgramme("Software Engineering");
            profile.setDegree("MSc");
            profile.setCvFilePath("uploads/detail-ta.pdf");
            profile.getSavedJobIds().add(job1.getId());
            profile.getSkills().add("Java");
            storage.saveProfile(profile);

            Application selected = createApplication(storage, job1.getId(), ta.getId(), ta.getRealName(), "SELECTED");
            selected.setAppliedAt("2026-04-10T11:00:00");
            storage.saveApplication(selected);

            Application waitlist = createApplication(storage, job2.getId(), ta.getId(), ta.getRealName(), "WAITLIST");
            waitlist.setAppliedAt("2026-04-11T08:00:00");
            waitlist.setInterviewTime("2026-04-12 10:00");
            waitlist.setInterviewLocation("Mile End");
            storage.saveApplication(waitlist);

            SiteNotification note = new SiteNotification();
            note.setRecipientUserId(ta.getId());
            note.setApplicationId(waitlist.getId());
            note.setTitle("Interview update");
            note.setBody("Please attend the interview.");
            note.setKind("STATUS_INTERVIEW");
            note.setRead(false);
            storage.addSiteNotification(note);

            AdminService.TADetailReport report = adminService.buildTADetailReport(storage, ta.getId());
            assertNotNull(report);
            assertEquals(ta.getId(), report.getUser().getId());
            assertEquals(2, report.getApplicationRows().size());
            assertEquals(waitlist.getId(), report.getApplicationRows().get(0).getApplication().getId());
            assertEquals(1, report.getSavedJobsCount());
            assertEquals(1, report.getNotificationCount());
            assertEquals(1, report.getUnreadNotificationCount());
            assertEquals(1, report.getSelectedCount());
            assertEquals(1, report.getWaitlistCount());
        } finally {
            deleteRecursive(tmp);
        }
    }

    @Test
    public void testBuildMODetailReportAggregatesJobsAndRiskFlags() throws Exception {
        Path tmp = Files.createTempDirectory("ta-admin-test");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            User mo = createUser(storage, "mo-report", "Report MO", "MO");
            User ta1 = createUser(storage, "mo-report-ta1", "MO Report TA One");
            User ta2 = createUser(storage, "mo-report-ta2", "MO Report TA Two");

            Job cappedJob = createJob(storage, "Capped Report Job", "EBU7400", 1, mo.getId(), mo.getRealName());
            Job closedJob = createJob(storage, "Closed Report Job", "EBU7401", 2, mo.getId(), mo.getRealName());
            closedJob.setStatus("CLOSED");
            storage.saveJob(closedJob);

            createApplication(storage, cappedJob.getId(), ta1.getId(), ta1.getRealName(), "SELECTED");
            createApplication(storage, cappedJob.getId(), ta2.getId(), ta2.getRealName(), "SELECTED");
            createApplication(storage, cappedJob.getId(), ta1.getId(), ta1.getRealName(), "WAITLIST");
            createApplication(storage, closedJob.getId(), ta1.getId(), ta1.getRealName(), "PENDING");

            AdminService.MODetailReport report = adminService.buildMODetailReport(storage, mo.getId());
            assertNotNull(report);
            assertEquals(2, report.getTotalJobs());
            assertEquals(1, report.getActiveJobs());
            assertEquals(1, report.getInactiveJobs());
            assertEquals(4, report.getTotalApplications());
            assertEquals(2, report.getDistinctApplicants());
            assertEquals(2, report.getSelectedCount());
            assertEquals(1, report.getPendingCount());
            assertEquals(1, report.getWaitlistCount());
            assertEquals(1, report.getCapacityRiskCount());
            assertEquals(1, report.getInactiveActiveRiskCount());
            assertTrue(report.getJobRows().stream().anyMatch(AdminService.MOJobDetailRow::isOverCapacity));
            assertTrue(report.getJobRows().stream().anyMatch(AdminService.MOJobDetailRow::isInactiveWithActiveApplications));
        } finally {
            deleteRecursive(tmp);
        }
    }

    private static User createUser(DataStorage storage, String username, String realName) throws IOException {
        return createUser(storage, username, realName, "TA");
    }

    private static User createUser(DataStorage storage, String username, String realName, String role) throws IOException {
        User user = new User();
        user.setUsername(username);
        user.setPassword("test123");
        user.setRole(role);
        user.setEmail(username + "@example.com");
        user.setRealName(realName);
        return storage.addUser(user);
    }

    private static Job createJob(DataStorage storage, String title, String moduleCode, int maxApplicants) throws IOException {
        return createJob(storage, title, moduleCode, maxApplicants, "U003", "Wang MO");
    }

    private static Job createJob(DataStorage storage, String title, String moduleCode, int maxApplicants,
                                 String postedBy, String postedByName) throws IOException {
        Job job = new Job();
        job.setTitle(title);
        job.setModuleCode(moduleCode);
        job.setModuleName(title);
        job.setPostedBy(postedBy);
        job.setPostedByName(postedByName);
        job.setMaxApplicants(maxApplicants);
        return storage.addJob(job);
    }

    /** One arrangement row so {@link bupt.ta.util.JobWorkloadEstimator} yields {@code estHoursPerTa} for one recruited TA. */
    private static Job createJobWithEstimatedHours(DataStorage storage, String title, String moduleCode,
                                                   int maxApplicants, double estHoursPerTa) throws IOException {
        Job job = createJob(storage, title, moduleCode, maxApplicants);
        job.setTaSlots(1);
        job.setWorkArrangements(Collections.singletonList(
                new WorkArrangementItem("Estimated load", estHoursPerTa + " hours", 1, 1, null)));
        storage.saveJob(job);
        return job;
    }

    private static Application createApplication(DataStorage storage, String jobId, String applicantId,
                                                 String applicantName, String status) throws IOException {
        Application app = new Application();
        app.setJobId(jobId);
        app.setApplicantId(applicantId);
        app.setApplicantName(applicantName);
        storage.addApplication(app);
        app.setStatus(status);
        storage.saveApplication(app);
        return app;
    }

    private static void deleteRecursive(Path p) throws IOException {
        if (Files.exists(p)) {
            Files.walk(p).sorted((a, b) -> -a.compareTo(b)).forEach(path -> {
                try { Files.delete(path); } catch (IOException ignored) {}
            });
        }
    }
}
