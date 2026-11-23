package io.descoped.dc.api.services;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class ObjectCreator {

    private ObjectCreator() {
    }

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
