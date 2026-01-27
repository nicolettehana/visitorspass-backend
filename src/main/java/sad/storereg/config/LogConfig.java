package sad.storereg.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.db.jdbc.ColumnConfig;
import org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

//import some.package.logging.JdbcConnectionSource;

import jakarta.annotation.PostConstruct;

@Configuration
public class LogConfig {
	
	@Autowired
	private Environment env;

	@PostConstruct
	public void onStartUp() {
		String url = env.getProperty("spring.datasource.url");
		String userName = env.getProperty("spring.datasource.username");
		String password = env.getProperty("spring.datasource.password");
		String validationQuery = env.getProperty("spring.datasource.validation-query");
		// Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		// Create a new connectionSource build from the Spring properties
		// ConnectionClass connectionSource = new ConnectionClass(url, userName,
		// password, validationQuery);
		// ConnectionClass connectionSource = new ConnectionClass(env);
		JdbcConnectionSource connectionSource = new JdbcConnectionSource(url, userName, password, validationQuery);
		// This is the mapping between the columns in the table and what to insert in
		// it.
		ColumnConfig[] columnConfigs = new ColumnConfig[6];
		//columnConfigs[0] = ColumnConfig.newBuilder().setName("username").setPattern("%X{username}").setClob(false).build();
		columnConfigs[0] = ColumnConfig.createColumnConfig(null, "username", "%X{username}", null, null, "false", null);
		//columnConfigs[1] = ColumnConfig.newBuilder().setName("event_date").setEventTimestamp(true).build();
		columnConfigs[1] = ColumnConfig.createColumnConfig(null, "event_date", null, null, "true", null, null);
		//columnConfigs[2] = ColumnConfig.newBuilder().setName("logger").setPattern("%logger").setClob(false).build();
		columnConfigs[2] = ColumnConfig.createColumnConfig(null, "logger", "%logger", null, null, "false", null);
		//columnConfigs[3] = ColumnConfig.newBuilder().setName("level").setPattern("%level").setClob(false).build();
		columnConfigs[3] = ColumnConfig.createColumnConfig(null, "level", "%level", null, null, "false", null);
		//columnConfigs[4] = ColumnConfig.newBuilder().setName("message").setPattern("%message").setClob(false).build();
		columnConfigs[4] = ColumnConfig.createColumnConfig(null, "message", "%message", null, null, "false", null);
		//columnConfigs[5] = ColumnConfig.newBuilder().setName("stacktrace").setPattern("%exception").setClob(false).build();
		columnConfigs[5] = ColumnConfig.createColumnConfig(null, "stacktrace", "%exception", null, null, "false", null);

		// filter for the appender to keep only errors
		ThresholdFilter filter = ThresholdFilter.createFilter(Level.INFO, null, null);

		JdbcAppender appender = JdbcAppender.createAppender("DB", "true", filter, connectionSource, "1", "LOGS.logs",
				columnConfigs);

		// start the appender, and this is it...
		appender.start();
		((Logger) LogManager.getRootLogger()).addAppender(appender);
	}
}
