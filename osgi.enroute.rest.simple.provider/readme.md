---
title: osgi.enroute.rest.api
layout: service
version: 1.0
summary: Provides REST endpoints that are based on method naming pattern with type safe use of the payload, parameters, and result body.
---

![REST Service](/img/services/osgi.enroute.rest.overview.png)

## Introduction

The OSGi enRoute REST API specifies a service contract for components to provide REST _endpoints_. Representational State Transfer (REST) is an architectural style that allows the interchange of information between elements of a distributed system. A REST endpoint has an HTTPS URI and can thus be accessed from all modern computing environments. An endpoint defines the meaning of the segments of this URI, any specified parameters in the URI, as well as the used HTTP verb (GET, PUT, POST, etc). For example:

        GET /rest/upper/<word>?alphaonly=true

This endpoint is mapped to the URI `/rest/upper` and the next segment specifies the word to translate to upper case. The `alphaonly` is a _parameter_ on the URI, in this case a boolean.

A HTTPS request can specify a _payload_, a payload can be associated with the `POST` or the `PUT` verb. in the response, the HTTPS request can return a _body_.

Since having REST capability is of such major importance for many modern systems, it is crucial that the overhead for the programmer to support this interface be absolutely minimized. This specification leverages the Java type system to provide REST endpoints. It provides a deterministic mapping from a URI request to a REST method name of a restricted set of methods. Adding a method that is named according to a defined pattern is all that is required to add a new REST endpoint.

## HTTP vs. HTTPS

