package io.descoped.dc.api.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates objects whose constructor parameters are services.
 */
public class ObjectCreator {

    private ObjectCreator() {
    }

    /**
     * Creates an instance with the first public constructor whose parameter types are all registered services, or
     * with the declared no-argument constructor if the class has no public constructor.
     *
     * @param clazz    the class
     * @param services the services passed to the constructor
     * @param <T>      the class's type
     * @return the instance
     * @throws IllegalStateException if no constructor fits or the constructor fails
     */
    @SuppressWarnings("unchecked") // the constructor is the class's own
    public static <T> T newInstance(Class<T> clazz, Services services) {
        List<Constructor<?>> constructors = new ArrayList<>(List.of(clazz.getConstructors()));

        // add default constructor unless declared
        if (constructors.isEmpty()) {
            constructors.add(getDefaultConstructor(clazz));
        }

        for (Constructor<?> constructor : constructors) {
            Deque<Object> actualParams = new LinkedList<>();

            Class<?>[] paramTypes = constructor.getParameterTypes();
            boolean determined = true;
            for (Class<?> paramType : paramTypes) {
                if (!services.contains(paramType)) {
                    determined = false;
                    break;
                }
                actualParams.add(services.get(paramType));
            }

            determined = determined && (paramTypes.length == actualParams.size());

            if (determined) {
                try {
                    return (T) constructor.newInstance(actualParams.toArray(new Object[actualParams.size()]));

                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        throw new IllegalStateException("Error creating class: " + clazz.getName() + ". Please check class visibility!");
    }

    private static <T> Constructor<T> getDefaultConstructor(Class<T> clazz) {
        try {
            Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            return declaredConstructor;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
