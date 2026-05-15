package bupt.ta.service;

import bupt.ta.model.Application;
import bupt.ta.model.InterviewSlot;
import bupt.ta.model.Job;
import bupt.ta.storage.DataStorage;
import bupt.ta.util.InterviewCalendarSupport;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Coordinates bookable interview slots for applicants and module organisers.
 */
public class InterviewBookingService {

    private static final DateTimeFormatter APP_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final DateTimeFormatter INPUT_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.ROOT);
    private static final int DEFAULT_DURATION_MINUTES = 45;

    /** Interview slot with current bookings and remaining capacity. */
    public static final class SlotSummary {
        private final InterviewSlot slot;
        private final List<Application> bookedApplications;

        public SlotSummary(InterviewSlot slot, List<Application> bookedApplications) {
            this.slot = slot;
            this.bookedApplications = bookedApplications != null ? bookedApplications : new ArrayList<>();
        }

        public InterviewSlot getSlot() { return slot; }
        public List<Application> getBookedApplications() { return bookedApplications; }
        public int getBookedCount() { return bookedApplications.size(); }
        public int getCapacity() { return slot != null && slot.getCapacity() > 0 ? slot.getCapacity() : 1; }
        public int getRemainingCount() { return Math.max(0, getCapacity() - getBookedCount()); }
        public boolean isFull() { return getBookedCount() >= getCapacity(); }
    }

    /** Success or failure from a booking action with a user-facing message. */
    public static final class ActionResult {
        private final boolean success;
        private final String detail;

        public ActionResult(boolean success, String detail) {
            this.success = success;
            this.detail = detail;
        }

        public boolean isSuccess() { return success; }
        public String getDetail() { return detail; }
    }

    /** Builds slot summaries including booked applicants for one job. */
    public List<SlotSummary> buildSlotSummaries(DataStorage storage, String jobId) throws IOException {
        List<InterviewSlot> slots = storage.getInterviewSlotsByJobId(jobId);
        List<Application> apps = storage.getApplicationsByJobId(jobId);
        List<SlotSummary> out = new ArrayList<>();
        for (InterviewSlot slot : slots) {
            List<Application> booked = apps.stream()
                    .filter(a -> Objects.equals(slot.getId(), a.getInterviewSlotId()))
                    .filter(this::countsAsBooking)
                    .sorted(Comparator.comparing(Application::getApplicantName, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .collect(Collectors.toList());
            out.add(new SlotSummary(slot, booked));
        }
        return out;
    }

    /** Creates an interview slot for an MO-owned job posting. */
    public ActionResult createSlot(DataStorage storage, String moId, String jobId,
                                   String startsAtRaw, String durationRaw, String location,
                                   String notes, String capacityRaw) throws IOException {
        Job job = storage.getJobById(jobId);
        if (job == null || !Objects.equals(moId, job.getPostedBy())) {
            return new ActionResult(false, "Posting not found.");
        }

        LocalDateTime start = parseInputDateTime(startsAtRaw);
        if (start == null) {
            return new ActionResult(false, "Start time must use date and time.");
        }
        int durationMinutes = parsePositiveInt(durationRaw, DEFAULT_DURATION_MINUTES);
        if (durationMinutes < 15) {
            return new ActionResult(false, "Duration must be at least 15 minutes.");
        }
        int capacity = parsePositiveInt(capacityRaw, 1);
        if (capacity <= 0) {
            return new ActionResult(false, "Capacity must be at least 1.");
        }
        if (location == null || location.trim().isEmpty()) {
            return new ActionResult(false, "Location is required.");
        }

        InterviewSlot slot = new InterviewSlot();
        slot.setJobId(jobId);
        slot.setStartsAt(start.toString());
        slot.setEndsAt(start.plusMinutes(durationMinutes).toString());
        slot.setLocation(location.trim());
        slot.setCapacity(capacity);
        slot.setNotes(notes != null ? notes.trim() : "");
        storage.addInterviewSlot(slot);
        return new ActionResult(true, "Interview slot created.");
    }

    /** Deletes an empty interview slot. */
    public ActionResult deleteSlot(DataStorage storage, String moId, String jobId, String slotId) throws IOException {
        InterviewSlot slot = storage.getInterviewSlotById(slotId);
        Job job = storage.getJobById(jobId);
        if (slot == null || job == null || !Objects.equals(jobId, slot.getJobId()) || !Objects.equals(moId, job.getPostedBy())) {
            return new ActionResult(false, "Interview slot not found.");
        }
        long bookings = storage.getApplicationsByJobId(jobId).stream()
                .filter(a -> Objects.equals(slotId, a.getInterviewSlotId()))
                .filter(this::countsAsBooking)
                .count();
        if (bookings > 0) {
            return new ActionResult(false, "This slot already has booked applicants.");
        }
        storage.deleteInterviewSlot(slotId);
        return new ActionResult(true, "Interview slot deleted.");
    }

    /** Books an interview slot for a TA application. */
    public ActionResult bookSlot(DataStorage storage, String applicantId, String applicationId, String slotId) throws IOException {
        Application application = findApplicationForApplicant(storage, applicantId, applicationId);
        if (application == null) {
            return new ActionResult(false, "Application not found.");
        }
        if (!isBookableStatus(application.getStatus())) {
            return new ActionResult(false, "This application cannot book an interview slot.");
        }

        InterviewSlot slot = storage.getInterviewSlotById(slotId);
        if (slot == null || !Objects.equals(slot.getJobId(), application.getJobId())) {
            return new ActionResult(false, "Interview slot not found.");
        }

        LocalDateTime slotStart = parseStoredDateTime(slot.getStartsAt());
        LocalDateTime slotEnd = parseStoredDateTime(slot.getEndsAt());
        if (slotStart == null || slotEnd == null || !slotEnd.isAfter(slotStart)) {
            return new ActionResult(false, "Interview slot time is invalid.");
        }

        long bookedCount = storage.loadApplications().stream()
                .filter(a -> Objects.equals(slotId, a.getInterviewSlotId()))
                .filter(a -> !Objects.equals(a.getId(), application.getId()))
                .filter(this::countsAsBooking)
                .count();
        int capacity = slot.getCapacity() > 0 ? slot.getCapacity() : 1;
        if (bookedCount >= capacity) {
            return new ActionResult(false, "This interview slot is already full.");
        }

        for (Application other : storage.getApplicationsByApplicantId(applicantId)) {
            if (Objects.equals(other.getId(), application.getId()) || other.getInterviewSlotId() == null || other.getInterviewSlotId().trim().isEmpty()) {
                continue;
            }
            if (!countsAsBooking(other)) {
                continue;
            }
            InterviewSlot otherSlot = storage.getInterviewSlotById(other.getInterviewSlotId());
            if (otherSlot == null) {
                continue;
            }
            LocalDateTime otherStart = parseStoredDateTime(otherSlot.getStartsAt());
            LocalDateTime otherEnd = parseStoredDateTime(otherSlot.getEndsAt());
            if (otherStart != null && otherEnd != null && overlaps(slotStart, slotEnd, otherStart, otherEnd)) {
                return new ActionResult(false, "This time conflicts with another interview you have already booked.");
            }
        }

        application.setInterviewSlotId(slot.getId());
        application.setInterviewBookedAt(LocalDateTime.now().toString());
        application.setInterviewTime(slotStart.format(APP_DISPLAY));
        application.setInterviewLocation(slot.getLocation());
        storage.saveApplication(application);
        return new ActionResult(true, "Interview slot booked.");
    }

    /** Clears the booked interview slot from a TA application. */
    public ActionResult cancelBooking(DataStorage storage, String applicantId, String applicationId) throws IOException {
        Application application = findApplicationForApplicant(storage, applicantId, applicationId);
        if (application == null) {
            return new ActionResult(false, "Application not found.");
        }
        if (application.getInterviewSlotId() == null || application.getInterviewSlotId().trim().isEmpty()) {
            return new ActionResult(false, "No booked slot to cancel.");
        }
        application.setInterviewSlotId(null);
        application.setInterviewBookedAt(null);
        application.setInterviewTime(null);
        application.setInterviewLocation(null);
        storage.saveApplication(application);
        return new ActionResult(true, "Interview booking cancelled.");
    }

    private Application findApplicationForApplicant(DataStorage storage, String applicantId, String applicationId) throws IOException {
        return storage.getApplicationsByApplicantId(applicantId).stream()
                .filter(a -> Objects.equals(applicationId, a.getId()))
                .findFirst()
                .orElse(null);
    }

    private boolean countsAsBooking(Application app) {
        return app != null
                && app.getInterviewSlotId() != null
                && !app.getInterviewSlotId().trim().isEmpty()
                && !"WITHDRAWN".equals(app.getStatus())
                && !"REJECTED".equals(app.getStatus())
                && !"AUTO_CLOSED".equals(app.getStatus());
    }

    private boolean isBookableStatus(String status) {
        return "INTERVIEW".equals(status) || "WAITLIST".equals(status);
    }

    private LocalDateTime parseInputDateTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim(), INPUT_DATE_TIME);
        } catch (Exception ex) {
            return InterviewCalendarSupport.parseInterviewTime(raw);
        }
    }

    private LocalDateTime parseStoredDateTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (Exception ex) {
            return InterviewCalendarSupport.parseInterviewTime(raw);
        }
    }

    private int parsePositiveInt(String raw, int fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private boolean overlaps(LocalDateTime leftStart, LocalDateTime leftEnd, LocalDateTime rightStart, LocalDateTime rightEnd) {
        return leftStart.isBefore(rightEnd) && rightStart.isBefore(leftEnd);
    }
}
