package bupt.ta.service;

import bupt.ta.model.EmailOtpRecord;
import bupt.ta.storage.DataStorage;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/** Unit tests for {@link EmailOtpService}. */
public class EmailOtpServiceTest {

    @Test
    public void verifyOtpConsumesRecord() throws Exception {
        Path tmp = Files.createTempDirectory("ta-otp-test");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            EmailOtpService svc = new EmailOtpService();

            // Create a record directly (avoid sending emails in tests).
            // Mimic requestOtp() behavior with known code.
            String email = "otp@example.com";
            String purpose = "REGISTER";
            String code = "123456";

            // Use reflection-free approach: ask service to create via requestOtp-like record.
            // We can't call requestOtp() here because it requires configured SMTP.
            java.lang.reflect.Method m = EmailOtpService.class.getDeclaredMethod("createRecord", String.class, String.class, String.class);
            m.setAccessible(true);
            EmailOtpRecord record = (EmailOtpRecord) m.invoke(svc, email, purpose, code);
            storage.addEmailOtpRecord(record);

            EmailOtpService.OtpVerifyResult bad = svc.verifyOtp(storage, email, purpose, "000000");
            assertFalse(bad.isSuccess());

            EmailOtpService.OtpVerifyResult ok = svc.verifyOtp(storage, email, purpose, code);
            assertTrue(ok.isSuccess());

            EmailOtpRecord latest = storage.findLatestEmailOtp(email, purpose);
            assertNotNull(latest);
            assertNotNull(latest.getConsumedAt());
            assertFalse(latest.getConsumedAt().trim().isEmpty());

            EmailOtpService.OtpVerifyResult reused = svc.verifyOtp(storage, email, purpose, code);
            assertFalse(reused.isSuccess());
        } finally {
            // best-effort cleanup
            Files.walk(tmp).sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            });
        }
    }
}

