<h1><img src="http://enroute.osgi.org/img/enroute-logo-64.png" witdh=40px style="float:left;margin: 0 1em 1em 0;width:40px">
OSGi enRoute</h1>

The [OSGi enRoute][enroute] project provides a programming model of OSGi applications using [Bndtools][1]. The OSGi specifications provide a powerful and solid platform for component oriented programming but by their nature lack ease of use, especially for newcomers to get started. Using Bndtools, an Eclipse plugin, and v2Archive OSGi enRoute makes it really easy to get started.

This repository contains bundles providing the API for the OSGi enRoute base profile
the bundles that had to be developed for OSGi enRoute because such bundles did not exist in any open source project.
The base profile establishes a runtime that contains a minimal set of services that can be used as a base for applications.
These bundles implement services defined in the [OSGi enRoute APIs][2] and/or provide common functions. 

## Contributing

Want to hack on osgi.enroute? See [CONTRIBUTING.md](CONTRIBUTING.md) for information on building, testing and contributing changes.

They are probably not perfect, please let us know if anything feels
wrong or incomplete.

## License

The contents of this repository are made available to the public under the terms of the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
Bundles may depend on non Apache Licensed code.

[enroute]: http://v2archive.enroute.osgi.org
[1]: http://bndtools.org
[2]: https://github.com/osgi/v2archive.osgi.enroute/tree/master/osgi.enroute.base.api/src/osgi/enroute
