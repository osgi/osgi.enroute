# OSGI ENROUTE CONFIGURER SIMPLE PROVIDER

## Problem

You need to configure the bundles in a running framework without requiring the deployer to do this manually. It should be possible to update configurations using the same mechanism as a bundle.

## Description

This bundle is an extender to create Configuration Admin records from an embedded JSON file. The JSON file must be in the bundle at `/configuration/configuration.json`. This directory can also contain (binary) files with the @{resource:} macro.

Additionally, you can set a `enRoute.configurer.extra` as System property (In your bndrun file you can use `-runproperties=enRoute.configurer.extra='[{...}]'`). This property is read as if it was a file. This is simpler to use in test environments.

## Factory Configurations
The PID of a factory configuration is normally chosen by the Configuration Admin when a new instance is created. This would create problems if a bundle was updated multiple times, each time a new instance would be added. It is therefore necessary to choose a logical service.pid. If the logical PID has never been used it will create a new instance, otherwise it will update the existing instance when the fields in the Configuration record differ.

The Configurer knows what instance is related to a logical PID by storing the logical PID as an extra property in the Configuration record; the name of this property is aQute.pid.

## Resources
A special problem in configuration data is handling binary files, for example certificates. In general these binary files need to be available from the file system to be used by other units of the system. A logistic problem is now, where are those files stored, and who will manage these files?

The Configurer can handle this problem through the @{resource:<file>} macro. This macro expects a resource in its bundle at . The parameter should be a path inside the configuration directory in the bundle. That is, it is relative to the configuration file.

When the Configurer first sees this file, it will expand it somewhere on the file system. The expansion of the macro will then be the absolute file path in OS specific format. For example:

   "org.apache.felix.https.keystore":   "@{resource:keystore.ks}",
    
This requires a file /configuration/keystore.ks in the bundle. This file is then copied to the file system and the macro is expanded to something like: c:\ws\conf\bundle15\v1\data\__config\keystore.ks.

## Macros
The configuration JSON file is pre-processed before it is used. You can therefore use the System properties and the local settings in ~/bnd/settings.json. Local settings can override System properties. Local settings can be maintained with the bnd settings command.

\$ bnd settings mail.user='Alice'
\$ bnd settings .mail.secret='Cheshire'

configuration/configuration.json:
  ...
  "user": "\${mail.user}",
  "password":"\${.mail.secret}"
  ...
Properties that start with a dot ('.') are intended to be kept form prying eyes.

It is possible to use @{...} instead of \${...} to prevent the macros being expanded too early, for example in bnd build time processing.

## Profiles

Profiles can be used to maintain a single configuration file for both debug and runtime. Profiles prefix a key with [<profile>], for example [debug]key. If the current profile matches the prefix of a key, then that raw key is added to the Configuration dictionary. In the previous example, if the profile was debug, then key would be added.

The profile can be overridden with --profile from the command line (if the bnd launcher is used). Otherwise the value of the System property or local setting profile is used. If there is no such value then the default profile is debug.

## Logging

All errors are logged in the OSGi Log Service. Additionally, a .log key will log its value. For example:

	".log": "This will be logged as INFO" 

## Comments

An record with a .comment key will be ignored.

".comment": "this comment is ignored" 

## Example

	[{ 
		"service.pid": "org.apache.felix.http", 
		"org.apache.felix.http.enable": true, 
		"org.osgi.service.http.port": 8080, 
		"org.apache.felix.http.debug": true, 
		"[debug]org.apache.felix.https.debug": true, 
		"[debug]org.apache.felix.https.enable": true, 
		"org.osgi.service.http.port.secure": 8085, 
		"org.apache.felix.https.keystore": "@{resource:jpm4j.ks}",
		"org.apache.felix.https.keystore.password":"@{.jpm4j.keystore}"
	},{ 
		"service.factoryPid": "aQute.executor.ExecutorImpl", 
		"service.pid": "DefaultExecutor", "type": "FIXED", "size": 10 
	}
	]

