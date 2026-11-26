package io.descoped.dc.test.controller;

/**
 * An entry of the mock event list: its position and the id of its event.
 */
public class EventListItem {

    private final Integer id;
    private final Integer eventId;

    /**
     * Creates the entry.
     *
     * @param id      the entry's position
     * @param eventId the event id
     */
    public EventListItem(Integer id, Integer eventId) {
        this.id = id;
        this.eventId = eventId;
    }

    /**
     * The entry's position.
     *
     * @return the position
     */
    public Integer getId() {
        return id;
    }

    /**
     * The event id.
     *
     * @return the event id
     */
    public Integer getEventId() {
        return eventId;
    }
}
