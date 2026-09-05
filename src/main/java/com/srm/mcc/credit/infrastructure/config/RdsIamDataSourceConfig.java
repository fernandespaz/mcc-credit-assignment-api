package com.srm.mcc.credit.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;

import javax.sql.DataSource;

/**
 * Production datasource: authenticates against AWS RDS with short-lived IAM tokens instead of a
 * static database password.
 * <p>
 * Requirements on the RDS side:
 * <ul>
 *   <li>IAM database authentication enabled on the instance/cluster.</li>
 *   <li>The database user (e.g. {@code postgres}) granted the {@code rds_iam} role
 *       ({@code GRANT rds_iam TO postgres;}).</li>
 *   <li>The IAM principal running this application (instance profile / task role / user)
 *       allowed the {@code rds-db:connect} action for that DB user via IAM policy.</li>
 * </ul>
 * AWS credentials for signing the auth token are resolved by the AWS SDK default credential
 * chain (instance profile, ECS task role, environment variables, etc.) — never hardcoded.
 */
@Configuration
@Profile("prod")
public class RdsIamDataSourceConfig {

    @Value("${RDSHOST:${DB_HOST:}}")
    private String rdsHost;

    @Value("${DB_PORT:5432}")
    private int dbPort;

    @Value("${DB_NAME:postgres}")
    private String dbName;

    @Value("${DB_USERNAME:postgres}")
    private String dbUsername;

    @Value("${DB_SSL_MODE:require}")
    private String sslMode;

    @Value("${AWS_REGION:sa-east-1}")
    private String awsRegion;

    @Value("${DB_POOL_MAX:10}")
    private int poolMax;

    @Value("${DB_POOL_MIN:2}")
    private int poolMin;

    @Bean
    public DataSource dataSource() {
        RdsUtilities rdsUtilities = RdsUtilities.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        DataSource iamAuthDataSource = new IamAuthPostgresDataSource(
                rdsUtilities, rdsHost, dbPort, dbName, dbUsername, sslMode);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(iamAuthDataSource);
        hikariConfig.setMaximumPoolSize(poolMax);
        hikariConfig.setMinimumIdle(poolMin);
        hikariConfig.setConnectionTimeout(30_000);
        hikariConfig.setIdleTimeout(600_000);
        // Below the 15-minute IAM token validity window, forcing periodic re-authentication
        // with a fresh token rather than holding physical connections indefinitely.
        hikariConfig.setMaxLifetime(840_000);
        hikariConfig.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(hikariConfig);
    }
}
