package bupt.ta.util;

import bupt.ta.model.Application;
import bupt.ta.model.Job;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link JobSelectionCapacity}. */
public class JobSelectionCapacityTest {

    @Test
    public void selectionSlotsDefaultsToOneWhenPostingDoesNotSetTaSlots() {
        Job job = new Job();
        assertEquals(1, JobSelectionCapacity.selectionSlots(job));
    }

    @Test
    public void hasVacancyReturnsFalseWhenSelectedApplicantsReachPlannedSlots() {
        Job job = new Job();
        job.setId("J0001");
        job.setTaSlots(2);

        Application first = selected("A00001", "J0001");
        Application second = selected("A00002", "J0001");

        assertFalse(JobSelectionCapacity.hasVacancy(job, Arrays.asList(first, second), null));
    }

    @Test
    public void hasVacancyCanExcludeCurrentApplicationDuringStatusTransitions() {
        Job job = new Job();
        job.setId("J0001");
        job.setTaSlots(1);

        Application selected = selected("A00001", "J0001");

        assertTrue(JobSelectionCapacity.hasVacancy(job, Collections.singletonList(selected), "A00001"));
    }

    private static Application selected(String applicationId, String jobId) {
        Application app = new Application();
        app.setId(applicationId);
        app.setJobId(jobId);
        app.setStatus("SELECTED");
        return app;
    }
}
