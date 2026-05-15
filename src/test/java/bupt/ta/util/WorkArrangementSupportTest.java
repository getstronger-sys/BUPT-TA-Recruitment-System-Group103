package bupt.ta.util;

import bupt.ta.model.Job;
import bupt.ta.model.WorkArrangementItem;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link WorkArrangementSupport}. */
public class WorkArrangementSupportTest {

    @Test
    public void parseStructuredRowsNormalizesHoursAndSpecificTime() {
        Map<String, String[]> params = new HashMap<>();
        params.put("waWorkName", new String[]{"Lab support"});
        params.put("waSessionDuration", new String[]{"1.5"});
        params.put("waOccurrenceCount", new String[]{"3"});
        params.put("waTaCount", new String[]{"2"});
        params.put("waSpecificDay", new String[]{"Wednesday"});
        params.put("waSpecificStartTime", new String[]{"9:05"});

        List<WorkArrangementItem> rows = WorkArrangementSupport.parseWorkRowsFromRequest(request(params));
        assertEquals(1, rows.size());
        assertNull(WorkArrangementSupport.validateWorkRowsForPosting(rows));

        WorkArrangementItem row = rows.get(0);
        assertEquals("1.5 hours", row.getSessionDuration());
        assertEquals("Wednesday 09:05", row.getSpecificTime());

        Job job = new Job();
        WorkArrangementSupport.applyDerivedFields(job, rows);
        assertTrue(job.getWorkload().contains("Lab support 1.5 hours x 3 occurrence(s)"));
        assertTrue(job.getWorkingHours().contains("Lab support: Wednesday 09:05"));
    }

    @Test
    public void validateRejectsFreeTextDuration() {
        WorkArrangementItem row = new WorkArrangementItem();
        row.setWorkName("Marking");
        row.setSessionDuration("two hours");
        row.setOccurrenceCount(1);
        row.setTaCount(1);

        String error = WorkArrangementSupport.validateWorkRowsForPosting(java.util.Collections.singletonList(row));

        assertEquals("Work arrangement row 1: per-session duration must be a positive number of hours.", error);
    }

    @Test
    public void validateRejectsPartialSpecificTime() {
        Map<String, String[]> params = new HashMap<>();
        params.put("waWorkName", new String[]{"Office hour"});
        params.put("waSessionDuration", new String[]{"1"});
        params.put("waOccurrenceCount", new String[]{"1"});
        params.put("waTaCount", new String[]{"1"});
        params.put("waSpecificDay", new String[]{"Monday"});
        params.put("waSpecificStartTime", new String[]{""});

        List<WorkArrangementItem> rows = WorkArrangementSupport.parseWorkRowsFromRequest(request(params));
        String error = WorkArrangementSupport.validateWorkRowsForPosting(rows);

        assertEquals("Work arrangement row 1: specific time must use weekday and 24-hour time, e.g. Wednesday 14:00.", error);
    }

    @Test
    public void inputHelpersExtractValuesFromLegacyText() {
        assertEquals("2", WorkArrangementSupport.durationHoursInputValue("2 hours"));
        assertEquals("1.5", WorkArrangementSupport.durationHoursInputValue("90 minutes"));
        assertEquals("Wednesday", WorkArrangementSupport.specificDayInputValue("Wed 14:00"));
        assertEquals("14:00", WorkArrangementSupport.specificTimeInputValue("Wed 14:00"));
    }

    private static HttpServletRequest request(Map<String, String[]> params) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getParameterValues".equals(method.getName())) {
                        return params.get((String) args[0]);
                    }
                    if ("toString".equals(method.getName())) {
                        return "MapBackedRequest";
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    return null;
                }
        );
    }
}
