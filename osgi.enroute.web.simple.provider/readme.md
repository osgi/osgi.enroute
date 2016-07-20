# OSGI ENROUTE WEB SIMPLE PROVIDER

## Bundle Description

${Bundle-Description}

## Overview

The enRoute WebServer helps you to develop web applications in an OSGi framework.
In essence, a web application is simply a means of organizing your code in order to
serve html, css, and javascript files. In the end, it all boils down to serving
static resources. Using OSGi as the web framework can make this task surprisingly simple. 

The enRoute WebServer allows you to serve these static resources from a bundle, and
use all the benfits of OSGi for the organization and deployment of your code.

You can organize your code in any of the following ways:

 * As a "private" or "segregated" collection of resources
 * As a collection of resources that can be mixed or mashed
 * As a sharable "WebResource" that is deployed to multiple applications
 * As any combination of the above
 
Additionally, the configuration options of the WebServer make it a very flexible
environment to work with.

If you have experience working with OSGi, or if you would like to benefit from how
OSGi can help you organize your code, then the WebServer could be a very good choice.

## Important concepts

<dl>
  <dt>application</dt>
  <dd>See: web application</dd>

  <dt>web application</dt>
  <dd>a means of organizing your code in order to serve resources such as html, css, and javascript</dd>

  <dt>WebResource</dt>
  <dd>a resource or resources delivered from a bundle that is controlled through requirements and capabilities</dd>
</dl>

## Code organization

### Segregated content

Sometimes you may want to deploy your web application as a single, self-contained unit.
In this case, it is possible to put your static resources (and possibly java code)
together into a bundle, and use the bundle as the deployable unit.

This is accomplished by shipping your code in the `/static/[BSN]` folder of the
bundle. The WebServer will validate that *only* the bundle with the actual Bundle-SymbolicName (BSN) of
the folder can serve the files, and will make the files available on the URL path:
`/bnd/[BSN]/`

For example, given a bundle `osgi.enroute.web.example`, and a file `index.txt` 
with the contents "Hello, World!", you would ensure that you ship the file in this directory
relative to the root path of your bundle:

   /static/osgi.enroute.web.example/index.txt

When you access, for instance, `http://localhost:8080/bnd/osgi.enroute.web.example/index.txt`
you would be served the content "Hello, World!".

Of course, the URL starting with `/bnd/osgi.enroute.web.example` is not very friendly, so you would
either use a front-end proxy (such as ngnix) or a `ConditionalServlet` (see below) at the front end
to accept more friendly URLs, and then forward to the correct internal path.

The `BundleFileServer` tracks all bundles having a `/static/[BSN]` folder, and serves these 
resources via http(s) on the `/bnd/[BSN]` path. Any bundle that contains such a folder is included
in the list of segregated content web application bundles.

Note that the BundleFileServer is unforgiving about paths. It will not append "/" to a directory
or "index.html" to a path ending in "/". The exact matching path is required. If you want redirection,
you should use the `RedirectServer`.

### Mixin content

The principle of mixin content is similar to the segregated content described above, but the WebServer
does not ensure content segregation. This means that instead of serving a self-contained application
from a single bundle, it is possible to mix content from different bundles together in a composed
application. As above, the static files are shipped in the `/static` directory, but this time,
instead of using a directory corresponding to the BSN of the bundle, you use virtually any other
name that is appropriate to your application.

For example, given two bundles com.acme.foo and com.acme.bar:

  In com.acme.foo:
    /static/myapp/foo.html
    
  In com.acme.bar:
    /static/myapp/bar.html

Accessing `http://localhost:8080/myapp/foo.html` will display the contents of foo.html from `com.acme.foo`,
and `http://localhost:8080/myapp/foo.html` will display the contents of bar.html from com.acme.bar.

Note that using the mixin mechanism, it is also possible to access files from a segregated content bundle
via the BSN path (i.e. without the `/bnd` part): `http://localhost:8080/osgi.enroute.web.example/index.txt`.
This is provided for convenience and backwards-compatibility.

The `BundleMixinServer` tracks all bundles having a `/static/` folder (which includes those bundles
tracked by the BundleFileServer), and serves these resources via http on the `/bnd/` path.
Any bundle that contains such a folder is included in the list of mixin content web application bundles.

Note: since the resources are mixed together, you need to be careful about how you organize them.
See the "Configuration" section below for more information.


### WebResources

WebResources are quite different from the web applications described above.

Whereas the application is generally intended to provide a complete and coherent unit of deployment,
the WebResource is intended to be used as building blocks for your applications.

Oftentimes, applications will leverage a CDN to serve resources, such as javascript libraries. However, using
a CDN is by no means a panacea. There are certain problems that come with this approach. Using WebResources
allows you to manage resources as if you had your own local CDN.

