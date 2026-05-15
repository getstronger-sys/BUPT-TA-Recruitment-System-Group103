package bupt.ta.util;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes structured payment inputs on the MO posting form.
 */
public final class PaymentSupport {
    private static final String[] CURRENCIES = {"GBP", "CNY", "USD", "EUR"};
    private static final String[] RATE_TYPES = {"hour", "session", "flat stipend"};
    private static final Pattern AMOUNT_FIRST = Pattern.compile(
            "^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([A-Za-z]{3})(?:\\s*/\\s*([A-Za-z ]+))?.*$");
    private static final Pattern CURRENCY_FIRST = Pattern.compile(
            "^\\s*([A-Za-z]{3})\\s*([0-9]+(?:\\.[0-9]+)?)(?:\\s*/\\s*([A-Za-z ]+))?.*$");
    private static final Pattern FLAT_STIPEND = Pattern.compile(
            "^\\s*Flat\\s+stipend\\s+([0-9]+(?:\\.[0-9]+)?)\\s*([A-Za-z]{3}).*$",
            Pattern.CASE_INSENSITIVE);

    private PaymentSupport() {
    }

    /**
     * Validates form fields and returns a canonical payment string (e.g. {@code 12 GBP/hour}).
     *
     * @param amountRaw   numeric amount from the form
     * @param currencyRaw currency code
     * @param rateTypeRaw rate type (hour, session, flat stipend)
     * @return normalized payment text stored on {@link bupt.ta.model.Job}
     * @throws IllegalArgumentException when validation fails
     */
    public static String normalizeFromForm(String amountRaw, String currencyRaw, String rateTypeRaw) {
        String amount = trim(amountRaw);
        String currency = normalizeCurrency(currencyRaw);
        String rateType = normalizeRateType(rateTypeRaw);

        if (amount.isEmpty()) {
            throw new IllegalArgumentException("Payment amount is required.");
        }
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(amount);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Payment amount must be a valid number.");
        }
        if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0.");
        }
        if (currency.isEmpty()) {
            throw new IllegalArgumentException("Payment currency is required.");
        }
        if (rateType.isEmpty()) {
            throw new IllegalArgumentException("Payment rate type is required.");
        }

        return formatAmount(parsed) + " " + currency + "/" + rateType;
    }

    /**
     * @param raw stored payment string
     * @return amount for repopulating the MO form, or empty when unparseable
     */
    public static String amountInputValue(String raw) {
        ParsedPayment parsed = parse(raw);
        return parsed != null ? parsed.amount : "";
    }

    /**
     * @param raw stored payment string
     * @return currency code for the form (defaults to GBP)
     */
    public static String currencyInputValue(String raw) {
        ParsedPayment parsed = parse(raw);
        return parsed != null ? parsed.currency : "GBP";
    }

    /**
     * @param raw stored payment string
     * @return rate type for the form (defaults to hour)
     */
    public static String rateTypeInputValue(String raw) {
        ParsedPayment parsed = parse(raw);
        return parsed != null ? parsed.rateType : "hour";
    }

    /**
     * @return supported currency codes for the MO posting form
     */
    public static String[] currencies() {
        return CURRENCIES.clone();
    }

    /**
     * @return supported rate types for the MO posting form
     */
    public static String[] rateTypes() {
        return RATE_TYPES.clone();
    }

    private static ParsedPayment parse(String raw) {
        String value = trim(raw);
        if (value.isEmpty()) {
            return null;
        }

        Matcher flat = FLAT_STIPEND.matcher(value);
        if (flat.matches()) {
            return new ParsedPayment(flat.group(1), normalizeCurrency(flat.group(2)), "flat stipend");
        }

        Matcher amountFirst = AMOUNT_FIRST.matcher(value);
        if (amountFirst.matches()) {
            return new ParsedPayment(amountFirst.group(1), normalizeCurrency(amountFirst.group(2)),
                    normalizeRateType(amountFirst.group(3)));
        }

        Matcher currencyFirst = CURRENCY_FIRST.matcher(value);
        if (currencyFirst.matches()) {
            return new ParsedPayment(currencyFirst.group(2), normalizeCurrency(currencyFirst.group(1)),
                    normalizeRateType(currencyFirst.group(3)));
        }

        return null;
    }

    private static String normalizeCurrency(String raw) {
        String currency = trim(raw).toUpperCase(Locale.ROOT);
        for (String allowed : CURRENCIES) {
            if (allowed.equals(currency)) {
                return allowed;
            }
        }
        return "";
    }

    private static String normalizeRateType(String raw) {
        String rateType = trim(raw).toLowerCase(Locale.ROOT);
        if (rateType.isEmpty()) {
            return "hour";
        }
        if ("per hour".equals(rateType) || "hourly".equals(rateType) || "hr".equals(rateType)) {
            return "hour";
        }
        if ("per session".equals(rateType)) {
            return "session";
        }
        if ("flat".equals(rateType) || "stipend".equals(rateType) || "flat stipend".equals(rateType)) {
            return "flat stipend";
        }
        for (String allowed : RATE_TYPES) {
            if (allowed.equals(rateType)) {
                return allowed;
            }
        }
        return "";
    }

    private static String formatAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ParsedPayment {
        private final String amount;
        private final String currency;
        private final String rateType;

        private ParsedPayment(String amount, String currency, String rateType) {
            this.amount = amount;
            this.currency = currency;
            this.rateType = rateType.isEmpty() ? "hour" : rateType;
        }
    }
}
