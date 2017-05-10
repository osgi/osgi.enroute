# Angular JS ${base.version}

## Why AngularJS?

HTML is great for declaring static documents, but it falters when we try to use it for declaring dynamic views in web-applications. AngularJS lets you extend HTML vocabulary for your application. The resulting environment is extraordinarily expressive, readable, and quick to develop.

## Alternatives

Other frameworks deal with HTML’s shortcomings by either abstracting away HTML, CSS, and/or JavaScript or by providing an imperative way for manipulating the DOM. Neither of these address the root problem that HTML was not designed for dynamic views.

## Extensibility

AngularJS is a toolset for building the framework most suited to your application development. It is fully extensible and works well with other libraries. Every feature can be modified or replaced to suit your unique development workflow and feature needs. Read on to find out how.

## Why a Bundle?

This bundle can now be used with the OSGi version management and automatic application composition.

## How to Use

This bundle provides the `osgi.enroute.webresource` capability with the name `/google/angular` and version=${base.version}. If you require this capability then you will automatically get this resource included in your html page when you add:
	
	<link rel="stylesheet" type="text/css"
		href="/osgi.enroute.webresource/\${bsn}/\${Bundle-Version}/*.css">
	<script src="/osgi.enroute.webresource/\${bsn}/\${Bundle-Version}/*.js"></script>

The resources are also mapped to a web path as:

	${subst;${path};static;→}
	

## The Capability

	${Provide-Capability} 

## License(s)

	${Bundle-License}


_${Bundle-Version}_
