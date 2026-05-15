package bupt.ta.storage;

import bupt.ta.model.*;
import bupt.ta.util.JobActivity;
import bupt.ta.util.PasswordHasher;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import javax.servlet.ServletContext;
import java.io.*;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Thread-safe JSON file persistence for users, jobs, applications, and related entities.
 * <p>
 * Data is stored under a {@code data/} directory, resolved from {@code TA_DATA_DIR} /
 * {@code ta.data.dir}, the project tree, or the servlet context. The
 * {@link #DataStorage(javax.servlet.ServletContext)} constructor seeds demo data when empty;
 * {@link #DataStorage(String)} is intended for tests and does not seed.
 */
public class DataStorage {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = "users.json";
    private static final String PROFILES_FILE = "profiles.json";
    private static final String JOBS_FILE = "jobs.json";
    private static final String APPLICATIONS_FILE = "applications.json";
    private static final String APPLICATION_EVENTS_FILE = "application-events.json";
    private static final String SETTINGS_FILE = "settings.json";
    private static final String SITE_NOTIFICATIONS_FILE = "site-notifications.json";
    private static final String EMAIL_OTP_FILE = "email-otp.json";
    private static final String INTERVIEW_SLOTS_FILE = "interview-slots.json";
    private static final String INTERVIEW_EVALUATIONS_FILE = "interview-evaluations.json";
    private static final String MO_MODULE_ASSIGNMENTS_FILE = "mo-module-assignments.json";
    private static final String AI_API_SETTINGS_FILE = "ai-api-settings.json";
    private static final String REMEMBER_ME_TOKENS_FILE = "remember-me-tokens.json";
    private static final int REMEMBER_ME_DAYS = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Map<Path, ReentrantReadWriteLock> LOCKS_BY_BASE_PATH = new ConcurrentHashMap<>();

    private final Path basePath;
    private final Gson gson;
    private final boolean seedDemoData;
    private final ReentrantReadWriteLock lock;

    /**
     * @param ctx servlet context used to resolve the data directory
     */
    public DataStorage(ServletContext ctx) {
        this.basePath = resolveServletBasePath(ctx);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.seedDemoData = true;
        this.lock = LOCKS_BY_BASE_PATH.computeIfAbsent(this.basePath, key -> new ReentrantReadWriteLock());
        ensureDataDir();
    }

    /**
     * @param baseDir parent directory; JSON files are stored in {@code baseDir/data}
     */
    public DataStorage(String baseDir) {
        this.basePath = Paths.get(baseDir, DATA_DIR).toAbsolutePath().normalize();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.seedDemoData = false;
        this.lock = LOCKS_BY_BASE_PATH.computeIfAbsent(this.basePath, key -> new ReentrantReadWriteLock());
        ensureDataDir();
    }

    private Path resolveServletBasePath(ServletContext ctx) {
        String override = firstNonBlank(System.getProperty("ta.data.dir"), System.getenv("TA_DATA_DIR"));
        if (override != null) {
            return Paths.get(override).toAbsolutePath().normalize();
        }

        Path projectDataPath = detectProjectDataPath();
        if (projectDataPath != null) {
            return projectDataPath;
        }

        String realPath = ctx != null ? ctx.getRealPath("/") : null;
        return Paths.get(realPath != null ? realPath : ".", DATA_DIR).toAbsolutePath().normalize();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private Path detectProjectDataPath() {
        String userDir = System.getProperty("user.dir");
        if (userDir == null || userDir.trim().isEmpty()) {
            return null;
        }

        Path current = Paths.get(userDir).toAbsolutePath().normalize();
        for (Path path = current; path != null; path = path.getParent()) {
            if (Files.exists(path.resolve("pom.xml")) && Files.exists(path.resolve("src"))) {
                return path.resolve(DATA_DIR);
            }
        }
        return null;
    }

    private void ensureDataDir() {
        lock.writeLock().lock();
        try {
            Files.createDirectories(basePath);
            initSampleDataIfEmpty();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create data directory: " + basePath, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void initSampleDataIfEmpty() throws IOException {
        Path usersPath = basePath.resolve(USERS_FILE);
        if (!Files.exists(usersPath) || Files.size(usersPath) == 0) {
            List<User> defaultUsers = Arrays.asList(
                    createUser("U001", "ta1", "ta123", "TA", "ta1@bupt.edu.cn", "Zhang San"),
                    createUser("U002", "ta2", "ta123", "TA", "ta2@bupt.edu.cn", "Li Si"),
                    createUser("U005", "ta3", "ta123", "TA", "ta3@bupt.edu.cn", "Wang Wu"),
                    createUser("U006", "ta4", "ta123", "TA", "ta4@bupt.edu.cn", "Zhao Liu"),
                    createUser("U003", "mo1", "mo123", "MO", "mo1@bupt.edu.cn", "Wang MO"),
                    createUser("U004", "admin", "admin123", "ADMIN", "admin@bupt.edu.cn", "System Admin")
            );
            save(USERS_FILE, defaultUsers);
        }
        ensureDemoUsersExist();
        Path jobsPath = basePath.resolve(JOBS_FILE);
        if (!Files.exists(jobsPath) || Files.size(jobsPath) == 0) {
            Job j1 = new Job();
            j1.setId("J0001");
            j1.setTitle("TA for Software Engineering");
            j1.setModuleCode("EBU6304");
            j1.setModuleName("Software Engineering");
            j1.setDescription("Support lectures, labs and coursework marking.");
            j1.setRequiredSkills(Arrays.asList("Java", "Software Engineering"));
            j1.setPostedBy("U003");
            j1.setPostedByName("Wang MO");
            j1.setStatus("OPEN");
            j1.setCreatedAt(java.time.LocalDateTime.now().toString());
            j1.setMaxApplicants(8);
            j1.setJobType("MODULE_TA");
            j1.setWorkload("8 hours/week including one coding lab and one office hour block.");
            j1.setWorkingHours("Wed 14:00-16:00 lab, Fri 11:00-12:00 office hour.");
            j1.setPayment("15 GBP/hour.");
            j1.setDeadline(java.time.LocalDate.now().plusDays(18).toString());
            j1.setResponsibilities("Support lecture labs, answer Piazza/forum questions, and help with coursework marking and moderation.");
            j1.setTaSlots(3);
            j1.setExamTimeline("Week 4 quiz clinic; Week 8 midterm mock review; Week 12 final exam revision and marking.");
            j1.setTaAllocationPlan("3 TAs: TA-1 handles labs and office hours; TA-2 covers coursework marking; TA-3 supports Piazza/forum and final exam script checks.");
            j1.setInterviewSchedule("2026-04-18 14:00-17:00, 15 minutes per candidate.");
            j1.setInterviewLocation("EECS Building Room 402 / Teams backup link.");
            j1.setWorkArrangements(Arrays.asList(
                    new WorkArrangementItem("Weekly lab support", "2 hours", 12, 1, "Wed 14:00-16:00"),
                    new WorkArrangementItem("Coursework marking", "3 hours", 8, 1, null),
                    new WorkArrangementItem("Office hours & forum", "1 hour", 10, 1, null)
            ));

            Job j2 = new Job();
            j2.setId("J0002");
            j2.setTitle("TA for Data Structures");
            j2.setModuleCode("EBU6202");
            j2.setModuleName("Data Structures and Algorithms");
            j2.setDescription("Lead problem-solving sessions and support coding assessments.");
            j2.setRequiredSkills(Arrays.asList("Java", "Algorithms", "Complexity Analysis"));
            j2.setPostedBy("U003");
            j2.setPostedByName("Wang MO");
            j2.setStatus("OPEN");
            j2.setCreatedAt(java.time.LocalDateTime.now().minusDays(2).toString());
            j2.setMaxApplicants(6);
            j2.setJobType("MODULE_TA");
            j2.setTaSlots(2);
            j2.setWorkload("6-8 hours/week including one lab block.");
            j2.setWorkingHours("Tue 16:00-18:00 lab + weekly office hour.");
            j2.setPayment("15 GBP/hour.");
            j2.setDeadline(java.time.LocalDate.now().plusDays(20).toString());
            j2.setResponsibilities("Support lab delivery, debug student code, and mark weekly coding exercises.");
            j2.setExamTimeline("Week 6 timed coding quiz; Week 10 practical assessment; Week 13 final exam.");
            j2.setTaAllocationPlan("2 TAs split by student groups (A-L and M-Z), rotate final exam invigilation.");
            j2.setInterviewSchedule("2026-04-19 10:00-12:00.");
            j2.setInterviewLocation("CS Teaching Lab 1.");

            Job j3 = new Job();
            j3.setId("J0003");
            j3.setTitle("Invigilation Assistant - Digital Systems");
            j3.setModuleCode("EBU5101");
            j3.setModuleName("Digital Systems");
            j3.setDescription("Invigilation and exam logistics support.");
            j3.setRequiredSkills(Arrays.asList("Communication", "Time Management"));
            j3.setPostedBy("U003");
            j3.setPostedByName("Wang MO");
            j3.setStatus("OPEN");
            j3.setCreatedAt(java.time.LocalDateTime.now().minusDays(4).toString());
            j3.setMaxApplicants(10);
            j3.setJobType("INVIGILATION");
            j3.setTaSlots(3);
            j3.setWorkload("Two exam-day shifts + one prep briefing.");
            j3.setWorkingHours("Exam week shifts, 3 hours each.");
            j3.setPayment("Flat stipend 120 GBP.");
            j3.setDeadline(java.time.LocalDate.now().plusDays(15).toString());
            j3.setResponsibilities("Room setup, attendance checks, exam logistics, and script handover.");
            j3.setExamTimeline("Week 11 briefing; Week 12 invigilation; Week 13 make-up exam support.");
            j3.setTaAllocationPlan("3 TAs: entrance check, in-room invigilation, and script processing.");
            j3.setInterviewSchedule("2026-04-21 15:00-16:30 (group interview).");
            j3.setInterviewLocation("Main Building N201.");

            save(JOBS_FILE, Arrays.asList(j1, j2, j3));
        }

        Path settingsPath = basePath.resolve(SETTINGS_FILE);
        if (!Files.exists(settingsPath) || Files.size(settingsPath) == 0) {
            save(SETTINGS_FILE, new AdminSettings());
        }
        if (!seedDemoData) {
            return;
        }
        ensureDefaultMoModuleAssignments();
        Path appsPath = basePath.resolve(APPLICATIONS_FILE);
        if (!Files.exists(appsPath) || Files.size(appsPath) == 0) {
            List<Application> demoApps = new ArrayList<>();

            Application a1 = new Application();
            a1.setId("A00001");
            a1.setJobId("J0001");
            a1.setApplicantId("U001");
            a1.setApplicantName("Zhang San");
            a1.setStatus("PENDING");
            a1.setAppliedAt(java.time.LocalDateTime.now().minusDays(1).toString());
            a1.setPreferredRole("TA-1");
            demoApps.add(a1);

            Application a2 = new Application();
            a2.setId("A00002");
            a2.setJobId("J0001");
            a2.setApplicantId("U002");
            a2.setApplicantName("Li Si");
            a2.setStatus("INTERVIEW");
            a2.setAppliedAt(java.time.LocalDateTime.now().minusDays(2).toString());
            a2.setInterviewTime("2026-04-18 14:30");
            a2.setInterviewLocation("EECS Building Room 402");
            a2.setInterviewAssessment("Teaching demo + Java debugging");
            a2.setPreferredRole("TA-2");
            demoApps.add(a2);

            Application a3 = new Application();
            a3.setId("A00003");
            a3.setJobId("J0002");
            a3.setApplicantId("U005");
            a3.setApplicantName("Wang Wu");
            a3.setStatus("PENDING");
            a3.setAppliedAt(java.time.LocalDateTime.now().minusDays(1).toString());
            a3.setPreferredRole("TA-1");
            demoApps.add(a3);

            Application a4 = new Application();
            a4.setId("A00004");
            a4.setJobId("J0003");
            a4.setApplicantId("U006");
            a4.setApplicantName("Zhao Liu");
            a4.setStatus("SELECTED");
            a4.setAppliedAt(java.time.LocalDateTime.now().minusDays(3).toString());
            a4.setNotes("Strong exam supervision experience.");
            a4.setPreferredRole("TA-3");
            demoApps.add(a4);

            save(APPLICATIONS_FILE, demoApps);
        }
        ensureDemoJobsAndApplicationsExist();
    }

    private void ensureDemoUsersExist() throws IOException {
        List<User> users = loadUsers();
        boolean changed = false;
        changed |= addUserIfMissing(users, createUser("U005", "ta3", "ta123", "TA", "ta3@bupt.edu.cn", "Wang Wu"));
        changed |= addUserIfMissing(users, createUser("U006", "ta4", "ta123", "TA", "ta4@bupt.edu.cn", "Zhao Liu"));
        if (changed) {
            save(USERS_FILE, users);
        }
    }

    private boolean addUserIfMissing(List<User> users, User candidate) {
        boolean exists = users.stream().anyMatch(u ->
                Objects.equals(u.getId(), candidate.getId())
                        || Objects.equals(u.getUsername(), candidate.getUsername()));
        if (exists) {
            return false;
        }
        users.add(candidate);
        return true;
    }

    private void ensureDemoJobsAndApplicationsExist() throws IOException {
        List<Job> jobs = loadJobs();
        List<Application> apps = loadApplications();
        boolean softwareEngUpdated = enrichSoftwareEngineeringJob(jobs);
        if (softwareEngUpdated) {
            save(JOBS_FILE, jobs);
            jobs = loadJobs();
        }
        boolean normalizedRole = false;
        for (Application app : apps) {
            if (app.getPreferredRole() == null || app.getPreferredRole().trim().isEmpty()) {
                app.setPreferredRole("TA-1");
                normalizedRole = true;
            }
        }
        if (normalizedRole) {
            save(APPLICATIONS_FILE, apps);
            apps = loadApplications();
        }
        if (jobs.size() >= 3 && apps.size() >= 4) {
            return;
        }
        boolean jobsChanged = false;
        boolean appsChanged = false;
        if (jobs.stream().noneMatch(j -> "J0002".equals(j.getId()))) {
            Job j2 = new Job();
            j2.setId("J0002");
            j2.setTitle("TA for Data Structures");
            j2.setModuleCode("EBU6202");
            j2.setModuleName("Data Structures and Algorithms");
            j2.setDescription("Lead problem-solving sessions and support coding assessments.");
            j2.setRequiredSkills(Arrays.asList("Java", "Algorithms", "Complexity Analysis"));
            j2.setPostedBy("U003");
            j2.setPostedByName("Wang MO");
            j2.setStatus("OPEN");
            j2.setCreatedAt(java.time.LocalDateTime.now().minusDays(2).toString());
            j2.setMaxApplicants(6);
            j2.setJobType("MODULE_TA");
            j2.setTaSlots(2);
            j2.setWorkload("6-8 hours/week including one lab block.");
            j2.setWorkingHours("Tue 16:00-18:00 lab + weekly office hour.");
            j2.setPayment("15 GBP/hour.");
            j2.setDeadline(java.time.LocalDate.now().plusDays(20).toString());
            j2.setResponsibilities("Support lab delivery, debug student code, and mark weekly coding exercises.");
            j2.setExamTimeline("Week 6 timed coding quiz; Week 10 practical assessment; Week 13 final exam.");
            j2.setTaAllocationPlan("2 TAs split by student groups (A-L and M-Z), rotate final exam invigilation.");
            j2.setInterviewSchedule("2026-04-19 10:00-12:00.");
            j2.setInterviewLocation("CS Teaching Lab 1.");
            jobs.add(j2);
            jobsChanged = true;
        }
        if (jobs.stream().noneMatch(j -> "J0003".equals(j.getId()))) {
            Job j3 = new Job();
            j3.setId("J0003");
            j3.setTitle("Invigilation Assistant - Digital Systems");
            j3.setModuleCode("EBU5101");
            j3.setModuleName("Digital Systems");
            j3.setDescription("Invigilation and exam logistics support.");
            j3.setRequiredSkills(Arrays.asList("Communication", "Time Management"));
            j3.setPostedBy("U003");
            j3.setPostedByName("Wang MO");
            j3.setStatus("OPEN");
            j3.setCreatedAt(java.time.LocalDateTime.now().minusDays(4).toString());
            j3.setMaxApplicants(10);
            j3.setJobType("INVIGILATION");
            j3.setTaSlots(3);
            j3.setWorkload("Two exam-day shifts + one prep briefing.");
            j3.setWorkingHours("Exam week shifts, 3 hours each.");
            j3.setPayment("Flat stipend 120 GBP.");
            j3.setDeadline(java.time.LocalDate.now().plusDays(15).toString());
            j3.setResponsibilities("Room setup, attendance checks, exam logistics, and script handover.");
            j3.setExamTimeline("Week 11 briefing; Week 12 invigilation; Week 13 make-up exam support.");
            j3.setTaAllocationPlan("3 TAs: entrance check, in-room invigilation, and script processing.");
            j3.setInterviewSchedule("2026-04-21 15:00-16:30 (group interview).");
            j3.setInterviewLocation("Main Building N201.");
            jobs.add(j3);
            jobsChanged = true;
        }
        if (jobsChanged) {
            save(JOBS_FILE, jobs);
        }

        if (apps.stream().noneMatch(a -> "A00003".equals(a.getId()))) {
            Application a3 = new Application();
            a3.setId("A00003");
            a3.setJobId("J0002");
            a3.setApplicantId("U005");
            a3.setApplicantName("Wang Wu");
            a3.setStatus("PENDING");
            a3.setAppliedAt(java.time.LocalDateTime.now().minusDays(1).toString());
            a3.setPreferredRole("TA-1");
            apps.add(a3);
            appsChanged = true;
        }
        if (apps.stream().noneMatch(a -> "A00004".equals(a.getId()))) {
            Application a4 = new Application();
            a4.setId("A00004");
            a4.setJobId("J0003");
            a4.setApplicantId("U006");
            a4.setApplicantName("Zhao Liu");
            a4.setStatus("SELECTED");
            a4.setAppliedAt(java.time.LocalDateTime.now().minusDays(3).toString());
            a4.setNotes("Strong exam supervision experience.");
            a4.setPreferredRole("TA-3");
            apps.add(a4);
            appsChanged = true;
        }
        if (appsChanged) {
            save(APPLICATIONS_FILE, apps);
        }
    }

    private boolean enrichSoftwareEngineeringJob(List<Job> jobs) {
        Job target = null;
        for (Job j : jobs) {
            boolean byCode = "EBU6304".equalsIgnoreCase(j.getModuleCode());
            boolean byName = j.getModuleName() != null && j.getModuleName().toLowerCase().contains("software engineering");
            if (byCode || byName) {
                target = j;
                break;
            }
        }
        if (target == null) {
            return false;
        }

        boolean changed = false;
        if (target.getTaSlots() <= 0) {
            target.setTaSlots(3);
            changed = true;
        }
        if (isBlank(target.getWorkingHours())) {
            target.setWorkingHours("Wed 14:00-16:00 lab block; Fri 11:00-12:00 office hour; ad-hoc pre-exam clinic.");
            changed = true;
        }
        if (isBlank(target.getWorkload())) {
            target.setWorkload("8-10 hours/week. Extra workload in Week 8 and Week 12 for assessment and exam support.");
            changed = true;
        }
        if (isBlank(target.getPayment())) {
            target.setPayment("15 GBP/hour.");
            changed = true;
        }
        if (isBlank(target.getDeadline())) {
            target.setDeadline(java.time.LocalDate.now().plusDays(18).toString());
            changed = true;
        }
        if (isBlank(target.getResponsibilities())) {
            target.setResponsibilities("Support weekly labs, maintain Piazza/forum Q&A, mark coursework with rubric, hold office hours, and assist exam logistics.");
            changed = true;
        }
        if (isBlank(target.getExamTimeline())) {
            target.setExamTimeline("Week 4: Lab onboarding and coding standards check; Week 8: Mid-course coursework demo and feedback triage; Week 10: Practice viva and revision clinic; Week 12: Final exam invigilation and script triage; Week 13: Re-sit support and moderation wrap-up.");
            changed = true;
        }
        if (isBlank(target.getTaAllocationPlan())) {
            target.setTaAllocationPlan("TA-1: Lead weekly labs (Groups A-L), maintain lab attendance, and first-pass debugging support; TA-2: Coursework marking (Groups M-Z), rubric calibration, and moderation notes; TA-3: Office hours, Piazza/forum response triage, and exam-day invigilation + script handover.");
            changed = true;
        }
        if (isBlank(target.getInterviewSchedule())) {
            target.setInterviewSchedule("2026-04-18 14:00-17:00, 15 minutes per candidate (teaching demo + Q&A).");
            changed = true;
        }
        if (isBlank(target.getInterviewLocation())) {
            target.setInterviewLocation("EECS Building Room 402 (backup: Teams meeting link in TA notice).");
            changed = true;
        }
        return changed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void ensureDefaultMoModuleAssignments() throws IOException {
        Map<String, List<AssignedModule>> map = loadMoModuleAssignmentsUnlocked();
        boolean changed = false;

        // Backward-compatibility: any module already posted by an MO is treated as assigned.
        List<Job> existingJobs = loadJobsUnlocked();
        for (Job j : existingJobs) {
            if (j == null || j.getPostedBy() == null || j.getPostedBy().trim().isEmpty()) {
                continue;
            }
            if (j.getModuleCode() == null || j.getModuleCode().trim().isEmpty()) {
                continue;
            }
            String moId = j.getPostedBy().trim();
            List<AssignedModule> assigned = map.computeIfAbsent(moId, key -> new ArrayList<>());
            changed |= addAssignedModuleIfMissing(assigned, j.getModuleCode(), j.getModuleName());
        }

        // Keep baseline demo assignments for default mo1.
        List<AssignedModule> mo1 = map.computeIfAbsent("U003", key -> new ArrayList<>());
        changed |= addAssignedModuleIfMissing(mo1, "EBU6304", "Software Engineering");
        changed |= addAssignedModuleIfMissing(mo1, "EBU6202", "Data Structures and Algorithms");
        changed |= addAssignedModuleIfMissing(mo1, "EBU5101", "Digital Systems");

        if (changed) {
            saveUnlocked(MO_MODULE_ASSIGNMENTS_FILE, map);
        }
    }

    private boolean addAssignedModuleIfMissing(List<AssignedModule> list, String moduleCode, String moduleName) {
        if (list == null || moduleCode == null || moduleCode.trim().isEmpty()) {
            return false;
        }
        String normalizedCode = moduleCode.trim().toUpperCase();
        boolean exists = list.stream()
                .anyMatch(m -> m != null && normalizedCode.equalsIgnoreCase(m.getModuleCode()));
        if (exists) {
            return false;
        }
        list.add(new AssignedModule(normalizedCode, moduleName != null ? moduleName.trim() : ""));
        return true;
    }

    private User createUser(String id, String uname, String pwd, String role, String email, String name) {
        User u = new User(id, uname, PasswordHasher.hash(pwd), role);
        u.setEmail(email);
        u.setRealName(name);
        return u;
    }

    private <T> void save(String filename, T data) throws IOException {
        withWriteLock(() -> saveUnlocked(filename, data));
    }

    private <T> T load(String filename, Type type) throws IOException {
        return withReadLock(() -> loadUnlocked(filename, type));
    }

    private <T> void saveUnlocked(String filename, T data) throws IOException {
        Path file = basePath.resolve(filename);
        String json = gson.toJson(data);
        Files.write(file, json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private <T> T loadUnlocked(String filename, Type type) throws IOException {
        Path file = basePath.resolve(filename);
        if (!Files.exists(file) || Files.size(file) == 0) {
            return null;
        }
        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        return gson.fromJson(json, type);
    }

    private List<User> loadUsersUnlocked() throws IOException {
        List<User> list = loadUnlocked(USERS_FILE, new TypeToken<ArrayList<User>>(){}.getType());
        return list != null ? list : new ArrayList<>();
    }

    private List<TAProfile> loadProfilesUnlocked() throws IOException {
        List<TAProfile> list = loadUnlocked(PROFILES_FILE, new TypeToken<ArrayList<TAProfile>>(){}.getType());
        return list != null ? list : new ArrayList<>();
    }

    private List<Job> loadJobsUnlocked() throws IOException {
        List<Job> list = loadUnlocked(JOBS_FILE, new TypeToken<ArrayList<Job>>(){}.getType());
        return list != null ? list : new ArrayList<>();
    }

    private List<Application> loadApplicationsUnlocked() throws IOException {
        List<Application> list = loadUnlocked(APPLICATIONS_FILE, new TypeToken<ArrayList<Application>>(){}.getType());
        return list != null ? list : new ArrayList<>();
    }

    private List<ApplicationEvent> loadApplicationEventsUnlocked() throws IOException {
        List<ApplicationEvent> list = loadUnlocked(APPLICATION_EVENTS_FILE, new TypeToken<ArrayList<ApplicationEvent>>(){}.getType());
        return list != null ? list : new ArrayList<>();
    }

    private List<InterviewSlot> loadInterviewSlotsUnlocked() throws IOException {
        List<InterviewSlot> list = loadUnlocked(INTERVIEW_SLOTS_FILE, new TypeToken<ArrayList<InterviewSlot>>(){}.getType());
        return list != null ? list : new ArrayList<>();
    }

    private List<InterviewEvaluation> loadInterviewEvaluationsUnlocked() throws IOException {
        List<InterviewEvaluation> list = loadUnlocked(INTERVIEW_EVALUATIONS_FILE, new TypeToken<ArrayList<InterviewEvaluation>>(){}.getType());
        return list != null ? list : new ArrayList<>();
    }

    private List<SiteNotification> loadSiteNotificationsUnlocked() throws IOException {
        List<SiteNotification> list = loadUnlocked(SITE_NOTIFICATIONS_FILE, new TypeToken<ArrayList<SiteNotification>>(){}.getType());
        return list != null ? list : new ArrayList<>();
    }

    private List<EmailOtpRecord> loadEmailOtpRecordsUnlocked() throws IOException {
        List<EmailOtpRecord> list = loadUnlocked(EMAIL_OTP_FILE, new TypeToken<ArrayList<EmailOtpRecord>>(){}.getType());
        return list != null ? list : new ArrayList<>();
    }

    private Map<String, List<AssignedModule>> loadMoModuleAssignmentsUnlocked() throws IOException {
        Type t = new TypeToken<LinkedHashMap<String, ArrayList<AssignedModule>>>(){}.getType();
        Map<String, List<AssignedModule>> map = loadUnlocked(MO_MODULE_ASSIGNMENTS_FILE, t);
        if (map == null) {
            return new LinkedHashMap<>();
        }
        LinkedHashMap<String, List<AssignedModule>> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, List<AssignedModule>> e : map.entrySet()) {
            String key = e.getKey();
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            List<AssignedModule> value = e.getValue() != null ? new ArrayList<>(e.getValue()) : new ArrayList<>();
            normalized.put(key.trim(), value);
        }
        return normalized;
    }

    private <T> T withReadLock(IOSupplier<T> supplier) throws IOException {
        lock.readLock().lock();
        try {
            return supplier.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void withWriteLock(IOAction action) throws IOException {
        lock.writeLock().lock();
        try {
            action.run();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @FunctionalInterface
    private interface IOSupplier<T> {
        T get() throws IOException;
    }

    @FunctionalInterface
    private interface IOAction {
        void run() throws IOException;
    }

    // ---- Users ----

    /** Returns the user with the given username, or {@code null}. */
    public User findByUsername(String username) throws IOException {
        List<User> users = loadUsers();
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
    }

    /** Returns the user with the given email (case-insensitive), or {@code null}. */
    public User findByEmail(String email) throws IOException {
        List<User> users = loadUsers();
        return users.stream()
                .filter(u -> equalsIgnoreCaseTrimmed(u.getEmail(), email))
                .findFirst()
                .orElse(null);
    }

    /** Returns the user with the given id, or {@code null}. */
    public User findUserById(String id) throws IOException {
        List<User> users = loadUsers();
        return users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    }

    /** Loads all users from {@code users.json}. */
    public List<User> loadUsers() throws IOException {
        return withReadLock(this::loadUsersUnlocked);
    }

    /** Inserts or replaces a user by id. */
    public void saveUser(User user) throws IOException {
        withWriteLock(() -> {
            List<User> users = loadUsersUnlocked();
            users.removeIf(u -> u.getId().equals(user.getId()));
            users.add(user);
            saveUnlocked(USERS_FILE, users);
        });
    }

    /** Assigns a new id and appends a user. */
    public User addUser(User user) throws IOException {
        withWriteLock(() -> {
            List<User> users = loadUsersUnlocked();
            String newId = "U" + String.format("%03d", users.size() + 1);
            user.setId(newId);
            users.add(user);
            saveUnlocked(USERS_FILE, users);
        });
        return user;
    }

    // ---- Remember-me tokens (persistent login) ----

    /** Creates a new remember-me token for the user and returns the raw cookie value. */
    public String issueRememberMeToken(String userId) throws IOException {
        lock.writeLock().lock();
        try {
            return issueRememberMeTokenUnlocked(userId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String issueRememberMeTokenUnlocked(String userId) throws IOException {
        List<RememberMeRecord> list = loadRememberMeRecordsUnlocked();
        long now = System.currentTimeMillis();
        long ttlMillis = REMEMBER_ME_DAYS * 24L * 60L * 60L * 1000L;
        list.removeIf(r -> r != null && userId.equals(r.getUserId()));
        list.removeIf(r -> r != null && r.getExpiresAtMillis() <= now);
        byte[] raw = new byte[32];
        SECURE_RANDOM.nextBytes(raw);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String hash = sha256Hex(rawToken);
        list.add(new RememberMeRecord(hash, userId, now + ttlMillis));
        saveUnlocked(REMEMBER_ME_TOKENS_FILE, list);
        return rawToken;
    }

    /** Resolves a remember-me token to a user, or {@code null} if invalid or expired. */
    public User validateRememberMeToken(String rawToken) throws IOException {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return null;
        }
        return withReadLock(() -> validateRememberMeTokenUnlocked(rawToken.trim()));
    }

    private User validateRememberMeTokenUnlocked(String rawToken) throws IOException {
        List<RememberMeRecord> list = loadRememberMeRecordsUnlocked();
        long now = System.currentTimeMillis();
        String hash = sha256Hex(rawToken);
        RememberMeRecord match = null;
        for (RememberMeRecord r : list) {
            if (r != null && hash.equals(r.getTokenHash()) && r.getExpiresAtMillis() > now) {
                match = r;
                break;
            }
        }
        if (match == null) {
            return null;
        }
        return findUserById(match.getUserId());
    }

    /** Invalidates a single remember-me token. */
    public void revokeRememberMeToken(String rawToken) throws IOException {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return;
        }
        withWriteLock(() -> {
            List<RememberMeRecord> list = loadRememberMeRecordsUnlocked();
            String hash = sha256Hex(rawToken.trim());
            boolean removed = list.removeIf(r -> r != null && hash.equals(r.getTokenHash()));
            if (removed) {
                saveUnlocked(REMEMBER_ME_TOKENS_FILE, list);
            }
        });
    }

    /** Invalidates all remember-me tokens for the given user. */
    public void revokeAllRememberMeTokensForUser(String userId) throws IOException {
        if (userId == null) {
            return;
        }
        withWriteLock(() -> {
            List<RememberMeRecord> list = loadRememberMeRecordsUnlocked();
            boolean changed = list.removeIf(r -> r != null && userId.equals(r.getUserId()));
            if (changed) {
                saveUnlocked(REMEMBER_ME_TOKENS_FILE, list);
            }
        });
    }

    private List<RememberMeRecord> loadRememberMeRecordsUnlocked() throws IOException {
        List<RememberMeRecord> list = loadUnlocked(REMEMBER_ME_TOKENS_FILE, new TypeToken<ArrayList<RememberMeRecord>>() {
        }.getType());
        return list != null ? list : new ArrayList<>();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ---- TA Profiles ----

    /** Loads all TA profiles from {@code profiles.json}. */
    public List<TAProfile> loadProfiles() throws IOException {
        return withReadLock(this::loadProfilesUnlocked);
    }

    /** Returns the profile for a user id, or {@code null}. */
    public TAProfile getProfileByUserId(String userId) throws IOException {
        return loadProfiles().stream().filter(p -> p.getUserId().equals(userId)).findFirst().orElse(null);
    }

    /** Returns a profile by student id (case-insensitive), or {@code null}. */
    public TAProfile findProfileByStudentId(String studentId) throws IOException {
        return loadProfiles().stream()
                .filter(p -> equalsIgnoreCaseTrimmed(p.getStudentId(), studentId))
                .findFirst()
                .orElse(null);
    }

    /** Inserts or replaces a TA profile by user id. */
    public void saveProfile(TAProfile profile) throws IOException {
        if (profile.getSavedJobIds() == null) {
            profile.setSavedJobIds(new ArrayList<>());
        }
        withWriteLock(() -> {
            List<TAProfile> profiles = loadProfilesUnlocked();
            profiles.removeIf(p -> p.getUserId().equals(profile.getUserId()));
            profiles.add(profile);
            saveUnlocked(PROFILES_FILE, profiles);
        });
    }

    /** Returns an existing profile or a new in-memory profile (not persisted). */
    public TAProfile getOrCreateProfile(String userId) throws IOException {
        TAProfile profile = getProfileByUserId(userId);
        if (profile == null) {
            profile = new TAProfile(userId);
        } else if (profile.getSavedJobIds() == null) {
            profile.setSavedJobIds(new ArrayList<>());
        }
        return profile;
    }

    /** Returns whether the TA has saved the given job. */
    public boolean isJobSaved(String userId, String jobId) throws IOException {
        TAProfile profile = getOrCreateProfile(userId);
        return profile.getSavedJobIds().contains(jobId);
    }

    /**
     * Adds or removes a saved job for the TA.
     *
     * @return {@code true} if the saved-job list changed
     */
    public boolean setJobSaved(String userId, String jobId, boolean saved) throws IOException {
        final boolean[] changedRef = {false};
        withWriteLock(() -> {
            List<TAProfile> profiles = loadProfilesUnlocked();
            TAProfile profile = profiles.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .orElseGet(() -> {
                        TAProfile created = new TAProfile(userId);
                        profiles.add(created);
                        return created;
                    });
            if (profile.getSavedJobIds() == null) {
                profile.setSavedJobIds(new ArrayList<>());
            }

            List<String> savedJobIds = new ArrayList<>(profile.getSavedJobIds());
            boolean changed;
            if (saved) {
                changed = !savedJobIds.contains(jobId);
                if (changed) {
                    savedJobIds.add(jobId);
                }
            } else {
                changed = savedJobIds.removeIf(jobId::equals);
            }

            if (changed) {
                profile.setSavedJobIds(savedJobIds);
                saveUnlocked(PROFILES_FILE, profiles);
                changedRef[0] = true;
            }
        });
        return changedRef[0];
    }

    // ---- Jobs ----

    /**
     * Persists any OPEN jobs whose application deadline is already past as CLOSED so listings and TA search stay consistent.
     */
    public void syncJobStatusesWithDeadlines() throws IOException {
        withWriteLock(() -> {
            List<Job> jobs = loadJobsUnlocked();
            boolean changed = false;
            for (Job j : jobs) {
                if (JobActivity.closeOpenJobIfDeadlinePassed(j)) {
                    changed = true;
                }
            }
            if (changed) {
                saveUnlocked(JOBS_FILE, jobs);
            }
        });
    }

    /** Loads all job postings from {@code jobs.json}. */
    public List<Job> loadJobs() throws IOException {
        return withReadLock(this::loadJobsUnlocked);
    }

    /** Returns a job by id, or {@code null}. */
    public Job getJobById(String id) throws IOException {
        return loadJobs().stream().filter(j -> j.getId().equals(id)).findFirst().orElse(null);
    }

    /** Inserts or replaces a job by id. */
    public void saveJob(Job job) throws IOException {
        withWriteLock(() -> {
            List<Job> jobs = loadJobsUnlocked();
            jobs.removeIf(j -> j.getId().equals(job.getId()));
            jobs.add(job);
            saveUnlocked(JOBS_FILE, jobs);
        });
    }

    /** Assigns a new id and appends an OPEN job. */
    public Job addJob(Job job) throws IOException {
        withWriteLock(() -> {
            List<Job> jobs = loadJobsUnlocked();
            String newId = "J" + String.format("%04d", jobs.size() + 1);
            job.setId(newId);
            job.setCreatedAt(java.time.LocalDateTime.now().toString());
            job.setStatus("OPEN");
            jobs.add(job);
            saveUnlocked(JOBS_FILE, jobs);
        });
        return job;
    }

    // ---- Applications ----

    /** Loads all applications from {@code applications.json}. */
    public List<Application> loadApplications() throws IOException {
        return withReadLock(this::loadApplicationsUnlocked);
    }

    /** Returns applications for one job. */
    public List<Application> getApplicationsByJobId(String jobId) throws IOException {
        return loadApplications().stream().filter(a -> a.getJobId().equals(jobId)).collect(Collectors.toList());
    }

    /** Returns applications submitted by one TA. */
    public List<Application> getApplicationsByApplicantId(String applicantId) throws IOException {
        return loadApplications().stream().filter(a -> a.getApplicantId().equals(applicantId)).collect(Collectors.toList());
    }

    /**
     * True if the applicant already has an active application for this job (blocks a new apply).
     * WITHDRAWN and REJECTED records do not block re-applying.
     */
    public boolean hasApplied(String jobId, String applicantId) throws IOException {
        return loadApplications().stream()
                .anyMatch(a -> a.getJobId().equals(jobId)
                        && a.getApplicantId().equals(applicantId)
                        && blocksNewApplicationToJob(a.getStatus()));
    }

    private static boolean blocksNewApplicationToJob(String status) {
        return "PENDING".equals(status) || "INTERVIEW".equals(status) || "SELECTED".equals(status);
    }

    /** Inserts or replaces an application by id. */
    public void saveApplication(Application app) throws IOException {
        withWriteLock(() -> {
            List<Application> apps = loadApplicationsUnlocked();
            apps.removeIf(a -> a.getId().equals(app.getId()));
            apps.add(app);
            saveUnlocked(APPLICATIONS_FILE, apps);
        });
    }

    /** Assigns a new id and appends a PENDING application. */
    public Application addApplication(Application app) throws IOException {
        withWriteLock(() -> {
            List<Application> apps = loadApplicationsUnlocked();
            String newId = "A" + String.format("%05d", apps.size() + 1);
            app.setId(newId);
            app.setAppliedAt(java.time.LocalDateTime.now().toString());
            app.setStatus("PENDING");
            apps.add(app);
            saveUnlocked(APPLICATIONS_FILE, apps);
        });
        return app;
    }

    // ---- Application timeline events ----

    /** Loads all application timeline events. */
    public List<ApplicationEvent> loadApplicationEvents() throws IOException {
        return withReadLock(this::loadApplicationEventsUnlocked);
    }

    /** Returns timeline events for one application, oldest first. */
    public List<ApplicationEvent> getApplicationEventsByApplicationId(String applicationId) throws IOException {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return loadApplicationEvents().stream()
                .filter(e -> Objects.equals(applicationId.trim(), e.getApplicationId()))
                .sorted(Comparator.comparing(ApplicationEvent::getCreatedAt, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    /** Returns timeline events grouped by application id (empty list when none). */
    public Map<String, List<ApplicationEvent>> getApplicationEventsByApplicationIds(Collection<String> applicationIds) throws IOException {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Set<String> idSet = applicationIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        Map<String, List<ApplicationEvent>> grouped = loadApplicationEvents().stream()
                .filter(e -> idSet.contains(e.getApplicationId()))
                .sorted(Comparator.comparing(ApplicationEvent::getCreatedAt, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.groupingBy(ApplicationEvent::getApplicationId, LinkedHashMap::new, Collectors.toList()));
        for (String id : idSet) {
            grouped.putIfAbsent(id, new ArrayList<>());
        }
        return grouped;
    }

    /** Assigns an id and appends a timeline event. */
    public ApplicationEvent addApplicationEvent(ApplicationEvent event) throws IOException {
        if (event == null || event.getApplicationId() == null || event.getApplicationId().trim().isEmpty()) {
            return event;
        }
        withWriteLock(() -> {
            List<ApplicationEvent> all = loadApplicationEventsUnlocked();
            int maxSuffix = 0;
            for (ApplicationEvent existing : all) {
                String id = existing != null ? existing.getId() : null;
                if (id != null && id.matches("^EV\\d{6}$")) {
                    maxSuffix = Math.max(maxSuffix, Integer.parseInt(id.substring(2)));
                }
            }
            event.setId("EV" + String.format("%06d", maxSuffix + 1));
            if (event.getCreatedAt() == null || event.getCreatedAt().trim().isEmpty()) {
                event.setCreatedAt(java.time.LocalDateTime.now().toString());
            }
            all.add(event);
            saveUnlocked(APPLICATION_EVENTS_FILE, all);
        });
        return event;
    }

    // ---- Interview evaluations ----

    /** Loads all interview evaluations. */
    public List<InterviewEvaluation> loadInterviewEvaluations() throws IOException {
        return withReadLock(this::loadInterviewEvaluationsUnlocked);
    }

    /** Returns the evaluation for an application, or {@code null}. */
    public InterviewEvaluation getInterviewEvaluationByApplicationId(String applicationId) throws IOException {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return null;
        }
        return loadInterviewEvaluations().stream()
                .filter(e -> Objects.equals(applicationId.trim(), e.getApplicationId()))
                .findFirst()
                .orElse(null);
    }

    /** Returns evaluations for a job, highest score first. */
    public List<InterviewEvaluation> getInterviewEvaluationsByJobId(String jobId) throws IOException {
        if (jobId == null || jobId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return loadInterviewEvaluations().stream()
                .filter(e -> Objects.equals(jobId.trim(), e.getJobId()))
                .sorted(Comparator.comparingInt(InterviewEvaluation::getTotalScore).reversed()
                        .thenComparing(InterviewEvaluation::getUpdatedAt, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    /** Inserts or updates an evaluation keyed by application id. */
    public InterviewEvaluation saveInterviewEvaluation(InterviewEvaluation evaluation) throws IOException {
        if (evaluation == null || evaluation.getApplicationId() == null || evaluation.getApplicationId().trim().isEmpty()) {
            return evaluation;
        }
        withWriteLock(() -> {
            List<InterviewEvaluation> all = loadInterviewEvaluationsUnlocked();
            InterviewEvaluation existing = all.stream()
                    .filter(e -> Objects.equals(e.getApplicationId(), evaluation.getApplicationId()))
                    .findFirst()
                    .orElse(null);
            String now = java.time.LocalDateTime.now().toString();
            if (existing != null) {
                evaluation.setId(existing.getId());
                if (evaluation.getCreatedAt() == null || evaluation.getCreatedAt().trim().isEmpty()) {
                    evaluation.setCreatedAt(existing.getCreatedAt());
                }
                all.removeIf(e -> Objects.equals(e.getId(), existing.getId()));
            } else if (evaluation.getId() == null || evaluation.getId().trim().isEmpty()) {
                int maxSuffix = 0;
                for (InterviewEvaluation e : all) {
                    String id = e != null ? e.getId() : null;
                    if (id != null && id.matches("^V\\d{5}$")) {
                        maxSuffix = Math.max(maxSuffix, Integer.parseInt(id.substring(1)));
                    }
                }
                evaluation.setId("V" + String.format("%05d", maxSuffix + 1));
            }
            if (evaluation.getCreatedAt() == null || evaluation.getCreatedAt().trim().isEmpty()) {
                evaluation.setCreatedAt(now);
            }
            evaluation.setUpdatedAt(now);
            all.add(evaluation);
            saveUnlocked(INTERVIEW_EVALUATIONS_FILE, all);
        });
        return evaluation;
    }

    // ---- Interview slots ----

    /** Loads all interview slots. */
    public List<InterviewSlot> loadInterviewSlots() throws IOException {
        return withReadLock(this::loadInterviewSlotsUnlocked);
    }

    /** Returns a slot by id, or {@code null}. */
    public InterviewSlot getInterviewSlotById(String slotId) throws IOException {
        return loadInterviewSlots().stream()
                .filter(s -> Objects.equals(slotId, s.getId()))
                .findFirst()
                .orElse(null);
    }

    /** Returns slots for a job, sorted by start time. */
    public List<InterviewSlot> getInterviewSlotsByJobId(String jobId) throws IOException {
        return loadInterviewSlots().stream()
                .filter(s -> Objects.equals(jobId, s.getJobId()))
                .sorted(Comparator.comparing(InterviewSlot::getStartsAt, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    /** Inserts or replaces a slot by id. */
    public void saveInterviewSlot(InterviewSlot slot) throws IOException {
        withWriteLock(() -> {
            List<InterviewSlot> slots = loadInterviewSlotsUnlocked();
            slots.removeIf(s -> Objects.equals(s.getId(), slot.getId()));
            slots.add(slot);
            saveUnlocked(INTERVIEW_SLOTS_FILE, slots);
        });
    }

    /** Assigns a new id and appends a slot. */
    public InterviewSlot addInterviewSlot(InterviewSlot slot) throws IOException {
        withWriteLock(() -> {
            List<InterviewSlot> slots = loadInterviewSlotsUnlocked();
            int maxSuffix = 0;
            for (InterviewSlot existing : slots) {
                String id = existing != null ? existing.getId() : null;
                if (id == null || !id.matches("^S\\d{5}$")) {
                    continue;
                }
                int suffix = Integer.parseInt(id.substring(1));
                if (suffix > maxSuffix) {
                    maxSuffix = suffix;
                }
            }
            String newId = "S" + String.format("%05d", maxSuffix + 1);
            slot.setId(newId);
            if (slot.getCreatedAt() == null || slot.getCreatedAt().trim().isEmpty()) {
                slot.setCreatedAt(java.time.LocalDateTime.now().toString());
            }
            slots.add(slot);
            saveUnlocked(INTERVIEW_SLOTS_FILE, slots);
        });
        return slot;
    }

    /**
     * @return {@code true} if a slot was removed
     */
    public boolean deleteInterviewSlot(String slotId) throws IOException {
        final boolean[] changedRef = {false};
        withWriteLock(() -> {
            List<InterviewSlot> slots = loadInterviewSlotsUnlocked();
            changedRef[0] = slots.removeIf(s -> Objects.equals(slotId, s.getId()));
            if (changedRef[0]) {
                saveUnlocked(INTERVIEW_SLOTS_FILE, slots);
            }
        });
        return changedRef[0];
    }

    // ---- Admin settings ----

    /** Loads platform admin settings, or defaults when missing. */
    public AdminSettings loadAdminSettings() throws IOException {
        AdminSettings settings = load(SETTINGS_FILE, AdminSettings.class);
        return settings != null ? settings : new AdminSettings();
    }

    /** Persists platform admin settings. */
    public void saveAdminSettings(AdminSettings settings) throws IOException {
        save(SETTINGS_FILE, settings != null ? settings : new AdminSettings());
    }

    // ---- AI API settings ----

    /**
     * Loads the admin-managed LLM API settings, or default values when the file is missing/empty.
     * Never returns {@code null}.
     */
    public AiApiSettings loadAiApiSettings() throws IOException {
        AiApiSettings settings = load(AI_API_SETTINGS_FILE, AiApiSettings.class);
        return settings != null ? settings : new AiApiSettings();
    }

    /** Persists admin-managed LLM API settings. */
    public void saveAiApiSettings(AiApiSettings settings) throws IOException {
        save(AI_API_SETTINGS_FILE, settings != null ? settings : new AiApiSettings());
    }

    // ---- MO module assignments ----

    /** Returns modules assigned to an MO for job posting. */
    public List<AssignedModule> loadAssignedModulesForMo(String moUserId) throws IOException {
        if (moUserId == null || moUserId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return withReadLock(() -> {
            Map<String, List<AssignedModule>> map = loadMoModuleAssignmentsUnlocked();
            List<AssignedModule> list = map.getOrDefault(moUserId.trim(), Collections.emptyList());
            return new ArrayList<>(list);
        });
    }

    /** Replaces the module list assigned to an MO. */
    public void saveAssignedModulesForMo(String moUserId, List<AssignedModule> modules) throws IOException {
        if (moUserId == null || moUserId.trim().isEmpty()) {
            return;
        }
        withWriteLock(() -> {
            Map<String, List<AssignedModule>> map = loadMoModuleAssignmentsUnlocked();
            List<AssignedModule> cleaned = new ArrayList<>();
            if (modules != null) {
                for (AssignedModule m : modules) {
                    if (m == null || m.getModuleCode() == null || m.getModuleCode().trim().isEmpty()) {
                        continue;
                    }
                    String code = m.getModuleCode().trim().toUpperCase();
                    String name = m.getModuleName() != null ? m.getModuleName().trim() : "";
                    cleaned.add(new AssignedModule(code, name));
                }
            }
            map.put(moUserId.trim(), cleaned);
            saveUnlocked(MO_MODULE_ASSIGNMENTS_FILE, map);
        });
    }

    // ---- Site notifications ----

    /** Loads all in-app site notifications. */
    public List<SiteNotification> loadSiteNotifications() throws IOException {
        return withReadLock(this::loadSiteNotificationsUnlocked);
    }

    /** Assigns an id and appends an in-app notification. */
    public SiteNotification addSiteNotification(SiteNotification n) throws IOException {
        withWriteLock(() -> {
            List<SiteNotification> all = loadSiteNotificationsUnlocked();
            String newId = "N" + String.format("%06d", all.size() + 1);
            n.setId(newId);
            if (n.getCreatedAt() == null || n.getCreatedAt().trim().isEmpty()) {
                n.setCreatedAt(java.time.LocalDateTime.now().toString());
            }
            all.add(n);
            saveUnlocked(SITE_NOTIFICATIONS_FILE, all);
        });
        return n;
    }

    /** Returns notifications for a user, newest first. */
    public List<SiteNotification> getSiteNotificationsForUser(String userId) throws IOException {
        return loadSiteNotifications().stream()
                .filter(n -> Objects.equals(userId, n.getRecipientUserId()))
                .sorted(Comparator.comparing(SiteNotification::getCreatedAt, Comparator.nullsLast(String::compareTo)).reversed())
                .collect(Collectors.toList());
    }

    /** Counts unread notifications for a user. */
    public int countUnreadSiteNotificationsForUser(String userId) throws IOException {
        return (int) loadSiteNotifications().stream()
                .filter(n -> Objects.equals(userId, n.getRecipientUserId()) && !n.isRead())
                .count();
    }

    /** Returns one notification if it belongs to the user, or {@code null}. */
    public SiteNotification getSiteNotificationByIdForUser(String notificationId, String userId) throws IOException {
        return loadSiteNotifications().stream()
                .filter(n -> Objects.equals(notificationId, n.getId()) && Objects.equals(userId, n.getRecipientUserId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return {@code true} if the notification was marked read
     */
    public boolean markSiteNotificationRead(String notificationId, String userId) throws IOException {
        final boolean[] changedRef = {false};
        withWriteLock(() -> {
            List<SiteNotification> all = loadSiteNotificationsUnlocked();
            for (SiteNotification n : all) {
                if (Objects.equals(notificationId, n.getId()) && Objects.equals(userId, n.getRecipientUserId()) && !n.isRead()) {
                    n.setRead(true);
                    changedRef[0] = true;
                }
            }
            if (changedRef[0]) {
                saveUnlocked(SITE_NOTIFICATIONS_FILE, all);
            }
        });
        return changedRef[0];
    }

    /** Marks every notification for the user as read; returns how many changed. */
    public int markAllSiteNotificationsReadForUser(String userId) throws IOException {
        final int[] changedRef = {0};
        withWriteLock(() -> {
            List<SiteNotification> all = loadSiteNotificationsUnlocked();
            for (SiteNotification n : all) {
                if (Objects.equals(userId, n.getRecipientUserId()) && !n.isRead()) {
                    n.setRead(true);
                    changedRef[0]++;
                }
            }
            if (changedRef[0] > 0) {
                saveUnlocked(SITE_NOTIFICATIONS_FILE, all);
            }
        });
        return changedRef[0];
    }

    /** Marks selected notifications as read; returns how many changed. */
    public int markSiteNotificationsReadForUser(String userId, Collection<String> notificationIds) throws IOException {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return 0;
        }
        Set<String> idSet = notificationIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        final int[] changedRef = {0};
        withWriteLock(() -> {
            List<SiteNotification> all = loadSiteNotificationsUnlocked();
            for (SiteNotification n : all) {
                if (Objects.equals(userId, n.getRecipientUserId()) && idSet.contains(n.getId()) && !n.isRead()) {
                    n.setRead(true);
                    changedRef[0]++;
                }
            }
            if (changedRef[0] > 0) {
                saveUnlocked(SITE_NOTIFICATIONS_FILE, all);
            }
        });
        return changedRef[0];
    }

    /**
     * @return {@code true} if the notification was marked unread
     */
    public boolean markSiteNotificationUnread(String notificationId, String userId) throws IOException {
        final boolean[] changedRef = {false};
        withWriteLock(() -> {
            List<SiteNotification> all = loadSiteNotificationsUnlocked();
            for (SiteNotification n : all) {
                if (Objects.equals(notificationId, n.getId()) && Objects.equals(userId, n.getRecipientUserId()) && n.isRead()) {
                    n.setRead(false);
                    changedRef[0] = true;
                }
            }
            if (changedRef[0]) {
                saveUnlocked(SITE_NOTIFICATIONS_FILE, all);
            }
        });
        return changedRef[0];
    }

    // ---- Email OTP ----

    /** Loads all email OTP records. */
    public List<EmailOtpRecord> loadEmailOtpRecords() throws IOException {
        return withReadLock(this::loadEmailOtpRecordsUnlocked);
    }

    /** Inserts or replaces an OTP record by id. */
    public void saveEmailOtpRecord(EmailOtpRecord record) throws IOException {
        if (record == null || record.getId() == null) {
            return;
        }
        withWriteLock(() -> {
            List<EmailOtpRecord> all = loadEmailOtpRecordsUnlocked();
            all.removeIf(r -> Objects.equals(r.getId(), record.getId()));
            all.add(record);
            saveUnlocked(EMAIL_OTP_FILE, all);
        });
    }

    /** Assigns a new id and appends an OTP record. */
    public EmailOtpRecord addEmailOtpRecord(EmailOtpRecord record) throws IOException {
        if (record == null) {
            return null;
        }
        withWriteLock(() -> {
            List<EmailOtpRecord> all = loadEmailOtpRecordsUnlocked();
            String newId = "E" + String.format("%06d", all.size() + 1);
            record.setId(newId);
            all.add(record);
            saveUnlocked(EMAIL_OTP_FILE, all);
        });
        return record;
    }

    /** Returns the most recent OTP for an email and purpose, or {@code null}. */
    public EmailOtpRecord findLatestEmailOtp(String email, String purpose) throws IOException {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        String e = email.trim();
        String p = purpose != null ? purpose.trim() : "";
        return loadEmailOtpRecords().stream()
                .filter(r -> equalsIgnoreCaseTrimmed(r.getEmail(), e)
                        && (p.isEmpty() || equalsIgnoreCaseTrimmed(r.getPurpose(), p)))
                .sorted(Comparator.comparing(EmailOtpRecord::getCreatedAt, Comparator.nullsLast(String::compareTo)).reversed())
                .findFirst()
                .orElse(null);
    }

    /** Marks selected notifications as unread; returns how many changed. */
    public int markSiteNotificationsUnreadForUser(String userId, Collection<String> notificationIds) throws IOException {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return 0;
        }
        Set<String> idSet = notificationIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        final int[] changedRef = {0};
        withWriteLock(() -> {
            List<SiteNotification> all = loadSiteNotificationsUnlocked();
            for (SiteNotification n : all) {
                if (Objects.equals(userId, n.getRecipientUserId()) && idSet.contains(n.getId()) && n.isRead()) {
                    n.setRead(false);
                    changedRef[0]++;
                }
            }
            if (changedRef[0] > 0) {
                saveUnlocked(SITE_NOTIFICATIONS_FILE, all);
            }
        });
        return changedRef[0];
    }

    /** Root directory containing JSON data files. */
    public Path getBasePath() { return basePath; }

    /** Directory for uploaded CV files. */
    public Path getUploadPath() { return basePath.resolve("uploads"); }

    private boolean equalsIgnoreCaseTrimmed(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }
}
