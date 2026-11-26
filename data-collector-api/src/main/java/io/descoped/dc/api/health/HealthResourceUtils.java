package io.descoped.dc.api.health;

import io.descoped.dc.api.context.ExecutionContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helpers of the health resources.
 */
public class HealthResourceUtils {

    private HealthResourceUtils() {
    }

    /**
     * Divides, giving zero for a zero denominator.
     *
     * @param numerator   the numerator
     * @param denominator the denominator
     * @return the quotient, or 0 if the denominator is 0
     */
    public static float divide(long numerator, long denominator) {
        return denominator == 0 ? 0 : (float) (((double) numerator) / ((double) denominator));
    }

    /**
     * The time elapsed since a moment, as days, hours, minutes, seconds and milliseconds.
     *
     * @param since the moment, in milliseconds since the epoch
     * @return the text
     */
    public static String durationAsString(long since) {
        long elapsedTime = System.currentTimeMillis() - since;

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = elapsedTime / daysInMilli;
        elapsedTime = elapsedTime % daysInMilli;

        long elapsedHours = elapsedTime / hoursInMilli;
        elapsedTime = elapsedTime % hoursInMilli;

        long elapsedMinutes = elapsedTime / minutesInMilli;
        elapsedTime = elapsedTime % minutesInMilli;

        long elapsedSeconds = elapsedTime / secondsInMilli;
        elapsedTime = elapsedTime % secondsInMilli;

        long elapsedMillisSeconds = elapsedTime;

        return String.format(
                "%d days, %d hours, %d minutes, %d seconds, %d millis",
                elapsedDays,
                elapsedHours,
                elapsedMinutes,
                elapsedSeconds,
                elapsedMillisSeconds
        );
    }

    /**
     * Sets the last position on the worker's health monitor in the context's services, if there is one. It reaches
     * the core module's monitor by reflection.
     *
     * @param context      the context
     * @param lastPosition the position
     * @throws RuntimeException if the monitor cannot be reached
     */
    public static void updateMonitorLastPosition(ExecutionContext context, String lastPosition) {
        try {
            Method getServiceMethod = context.services().getClass().getDeclaredMethod("get", Class.class);
            Object monitor = getServiceMethod.invoke(context.services(), Class.forName("io.descoped.dc.core.health.HealthWorkerMonitor"));
            if (monitor == null) {
                return;
            }
            // monitor.contentStream().setLastPosition(String lastPosition)
            Method contentStreamMethod = monitor.getClass().getDeclaredMethod("contentStream");
            Object contentStream = contentStreamMethod.invoke(monitor);
            Method setLastPositionMethod = contentStream.getClass().getDeclaredMethod("setLastPosition", String.class);
            setLastPositionMethod.invoke(contentStream, lastPosition);
        } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
