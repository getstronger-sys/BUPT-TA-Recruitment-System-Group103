package bupt.ta.service;

import bupt.ta.model.AdminSettings;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Sends emails using SMTP settings from admin settings, system properties or environment variables.
 * Disabled by default until host/from are configured.
 */
public class EmailNotificationService {

    private static final Path LOCAL_MAIL_PROPERTIES = Paths.get("data", "mail.properties");

    /** Resolved SMTP and portal settings used to send mail. */
    public static class EmailSettings {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final String from;
        private final boolean enabled;
        private final boolean auth;
        private final boolean startTls;
        private final boolean ssl;
        private final String appBaseUrl;

        EmailSettings(String host, int port, String username, String password, String from, boolean enabled,
                      boolean auth, boolean startTls, boolean ssl, String appBaseUrl) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.from = from;
            this.enabled = enabled;
            this.auth = auth;
            this.startTls = startTls;
            this.ssl = ssl;
            this.appBaseUrl = appBaseUrl;
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getFrom() { return from; }
        public boolean isEnabled() { return enabled; }
        public boolean isAuth() { return auth; }
        public boolean isStartTls() { return startTls; }
        public boolean isSsl() { return ssl; }
        public String getAppBaseUrl() { return appBaseUrl; }

        /**
         * @return {@code true} when email is enabled and host/from are set
         */
        public boolean isConfigured() {
            return enabled && !isBlank(host) && !isBlank(from);
        }
    }

    /** Outcome of a single send attempt. */
    public static class SendResult {
        private final boolean success;
        private final String detail;

        SendResult(boolean success, String detail) {
            this.success = success;
            this.detail = detail;
        }

        public boolean isSuccess() { return success; }
        public String getDetail() { return detail; }
    }

    /**
     * Loads SMTP settings from environment, local properties, and optional admin overrides.
     *
     * @return resolved settings (never {@code null})
     */
    public EmailSettings loadSettings() {
        return loadSettings(null);
    }

    /**
     * @param adminSettings optional admin UI settings (may be {@code null})
     * @return resolved settings (never {@code null})
     */
    public EmailSettings loadSettings(AdminSettings adminSettings) {
        Properties localProperties = loadLocalProperties();
        String host = firstNonBlank(
                System.getProperty("ta.mail.host"),
                System.getenv("TA_MAIL_HOST"),
                adminSetting(adminSettings != null ? adminSettings.getMailHost() : null),
                property(localProperties, "ta.mail.host"));
        int port = parseInt(firstNonBlank(
                System.getProperty("ta.mail.port"),
                System.getenv("TA_MAIL_PORT"),
                adminSetting(adminSettings != null ? String.valueOf(adminSettings.getMailPort()) : null),
                property(localProperties, "ta.mail.port")), 587);
        String username = firstNonBlank(
                System.getProperty("ta.mail.username"),
                System.getenv("TA_MAIL_USERNAME"),
                adminSetting(adminSettings != null ? adminSettings.getMailUsername() : null),
                property(localProperties, "ta.mail.username"));
        String password = firstNonBlank(
                System.getProperty("ta.mail.password"),
                System.getenv("TA_MAIL_PASSWORD"),
                adminSetting(adminSettings != null ? adminSettings.getMailPassword() : null),
                property(localProperties, "ta.mail.password"));
        String from = firstNonBlank(
                System.getProperty("ta.mail.from"),
                System.getenv("TA_MAIL_FROM"),
                adminSetting(adminSettings != null ? adminSettings.getMailFrom() : null),
                property(localProperties, "ta.mail.from"));
        boolean enabled = parseBoolean(firstNonBlank(
                System.getProperty("ta.mail.enabled"),
                System.getenv("TA_MAIL_ENABLED"),
                adminSetting(adminSettings != null ? String.valueOf(adminSettings.isMailEnabled()) : null),
                property(localProperties, "ta.mail.enabled")), true);
        boolean auth = parseBoolean(firstNonBlank(
                System.getProperty("ta.mail.auth"),
                System.getenv("TA_MAIL_AUTH"),
                adminSetting(adminSettings != null ? String.valueOf(adminSettings.isMailAuth()) : null),
                property(localProperties, "ta.mail.auth")), !isBlank(username));
        boolean startTls = parseBoolean(firstNonBlank(
                System.getProperty("ta.mail.starttls"),
                System.getenv("TA_MAIL_STARTTLS"),
                adminSetting(adminSettings != null ? String.valueOf(adminSettings.isMailStartTls()) : null),
                property(localProperties, "ta.mail.starttls")), true);
        boolean ssl = parseBoolean(firstNonBlank(
                System.getProperty("ta.mail.ssl"),
                System.getenv("TA_MAIL_SSL"),
                adminSetting(adminSettings != null ? String.valueOf(adminSettings.isMailSsl()) : null),
                property(localProperties, "ta.mail.ssl")), false);
        String appBaseUrl = trimToNull(firstNonBlank(
                System.getProperty("ta.mail.appBaseUrl"),
                System.getenv("TA_MAIL_APP_BASE_URL"),
                adminSetting(adminSettings != null ? adminSettings.getMailAppBaseUrl() : null),
                property(localProperties, "ta.mail.appBaseUrl")));

        return new EmailSettings(host, port, username, password, from, enabled, auth, startTls, ssl, appBaseUrl);
    }

