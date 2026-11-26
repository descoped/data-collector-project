package io.descoped.dc.api.context;

import io.descoped.dc.api.services.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * The context of an execution: the services, the global state shared by a whole run, the variables that
 * expressions read, and the state that nodes pass on. A lookup of state falls back to the global state.
 */
public class ExecutionContext {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionContext.class);

    final Services services;
    final Map<Object, Object> globalState;
    final Map<String, Object> variables;
    final Map<Object, Object> state;

    private ExecutionContext(Services services, Map<Object, Object> globalState, Map<String, Object> variables, Map<Object, Object> state) {
        this.globalState = globalState;
        this.services = services;
        this.variables = variables;
        this.state = state;
    }

    /**
     * A context with new services, global state, variables and state.
     *
     * @return the context
     */
    public static ExecutionContext empty() {
        return new Builder().build();
    }

    /**
     * A copy of a context: it shares the services and the global state, and copies the variables and the state.
     *
     * @param input the context
     * @return the copy
     */
    public static ExecutionContext of(ExecutionContext input) {
        return new Builder().of(input).build();
    }

    /**
     * Adds the variables, global state and state of another context that this one lacks; on a key in both, this
     * context's value is kept. The other context's global state receives this context's global state as well.
     *
     * @param anotherContext the other context
     * @return this context
     */
    public ExecutionContext join(ExecutionContext anotherContext) {
        ExecutionContext copyOfThis = of(this);
        ExecutionContext accumulated = of(anotherContext);
        accumulated.variables.putAll(copyOfThis.variables);
        accumulated.globalState.putAll(copyOfThis.globalState);
        accumulated.state.putAll(copyOfThis.state);
        this.variables.putAll(accumulated.variables);
        this.globalState.putAll(accumulated.globalState);
        this.state.putAll(accumulated.state);
        return this;
    }

    /**
     * The services.
     *
     * @return the services
     */
    public Services services() {
        Objects.requireNonNull(services);
        return services;
    }

    /**
     * Sets a global state value.
     *
     * @param key   the key
     * @param value the value
     * @return this context
     */
    public ExecutionContext globalState(Object key, Object value) {
        globalState.put(key, value);
        return this;
    }

    /**
     * The global state value of a key, set from the supplier if absent.
     *
     * @param key      the key
     * @param supplier gives the value if the key is absent
     * @param <R>      the value's type
     * @return the value
     */
    @SuppressWarnings("unchecked") // the caller names the value's type
    public <R> R globalStateIfAbsent(Object key, Supplier<R> supplier) {
        return (R) globalState.computeIfAbsent(key, k -> supplier.get());
    }

    /**
     * The variables.
     *
     * @return the variables, not a copy
     */
    public Map<String, Object> variables() {
        return variables;
    }

    /**
     * The value of a variable.
     *
     * @param key the variable name
     * @return the value, or {@code null} if absent
     */
    public Object variable(String key) {
        return variables.get(key);
    }

    /**
     * Sets a variable.
     *
     * @param key   the variable name
     * @param value the value
     * @return this context
     */
    public ExecutionContext variable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    /**
     * Sets a state value.
     *
     * @param key   the key
     * @param value the value
     * @return this context
     */
    public ExecutionContext state(Object key, Object value) {
        state.put(key, value);
        return this;
    }

    /**
     * The state value of a class key, or else the global state value.
     *
     * @param key the key
     * @param <R> the value's type
     * @return the value, or {@code null} if absent from both
     */
    @SuppressWarnings("unchecked") // the caller names the state's type
    public <R> R state(Class<R> key) {
        return (R) state((Object) key);
    }

    /**
     * The state value of a key, or else the global state value.
     *
     * @param key the key
     * @param <R> the value's type
     * @return the value, or {@code null} if absent from both
     */
    @SuppressWarnings("unchecked") // the caller names the state's type
    public <R> R state(Object key) {
        R r = (R) state.get(key);
        if (r == null) {
            r = (R) globalState.get(key);
        }
        return r;
    }

    /**
     * Removes a state value; logs an error if it is absent.
     *
     * @param key the key
     */
    public void releaseState(Object key) {
        if (state.remove(key) == null) {
            LOG.error("unable to release state key={}", key);
        }
    }

    /**
     * Puts the state and the variables of another context into this one, replacing values of the same key.
     *
     * @param context the other context
     * @return this context
     */
    public ExecutionContext merge(ExecutionContext context) {
        this.state.putAll(context.state);
        this.variables.putAll(context.variables);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionContext context = (ExecutionContext) o;
        return Objects.equals(globalState, context.globalState) &&
                Objects.equals(variables, context.variables) &&
                Objects.equals(state, context.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(globalState, variables, state);
    }

    @Override
    public String toString() {
        return "ExecutionInput{" +
                "globalState=" + globalState +
                ", state=" + state +
                ", variables=" + variables +
                '}';
    }

    /**
     * A builder of an {@link ExecutionContext}. What is not set is created empty, and the services are created
     * with {@link Services#create()}.
     */
    public static class Builder {

        private Services services;
        private Map<Object, Object> globalState;
        private Map<String, Object> variables;
        private Map<Object, Object> state;

        /**
         * Creates an empty builder.
         */
        public Builder() {
            // the state is in the fields
        }

        /**
         * Sets the services.
         *
         * @param services the services
         * @return this builder
         */
        public Builder services(Services services) {
            this.services = services;
            return this;
        }

        /**
         * Shares the services of a context.
         *
         * @param input the context
         * @return this builder
         */
        public Builder services(ExecutionContext input) {
            this.services = input.services;
            return this;
        }

        /**
         * Sets the global state.
         *
         * @param globalState the global state; kept, not copied
         * @return this builder
         */
        public Builder globalState(Map<Object, Object> globalState) {
            this.globalState = globalState;
            return this;
        }

        /**
         * Shares the global state of a context.
         *
         * @param input the context
         * @return this builder
         */
        public Builder globalState(ExecutionContext input) {
            this.globalState = input.globalState;
            return this;
        }

        /**
         * Sets the variables.
         *
         * @param variables the variables; kept, not copied
         * @return this builder
         */
        public Builder variables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        /**
         * Shares the variables of a context.
         *
         * @param input the context
         * @return this builder
         */
        public Builder variables(ExecutionContext input) {
            this.variables = input.variables;
            return this;
        }

        /**
         * Sets the state.
         *
         * @param state the state; kept, not copied
         * @return this builder
         */
        public Builder state(Map<Object, Object> state) {
            this.state = state;
            return this;
        }

        /**
         * Shares the state of a context.
         *
         * @param input the context
         * @return this builder
         */
        public Builder state(ExecutionContext input) {
            this.state = input.state;
            return this;
        }

        /**
         * Shares the services and the global state of a context, and copies its variables and state into this
         * builder's.
         *
         * @param input the context
         * @return this builder
         */
        public Builder of(ExecutionContext input) {
            this.services = input.services;
            this.globalState = input.globalState;
            if (this.variables == null) {
                this.variables = new LinkedHashMap<>();
            }
            this.variables.putAll(input.variables);
            if (this.state == null) {
                this.state = new LinkedHashMap<>();
            }
            this.state.putAll(input.state);
            return this;
        }

        /**
         * Builds the context.
         *
         * @return the context
         */
        public ExecutionContext build() {
            return new ExecutionContext(
                    ofNullable(services).orElseGet(Services::create),
                    ofNullable(globalState).orElseGet(ConcurrentHashMap::new),
                    ofNullable(variables).orElseGet(LinkedHashMap::new),
                    ofNullable(state).orElseGet(LinkedHashMap::new)
            );
        }
    }
}
