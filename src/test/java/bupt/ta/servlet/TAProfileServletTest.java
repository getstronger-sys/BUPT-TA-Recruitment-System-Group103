package bupt.ta.servlet;

import bupt.ta.model.TAProfile;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Unit tests for {@link bupt.ta.servlet.TAProfileServlet} helpers. */
public class TAProfileServletTest {

    @Test
    public void validateProfileAcceptsCompleteProfile() {
        TAProfile profile = new TAProfile("U001");
        TAProfileServlet.populateProfile(
                profile,
                "20230001",
                "ta@bupt.edu.cn",
                "13800138000",
                "Mon/Wed 9-12",
                "Patient and organized teaching assistant candidate.",
                "BSc",
                "Computer Science",
                "Year 3",
                "Tutored data structures labs.",
                Arrays.asList("Java", "Teaching")
        );

        assertNull(TAProfileServlet.validateProfile(profile));
    }

    @Test
    public void validateProfileRejectsMissingYearOfStudy() {
        TAProfile profile = new TAProfile("U001");
        TAProfileServlet.populateProfile(
                profile,
                "20230001",
                "ta@bupt.edu.cn",
                "13800138000",
                "Mon/Wed 9-12",
                "Patient and organized teaching assistant candidate.",
                "BSc",
                "Computer Science",
                "",
                "Tutored data structures labs.",
                Collections.singletonList("Java")
        );

        assertEquals("Year of study is required.", TAProfileServlet.validateProfile(profile));
    }

    @Test
    public void validateProfileRejectsMissingSkills() {
        TAProfile profile = new TAProfile("U001");
        TAProfileServlet.populateProfile(
                profile,
                "20230001",
                "ta@bupt.edu.cn",
                "13800138000",
                "Mon/Wed 9-12",
                "Patient and organized teaching assistant candidate.",
                "BSc",
                "Computer Science",
                "Year 3",
                "Tutored data structures labs.",
                Collections.emptyList()
        );

        assertEquals("At least one skill is required.", TAProfileServlet.validateProfile(profile));
    }
}
