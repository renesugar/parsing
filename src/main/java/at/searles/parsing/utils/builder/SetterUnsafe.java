package at.searles.parsing.utils.builder;

import at.searles.parsing.Environment;
import at.searles.parsing.Fold;
import at.searles.parsing.ParserStream;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * SetterUnsafe does not require the input and parameter type.
 * They are fetched by reflection. This is not possible
 * or at least significantly more difficult for Apply and BuilderInitializer
 * because they need to fetch methods based on parameters. In this
 * case inheritance is an issue.
 * @param <T>
 * @param <V>
 */
public class SetterUnsafe<T, V> implements Fold<T, V, T> {

    private final String property;

    public SetterUnsafe(String property) {
        this.property = property;
    }

    @Override
    public T apply(Environment env, T left, V right, ParserStream stream) {
        try {
            Class<T> type = (Class<T>) left.getClass();
            Field field = type.getField(property);
            Method copyMethod = type.getMethod("copy");

            // clone to allow backtracking
            T copy = (T) copyMethod.invoke(left);
            field.set(copy, right);
            return copy;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException | NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public T leftInverse(Environment env, T result) {
        try {
            Class<T> type = (Class<T>) result.getClass();
            Field field = type.getField(property);

            Object value = field.get(result);

            if(value == null) {
                return null;
            }

            Method copyMethod = type.getMethod("copy");

            T copy = (T) copyMethod.invoke(result);

            field.set(copy, null);

            return copy;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException | NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public V rightInverse(Environment env, T result) {
        try {
            Class<T> type = (Class<T>) result.getClass();
            Field field = type.getField(property);

            Object value = field.get(result);

            if(value == null) {
                return null;
            }

            return (V) value;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toString() {
        return "{set " + property + "}";
    }
}