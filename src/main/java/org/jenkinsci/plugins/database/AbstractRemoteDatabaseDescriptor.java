package org.jenkinsci.plugins.database;

import hudson.util.FormValidation;
import hudson.util.Secret;
import java.sql.Statement;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractRemoteDatabaseDescriptor extends DatabaseDescriptor {
    protected AbstractRemoteDatabaseDescriptor() {
    }

    protected AbstractRemoteDatabaseDescriptor(Class<? extends Database> clazz) {
        super(clazz);
    }

    @POST
    public FormValidation doValidate(
            @QueryParameter String hostname,
            @QueryParameter String database,
            @QueryParameter String username,
            @QueryParameter Secret password,
            @QueryParameter String properties) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        
        try {
            Database db = clazz.getConstructor(String.class,String.class,String.class,Secret.class,String.class).newInstance(hostname, database, username, password, properties);
            DataSource ds = db.getDataSource();
            try (Connection con = ds.getConnection(); Statement statement = con.createStatement()) {
                statement.execute("SELECT 1");
            }
            return FormValidation.ok("OK");
        } catch (SQLException e) {
            return FormValidation.error(e,"Failed to connect to "+getDisplayName());
        }

    }
}
