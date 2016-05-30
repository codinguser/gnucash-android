package org.gnucash.android.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;

/**
 * Parses amounts as String into BigDecimal.
 */
public class AmountParser {
    /**
     * Parses {@code amount} and returns it as a BigDecimal.
     *
     * @param amount String with the amount to parse.
     * @return The amount parsed as a BigDecimal.
     * @throws ParseException if the full string couldn't be parsed as an amount.
     */
    public static BigDecimal parse(String amount) throws ParseException {
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getNumberInstance();
        formatter.setParseBigDecimal(true);
        ParsePosition parsePosition = new ParsePosition(0);
        BigDecimal parsedAmount = (BigDecimal) formatter.parse(amount, parsePosition);

        // Ensure any mistyping by the user is caught instead of partially parsed
        if ((parsedAmount == null) || (parsePosition.getIndex() < amount.length()))
            throw new ParseException("Parse error", parsePosition.getErrorIndex());

        return parsedAmount;
    }
}