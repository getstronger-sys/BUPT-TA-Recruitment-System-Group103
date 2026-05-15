package bupt.ta.service;

import bupt.ta.model.Application;
import bupt.ta.model.InterviewSlot;
import bupt.ta.model.Job;
import bupt.ta.model.User;
import bupt.ta.storage.DataStorage;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link InterviewBookingService}. */
public class InterviewBookingServiceTest {

    private final InterviewBookingService bookingService = new InterviewBookingService();

    @Test
    public void bookingRespectsCapacityAndConflicts() throws Exception {
        Path tmp = Files.createTempDirectory("ta-interview-booking");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            User mo = createUser(storage, "mo-slots", "MO", "MO");
            User ta1 = createUser(storage, "ta-slot-1", "TA One", "TA");
            User ta2 = createUser(storage, "ta-slot-2", "TA Two", "TA");

            Job job1 = createJob(storage, "Slot Job One", "EBU9001", mo);
            Job job2 = createJob(storage, "Slot Job Two", "EBU9002", mo);

            Application app1 = createApplication(storage, job1.getId(), ta1, "INTERVIEW");
            Application app2 = createApplication(storage, job2.getId(), ta1, "INTERVIEW");
            Application app3 = createApplication(storage, job1.getId(), ta2, "INTERVIEW");

            InterviewBookingService.ActionResult created1 = bookingService.createSlot(storage, mo.getId(), job1.getId(),
                    "2026-04-21T10:00", "45", "Room 401", "Teaching demo", "1");
            InterviewBookingService.ActionResult created2 = bookingService.createSlot(storage, mo.getId(), job2.getId(),
                    "2026-04-21T10:15", "45", "Room 402", "Technical Q&A", "2");
            assertTrue(created1.isSuccess());
            assertTrue(created2.isSuccess());

            InterviewSlot slot1 = storage.getInterviewSlotsByJobId(job1.getId()).get(0);
            InterviewSlot slot2 = storage.getInterviewSlotsByJobId(job2.getId()).get(0);

            InterviewBookingService.ActionResult booked = bookingService.bookSlot(storage, ta1.getId(), app1.getId(), slot1.getId());
            assertTrue(booked.isSuccess());

            InterviewBookingService.ActionResult full = bookingService.bookSlot(storage, ta2.getId(), app3.getId(), slot1.getId());
            assertFalse(full.isSuccess());
            assertTrue(full.getDetail().toLowerCase().contains("full"));

            InterviewBookingService.ActionResult conflict = bookingService.bookSlot(storage, ta1.getId(), app2.getId(), slot2.getId());
            assertFalse(conflict.isSuccess());
            assertTrue(conflict.getDetail().toLowerCase().contains("conflict"));
        } finally {
            deleteRecursive(tmp);
        }
    }

    @Test
    public void cancelBookingClearsFieldsAndBookedSlotsCannotBeDeleted() throws Exception {
        Path tmp = Files.createTempDirectory("ta-interview-booking");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            User mo = createUser(storage, "mo-delete-slot", "MO", "MO");
            User ta = createUser(storage, "ta-delete-slot", "TA", "TA");

            Job job = createJob(storage, "Delete Slot Job", "EBU9003", mo);
            Application app = createApplication(storage, job.getId(), ta, "INTERVIEW");

            InterviewBookingService.ActionResult created = bookingService.createSlot(storage, mo.getId(), job.getId(),
                    "2026-04-22T14:00", "30", "Room 501", "Bring ID", "1");
            assertTrue(created.isSuccess());

            InterviewSlot slot = storage.getInterviewSlotsByJobId(job.getId()).get(0);
            InterviewBookingService.ActionResult booked = bookingService.bookSlot(storage, ta.getId(), app.getId(), slot.getId());
            assertTrue(booked.isSuccess());

            Application saved = storage.getApplicationsByApplicantId(ta.getId()).get(0);
            assertEquals(slot.getId(), saved.getInterviewSlotId());
            assertEquals("2026-04-22 14:00", saved.getInterviewTime());
            assertEquals("Room 501", saved.getInterviewLocation());

            InterviewBookingService.ActionResult deleteBlocked = bookingService.deleteSlot(storage, mo.getId(), job.getId(), slot.getId());
            assertFalse(deleteBlocked.isSuccess());

            InterviewBookingService.ActionResult cancelled = bookingService.cancelBooking(storage, ta.getId(), app.getId());
            assertTrue(cancelled.isSuccess());

            Application afterCancel = storage.getApplicationsByApplicantId(ta.getId()).get(0);
            assertNull(afterCancel.getInterviewSlotId());
            assertNull(afterCancel.getInterviewTime());
            assertNull(afterCancel.getInterviewLocation());

            InterviewBookingService.ActionResult deleted = bookingService.deleteSlot(storage, mo.getId(), job.getId(), slot.getId());
            assertTrue(deleted.isSuccess());
            assertTrue(storage.getInterviewSlotsByJobId(job.getId()).isEmpty());
        } finally {
            deleteRecursive(tmp);
        }
    }

    private static User createUser(DataStorage storage, String username, String realName, String role) throws Exception {
        User user = new User();
        user.setUsername(username);
        user.setPassword("x");
        user.setRole(role);
        user.setRealName(realName);
        storage.addUser(user);
        return user;
    }

    private static Job createJob(DataStorage storage, String title, String moduleCode, User mo) throws Exception {
        Job job = new Job();
        job.setTitle(title);
        job.setModuleCode(moduleCode);
        job.setPostedBy(mo.getId());
        job.setPostedByName(mo.getRealName());
        storage.addJob(job);
        return job;
    }

    private static Application createApplication(DataStorage storage, String jobId, User applicant, String status) throws Exception {
        Application app = new Application();
        app.setJobId(jobId);
        app.setApplicantId(applicant.getId());
        app.setApplicantName(applicant.getRealName());
        storage.addApplication(app);
        app.setStatus(status);
        storage.saveApplication(app);
        return app;
    }

    private static void deleteRecursive(Path p) throws Exception {
        if (Files.exists(p)) {
            Files.walk(p).sorted((a, b) -> -a.compareTo(b)).forEach(path -> {
                try { Files.delete(path); } catch (Exception ignored) {}
            });
        }
    }
}
