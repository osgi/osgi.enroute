/*
 * Copyright (c) OSGi Alliance (2014). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.util.promise;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Success callback for a Promise.
 * 
 * <p>
 * A Success callback is registered with a {@link Promise} using the
 * {@link Promise#then(Success)} method and is called if the Promise is resolved
 * successfully.
 * 
 * <p>
 * This is a functional interface and can be used as the assignment target for a
 * lambda expression or method reference.
 * 
 * @param <T> The value type of the resolved Promise passed as input to this
 *        callback.
 * @param <R> The value type of the returned Promise from this callback.
 * 
 * @ThreadSafe
 * @author $Id: c29fc4fb6df284dd08c4a18e231fe626e820ffab $
 */
@ConsumerType
public interface Success<T, R> {
	/**
	 * Success callback for a Promise.
	 * 
	 * <p>
	 * This method is called if the Promise with which it is registered resolves
	 * successfully.
	 * 
	 * <p>
	 * In the remainder of this description we will refer to the Promise
	 * returned by this method as the returned Promise and the Promise returned
	 * by {@link Promise#then(Success)} when this Success callback was
	 * registered as the chained Promise.
	 * 
	 * <p>
	 * If the returned Promise is {@code null} then the chained Promise must
	 * resolve immediately with a successful value of {@code null}. If the
	 * returned Promise is not {@code null} then the chained Promise must be
	 * resolved when the returned Promise is resolved.
	 * 
	 * @param resolved The successfully resolved {@link Promise}.
	 * @return The Promise to use to resolve the chained Promise, or
	 *         {@code null} if the chained Promise is to be resolved immediately
	 *         with the value {@code null}.
	 * @throws Exception The chained Promise must be failed with the thrown
	 *         exception.
	 */
	Promise<R> call(Promise<T> resolved) throws Exception;
}
