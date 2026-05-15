package bupt.ta.cv;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Extracts plain text from uploaded CV files for LLM processing.
 * Supports .txt, .pdf, .docx. Legacy .doc is intentionally excluded here.
 */
public final class ResumeTextExtractor {

    private static final int MAX_CHARS = 12_000;
    private static final String TXT = ".txt";
    private static final String PDF = ".pdf";
    private static final String DOCX = ".docx";

    private ResumeTextExtractor() {
    }

    /** Returns whether the file extension can be parsed for text extraction. */
    public static boolean supportsExtension(String extension) {
        String normalized = normalizeExtension(extension);
        return TXT.equals(normalized) || PDF.equals(normalized) || DOCX.equals(normalized);
    }

    /**
     * @return human-readable list of allowed CV extensions for upload UI
     */
    public static String supportedExtensionsDisplay() {
        return "PDF, DOCX or TXT";
    }

    /**
     * Extracts plain text from a CV file, truncated to {@link #MAX_CHARS} characters.
     */
    public static String extract(Path absoluteFile, String extensionLowercase) throws IOException {
        if (absoluteFile == null || !Files.isRegularFile(absoluteFile)) {
            return "";
        }
        String ext = normalizeExtension(extensionLowercase);
        String raw;
        switch (ext) {
            case TXT:
                raw = Files.readString(absoluteFile, StandardCharsets.UTF_8);
                break;
            case PDF:
                try (PDDocument doc = PDDocument.load(absoluteFile.toFile())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    raw = stripper.getText(doc);
                }
                break;
            case DOCX:
                try (FileInputStream fis = new FileInputStream(absoluteFile.toFile());
                     XWPFDocument doc = new XWPFDocument(fis)) {
                    StringBuilder sb = new StringBuilder();
                    for (XWPFParagraph p : doc.getParagraphs()) {
                        sb.append(p.getText()).append('\n');
                    }
                    raw = sb.toString();
                }
                break;
            default:
                return "";
        }
        return limit(raw);
    }

    private static String normalizeExtension(String extension) {
        return extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
    }

    private static String limit(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\0', ' ').trim();
        if (t.length() <= MAX_CHARS) {
            return t;
        }
        return t.substring(0, MAX_CHARS);
    }
}
