package org.jenkinsci.plugins.database;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import io.jenkins.plugins.opentelemetry.api.ReconfigurableOpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * {@link DataSource} configured by the user in Jenkins.
 *
 * <p>
 * This encapsulates a JDBC driver, connection string, and a credential to connect to it.
 * It serves as a factory to JDBC {@link Connection}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Database extends AbstractDescribableImpl<Database> implements ExtensionPoint {

    public abstract DataSource getDataSource() throws SQLException;

    /**
     * <p>
     * Returns true if OpenTelemetry JDBC instrumentation is enabled.
     * </p>
     * <p>
     * Implementations couldn't use {@link io.jenkins.plugins.opentelemetry.api.OpenTelemetryLifecycleListener} to
     * retrieve the configuration and get configuration changes because it's the {@link DatabaseDescriptor} that should
     * have implemented the {@link io.jenkins.plugins.opentelemetry.api.OpenTelemetryLifecycleListener} interface and
     * {@link hudson.model.Descriptor} instances are not available on Jenkins  build agent JVMs, only on the Jenkins
     * controller.
     * </p>
     */
    protected boolean isOTelJdbcInstrumentationEnabled() {
        ReconfigurableOpenTelemetry reconfigurableOpenTelemetry = ReconfigurableOpenTelemetry.get();
        ConfigProperties config = reconfigurableOpenTelemetry.getConfig();
        return config.getBoolean("otel.instrumentation.jdbc.enabled", false);
    }

    @Override
    public DatabaseDescriptor getDescriptor() {
        return (DatabaseDescriptor) super.getDescriptor();
    }

}