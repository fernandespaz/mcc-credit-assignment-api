package com.srm.mcc.credit.infrastructure.config;

import org.springframework.jdbc.datasource.AbstractDataSource;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * {@link javax.sql.DataSource} that authenticates against AWS RDS using IAM auth tokens
 * instead of a static password.
 * <p>
 * AWS IAM auth tokens are valid for 15 minutes and can only be used once to establish a
 * connection. Because {@link #getConnection()} is invoked by the connection pool every time a
 * new physical connection is opened (e.g. on pool startup, after {@code max-lifetime} eviction,
 * or when growing the pool), a brand-new token is generated on every call here — the pool never
 * reuses a stale token, and the application never needs a manually exported token or static
 * database password.
 */
public class IamAuthPostgresDataSource extends AbstractDataSource {

    private final RdsUtilities rdsUtilities;
    private final String hostname;
    private final int port;
    private final String database;
    private final String username;
    private final String sslMode;

    public IamAuthPostgresDataSource(RdsUtilities rdsUtilities,
                                      String hostname,
                                      int port,
                                      String database,
                                      String username,
                                      String sslMode) {
        this.rdsUtilities = rdsUtilities;
        this.hostname = hostname;
        this.port = port;
        this.database = database;
        this.username = username;
        this.sslMode = sslMode;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(username, null);
    }

    @Override
    public Connection getConnection(String requestedUsername, String ignoredPassword) throws SQLException {
        String authToken = generateFreshAuthToken();

        String url = "jdbc:postgresql://%s:%d/%s?sslmode=%s".formatted(hostname, port, database, sslMode);
        Properties props = new Properties();
        props.setProperty("user", requestedUsername);
        props.setProperty("password", authToken);

        return DriverManager.getConnection(url, props);
    }

    private String generateFreshAuthToken() {
        return rdsUtilities.generateAuthenticationToken(GenerateAuthenticationTokenRequest.builder()
                .hostname(hostname)
                .port(port)
                .username(username)
                .build());
    }
}
