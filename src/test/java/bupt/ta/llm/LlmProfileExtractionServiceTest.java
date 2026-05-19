package bupt.ta.llm;

import bupt.ta.model.TAProfile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void containsCjkDetectsChinese() {
        assertTrue(LlmProfileExtractionService.containsCjk("本科"));
        assertFalse(LlmProfileExtractionService.containsCjk("Bachelor"));
    }

    @Test
    public void jsonObjectHasCjkFlagsChineseFields() {
        JsonObject o = new JsonObject();
        o.addProperty("degree", "本科");
        assertTrue(LlmProfileExtractionService.jsonObjectHasCjk(o));
        o.addProperty("degree", "Bachelor");
        assertFalse(LlmProfileExtractionService.jsonObjectHasCjk(o));
    }

    @Test
    public void jsonObjectHasCjkInSkillsArray() {
        JsonObject o = new JsonObject();
        JsonArray skills = new JsonArray();
        skills.add("Java");
        o.add("skills", skills);
        assertFalse(LlmProfileExtractionService.jsonObjectHasCjk(o));
        skills.add("数据结构");
        assertTrue(LlmProfileExtractionService.jsonObjectHasCjk(o));
    }
}
