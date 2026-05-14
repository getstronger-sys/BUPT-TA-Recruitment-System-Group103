package bupt.ta.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PaymentSupportTest {

    @Test
    public void normalizeFromFormBuildsStablePaymentText() {
        assertEquals("15 GBP/hour", PaymentSupport.normalizeFromForm("15.00", "GBP", "hour"));
        assertEquals("120 CNY/flat stipend", PaymentSupport.normalizeFromForm("120", "CNY", "flat stipend"));
    }

    @Test
    public void normalizeFromFormRejectsInvalidAmount() {
        try {
            PaymentSupport.normalizeFromForm("0", "GBP", "hour");
            fail("Expected invalid payment amount");
        } catch (IllegalArgumentException ex) {
            assertEquals("Payment amount must be greater than 0.", ex.getMessage());
        }
    }

    @Test
    public void inputHelpersExtractValuesFromStoredPayment() {
        String raw = "15 GBP/hour.";

        assertEquals("15", PaymentSupport.amountInputValue(raw));
        assertEquals("GBP", PaymentSupport.currencyInputValue(raw));
        assertEquals("hour", PaymentSupport.rateTypeInputValue(raw));
    }

    @Test
    public void inputHelpersExtractValuesFromLegacyFlatStipend() {
        String raw = "Flat stipend 120 GBP.";

        assertEquals("120", PaymentSupport.amountInputValue(raw));
        assertEquals("GBP", PaymentSupport.currencyInputValue(raw));
        assertEquals("flat stipend", PaymentSupport.rateTypeInputValue(raw));
    }
}
