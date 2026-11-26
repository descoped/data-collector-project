package io.descoped.dc.api.el;

import io.descoped.dc.api.util.CommonUtils;

/**
 * The built-in expression functions {@code cast}: string to number conversions.
 */
public class ELCast {

    /**
     * Creates the functions; expressions call them on the instance bound in the context.
     */
    public ELCast() {
        // no state
    }

    /**
     * Parses a string as {@code Float}.
     *
     * @param value the string
     * @return the value, or {@code null} if the string is {@code null}
     * @throws NumberFormatException if the string is not a number of the type
     */
    public Float toFloat(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Float.valueOf(value);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException("Error casting '" + value + "' to Float!\n" + CommonUtils.captureStackTrace(e));
        }
    }

    /**
     * Parses a string as {@code Double}.
     *
     * @param value the string
     * @return the value, or {@code null} if the string is {@code null}
     * @throws NumberFormatException if the string is not a number of the type
     */
    public Double toDouble(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException("Error casting '" + value + "' to Double!\n" + CommonUtils.captureStackTrace(e));
        }
    }

    /**
     * Parses a string as {@code Integer}.
     *
     * @param value the string
     * @return the value, or {@code null} if the string is {@code null}
     * @throws NumberFormatException if the string is not a number of the type
     */
    public Integer toInteger(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException("Error casting '" + value + "' to Integer!\n" + CommonUtils.captureStackTrace(e));
        }
    }

    /**
     * Parses a string as {@code Long}.
     *
     * @param value the string
     * @return the value, or {@code null} if the string is {@code null}
     * @throws NumberFormatException if the string is not a number of the type
     */
    public Long toLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException("Error casting '" + value + "' to Long!\n" + CommonUtils.captureStackTrace(e));
        }
    }
}
