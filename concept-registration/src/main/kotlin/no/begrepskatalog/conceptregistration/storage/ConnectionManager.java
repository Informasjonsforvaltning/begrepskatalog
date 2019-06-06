package no.begrepskatalog.conceptregistration.storage;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class ConnectionManager {
    public static final String DB_SCHEMA = "conceptregistration";
    static private final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    @Value("${spring.datasource.url}")
    public String jdbcURL;

    @Value("${spring.datasource.username}")
    public String username;

    @Value("${spring.datasource.password}")
    public String password;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDatabase() {
        try (Connection connection = DriverManager.getConnection(jdbcURL, username, password)) {
            try {
                connection.setAutoCommit(false);
                boolean somePasswordHasBeenDefined = (password != null && password.length() > 0);
                logger.info("Connection Manager, starting up. JDBC Url from environment is {}. Username is  {}. A password has been defined: {}", jdbcURL, username, somePasswordHasBeenDefined);

                try (Statement stmt = connection.createStatement()) {
                    logger.info("Creating schema " + DB_SCHEMA + " if not exists");
                    stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS " + DB_SCHEMA);
                    connection.commit();
                } catch (Exception e) {
                    logger.info("Tried to create SCHEMA " + DB_SCHEMA + " but got exception", e);
                    throw e;
                }

                logger.info("Liquibase sync started.");
                Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setLiquibaseSchemaName(ConnectionManager.DB_SCHEMA);
                Liquibase liquibase = new Liquibase("liquibase/changelog/changelog-master.xml", new ClassLoaderResourceAccessor(), database);
                liquibase.update(new Contexts(), new LabelExpression());
                //connectionManager.createRegularUser(connection);
                connection.commit();
                logger.info("Liquibase synced OK.");
            } catch (LiquibaseException | SQLException e) {
                try {
                    logger.error("Initializing DB failed: " + e.getMessage());
                    connection.rollback();
                    throw new SQLException(e);
                } catch (SQLException e2) {
                    logger.error("Rollback after fail failed: " + e2.getMessage());
                    throw new SQLException(e2);
                }
            }
        } catch (SQLException e) {
            logger.error("Getting connection for Liquibase update failed: " + e.getMessage());
            System.exit(-1);
        }
    }
}
