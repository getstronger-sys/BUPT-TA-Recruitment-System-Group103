package bupt.ta.util;

import bupt.ta.model.Application;
import bupt.ta.model.Job;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link InterviewCalendarSupport}. */
public class InterviewCalendarSupportTest {

    @Test
    public void parseInterviewTimeAcceptsCommonFormats() {
        LocalDateTime parsed = InterviewCalendarSupport.parseInterviewTime("2026-04-18 14:30");
        assertNotNull(parsed);
        assertEquals(2026, parsed.getYear());
        assertEquals(4, parsed.getMonthValue());
        assertEquals(18, parsed.getDayOfMonth());
        assertEquals(14, parsed.getHour());
        assertEquals(30, parsed.getMinute());
    }

    @Test
    public void parseInterviewTimeExtractsFromFreeTextAndRanges() {
        LocalDateTime fromNotice = InterviewCalendarSupport.parseInterviewTime(
                "Please attend on 2026-04-20 14:30; bring your ID.");
        assertNotNull(fromNotice);
        assertEquals(14, fromNotice.getHour());
        assertEquals(30, fromNotice.getMinute());

        LocalDateTime rangeStart = InterviewCalendarSupport.parseInterviewTime("2026-04-20 14:00-17:00");
        assertNotNull(rangeStart);
        assertEquals(14, rangeStart.getHour());
    }

    @Test
    public void buildCalendarUsesSameDayRangeForEnd() {
        Application app = new Application();
        app.setId("A00099");
        app.setJobId("J0001");
        app.setInterviewTime("2026-04-20 14:00-17:00");
        app.setInterviewLocation("Room 101");

        Job job = new Job();
        job.setTitle("TA for Python");
        job.setModuleCode("EBU6377");

        String calendar = InterviewCalendarSupport.buildCalendarFile(app, job);
        assertTrue(calendar.contains("DTSTART:20260420T140000"));
        assertTrue(calendar.contains("DTEND:20260420T170000"));
    }

    @Test
    public void buildCalendarFileIncludesInterviewFields() {
        Application app = new Application();
        app.setId("A00002");
        app.setJobId("J0001");
        app.setInterviewTime("2026-04-18 14:30");
        app.setInterviewLocation("EECS Building Room 402");
        app.setInterviewAssessment("Teaching demo");
        app.setPreferredRole("TA-2");

        Job job = new Job();
        job.setTitle("TA for Software Engineering");
        job.setModuleCode("EBU6304");
        job.setModuleName("Software Engineering");

        String calendar = InterviewCalendarSupport.buildCalendarFile(app, job);
        assertTrue(calendar.contains("BEGIN:VCALENDAR"));
        assertTrue(calendar.contains("SUMMARY:Interview - TA for Software Engineering \\(EBU6304\\)".replace("\\", "")));
        assertTrue(calendar.contains("DTSTART:20260418T143000"));
        assertTrue(calendar.contains("LOCATION:EECS Building Room 402"));
        assertTrue(calendar.contains("Assessment: Teaching demo"));
    }
}
