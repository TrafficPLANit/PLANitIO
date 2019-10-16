package org.planit.xml.util;

import org.planit.exceptions.PlanItException;

/**
 * Function Interface which can process three input objects.
 * 
 * Used to register initial costs in test cases.  
 * 
 * There is no equivalent functional interface in the java.util.function library so we have created this one.
 * 
 * @author gman6028
 *
 * @param <T>  first object to be processed
 * @param <U> second object to be processed
 * @param <V> third object to be processed
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

/**
 * Performs this operation on the given argument
 * 
 * @param t first object to be processed
 * @param u second object to be processed
 * @param v third object to be processed
 * @throws PlanItException thrown if there is an error
 */
	public void accept(T t, U u, V v) throws PlanItException;
}
