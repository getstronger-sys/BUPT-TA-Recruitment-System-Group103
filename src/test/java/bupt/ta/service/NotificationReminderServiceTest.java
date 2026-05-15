package bupt.ta.service;

import bupt.ta.model.SiteNotification;
import bupt.ta.model.TAProfile;
import bupt.ta.model.User;
import bupt.ta.storage.DataStorage;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** Unit tests for {@link NotificationReminderService}. */
public class NotificationReminderServiceTest {

    private final NotificationReminderService reminderService = new NotificationReminderService();

    @Test
    public void buildPreviewCountsUnreadUsersAndMessages() throws Exception {
        System.setProperty("ta.mail.enabled", "false");
        Path tmp = Files.createTempDirectory("ta-reminder-test");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            User user = createUser(storage, "ta-reminder");
            TAProfile profile = new TAProfile(user.getId());
            profile.setEmail("ta-reminder@example.com");
            storage.saveProfile(profile);

            SiteNotification unread1 = new SiteNotification();
            unread1.setRecipientUserId(user.getId());
            unread1.setTitle("Interview stage");
            unread1.setRead(false);
            storage.addSiteNotification(unread1);

            SiteNotification unread2 = new SiteNotification();
            unread2.setRecipientUserId(user.getId());
            unread2.setTitle("Interview details");
            unread2.setRead(false);
            storage.addSiteNotification(unread2);

            NotificationReminderService.ReminderPreview preview = reminderService.buildPreview(storage);
            assertEquals(1, preview.getUsersWithUnread());
            assertEquals(2, preview.getUnreadMessages());
            assertEquals(1, preview.getRemindableUsers());
            assertFalse(preview.isEmailConfigured());
        } finally {
            System.clearProperty("ta.mail.enabled");
            deleteRecursive(tmp);
        }
    }

    @Test
    public void sendUnreadRemindersSkipsWhenEmailNotConfigured() throws Exception {
        System.setProperty("ta.mail.enabled", "false");
        Path tmp = Files.createTempDirectory("ta-reminder-test");
        try {
            DataStorage storage = new DataStorage(tmp.toString());
            User user = createUser(storage, "ta-reminder-send");

            SiteNotification unread = new SiteNotification();
            unread.setRecipientUserId(user.getId());
            unread.setTitle("Unread");
            unread.setRead(false);
            storage.addSiteNotification(unread);

            NotificationReminderService.ReminderResult result = reminderService.sendUnreadReminders(storage);
            assertFalse(result.isEmailConfigured());
            assertEquals(1, result.getAttemptedUsers());
            assertEquals(0, result.getEmailedUsers());
            assertEquals(1, result.getSkippedUsers());
        } finally {
            System.clearProperty("ta.mail.enabled");
            deleteRecursive(tmp);
        }
    }

    private static User createUser(DataStorage storage, String username) throws Exception {
        User user = new User();
        user.setUsername(username);
        user.setPassword("test123");
        user.setRole("TA");
        user.setEmail(username + "@example.com");
        user.setRealName(username);
        return storage.addUser(user);
    }

    private static void deleteRecursive(Path p) throws Exception {
        if (Files.exists(p)) {
            Files.walk(p).sorted((a, b) -> -a.compareTo(b)).forEach(path -> {
                try { Files.delete(path); } catch (Exception ignored) {}
            });
        }
    }
}
