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

package org.osgi.service.async.delegate;

import java.lang.reflect.Method;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.util.promise.Promise;

/**
 * This interface is used by services to allow them to optimize Asynchronous calls where they are capable
 * of executing more efficiently.
 * 
 * This may mean that the service has access to its own thread pool, or that it can delegate work to a remote 
 * node, or act in some other way to reduce the load on the Asynchronous Services implementation when making
 * an asynchronous call.
 */
@ConsumerType
public interface AsyncDelegate {
	/**
	 * This method can be used by clients, or the Async service, to optimize
	 * Asynchronous execution of methods.
	 * 
	 * When called, the {@link AsyncDelegate} should execute the supplied method
	 * using the supplied arguments asynchronously, returning a promise that can
	 * be used to access the result.
	 * 
	 * If the method cannot be executed asynchronously by the delegate then it
	 * should return <code>null</code>.
	 * 
	 * @param m the method that should be asynchronously executed
	 * @param args the arguments that should be used to invoke the method
	 * 
	 * @return A promise representing the asynchronous result, or
	 *         <code>null</code> if this method cannot be asynchronously invoked
	 *         by the AsyncDelegate.
	 * @throws Exception An exception should be thrown only if there was an
	 *         serious error that prevented the asynchronous call from starting,
	 *         for example the supplied method does not exist on this object.
	 *         Exceptions should not be thrown to indicate that the call does
	 *         not support asynchronous invocation, instead the AsyncDelegate
	 *         should return
	 *         <code>null<code>. Exceptions should also not be thrown to
	 *         indicate a failure from the execution of the underlying method, 
	 *         this should be handled by failing the returned promise.
	 */
	Promise<?> async(Method m, Object[] args) throws Exception;

	/**
	 * This method can be used by clients, or the Async service, to optimize
	 * Asynchronous execution of methods.
	 * 
	 * When called, the {@link AsyncDelegate} should execute the supplied method
	 * using the supplied arguments asynchronously. This method differs from
	 * {@link #async(Method, Object[])} in that it does not return a promise.
	 * This method therefore allows the implementation to perform more
	 * aggressive optimisations because the end result of the invocation does
	 * not need to be returned to the client.
	 * 
	 * If the method cannot be executed asynchronously by the delegate then it
	 * should return <code>false</code>.
	 * 
	 * @param m the method that should be asynchronously executed
	 * @param args the arguments that should be used to invoke the method
	 * 
	 * @return <code>true<code> if the asynchronous execution request has
	 *         been accepted, or <code>false</code> if this method cannot be
	 *         asynchronously invoked by the AsyncDelegate.
	 * @throws Exception An exception should be thrown only if there was an
	 *         serious error that prevented the asynchronous call from starting,
	 *         for example the supplied method does not exist on this object.
	 *         Exceptions should not be thrown to indicate that the call does
	 *         not support asynchronous invocation, instead the AsyncDelegate
	 *         should return
	 *         <code>false<code>. Exceptions should also not be thrown to
	 *         indicate a failure from the execution of the underlying method.
	 */
	boolean execute(Method m, Object[] args) throws Exception;
}
