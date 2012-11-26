package org.jenkinsci.plugins.database;

import hudson.util.FormValidation;
import hudson.util.Secret;
import org.kohsuke.stapler.QueryParameter;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractRemoteDatabaseDescriptor extends DatabaseDescriptor {
    protected AbstractRemoteDatabaseDescriptor() {
    }

    protected AbstractRemoteDatabaseDescriptor(Class<? extends Database> clazz) {
        super(clazz);
    }

    public FormValidation doValidate(
            @QueryParameter String hostname,
            @QueryParameter String database,
            @QueryParameter String username,
            @QueryParameter String password,
            @QueryParameter String properties) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {

        try {
            Database db = clazz.getConstructor(String.class,String.class,String.class,Secret.class,String.class).newInstance(hostname, database, username, Secret.fromString(password), properties);
            DataSource ds = db.getDataSource();
            Connection con = ds.getConnection();
            con.createStatement().execute("SELECT 1");
            con.close();
            return FormValidation.ok("OK");
        } catch (SQLException e) {
            return FormValidation.error(e,"Failed to connect to "+getDisplayName());
        }

    }
}