    /**
     * @return {@code true} when SMTP host and from-address are configured
     */
    public boolean isConfigured() {
        return loadSettings().isConfigured();
    }

    /**
     * @param adminSettings optional admin UI settings
     * @return {@code true} when SMTP host and from-address are configured
     */
    public boolean isConfigured(AdminSettings adminSettings) {
        return loadSettings(adminSettings).isConfigured();
    }

    /**
     * @param to      recipient address
     * @param subject message subject
     * @param body    plain-text body
     * @return send outcome
     */
    public SendResult sendPlainText(String to, String subject, String body) {
        return sendPlainText(to, subject, body, null);
    }

    /**
     * @param to            recipient address
     * @param subject       message subject
     * @param body          plain-text body
     * @param adminSettings optional admin SMTP overrides
     * @return send outcome
     */
    public SendResult sendPlainText(String to, String subject, String body, AdminSettings adminSettings) {
        if (isBlank(to)) {
            return new SendResult(false, "Missing recipient email.");
        }

        EmailSettings settings = loadSettings(adminSettings);
        if (!settings.isConfigured()) {
            return new SendResult(false, "SMTP is not configured.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", settings.getHost());
        props.put("mail.smtp.port", String.valueOf(settings.getPort()));
        props.put("mail.smtp.auth", String.valueOf(settings.isAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(settings.isStartTls()));
        if (settings.isSsl()) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        Authenticator authenticator = null;
        if (settings.isAuth()) {
            final String username = settings.getUsername();
            final String password = settings.getPassword();
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
        }

        try {
            Session session = Session.getInstance(props, authenticator);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(settings.getFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            message.setSubject(subject != null ? subject : "", StandardCharsets.UTF_8.name());
            message.setText(body != null ? body : "", StandardCharsets.UTF_8.name());
            Transport.send(message);
            return new SendResult(true, "sent");
        } catch (MessagingException e) {
            return new SendResult(false, e.getMessage());
        }
    }

    /**
     * Sends a multipart alternative message (plain + HTML).
     *
     * @param to            recipient address
     * @param subject       message subject
     * @param plainBody     plain-text fallback
     * @param htmlBody      HTML body
     * @param adminSettings optional admin SMTP overrides
     * @return send outcome
     */
    public SendResult sendHtml(String to, String subject, String plainBody, String htmlBody, AdminSettings adminSettings) {
        if (isBlank(to)) {
            return new SendResult(false, "Missing recipient email.");
        }

        EmailSettings settings = loadSettings(adminSettings);
        if (!settings.isConfigured()) {
            return new SendResult(false, "SMTP is not configured.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", settings.getHost());
        props.put("mail.smtp.port", String.valueOf(settings.getPort()));
        props.put("mail.smtp.auth", String.valueOf(settings.isAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(settings.isStartTls()));
        if (settings.isSsl()) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        Authenticator authenticator = null;
        if (settings.isAuth()) {
            final String username = settings.getUsername();
            final String password = settings.getPassword();
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
        }

        try {
            Session session = Session.getInstance(props, authenticator);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(settings.getFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            message.setSubject(subject != null ? subject : "", StandardCharsets.UTF_8.name());

            MimeMultipart alternative = new MimeMultipart("alternative");

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(plainBody != null ? plainBody : "", StandardCharsets.UTF_8.name());
            alternative.addBodyPart(textPart);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody != null ? htmlBody : "", "text/html; charset=UTF-8");
            alternative.addBodyPart(htmlPart);

            message.setContent(alternative);
            Transport.send(message);
            return new SendResult(true, "sent");
        } catch (MessagingException e) {
            return new SendResult(false, e.getMessage());
        }
    }

    /**
     * Appends a portal URL line when {@code ta.mail.appBaseUrl} is configured.
     *
     * @param body         existing message body
     * @param relativePath path under the app base URL (e.g. {@code /ta/applications})
     * @return body with link appended, or unchanged when not configured
     */
    public String maybeAppendPortalLink(String body, String relativePath) {
        return maybeAppendPortalLink(body, relativePath, null);
    }

    /**
     * @param adminSettings optional admin SMTP overrides
     * @return body with link appended, or unchanged when not configured
     */
    public String maybeAppendPortalLink(String body, String relativePath, AdminSettings adminSettings) {
        EmailSettings settings = loadSettings(adminSettings);
        if (!settings.isConfigured() || isBlank(settings.getAppBaseUrl()) || isBlank(relativePath)) {
            return body;
        }
        String base = settings.getAppBaseUrl().endsWith("/")
                ? settings.getAppBaseUrl().substring(0, settings.getAppBaseUrl().length() - 1)
                : settings.getAppBaseUrl();
        String path = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return body + "\n\nPortal: " + base + path;
    }

    /**
     * Builds an absolute portal URL for a relative path.
     *
     * @param relativePath  path under the app base URL
     * @param adminSettings optional admin SMTP overrides
     * @return absolute URL, or {@code null} when base URL is not configured
     */
    public String resolvePortalUrl(String relativePath, AdminSettings adminSettings) {
        EmailSettings settings = loadSettings(adminSettings);
        if (!settings.isConfigured() || isBlank(settings.getAppBaseUrl()) || isBlank(relativePath)) {
            return null;
        }
        String base = settings.getAppBaseUrl().endsWith("/")
                ? settings.getAppBaseUrl().substring(0, settings.getAppBaseUrl().length() - 1)
                : settings.getAppBaseUrl();
        String path = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return base + path;
    }

    /**
     * @return branded HTML email document
     */
    public String renderHtmlTemplate(String title, String bodyText, String actionText, String actionUrl) {
        return renderHtmlTemplate(title, null, bodyText, actionText, actionUrl);
    }

    /**
     * @param title       email heading
     * @param displayName optional recipient name shown in the body
     * @param bodyText    plain message (newlines converted to HTML)
     * @param actionText  optional button label
     * @param actionUrl   optional button href
     * @return branded HTML email document
     */
    public String renderHtmlTemplate(String title, String displayName, String bodyText, String actionText, String actionUrl) {
        String safeTitle = escHtml(title != null ? title : "");
        String safeBody = formatBodyText(bodyText != null ? bodyText : "");

        String button = "";
        if (!isBlank(actionText) && !isBlank(actionUrl)) {
            button =
                    "<div style=\"margin-top:18px\">" +
                    "<a href=\"" + escAttr(actionUrl) + "\" " +
                    "style=\"display:inline-block;background:#2575fc;color:#ffffff;text-decoration:none;padding:11px 18px;border-radius:12px;font-weight:800;box-shadow:0 10px 22px rgba(37,117,252,0.25)\">" +
                    escHtml(actionText) +
                    "</a>" +
                    "</div>";
        }

        String who = "";
        if (!isBlank(displayName)) {
            who = "<div style=\"margin-top:12px;font-size:14px;color:#2a2f45\">" +
                    "<span style=\"font-weight:900;color:#c5006b\">To:</span> " +
                    "<span style=\"font-weight:900;color:#123b7a\">" + escHtml(displayName.trim()) + "</span>" +
                    "</div>";
        }

        // Inline CSS for maximum email-client compatibility.
        return "<!doctype html>" +
                "<html><head><meta charset=\"utf-8\"></head>" +
                "<body style=\"margin:0;background:#eef3f9;color:#1f2f46;font-family:Segoe UI,Tahoma,Geneva,Verdana,sans-serif\">" +
                "<div style=\"max-width:640px;margin:0 auto;padding:22px 14px\">" +
                "<div style=\"background:#083b84;color:#fff;border-radius:16px;padding:16px 18px;box-shadow:0 14px 30px rgba(8,59,132,0.18);position:relative;overflow:hidden\">" +
                "<div style=\"position:absolute;top:0;left:0;width:10px;height:100%;background:#c5006b\"></div>" +
                "<div style=\"font-size:13px;letter-spacing:0.35px;opacity:0.92;font-weight:800;padding-left:12px\">BUPT TA Recruitment System</div>" +
                "<div style=\"font-size:20px;font-weight:900;margin-top:8px;line-height:1.2;padding-left:12px\">" + safeTitle + "</div>" +
                "</div>" +
                "<div style=\"background:#ffffff;border:1px solid #d6e2f0;border-radius:16px;padding:18px 18px;margin-top:14px\">" +
                who +
                "<div style=\"font-size:15px;line-height:1.7;color:#1f2f46;margin-top:10px\">" + safeBody + "</div>" +
                button +
                "</div>" +
                "<div style=\"font-size:12px;line-height:1.6;color:#5a6f8c;margin-top:12px;padding:0 4px\">" +
                "This is an automated message. Please sign in to the portal for the latest updates." +
                "</div>" +
                "</div></body></html>";
    }

    private static String adminSetting(String value) {
        return isBlank(value) ? null : value;
    }

    private static String nl2br(String s) {
        return s.replace("\r\n", "\n").replace("\n", "<br/>");
    }

    private static String formatBodyText(String raw) {
        // Escape first, then add minimal rich formatting.
        String escaped = escHtml(raw == null ? "" : raw);
        String withBreaks = nl2br(escaped);

        // Highlight common label lines like "Time: xxx" or "Location: yyy"
        String[] labels = new String[] {
                "Time:", "Location:", "Assessment / notes:", "Assessment:", "Notes:", "Module:", "Status:"
        };
        for (String label : labels) {
            withBreaks = withBreaks.replace(label,
                    "<span style=\"font-weight:900;color:#c5006b\">" + label + "</span>");
        }

        // Emphasize bullet starts "- " as a colored dot.
        withBreaks = withBreaks.replace("<br/>- ", "<br/><span style=\"color:#2575fc;font-weight:900\">•</span> ");
        if (withBreaks.startsWith("- ")) {
            withBreaks = "<span style=\"color:#2575fc;font-weight:900\">•</span> " + withBreaks.substring(2);
        }

        // Slightly emphasize the word "Congratulations" if present.
        withBreaks = withBreaks.replace("Congratulations",
                "<span style=\"font-weight:900;color:#123b7a\">Congratulations</span>");

        return withBreaks;
    }

    private static String escAttr(String s) {
        return escHtml(s).replace("\"", "&quot;");
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }

    private static int parseInt(String raw, int defaultValue) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(String raw, boolean defaultValue) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        String normalized = raw.trim().toLowerCase();
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
            return false;
        }
        return defaultValue;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String property(Properties properties, String key) {
        return properties != null ? properties.getProperty(key) : null;
    }

    private static Properties loadLocalProperties() {
        if (!Files.exists(LOCAL_MAIL_PROPERTIES)) {
            return new Properties();
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(LOCAL_MAIL_PROPERTIES)) {
            properties.load(in);
            return properties;
        } catch (IOException e) {
            return new Properties();
        }
    }
}
