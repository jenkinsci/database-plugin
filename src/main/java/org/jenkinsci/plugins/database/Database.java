package org.jenkinsci.plugins.database;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;

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

    @Override
    public DatabaseDescriptor getDescriptor() {
        return (DatabaseDescriptor)super.getDescriptor();
    }
}
