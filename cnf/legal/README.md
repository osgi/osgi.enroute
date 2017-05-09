# OSGi enRoute Base Profile

The OSGi enRoute project provides a programming model of OSGi applications. The OSGi specifications provide a powerful and solid platform for component oriented programming but by their nature lack ease of use, especially for newcomers to get started.

This is a bundle providing the API for the OSGi enRoute base profile. The base profile establishes a runtime that contains a minimal set of services that can be used as a base for applications.

The `.api` bundle contains the full API of the base profile. It should be used to compile against. This bundle should **never** be installed in a runtime since it falsely indicates that it provides the base profile capabilities. The `.guard` bundle requires all the capabilities provided in the `.api` bundle. If installed, it will only resolve in the case that all promised capabilities are actually provided.

----
${bsn} v${Bundle-Version} 
