package bupt.ta.llm;

import bupt.ta.model.TAProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Calls DeepSeek to extract structured profile fields from CV plain text.
 * When the model returns a non-empty value for a field, it overwrites the existing profile value.
 */
public final class LlmProfileExtractionService {

    private static final String SYSTEM_PROMPT = ""
            + "You extract structured data from a CV/resume for a university Teaching Assistant application form. "
            + "Reply with ONE JSON object only, no markdown fences, no commentary. "
            + "Keys (use null or omit only if truly absent from the document): "
            + "studentId (string), email (string), phone (string), "
            + "degree (string), programme (string), yearOfStudy (string), "
            + "skills (array of strings), availability (string), introduction (string), taExperience (string). "
            + "LANGUAGE: All string values MUST be in English, even if the source CV is Chinese or bilingual. "
            + "Translate faithfully; keep proper nouns (company/university names) in their commonly used English form or pinyin only when no English name exists. "
            + "Skills use standard English names (e.g. 'Data structures and algorithms' not Chinese). "
            + "RULES: "
            + "(1) skills — REQUIRED whenever the CV lists any: extract ALL programming languages, frameworks, tools, "
            + "and methods from Skills, Projects, Experience, and Education (e.g. Java, Python, Git, MATLAB). "
            + "Use short English tokens; include at least several items if the CV mentions them. "
            + "(2) taExperience — REQUIRED to contain substantive English text whenever the CV describes projects, internships, "
            + "part-time work, competitions, or any teaching/tutoring/lab/marking/TA roles. "
            + "Merge project experience and relevant work into this field as bullet-style sentences "
            + "(separate with newline or '; '). If the person has no teaching experience, still summarize "
            + "2–4 strongest project or internship lines that show responsibility and technical delivery. "
            + "(3) introduction — 2–4 sentences in English: who they are, focus area, and fit for TA work. "
            + "(4) availability — express in English (e.g. weekdays, time windows). "
            + "Do not invent employers, schools, or dates not present in the text.";

    private final DeepSeekClient client;

    /** Uses environment-driven {@link DeepSeekClient} configuration. */
    public LlmProfileExtractionService() {
        this(new DeepSeekClient());
    }

    /**
     * @param client LLM client (typically from admin settings in the web app)
     */
    public LlmProfileExtractionService(DeepSeekClient client) {
        this.client = client;
    }

    /**
     * Parses CV text and merges into profile. Non-empty values from the model overwrite existing fields.
     *
     * @param profile     profile to update in place
     * @param cvPlainText extracted CV plain text
     * @return {@code true} if the API was called and JSON was merged (even partially)
     * @throws IOException if the API fails or returns invalid JSON
     */
    public boolean extractAndMergeProfile(TAProfile profile, String cvPlainText) throws IOException {
        if (!client.isConfigured() || cvPlainText == null || cvPlainText.trim().isEmpty()) {
            return false;
        }
        String userMsg = "CV text (extract and translate all fields to English in the JSON):\n" + cvPlainText.trim();
        String raw = client.chat(SYSTEM_PROMPT, userMsg);
        String json = stripMarkdownFence(raw);
        JsonObject o;
        try {
            o = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            throw new IOException("DeepSeek did not return valid JSON: " + raw, e);
        }
        mergeJsonIntoProfile(profile, o);
        return true;
    }

    static void mergeJsonIntoProfile(TAProfile profile, JsonObject o) {
        if (profile == null || o == null) {
            return;
        }
        overwriteString(profile::setStudentId, o, "studentId");
        overwriteString(profile::setEmail, o, "email");
        overwriteString(profile::setPhone, o, "phone");
        overwriteString(profile::setDegree, o, "degree");
        overwriteString(profile::setProgramme, o, "programme");
        overwriteString(profile::setYearOfStudy, o, "yearOfStudy");
        overwriteString(profile::setAvailability, o, "availability");
        overwriteString(profile::setIntroduction, o, "introduction");
        overwriteString(profile::setTaExperience, o, "taExperience");

        List<String> skills = parseSkills(o.get("skills"));
        if (!skills.isEmpty()) {
            profile.setSkills(skills);
        }
    }

    private static List<String> parseSkills(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return new ArrayList<>();
        }
        if (el.isJsonArray()) {
            List<String> out = new ArrayList<>();
            for (JsonElement e : el.getAsJsonArray()) {
                if (e.isJsonPrimitive()) {
                    String s = e.getAsString().trim();
                    if (!s.isEmpty()) {
                        out.add(s);
                    }
                }
            }
            return out;
        }
        if (el.isJsonPrimitive()) {
            String s = el.getAsString().trim();
            if (s.isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.stream(s.split(",")).map(String::trim).filter(t -> !t.isEmpty()).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * When the model provides a non-blank string, overwrite the profile field.
     * Omitted or null keys leave the field unchanged.
     */
    private static void overwriteString(java.util.function.Consumer<String> setter, JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return;
        }
        String v = o.get(key).getAsString();
        if (v != null && !v.trim().isEmpty()) {
            setter.accept(v.trim());
        }
    }

    static String stripMarkdownFence(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (!s.startsWith("```")) {
            return s;
        }
        int firstNl = s.indexOf('\n');
        int lastFence = s.lastIndexOf("```");
        if (firstNl < 0 || lastFence <= firstNl) {
            return s;
        }
        String inner = s.substring(firstNl + 1, lastFence).trim();
        if (inner.toLowerCase().startsWith("json")) {
            inner = inner.substring(4).trim();
        }
        return inner;
    }
}
