package bupt.ta.service;

import bupt.ta.model.AdminSettings;
import bupt.ta.model.EmailOtpRecord;
import bupt.ta.storage.DataStorage;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Email OTP (one-time password) generation + verification.
 *
 * Storage is persisted via {@link DataStorage} so restarts don't invalidate codes.
 */
public class EmailOtpService {
    public static final int DEFAULT_CODE_LENGTH = 6;
    public static final int DEFAULT_TTL_MINUTES = 10;
    public static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final EmailNotificationService emailService = new EmailNotificationService();

    public static class OtpRequestResult {
        private final boolean success;
        private final String detail;

        OtpRequestResult(boolean success, String detail) {
            this.success = success;
            this.detail = detail;
        }

        public boolean isSuccess() { return success; }
        public String getDetail() { return detail; }
    }

    public static class OtpVerifyResult {
        private final boolean success;
        private final String detail;

        OtpVerifyResult(boolean success, String detail) {
            this.success = success;
            this.detail = detail;
        }

        public boolean isSuccess() { return success; }
        public String getDetail() { return detail; }
    }

    /**
     * Generates an OTP, persists it, and sends it via email (HTML + text).
     *
     * @param storage         persistence
     * @param email           recipient address
     * @param purpose         logical purpose (e.g. REGISTER, RESET_PASSWORD)
     * @param adminSettings   optional admin SMTP settings
     * @return success flag and detail message
     * @throws IOException if persistence fails
     */
    public OtpRequestResult requestOtp(DataStorage storage, String email, String purpose, AdminSettings adminSettings) throws IOException {
        if (storage == null) return new OtpRequestResult(false, "Storage not available.");
        if (isBlank(email)) return new OtpRequestResult(false, "Email is required.");
        if (adminSettings != null && !emailService.isConfigured(adminSettings)) {
            return new OtpRequestResult(false, "Email channel is not configured.");
        }
        if (adminSettings == null && !emailService.isConfigured()) {
            return new OtpRequestResult(false, "Email channel is not configured.");
        }

        String code = generateNumericCode(DEFAULT_CODE_LENGTH);
        EmailOtpRecord record = createRecord(email.trim(), purpose, code);
        storage.addEmailOtpRecord(record);

        String subject = "[TA Recruitment] Your verification code";
        String plain = buildPlainBody(code, record);
        String html = buildHtmlBody(code, record);

        EmailNotificationService.SendResult send = emailService.sendHtml(record.getEmail(), subject, plain, html, adminSettings);
        if (!send.isSuccess()) {
            return new OtpRequestResult(false, "Send failed: " + send.getDetail());
        }
        return new OtpRequestResult(true, "sent");
    }

    /**
     * Verifies an OTP for an email and purpose. On success, marks the latest record as consumed.
     *
     * @param storage persistence
     * @param email   address the code was sent to
     * @param purpose logical purpose matching the request
     * @param code    user-entered verification code
     * @return success flag and detail message
     * @throws IOException if persistence fails
     */
    public OtpVerifyResult verifyOtp(DataStorage storage, String email, String purpose, String code) throws IOException {
        if (storage == null) return new OtpVerifyResult(false, "Storage not available.");
        if (isBlank(email)) return new OtpVerifyResult(false, "Email is required.");
        if (isBlank(code)) return new OtpVerifyResult(false, "Code is required.");

        EmailOtpRecord record = storage.findLatestEmailOtp(email.trim(), purpose);
        if (record == null) {
            return new OtpVerifyResult(false, "No code requested.");
        }
        if (!isBlank(record.getConsumedAt())) {
            return new OtpVerifyResult(false, "Code already used.");
        }
        if (record.getAttempts() >= MAX_ATTEMPTS) {
            return new OtpVerifyResult(false, "Too many attempts.");
        }
        if (isExpired(record)) {
            return new OtpVerifyResult(false, "Code expired.");
        }

        record.setAttempts(record.getAttempts() + 1);
        boolean match = constantTimeEquals(hash(record.getSalt(), code.trim()), record.getCodeHash());
        if (!match) {
            storage.saveEmailOtpRecord(record);
            return new OtpVerifyResult(false, "Invalid code.");
        }

        record.setConsumedAt(Instant.now().toString());
        storage.saveEmailOtpRecord(record);
        return new OtpVerifyResult(true, "verified");
    }

    private EmailOtpRecord createRecord(String email, String purpose, String code) {
        String salt = randomHex(16);
        EmailOtpRecord record = new EmailOtpRecord();
        record.setEmail(email);
        record.setPurpose(isBlank(purpose) ? "" : purpose.trim());
        record.setSalt(salt);
        record.setCodeHash(hash(salt, code));
        record.setAttempts(0);
        record.setCreatedAt(Instant.now().toString());
        record.setExpiresAt(Instant.now().plus(DEFAULT_TTL_MINUTES, ChronoUnit.MINUTES).toString());
        record.setConsumedAt("");
        return record;
    }

    private String buildPlainBody(String code, EmailOtpRecord record) {
        return "Your verification code is: " + code + "\n\n"
                + "It expires at: " + safe(record.getExpiresAt()) + "\n"
                + "If you did not request this, please ignore this email.";
    }

    private String buildHtmlBody(String code, EmailOtpRecord record) {
        String body = "Your verification code is:\n\n"
                + "Code: " + code + "\n"
                + "Expires at: " + safe(record.getExpiresAt()) + "\n\n"
                + "If you did not request this, please ignore this email.";
        // No portal button for OTP emails.
        return emailService.renderHtmlTemplate("Verification code", record.getEmail(), body, null, null);
    }

    private boolean isExpired(EmailOtpRecord record) {
        if (record == null || isBlank(record.getExpiresAt())) return true;
        try {
            Instant exp = Instant.parse(record.getExpiresAt().trim());
            return Instant.now().isAfter(exp);
        } catch (Exception ignored) {
            return true;
        }
    }

    private String generateNumericCode(int length) {
        int n = Math.max(4, Math.min(10, length));
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append((char) ('0' + random.nextInt(10)));
        }
        return sb.toString();
    }

    private String randomHex(int bytes) {
        byte[] buf = new byte[Math.max(8, bytes)];
        random.nextBytes(buf);
        return toHex(buf);
    }

    private String hash(String salt, String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((salt != null ? salt : "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            md.update((code != null ? code : "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return toHex(md.digest());
        } catch (Exception e) {
            // Should never happen on JDK.
            return "";
        }
    }

    private static String toHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        char[] out = new char[bytes.length * 2];
        final char[] HEX = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= (a.charAt(i) ^ b.charAt(i));
        }
        return r == 0;
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }
}

