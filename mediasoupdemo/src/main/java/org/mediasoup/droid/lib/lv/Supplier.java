package org.mediasoup.droid.lib.lv;

/**
 * Compat version of {@link java.util.function.Supplier}
 * @param <T> the type of the input to the operation
 */
public interface Supplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get();
}
