package bupt.ta.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class InterviewScheduleSupportTest {

    @Test
    public void normalizeFromFormBuildsStableScheduleText() {
        assertEquals("2026-04-20 14:00-17:00",
                InterviewScheduleSupport.normalizeFromForm("2026-04-20", "14:00", "17:00"));
    }

    @Test
    public void normalizeFromFormRejectsInvalidRange() {
        try {
            InterviewScheduleSupport.normalizeFromForm("2026-04-20", "17:00", "14:00");
            fail("Expected invalid interview time range");
        } catch (IllegalArgumentException ex) {
            assertEquals("Estimated interview end time must be later than start time.", ex.getMessage());
        }
    }

    @Test
    public void inputHelpersExtractValuesFromStoredSchedule() {
        String raw = "2026-04-20 14:00-17:00";

        assertEquals("2026-04-20", InterviewScheduleSupport.dateInputValue(raw));
        assertEquals("14:00", InterviewScheduleSupport.startTimeInputValue(raw));
        assertEquals("17:00", InterviewScheduleSupport.endTimeInputValue(raw));
    }
}
