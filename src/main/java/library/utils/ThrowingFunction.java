package library.utils;

import java.io.Serializable;

/**
 * A functional interface that represents a function which may throw an exception.
 *
 * <p>This interface is useful when you need to pass a lambda or method reference that can throw
 * checked exceptions, for example in stream operations or other APIs that only accept the
 * standard {@link java.util.function.Function} type.</p>
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface ThrowingFunction<T, R> extends Serializable {
	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 * @throws Exception if the application fails for any reason
	 */
	R apply(T t) throws Exception;
}
