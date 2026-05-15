package bupt.ta.service;

import bupt.ta.model.AdminSettings;
import bupt.ta.model.SiteNotification;
import bupt.ta.model.TAProfile;
import bupt.ta.model.User;
import bupt.ta.storage.DataStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Summarizes unread site notifications and can send reminder emails in batch.
 */
public class NotificationReminderService {

    /** Counts for the admin unread-reminder preview panel. */
    public static class ReminderPreview {
        private final boolean emailConfigured;
        private final int usersWithUnread;
        private final int unreadMessages;
        private final int remindableUsers;

        public ReminderPreview(boolean emailConfigured, int usersWithUnread, int unreadMessages, int remindableUsers) {
            this.emailConfigured = emailConfigured;
            this.usersWithUnread = usersWithUnread;
            this.unreadMessages = unreadMessages;
            this.remindableUsers = remindableUsers;
        }

        public boolean isEmailConfigured() { return emailConfigured; }
        public int getUsersWithUnread() { return usersWithUnread; }
        public int getUnreadMessages() { return unreadMessages; }
        public int getRemindableUsers() { return remindableUsers; }
    }

    /** Outcome of a batch unread-reminder email run. */
    public static class ReminderResult {
        private final boolean emailConfigured;
        private final int attemptedUsers;
        private final int emailedUsers;
        private final int skippedUsers;

        public ReminderResult(boolean emailConfigured, int attemptedUsers, int emailedUsers, int skippedUsers) {
            this.emailConfigured = emailConfigured;
            this.attemptedUsers = attemptedUsers;
            this.emailedUsers = emailedUsers;
            this.skippedUsers = skippedUsers;
        }

        public boolean isEmailConfigured() { return emailConfigured; }
        public int getAttemptedUsers() { return attemptedUsers; }
        public int getEmailedUsers() { return emailedUsers; }
        public int getSkippedUsers() { return skippedUsers; }
    }

    private final EmailNotificationService emailNotificationService = new EmailNotificationService();

    /**
     * Summarises unread in-app notifications and email readiness.
     *
     * @param storage persistence
     * @return preview counts
     * @throws IOException if data cannot be read
     */
    public ReminderPreview buildPreview(DataStorage storage) throws IOException {
        AdminSettings adminSettings = storage.loadAdminSettings();
        List<SiteNotification> unread = loadUnreadNotifications(storage);
        Map<String, List<SiteNotification>> grouped = groupByRecipient(unread);
        int remindableUsers = 0;
        for (String userId : grouped.keySet()) {
            if (!isBlank(resolveRecipientEmail(storage, userId))) {
                remindableUsers++;
            }
        }
        return new ReminderPreview(
                emailNotificationService.isConfigured(adminSettings),
                grouped.size(),
                unread.size(),
                remindableUsers
        );
    }

    /**
     * Emails each user with unread site notifications a digest (when SMTP is configured).
     *
     * @param storage persistence
     * @return counts of attempted, sent, and skipped users
     * @throws IOException if data cannot be read
     */
    public ReminderResult sendUnreadReminders(DataStorage storage) throws IOException {
        AdminSettings adminSettings = storage.loadAdminSettings();
        List<SiteNotification> unread = loadUnreadNotifications(storage);
        Map<String, List<SiteNotification>> grouped = groupByRecipient(unread);
        if (!emailNotificationService.isConfigured(adminSettings)) {
            return new ReminderResult(false, grouped.size(), 0, grouped.size());
        }

        int attempted = 0;
        int emailed = 0;
        int skipped = 0;
        for (Map.Entry<String, List<SiteNotification>> entry : grouped.entrySet()) {
            String userId = entry.getKey();
            List<SiteNotification> notes = entry.getValue();
            String email = resolveRecipientEmail(storage, userId);
            if (isBlank(email)) {
                skipped++;
                continue;
            }
            attempted++;
            User user = storage.findUserById(userId);
            String displayName = resolveDisplayName(user, userId);
            String subject = "[TA Recruitment] You have unread updates";
            String body = buildReminderBody(displayName, notes, adminSettings);
            String html = buildReminderHtml(displayName, notes, adminSettings);
            EmailNotificationService.SendResult result = emailNotificationService.sendHtml(email, subject, body, html, adminSettings);
            if (result.isSuccess()) {
                emailed++;
            } else {
                skipped++;
            }
        }
        return new ReminderResult(true, attempted, emailed, skipped);
    }

