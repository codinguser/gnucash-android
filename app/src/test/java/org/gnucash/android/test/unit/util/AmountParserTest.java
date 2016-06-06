package org.gnucash.android.test.unit.util;

import org.gnucash.android.util.AmountParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class AmountParserTest {
    private Locale mPreviousLocale;

    @Before
    public void setUp() throws Exception {
        mPreviousLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void tearDown() throws Exception {
        Locale.setDefault(mPreviousLocale);
    }

    @Test
    public void testParseIntegerAmount() throws ParseException {
        assertThat(AmountParser.parse("123")).isEqualTo(new BigDecimal(123));
    }

    @Test
    public void parseDecimalAmount() throws ParseException {
        assertThat(AmountParser.parse("123.45")).isEqualTo(new BigDecimal("123.45"));
    }

    @Test
    public void parseDecimalAmountWithDifferentSeparator() throws ParseException {
        Locale.setDefault(Locale.GERMANY);
        assertThat(AmountParser.parse("123,45")).isEqualTo(new BigDecimal("123.45"));
    }

    @Test(expected = ParseException.class)
    public void withGarbageAtTheBeginning_shouldFailWithException() throws ParseException {
        AmountParser.parse("asdf123.45");
    }

    @Test(expected = ParseException.class)
    public void withGarbageAtTheEnd_shouldFailWithException() throws ParseException {
        AmountParser.parse("123.45asdf");
    }

    @Test(expected = ParseException.class)
    public void emptyString_shouldFailWithException() throws ParseException {
        AmountParser.parse("");
    }
}