Today with free tools available for generating SSL/TLS certificates (https://letsencrypt.org/), there is no reason *not* to require HTTPS.
Therefore, unless configured otherwise, HTTPS is required for each request. The default return method for an HTTP call is a 404, considering
that `http://example.com/some_uri` and `https://example.com/some_uri` are actually different URIs. This error can be configured. For backwards compatibility, you can allow allow non-SSL requests as well.

## REST Methods

A REST class will make available as a REST endpoint every public method that starts with `get`, `post`, `put`, `option`, or `delete`. The remainder of the method name and the parameters define the remainder of the URI. These methods can be POJO or take a special object providing the context. For example:

        public String getUpper() {
                return "UPPER";
        }
 
The first parameter of a REST method can be be an interface that is or extends the `RESTRequest` interface. This interface provides access to the underlying servlet objects as well as the host name. However, these objects are rarely needed; the primary purpose of this interface is to be extended with methods that have the name of the URI arguments. The interface does not have to be public, it can be a private interface of a class. That is, the previous example would need an interface defined as:

        interface UpperRequest extends RESTRequest {
                String upper();
        }  

        public String getUpper(UpperRequest rq) {
                return rq.upper().toUpperCase();
        }

Any return type can be used for the REST methods that is supported by the DTOs service conversion techniques. 

Any remaining segments in the URI are mapped to the parameters of the method. These method parameters are not required to be strings. Any parameter type that can be converted from a string according to the DTO conversion techniques can be used. The REST method can either have a fixed number of parameters or it can use varargs for a variable number.

        public String getUpper(String lower) {
                return lower.toUpperCase();
        }

The returned body is defined by the method's return type. In general, this type is converted to a JSON file according to the DTO JSON conversion rules. All Java's basic types and all DTO's can be used as return types.

Since REST requests are always copied (they have to move to another process) it is allowed to return original copies; the REST implementation must not modify the returned objects in any way.

Therefore, the previous example can be defined as:

        @Component
        public class UpperApplication implements REST {
        
                interface UpperRequest extends RESTRequest {
                        boolean alphaonly();
                }
                  
                public String getUpper( UpperRequest request, String string ) {
                        return string.toUpperCase();
                }
        }
        
Assuming a default root of `/rest`, this will provide a REST endpoint URI for the earlier example of `GET /rest/upper/<word>?alphaonly=true`.

## Handling OPTIONS

Except for OPTIONS requests, making a call to a non-implemented method on an existing endpoint will return a 405 error.
Making a call on a non-existing endpoint will return a 404. Only OPTIONS requests are handled a little differently by default
(i.e. unless implemented in the REST implementation as described above).

Making an OPTIONS call against a non-existing endpoint will still return a 404, but making it against an existing endpoint will
return a successful call (return value 204 no content) with the "Allow" header set to whichever methods are implemented
for that endpoint in the REST implementation. Any GET method will also return an allowed HEAD value.

Using the above example of the `UpperApplication`:

 * A request to `GET /rest/upper/<word>` will return a 204 with "Allow: OPTIONS, GET, HEAD"
 * A request to `GET /rest/lower/<word>` will return a 404

## Extra Conversions for the Body

Certain return types of the REST method are not mapped to JSON but are treated differently. These are the special conversions: 

* `InputStream` <E2><80><93> Will be copied directly to the output.
* `File` <E2><80><93> The File contents will be copied to the output.
* `byte[]` <E2><80><93> The content of the byte array will be copied to the output
* `null` <E2><80><93> Nothing will be copied to the output. The method could have used the servlet objects to send output for rare cases.
 
If the content type has not been set by the method then the default MIME type will be `application/octet-stream` for these conversions.

## Payloads

`POST` and `PUT` URIs carry a payload from the client to the server. For this specification this payload must be expressed as a JSON body (`application/json;charset=UTF-8`). No other type of bodies are allowed.

The Java type of the payload is defined by the return type of the `_body()` method on the request parameter of the REST method. The incoming JSON payload is mapped to this return type following the JSON DTO conversion rules. 

For example, a system has to handle people, so there is a (we know, simplistic) Person record.

        public class Person extends DTO {
                publci String id;
                public String name;
                public String middle;
                public String surname;
                public int birthYear;
        }

In REST protocols, the `PUT` verb would be used to store a new person. To create the proper endpoint, we can define the following REST method.

        interface PersonRequest extends RESTRequest {
                Person _body();
        }
        
        Person putPerson( PersonRequest request ) {
        
                // authorization
                
                Person p = request._body();
                
                // augment
                // validate
                // persist, set id
                
                return p;
        }
        
## Exceptions

Since the REST methods provide full type safe access to the parameters and remaining URI segments a significant amount of validation is executed by the implementation of this service. These validations will result in the appropriate HTTP error and status code. Implementation should also add explanatory texts to the response.

Regular Java Exceptions are translated to an HTTP error code. The conversions from exception to status code is as follows:

* IllegalStateException <E2><80><93> 400 BAD REQUEST
* SecurityException <E2><80><93> 403 FORBIDDEN
* FileNotFoundException <E2><80><93> 404 NOT FOUND
* NoSuchMethodException <E2><80><93> 405 METHOD NOT ALLOWED
* UnsupportedOperationException <E2><80><93> 501 NOT IMPLEMENTED
* All other exceptions <E2><80><93> 500 SERVER ERROR

Clients can always set the response of the request through the servlet objects that are available on the RESTRequest arguments. However, this should in general be a last resort since most incompatibilities are caused by the sometimes really subtle interpretations of these error codes. In general it is best to try to make requests binary: succeed when all goes OK and fail in all other cases.

## Example

There is an example project in [the OSGi enRoute examples repository][1]. This project implements the different GET, POST, PUT, and DELETE methods and shows how to get the arguments from the path and the query parameters.

<footer>This example was donated by Chuck Boecking</footer> 

## Discussion

*   **Couldn't we just annotate a service with a service property and then have it automatically extended regardless the service interface?** 

The issue is that you cannot just convert a service since a service in general has NO defenses against attacks. I think it is quite crucial to realize that any method in that class is open for external calls from anywhere in the world. The work you need to do to make this work secure dwarfs the fact that you must implement an interface <E2><80><A6> Not just on the security, I find that the way you create this facade is often without any real functionality, it is often just an orchestrator. It checks the authority and then calls other services, potentially with the current user as parameters. In this model the JSONRPC and REST services are just securing and orchestrating. This allows the other services to be very cohesive since they know they do not have to worry about security and get the user as parameter instead of having to link in the current user model.

## CORS

CORS support is provided for all endpoints. It is configured via the `@CORS` annotation. The REST implementation class can be annotated (in which case the configuration applies to all methods in that class), or each individual method can annotated. In the case that both are annotated, the method annotation takes priority. If no annotation exists, then CORS is disabled for that method call.

For public APIs, the simplest approach is to annotate the REST implementation class with a static
configuration (i.e. `Allow: *`).

For private APIs, it should usually be sufficient to provide a static configuration either on the
class level, or in a more fine-grained matter on the method level (i.e. `Allow: https://example.com, https://example.org`).

For more demanding cases, is possible to provide a dynamic configuration. This is done by implementing
a method (like "allowOrigin()") in the REST implementation. The method will be invoked as necessary, thereby permitting the dynamic resolution on a request-by-request basis. See the CORS API 
javadoc for details about how to implement these dynamic methods.

## Namespaces

In larger systems, you may want to reuse a REST implementation from a different base URI. To accomplish this, it is possible to register a REST implementation with a namespace. If two REST methods share the same name and are in the same namespace, then the highest-ranking method will win (accoring to the `service.ranking` configuration when the service is registered). If, however, they are registered in different namespaces, then they can co-exist. The namespace is determine via a `UriMapper` (see next section).

The default namespace is the empty string "".

## URI Mappers

REST endpoints are served from a base URI. For instance, if the base is `/this/is/the/base/`, then the `getUpper` method above would be located at the endpoint `/this/is/the/base/upper/<word>?alphaonly=true`.

To register a base URI, you can register a `UriMapper` with the `osgi.http.whiteboard.servlet.pattern` property
set to the servlet pattern for the base URI. Behind the scenes, the REST implementation will instantiate a servlet to listen to requests that match this pattern. The algorithm provided in the `namespaceFor` method of the `UriMapper` will determine which namespace to use. Return `null` will cause the system to consecutively try the next Mapper, and finally just use the default namespace. 

To add namespace support to the default namespace, simply register a `UriMapper` with the default REST servlet pattern, i.e. `/rest/*`.


## References

[1]: https://github.com/osgi/osgi.enroute.examples/tree/master/osgi.enroute.examples.rest.application
