package io.descoped.dc.core.executor;

import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommittedPositionsTest {

    // the last complete page is "11" (entries 11-20) and the last entry "23", in the next page
    static CommittedPositions marks() {
        return new CommittedPositions("11", "23");
    }

    static List<String> page(int first) {
        return IntStream.range(first, first + 10).mapToObj(String::valueOf).toList();
    }

    @Test
    void thatTheLastCompletePageIsCommittedWithEveryEntry() {
        CommittedPositions committedPositions = marks();
        assertTrue(committedPositions.isCommittedPage("11"));
        assertEquals(Set.copyOf(page(11)), committedPositions.committedInPage("11", page(11)));
        // the next page: up to and including the last entry
        assertEquals(Set.of("21", "22", "23"), committedPositions.committedInPage("21", page(21)));
        // the marks are used once
        assertEquals(Set.of(), committedPositions.committedInPage("31", List.of("23", "31")));
    }

    // the last page has grown since the earlier run: what follows the last committed entry is new
    @Test
    void thatAGrownLastPageKeepsItsNewEntries() {
        CommittedPositions committedPositions = new CommittedPositions("21", "22");
        assertEquals(Set.of("21", "22"), committedPositions.committedInPage("21", page(21)));
        // the mark is used: the next page is not affected
        assertEquals(Set.of(), committedPositions.committedInPage("31", List.of("22", "31")));
    }

    @Test
    void thatNothingIsCommittedWhenTheLastEntryIsNotInTheFirstPage() {
        CommittedPositions committedPositions = marks();
        assertEquals(Set.of(), committedPositions.committedInPage("24", page(24)));
        assertEquals(Set.of(), committedPositions.committedInPage("34", List.of("23", "34")));
    }

    @Test
    void thatACommittedPositionIsTakenOnce() {
        CommittedPositions committedPositions = marks();
        committedPositions.committedInPage("21", page(21));
        assertTrue(committedPositions.take("22"));
        assertFalse(committedPositions.take("22"));
        assertFalse(committedPositions.take("24"));
    }

    @Test
    void thatTheMarksAreReadFromTheTopicAndItsPageTopic() {
        ContentStore contentStore = ProviderConfigurator.configure(ContentStoreInitializer.class, "discarding", Map.of());
        contentStore.publish("topic", "23");
        contentStore.publish("topic-pages", "11");
        CommittedPositions committedPositions = new CommittedPositions();
        committedPositions.read(contentStore, "topic");
        assertTrue(committedPositions.isCommittedPage("11"));
        assertEquals(Set.of("21", "22", "23"), committedPositions.committedInPage("21", page(21)));
    }

    @Test
    void thatAnEmptyStoreCommitsNothing() {
        CommittedPositions committedPositions = new CommittedPositions();
        committedPositions.read(ProviderConfigurator.configure(ContentStoreInitializer.class, "discarding", Map.of()), "topic");
        assertFalse(committedPositions.isCommittedPage("1"));
        assertEquals(Set.of(), committedPositions.committedInPage("1", page(1)));
    }
}