The WebResource functionality allows you to do this using the requirement/capabilities
model in OSGi. It takes a bit of effort to set up a WebResource, but once you do, using it becomes as simple
as adding an annotation to the requiring bundle.

More information is available [here](https://github.com/osgi/design/blob/master/rfps/rfp-0171-Web-Resources.pdf?raw=true).


## Features

### Application indexing

Resources in web applications are generally accessed from hard-to-remember paths, since they are based
on the BSN. To solve this problem, the WebServer provides an application indexer. The indexer is available
from the root path, and lists all applications that have been declared as being "enRoute Applications".

To tag an application, put an "EnRoute-Application" header entry in your bundle's manifest file, with a
comma-separated list corresponding to the name(s) of your application(s).

```
EnRoute-Application: com.acme.foo, com.acme.bar
```

### File caching

Web servers are in an excellent position to optimize the traffic. They have access to the static resources
in the bundles and can easily cache and pre-zip the resources on the file system. They should support 
debug and production modes to handle caching.

The WebServer supports caching, compression, ranges, ETags, and If* headers to optimize the use of bandwidth.

### Error handling

For Segregated Content bundles, it is possible to provide a static error page for 404 errors. For any other usage, you should register a servlet with the `osgi.http.whiteboard.servlet.errorPage` property in the usual way. (See 140.4.1 of the Compendium.)


## Configuration

### Handling "/"

If you need access to the root path ("/"), there are two ways to accomplish this.

You could use your own servlet to override how requests on the root path are handled. If you do so,
you will lose all the functionality provided by the `ConditionalServlet`s (see below), but you can
still benefit from Segregated content and WebResources, since they are served from their own
servlets.

The other way to inject functionality into the root is to use `ConditionalServlet`s. Please see below
for more details.

### Redirects

By default, if a path ends with "/" (other than the empty path: see Application indexing above),
then the WebServer will append "index.html" and send a 302 redirect.

If you want to change the default, you have two options. Either you could configure the value of
the redirect to use something else (including the empty string, which in effect disables redirection),
or you could override the functionality by using your own redirect `ConditionalServlet`.

For more information about redirects, see `ConditionalServlet`s below.

### Handling folders

Segregated content requires an exact path match, and folders will cause a 404 to be returned.

Mixin content required a design decision. What does it mean to serve a "folder"? How should that be
accomplished? Since there is no easy or obvious answer, we decided instead that it should serve a 404.
If you have special needs, you always have the option of overriding the behaviour of the
`BundleMixinServer` using a `ConditionalServlet`.

### `ConditionalServlet`s

The problem with the special root ("/") path is that it is really coveted real estate. If you are only serving
a single application from root, there is no problem, but if you have multiple functions on a server
such as multiple applications or websites serving different domain names, you are in trouble.

`ConditionalServlet`s solve this problem by giving multiple servlet-like classes the chance (in order of
their `service.ranking`) to "try out" the request and decide either to handle it, or pass it off to somebody
else to handle.

`ConditionalServlet`s are controlled by the `DispatchServlet`, and the algorithm looks like this:

```java

	public void service(HttpServletRequest rq, HttpServletResponse rsp) throws ServletException, IOException {

		for (ConditionalServlet cs : targets) {

			try {
				if (isBlacklisted(cs))
					continue;

				if (cs.doConditionalService(rq, rsp)) {
					return;
				}
			}
			catch (Exception e) {
				// throw an Exception and blacklist the bad servlet
			}
		}

		// No ConditionalServlets were found. Since we don't know what to do, we return a 404.
		rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
```

The default list of `ConditionalServlet`s is, in order:

 * `RedirectServlet` (`servlet.ranking=1000`) - this servlet watches for a path that ends with "/".
      Any path that ends with a slash, other than the root path, will get appended by the value
      of `redirect` (default is "index.html").
 * `EnrouteApplicationIndexServer` (`service.ranking=1001`) - as described above in "Application indexing",
      this servlet tracks all `Enroute-Application`s and serves the root "/index.html".
 * `BundleMixinServer` (`service.ranking=1002`) - this servlet tracks all bundles that contain a
      `/static` folder, and serves the content (as described above in "Mixin content").

By adding, removing, and reordering `ConditionalServlet`s, you can gain unlimited control of what happens
on the root path.

### Configuration parameters

	boolean debug - use debug mode (default is "false")

	int expires - value to set the "Expires" http header for a cached file.
	              If this value is not set, the header will not be used.

	long expiration - value (in ms) to set the "Cache-Control: max-age=" http header for a cached file.
	              If this file is not set, a default of 120000 is used.

	boolean noproxy - ????
