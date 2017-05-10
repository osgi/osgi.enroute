# OSGI ENROUTE JDBC DATASOURCE PROVIDER

This bundle is a replacement for OPS4J PAX JDBC Config. The PAX bundle has a few issues:

* Assumes there are always Strings in the configuration which is not always true.
* Is quite complex because it does not use DS
* Does not provide Metatype for the configuration record 


## Configuration

The configuration is based on the OSGi JDBC Data Source factory properties

	Pid: osgi.enroute.jdbc.datasource
	
	Fields:
	
		String dataSourceName

		String factory_target

		String user

		String password

		String databaseName

		String description

		int initialPoolSize

		int maxPoolSize

		int minPoolSize

		int maxStatements

		int maxIdleTime

		String networkProtocol

		int portNumber

		String propertyCycle

		String roleName

		String serverName

		String url
		
	
