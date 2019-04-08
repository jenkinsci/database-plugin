package org.jenkinsci.plugins.database;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Driver;
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
}
