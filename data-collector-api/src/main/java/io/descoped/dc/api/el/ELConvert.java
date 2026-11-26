package io.descoped.dc.api.el;

import io.descoped.dc.api.util.CommonUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

public class ELConvert {

    /**
     * Creates the functions; expressions call them on the instance bound in the context.
     */
    public ELConvert() {
        // no state
    }

    public Long utcDateToEpoc(String value) {
        try {
            TemporalAccessor ta = DateTimeFormatter.ISO_DATE_TIME.parse(value);
            // a date with an offset or zone is read with it; one without is read as UTC
            long epochSecond = ta.isSupported(ChronoField.OFFSET_SECONDS)
                    ? ZonedDateTime.from(ta).toEpochSecond()
                    : LocalDateTime.from(ta).toEpochSecond(ZoneOffset.UTC);
            return Long.valueOf(epochSecond);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException("Error convert utc date '" + value + "' to Epoc (long)!\n" + CommonUtils.captureStackTrace(e));
        }
    }

}
