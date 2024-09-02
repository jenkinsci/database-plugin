package org.jenkinsci.plugins.database;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import io.jenkins.plugins.opentelemetry.api.OpenTelemetryLifecycleListener;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link DataSource} configured by the user in Jenkins.
 *
 * <p>
 * This encapsulates a JDBC driver, connection string, and a credential to connect to it.
 * It serves as a factory to JDBC {@link Connection}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Database extends AbstractDescribableImpl<Database> implements ExtensionPoint, OpenTelemetryLifecycleListener {
    protected final AtomicBoolean otelJdbcInstrumentationEnabled = new AtomicBoolean(false);

    public abstract DataSource getDataSource() throws SQLException;

    @Override
    public void afterConfiguration(ConfigProperties configProperties) {
        this.otelJdbcInstrumentationEnabled.set(configProperties.getBoolean("otel.instrumentation.jdbc.enabled", false));
    }

    @Override
    public DatabaseDescriptor getDescriptor() {
        return (DatabaseDescriptor) super.getDescriptor();
    }
}
