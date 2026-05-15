package bupt.ta.cv;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link ResumeTextExtractor}. */
public class ResumeTextExtractorTest {

    @Test
    public void supportsOnlyConfiguredExtensions() {
        assertTrue(ResumeTextExtractor.supportsExtension(".pdf"));
        assertTrue(ResumeTextExtractor.supportsExtension(".DOCX"));
        assertTrue(ResumeTextExtractor.supportsExtension(".txt"));
        assertFalse(ResumeTextExtractor.supportsExtension(".doc"));
        assertFalse(ResumeTextExtractor.supportsExtension(".rtf"));
    }

    @Test
    public void extractReadsPlainTextFiles() throws Exception {
        Path file = Files.createTempFile("resume-extractor", ".txt");
        try {
            Files.writeString(file, "  Java TA candidate  ", StandardCharsets.UTF_8);

            String text = ResumeTextExtractor.extract(file, ".txt");

            assertEquals("Java TA candidate", text);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void extractReturnsEmptyForUnsupportedExtensions() throws Exception {
        Path file = Files.createTempFile("resume-extractor", ".doc");
        try {
            Files.writeString(file, "legacy doc placeholder", StandardCharsets.UTF_8);

            String text = ResumeTextExtractor.extract(file, ".doc");

            assertEquals("", text);
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
