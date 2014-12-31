# OSGI ENROUTE EQUINOX LOG ADAPTER

${Bundle-Description}

## How it Works

This bundle is a bnd _Embedded Activator_. If it is added to the test or run path then the bnd launcher will pick it up and execute the `BundleActivator.start()` method with the System Bundle context. This is an _IMMEDIATE_ component which will run it before any normal bundles are started, the order between the embedded activators is undefined. In the start method a check is done to see if there is a Log Service (or that even the package for this service is available). If so, it is assumed to be registered by the framework and thus Equinox. It then uses the service hooks to hide this service from anybody else. However, it sees the Log Reader Service itself and will register as a listener. Each log record will then be forwarded to all registered Log Services.

## Example

You can add the Equinox log adapter to the `-runpath` or create a new property that bnd will append:

	-runpath.eqnx = osgi.enroute.equinox.log.adapter

The bundle is harmless if the bundle is not Equinox so you can add this to `build.bnd`. The enRoute project already sets this bundle.