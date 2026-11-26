package io.descoped.dc.api.el;

import io.descoped.dc.api.util.CommonUtils;

public class ELCast {

    /**
     * Creates the functions; expressions call them on the instance bound in the context.
     */
    public ELCast() {
        // no state
    }

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
