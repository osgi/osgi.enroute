# OSGi enRoute Twitter Bootstrap

## Problem

You want to use [Twitter Bootstrap][1] version ${base.version} in your OSGi application.

## Description

'Sleek, intuitive, and powerful mobile first front-end framework for faster and easier web development.' Ok,
that is the text on their web site. This bundle wraps all the components of a standard Bootstrap distribution. 


## How to Use

This bundle provides the `osgi.enroute.webresource` capability with the name `/twitter/bootstrap` and version=${base.version}. If you require this capability then you will automatically get this resource included in your html page when you add:
	
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

[1]: http://getbootstrap.com
