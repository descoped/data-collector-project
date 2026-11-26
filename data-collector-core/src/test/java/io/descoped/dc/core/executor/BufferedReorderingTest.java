package io.descoped.dc.core.executor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BufferedReorderingTest {

    @Test
    void testOrder() {

        List<String> positionList = new ArrayList<>();
        positionList.add("1");
        positionList.add("2");
        positionList.add("3");
        positionList.add("4");
        positionList.add("5");

        NavigableSet<Integer> completedSet = new TreeSet<>();
        completedSet.add(positionList.indexOf("1"));
        completedSet.add(positionList.indexOf("3"));
        completedSet.add(positionList.indexOf("5"));

        System.out.printf("ceiling 2L: %d%n", completedSet.ceiling(positionList.indexOf("2")));
        System.out.printf("floor 2L: %d%n", completedSet.floor(positionList.indexOf("5")));

        System.out.printf("ceiling 3L: %d%n", completedSet.ceiling(positionList.indexOf("3")));
        System.out.printf("floor 3L: %d%n", completedSet.floor(positionList.indexOf("3")));

        NavigableSet<Integer> headSet = completedSet.headSet(positionList.indexOf("5"), true);
        System.out.printf("headset to 5L: %s%n", headSet.stream().map(Object::toString).collect(Collectors.joining(",")));

        NavigableSet<Integer> subset = completedSet.subSet(positionList.indexOf("1"), true, positionList.indexOf("5"), false);
        System.out.printf("subset 1L to 5L: %s%n", subset.stream().map(Object::toString).collect(Collectors.joining(",")));

        assertEquals(2, completedSet.ceiling(positionList.indexOf("2")));
        assertEquals(4, completedSet.floor(positionList.indexOf("5")));
        assertEquals(List.of(0, 2, 4), List.copyOf(headSet));
        assertEquals(List.of(0, 2), List.copyOf(subset));

    }

    @Test
    void testCompletedPositions() {
        BufferedReordering<String> bufferedReordering = new BufferedReordering<>();
        bufferedReordering.addExpected("1");
        bufferedReordering.addExpected("2");
        bufferedReordering.addExpected("3");
        bufferedReordering.addExpected("4");
        bufferedReordering.addExpected("5");
        bufferedReordering.addExpected("6");
        bufferedReordering.addExpected("7");
        bufferedReordering.addExpected("8");
        bufferedReordering.addExpected("9");
        bufferedReordering.addExpected("10");
        bufferedReordering.addExpected("11");
        bufferedReordering.addExpected("12");
        bufferedReordering.addExpected("13");
        bufferedReordering.addExpected("14");
        bufferedReordering.addExpected("15");

        assertEquals(15, bufferedReordering.expected().size());
        assertEquals("1", bufferedReordering.expected().get(0));
    }

    @Test
    void testStringPositions() {
        Random rnd = new Random(System.currentTimeMillis());

        List<String> allPositions = List.of("abc", "bcd", "cde", "def", "efg", "fgh",
                "ghi", "hij", "ijk", "jkl", "klm", "lmn", "mno", "nop", "opq", "pqr",
                "qrs", "rst", "stu", "tuv", "uvw", "vwx", "wxy", "xyz", "yza", "zab");

        for (int i = 0; i < 100; i++) {

            // Pick a random position ordering
            List<String> positions = new ArrayList<>(allPositions);
            String p1 = positions.remove(rnd.nextInt(26));
            String p2 = positions.remove(rnd.nextInt(25));
            String p3 = positions.remove(rnd.nextInt(24));
            String p4 = positions.remove(rnd.nextInt(23));
            String p5 = positions.remove(rnd.nextInt(22));


            // run re-ordering test

            BufferedReordering<String> completedPositionSequence = new BufferedReordering<>();

            completedPositionSequence.addExpected(p1);
            completedPositionSequence.addExpected(p2);
            completedPositionSequence.addExpected(p3);
            completedPositionSequence.addExpected(p4);
            completedPositionSequence.addExpected(p5);

            assertEquals(List.of(p1, p2, p3, p4, p5), completedPositionSequence.expected());
        }
    }

    // a release is handed on before a later one, also when the callback locks nothing (#69): while position 1's
    // hand-on is held up, position 2 completes on another thread and must not be handed on first. Position 1's
    // callback waits until position 2 has either been handed on (without the lock) or is blocked on the lock.
    @Test
    void thatALaterReleaseWaitsForAnEarlierHandOn() throws Exception {
        BufferedReordering<String> bufferedReordering = new BufferedReordering<>();
        bufferedReordering.addExpected(List.of("1", "2"));
        List<String> handedOn = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch firstInCallback = new CountDownLatch(1);
        CountDownLatch secondHandedOn = new CountDownLatch(1);
        Thread second = new Thread(() -> bufferedReordering.addCompleted("2", released -> {
            handedOn.addAll(released);
            secondHandedOn.countDown();
        }));
        Thread first = new Thread(() -> bufferedReordering.addCompleted("1", released -> {
            firstInCallback.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (secondHandedOn.getCount() > 0 && second.getState() != Thread.State.BLOCKED
                    && System.nanoTime() < deadline) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
            handedOn.addAll(released);
        }));

        first.start();
        assertTrue(firstInCallback.await(10, TimeUnit.SECONDS));
        second.start();
        first.join(TimeUnit.SECONDS.toMillis(20));
        second.join(TimeUnit.SECONDS.toMillis(20));

        assertEquals(List.of("1", "2"), handedOn);
    }
}
