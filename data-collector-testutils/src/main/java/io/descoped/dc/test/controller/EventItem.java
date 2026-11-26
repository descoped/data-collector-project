package io.descoped.dc.test.controller;

/**
 * An event served by the mock data controller.
 */
public class EventItem {

    /**
     * The offset added to an item's position to make its event id.
     */
    public static final int EVENT_ID_OFFSET = 1000;

    private final String eventId;

    /**
     * Creates the event.
     *
     * @param eventId the event id
     */
    public EventItem(String eventId) {
        this.eventId = eventId;
    }

    /**
     * The event id.
     *
     * @return the event id
     */
    public String getEventId() {
        return eventId;
    }
}
