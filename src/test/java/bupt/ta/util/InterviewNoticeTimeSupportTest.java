package bupt.ta.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** Unit tests for {@link InterviewNoticeTimeSupport}. */
public class InterviewNoticeTimeSupportTest {

    @Test
    public void normalizeFromFormBuildsStableNoticeTime() {
        assertEquals("2026-04-10 14:30",
                InterviewNoticeTimeSupport.normalizeFromForm("2026-04-10", "14:30"));
    }

    @Test
    public void normalizeFromFormRejectsMissingTime() {
        try {
            InterviewNoticeTimeSupport.normalizeFromForm("2026-04-10", "");
            fail("Expected missing time to be rejected");
        } catch (IllegalArgumentException ex) {
            assertEquals("Interview notice requires both date and time.", ex.getMessage());
        }
    }
}
