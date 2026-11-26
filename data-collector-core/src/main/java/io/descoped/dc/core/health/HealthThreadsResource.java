package io.descoped.dc.core.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.descoped.dc.api.health.HealthRenderPriority;
import io.descoped.dc.api.health.HealthResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The health resource {@code thread-info}: every thread of the JVM, grouped by thread group, with its state and
 * stack trace. It is rendered when the query has {@code threads} or {@code all}.
 */
@HealthRenderPriority(priority = 50)
public class HealthThreadsResource implements HealthResource {

    /**
     * Creates the resource.
     */
    public HealthThreadsResource() {
        // no state
    }

    @Override
    public Optional<Boolean> isUp() {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "thread-info";
    }

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return Set.of("threads", "all").stream().anyMatch(queryParams::containsKey);
    }

    @Override
    public Object resource() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();

        SortedMap<String, List<ThreadInfo>> threadGroups = new TreeMap<>(Comparator.comparing(String::toString));
        for (Thread thread : threads) {
            String threadGroupName = Optional.ofNullable(thread.getThreadGroup()).map(ThreadGroup::getName).orElse("Not found");
            List<ThreadInfo> threadInfoList = threadGroups.computeIfAbsent(threadGroupName, list -> new ArrayList<>());

            String name = thread.getName();
            Thread.State state = thread.getState();
            int priority = thread.getPriority();
            String type = thread.isDaemon() ? "Daemon" : "Normal";
            List<String> stackTraceList = List.of(thread.getStackTrace()).stream().map(StackTraceElement::toString).toList();
            threadInfoList.add(new ThreadInfo(
                    thread.threadId(),
                    name,
                    state,
                    priority,
                    thread.isAlive(),
                    thread.isInterrupted(),
                    type,
                    stackTraceList));

            threadGroups.put(threadGroupName, threadInfoList);
        }

        return new ThreadStatus(threadGroups.size(), threads.size(), threadGroups);
    }

    /**
     * The threads of the JVM, grouped by thread group.
     */
    @SuppressWarnings("WeakerAccess")
    public static class ThreadStatus {
        /**
         * The number of thread groups.
         */
        @JsonProperty("thread-group-count")
        public final Integer threadGroupCount;
        /**
         * The number of threads.
         */
        @JsonProperty("thread-count")
        public final Integer threadCount;
        /**
         * The threads by thread group name.
         */
        @JsonProperty("threads")
        public final Map<String, List<ThreadInfo>> threadInfoList;

        /**
         * Creates the status.
         *
         * @param threadGroupCount the number of thread groups
         * @param threadCount      the number of threads
         * @param threadInfoMap    the threads by thread group name
         */
        public ThreadStatus(Integer threadGroupCount, Integer threadCount, Map<String, List<ThreadInfo>> threadInfoMap) {
            this.threadGroupCount = threadGroupCount;
            this.threadCount = threadCount;
            this.threadInfoList = threadInfoMap;
        }
    }

    /**
     * One thread.
     */
    @SuppressWarnings("WeakerAccess")
    public static class ThreadInfo {
        /**
         * The thread's id.
         */
        @JsonProperty
        public final Long id;
        /**
         * The thread's name.
         */
        @JsonProperty
        public final String name;
        /**
         * The thread's state.
         */
        @JsonProperty
        public final Thread.State state;
        /**
         * The thread's priority.
         */
        @JsonProperty
        public final Integer priority;
        /**
         * Whether the thread is alive.
         */
        @JsonProperty
        public final Boolean alive;
        /**
         * Whether the thread is interrupted.
         */
        @JsonProperty
        public final Boolean interrupted;
        /**
         * {@code Daemon} for a daemon thread, else {@code Normal}.
         */
        @JsonProperty
        public final String type;
        /**
         * The thread's stack trace, one element per frame.
         */
        @JsonProperty("stackTrace")
        public final List<String> stackTraceElement;

        /**
         * Creates the thread info.
         *
         * @param id                 the thread's id
         * @param name               the thread's name
         * @param state              the thread's state
         * @param priority           the thread's priority
         * @param alive              whether the thread is alive
         * @param interrupted        whether the thread is interrupted
         * @param type               {@code Daemon} or {@code Normal}
         * @param stackTraceElements the thread's stack trace
         */
        public ThreadInfo(Long id, String name, Thread.State state, Integer priority, Boolean alive, Boolean interrupted, String type, List<String> stackTraceElements) {
            this.id = id;
            this.name = name;
            this.state = state;
            this.priority = priority;
            this.alive = alive;
            this.interrupted = interrupted;
            this.type = type;
            this.stackTraceElement = stackTraceElements;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ThreadInfo that = (ThreadInfo) o;
            return Objects.equals(id, that.id) &&
                    state == that.state &&
                    Objects.equals(priority, that.priority) &&
                    Objects.equals(alive, that.alive) &&
                    Objects.equals(interrupted, that.interrupted) &&
                    Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, state, priority, alive, interrupted, type);
        }

        @Override
        public String toString() {
            return "ThreadInfo{" +
                    "id='" + id + '\'' +
                    ", state=" + state +
                    ", priority=" + priority +
                    ", alive=" + alive +
                    ", interrupted=" + interrupted +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
