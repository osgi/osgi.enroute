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
 * As a sharable resource that is deployed to multiple applications
 * As a plug-in (<<Need more info>>)
 * As any combination of the above
 
Additionally, the configuration options of the WebServer make it a very flexible
environment to work with.

If you have experience working with OSGi, or if you would like to benefit from how
OSGi can help you organize your code, then the WebServer could be a very good choice.

## Code organization

### Segregated bundles

Sometimes you may want to deploy your web application as a single, segregated unit.
In this case, it is possible to put your static resources (and possibly java code)
together into a bundle, and use the bundle as the deployable unit.

This is accomplished by shipping your code in the `/static/[PID]` folder of the
bundle. The WebServer will validate that *only* the bundle with the actual PID of
the folder can serve the files, and will make the files available on the URL path:
`/bnd/[PID]/`

For example, given a bundle `osgi.enroute.web.example`, and a file `index.txt` 
with the contents "Hello, World!", you would ensure that you ship the file in this directory:

   /static/osgi.enroute.web.example/index.txt

When you access, for instance, `http://localhost:8080/bnd/osgi.enroute.web.example/index.txt`
you would be served the content "Hello, World!".

Of course, the URL starting with `/bnd/osgi.enroute.web.example` is not very friendly, so you would
either use a front-end proxy (such as ngnix) or a `ConditionalServlet` (see below) at the front end
to accept more frienly URLs, and then forward to the correct internal path.

The `BundleFileServer` tracks all bundles having a `/static/[PID]` folder, and serves these 
resources via http on the `/bnd/[PID]` path.

### Mixins

The principle of mixins is similar to the segregated bundles described above, but the WebServer
does not ensure content segregation. This means that instead of serving a self-contained application
from a single bundle, it is possible to mix content from different bundles together in a composed
application. As above, the static files are shipped in the `/static` directory, but this time,
instead of using a directory corresponding to the PID of the bundle, you use virtually any other
name that is appropriate to your application.

For example, given two bundles com.acme.foo and com.acme.bar:

  In com.acme.foo:
    /static/myapp/foo.html
    
  In com.acme.bar:
    /static/myapp/bar.html

Accessing `http://localhost:8080/myapp/foo.html` will display the contents of foo.html from `com.acme.foo`,
and `http://localhost:8080/myapp/foo.html` will display the contents of bar.html from com.acme.bar.

Note that using the mixin mechanism, it is also possible to access files from a segregated bundle
via the PID path (without the `/bnd` part): `http://localhost:8080/osgi.enroute.web.example/index.txt`.
This is provided for convenience and backwards-compatibility.

The `BundleMixinServer` tracks all bundles having a `/static/` folder (which includes those bundles
tracked by the BundleFileServer), and serves these resources via http on the `/bnd/` path.

Since the resources are mixed together, you need to be careful about how you organize them.
See the "Configuration" section below for more information.


### WebResources

<<I still need to improve my understanding of this>>

### Plug-in contributions

<<I still need to improve my understanding of this>>

### Combinations

[TBD]

## Configuration

### Handling "/"

### Handling folders

### `ConditionalServlet`s
 
## Features

### File caching

