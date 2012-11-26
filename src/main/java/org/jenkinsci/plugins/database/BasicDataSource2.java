package org.jenkinsci.plugins.database;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverConnectionFactory;
import org.apache.commons.dbcp.SQLNestedException;

import javax.sql.DataSource;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * {@link BasicDataSource} with some convenience methods.
 *
 * @author Kohsuke Kawaguchi
 */
public class BasicDataSource2 extends BasicDataSource {
    public BasicDataSource2() {
    }

    public void setDriverClass(Class<? extends Driver> driverClass) {
        setDriverClassName(driverClass.getName());
        setDriverClassLoader(driverClass.getClassLoader());
    }

    @Override
    public DataSource createDataSource() throws SQLException {
        return super.createDataSource();
    }

    /**
     * Patched to fix DBCP-333.
     * This method should be deleted when switching to 1.4.1
     */
    protected ConnectionFactory createConnectionFactory() throws SQLException {
        // Load the JDBC driver class
        Class driverFromCCL = null;
        if (driverClassName != null) {
            try {
                if (driverClassLoader == null) {
                    driverFromCCL = Class.forName(driverClassName);
                } else {
                    driverFromCCL = Class.forName(driverClassName, true, driverClassLoader);
                }
            } catch (ClassNotFoundException cnfe) {
                throw new SQLException("Can't load "+driverClassName,cnfe);
            }
        }

        // Create a JDBC driver instance
        Driver driver = null;
        try {
            if (driverFromCCL == null) {
                driver = DriverManager.getDriver(url);
            } else {
                // Usage of DriverManager is not possible, as it does not
                // respect the ContextClassLoader
                driver = (Driver) driverFromCCL.newInstance();
                if (!driver.acceptsURL(url)) {
                    throw new SQLException("No suitable driver", "08001");
                }
            }
        } catch (Throwable t) {
            String message = "Cannot create JDBC driver of class '" +
                (driverClassName != null ? driverClassName : "") +
                "' for connect URL '" + url + "'";
            logWriter.println(message);
            t.printStackTrace(logWriter);
            throw new SQLNestedException(message, t);
        }

        // Can't test without a validationQuery
        if (validationQuery == null) {
            setTestOnBorrow(false);
            setTestOnReturn(false);
            setTestWhileIdle(false);
        }

        // Set up the driver connection factory we will use
        String user = username;
        if (user != null) {
            connectionProperties.put("user", user);
        } else {
            log("DBCP DataSource configured without a 'username'");
        }

        String pwd = password;
        if (pwd != null) {
            connectionProperties.put("password", pwd);
        } else {
            log("DBCP DataSource configured without a 'password'");
        }

        ConnectionFactory driverConnectionFactory = new DriverConnectionFactory(driver, url, connectionProperties);
        return driverConnectionFactory;
    }
}
