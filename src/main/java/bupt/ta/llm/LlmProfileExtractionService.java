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

    private static final String LANGUAGE_BLOCK = ""
            + "CRITICAL — OUTPUT LANGUAGE: English ONLY. "
            + "Every string value in the JSON must be written in English (Latin script). "
            + "Never output Chinese (中文), Japanese, or Korean characters. "
            + "If the CV is in Chinese, translate all fields into natural English. "
            + "Examples: degree 'Bachelor' not '本科'; programme 'Computer Science and Technology' not '计算机科学与技术'. "
            + "Skills: Java, Python, React.js (never Chinese skill names). ";

    private static final String SYSTEM_PROMPT = ""
            + LANGUAGE_BLOCK
            + "You extract structured data from a CV/resume for a university Teaching Assistant application form. "
            + "Reply with ONE JSON object only, no markdown fences, no commentary. "
            + "Keys (use null or omit only if truly absent from the document): "
            + "studentId (string), email (string), phone (string), "
            + "degree (string), programme (string), yearOfStudy (string), "
            + "skills (array of strings), availability (string), introduction (string), taExperience (string). "
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
        String cv = cvPlainText.trim();
        JsonObject parsed = requestProfileJson(cv, false);
        if (jsonObjectHasCjk(parsed)) {
            parsed = requestProfileJson(cv, true);
        }
        mergeJsonIntoProfile(profile, parsed);
        return true;
    }

    private JsonObject requestProfileJson(String cvPlainText, boolean strictRetry) throws IOException {
        String userMsg = strictRetry
                ? "RETRY — previous reply contained non-English text. "
                + "Return the same JSON schema with EVERY string value in English only. No Chinese characters.\n\nCV:\n"
                + cvPlainText
                : "Extract fields and translate ALL string values to English in the JSON.\n\nCV:\n" + cvPlainText;
        String raw = client.chat(SYSTEM_PROMPT, userMsg);
        String json = stripMarkdownFence(raw);
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            throw new IOException("DeepSeek did not return valid JSON: " + raw, e);
        }
    }

    /** True if any string value in the profile JSON contains CJK characters. */
    static boolean jsonObjectHasCjk(JsonObject o) {
        if (o == null) {
            return false;
        }
        for (String key : new String[]{
                "studentId", "email", "phone", "degree", "programme", "yearOfStudy",
                "availability", "introduction", "taExperience"
        }) {
            if (o.has(key) && !o.get(key).isJsonNull()) {
                if (containsCjk(o.get(key).getAsString())) {
                    return true;
                }
            }
        }
        if (o.has("skills") && o.get("skills").isJsonArray()) {
            for (JsonElement e : o.get("skills").getAsJsonArray()) {
                if (e.isJsonPrimitive() && containsCjk(e.getAsString())) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean containsCjk(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                return true;
            }
        }
        return false;
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
