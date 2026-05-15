package bupt.ta.llm;

import bupt.ta.model.TAProfile;
import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Unit tests for {@link LlmProfileExtractionService}. */
public class LlmProfileExtractionServiceTest {

    @Test
    public void stripMarkdownFenceStripsJsonFence() {
        String raw = "```json\n{\"degree\":\"BSc\"}\n```";
        assertEquals("{\"degree\":\"BSc\"}", LlmProfileExtractionService.stripMarkdownFence(raw));
    }

    @Test
    public void mergeOverwritesWhenModelProvides() {
        TAProfile p = new TAProfile("U1");
        p.setDegree("MSc");
        JsonObject o = new JsonObject();
        o.addProperty("degree", "BSc");
        o.addProperty("programme", "CS");
        LlmProfileExtractionService.mergeJsonIntoProfile(p, o);
        assertEquals("BSc", p.getDegree());
        assertEquals("CS", p.getProgramme());
    }

    @Test
    public void stripPlainJsonUnchanged() {
        String s = "{\"a\":1}";
        assertEquals(s, LlmProfileExtractionService.stripMarkdownFence(s));
    }
}
