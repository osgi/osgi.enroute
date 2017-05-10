package osgi.enroute.jdbc.datasource.provider;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * 
 */
@Designate(ocd = DatasourceImpl.Config.class, factory = true)
@Component(name = "osgi.enroute.jdbc.datasource", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DatasourceImpl implements DataSource {

	@Reference
	DataSourceFactory				factory;
	private DataSource	dataSource;

	@ObjectClassDefinition
	@interface Config {

		String dataSourceName();

		String factory_target();

		String user();

		String password();

		String databaseName();

		String description();

		int initialPoolSize();

		int maxPoolSize();

		int minPoolSize();

		int maxStatements();

		int maxIdleTime();

		String networkProtocol();

		int portNumber();

		String propertyCycle();

		String roleName();

		String serverName();

		String url();

	}

	@Activate
	void activate(Map<String, Object> config) throws SQLException {
		try {
			Properties p = new Properties();
			set(DataSourceFactory.JDBC_DATABASE_NAME, config, p);
			set(DataSourceFactory.JDBC_DESCRIPTION, config, p);
			set(DataSourceFactory.JDBC_INITIAL_POOL_SIZE, config, p);
			set(DataSourceFactory.JDBC_MAX_IDLE_TIME, config, p);
			set(DataSourceFactory.JDBC_MAX_POOL_SIZE, config, p);
			set(DataSourceFactory.JDBC_MAX_STATEMENTS, config, p);
			set(DataSourceFactory.JDBC_MIN_POOL_SIZE, config, p);
			set(DataSourceFactory.JDBC_NETWORK_PROTOCOL, config, p);
			set(DataSourceFactory.JDBC_PASSWORD, config, p);
			set(DataSourceFactory.JDBC_PORT_NUMBER, config, p);
			set(DataSourceFactory.JDBC_PROPERTY_CYCLE, config, p);
			set(DataSourceFactory.JDBC_ROLE_NAME, config, p);
			set(DataSourceFactory.JDBC_SERVER_NAME, config, p);
			set(DataSourceFactory.JDBC_URL, config, p);
			set(DataSourceFactory.JDBC_USER, config, p);
			dataSource = factory.createDataSource(p);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private void set(String key, Map<String, Object> config, Properties p) {
		Object v = config.get(key);
		if (v != null)
			p.put(key, v.toString());
	}

	public PrintWriter getLogWriter() throws SQLException {
		return dataSource.getLogWriter();
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return dataSource.unwrap(iface);
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		dataSource.setLogWriter(out);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return dataSource.isWrapperFor(iface);
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		dataSource.setLoginTimeout(seconds);
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return dataSource.getConnection(username, password);
	}

	public int getLoginTimeout() throws SQLException {
		return dataSource.getLoginTimeout();
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return dataSource.getParentLogger();
	}

}