    private List<SiteNotification> loadUnreadNotifications(DataStorage storage) throws IOException {
        return storage.loadSiteNotifications().stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
    }

    private Map<String, List<SiteNotification>> groupByRecipient(List<SiteNotification> unread) {
        Map<String, List<SiteNotification>> grouped = new LinkedHashMap<>();
        unread.stream()
                .sorted(Comparator.comparing(SiteNotification::getCreatedAt, Comparator.nullsLast(String::compareTo)).reversed())
                .forEach(n -> grouped.computeIfAbsent(n.getRecipientUserId(), key -> new ArrayList<>()).add(n));
        return grouped;
    }

    private String resolveRecipientEmail(DataStorage storage, String userId) throws IOException {
        TAProfile profile = storage.getProfileByUserId(userId);
        if (profile != null && !isBlank(profile.getEmail())) {
            return profile.getEmail().trim();
        }
        User user = storage.findUserById(userId);
        return user != null && !isBlank(user.getEmail()) ? user.getEmail().trim() : null;
    }

    private String buildReminderBody(String displayName, List<SiteNotification> notes, AdminSettings adminSettings) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(displayName).append(",\n\n");
        body.append("You have ").append(notes.size()).append(" unread recruitment update");
        if (notes.size() != 1) {
            body.append('s');
        }
        body.append(" in the TA Recruitment System.\n");
        body.append("Here are the latest items:\n");
        int limit = Math.min(notes.size(), 5);
        for (int i = 0; i < limit; i++) {
            SiteNotification note = notes.get(i);
            body.append("- ").append(safe(note.getTitle()));
            if (!isBlank(note.getCreatedAt())) {
                body.append(" (").append(note.getCreatedAt().replace('T', ' ')).append(")");
            }
            body.append('\n');
        }
        if (notes.size() > limit) {
            body.append("- and ").append(notes.size() - limit).append(" more unread updates\n");
        }
        body.append("\nPlease sign in and open Messages / My Applications to review them.");
        return emailNotificationService.maybeAppendPortalLink(body.toString(), "/ta/messages", adminSettings);
    }

    private String buildReminderHtml(String displayName, List<SiteNotification> notes, AdminSettings adminSettings) {
        StringBuilder bodyText = new StringBuilder();
        bodyText.append("Hello ").append(displayName).append(",\n\n");
        bodyText.append("You have ").append(notes.size()).append(" unread recruitment update");
        if (notes.size() != 1) {
            bodyText.append('s');
        }
        bodyText.append(" in the TA Recruitment System.\n");
        bodyText.append("Here are the latest items:\n");
        int limit = Math.min(notes.size(), 5);
        for (int i = 0; i < limit; i++) {
            SiteNotification note = notes.get(i);
            bodyText.append("- ").append(safe(note.getTitle()));
            if (!isBlank(note.getCreatedAt())) {
                bodyText.append(" (").append(note.getCreatedAt().replace('T', ' ')).append(")");
            }
            bodyText.append('\n');
        }
        if (notes.size() > limit) {
            bodyText.append("- and ").append(notes.size() - limit).append(" more unread updates\n");
        }
        bodyText.append("\nPlease sign in and open Messages / My Applications to review them.");

        String portalUrl = emailNotificationService.resolvePortalUrl("/ta/messages", adminSettings);
        return emailNotificationService.renderHtmlTemplate(
                "Unread updates",
                displayName,
                bodyText.toString(),
                portalUrl != null ? "View messages" : null,
                portalUrl
        );
    }

    private static String resolveDisplayName(User user, String fallback) {
        if (user != null && !isBlank(user.getRealName())) {
            return user.getRealName().trim();
        }
        if (user != null && !isBlank(user.getUsername())) {
            return user.getUsername().trim();
        }
        return fallback != null ? fallback : "user";
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Update" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
