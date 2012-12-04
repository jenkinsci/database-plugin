package org.jenkinsci.plugins.database;

import hudson.Extension;
import hudson.model.TopLevelItem;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sql.DataSource;
import jenkins.model.GlobalConfiguration;
import static jenkins.model.GlobalConfiguration.all;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Allows user to select, and if necessary, configure the preferred {@link PerItemDatabase}.
 */
@Extension
public class PerItemDatabaseConfiguration extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(PerItemDatabaseConfiguration.class.getName());

    private PerItemDatabase database;

    public PerItemDatabaseConfiguration() {
        load();
    }

    public @CheckForNull PerItemDatabase getDatabase() {
        if (database != null) {
            return database;
        }
        for (PerItemDatabaseDescriptor d : Jenkins.getInstance().getExtensionList(PerItemDatabaseDescriptor.class)) {
            for (Constructor<?> c : d.clazz.getConstructors()) {
                if (c.getParameterTypes().length == 0 && c.getAnnotation(DataBoundConstructor.class) != null) {
                    LOGGER.log(Level.INFO, "no configured database; falling back to {0}", d.getId());
                    try {
                        return database = (PerItemDatabase) c.newInstance();
                    } catch (Exception x) {
                        // XXX perhaps cache this failure
                        LOGGER.log(Level.WARNING, "cannot create no-arg instance of " + d.getId(), x);
                    }
                }
            }
        }
        return null;
    }

    public void setDatabase(PerItemDatabase database) {
        this.database = database;
    }

    @Override public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    public static @CheckForNull PerItemDatabase findOrNull() {
        return all().get(PerItemDatabaseConfiguration.class).getDatabase();
    }

    /**
     * Convenience method to find the configured database.
     * If no database has yet been configured, and none is available, the return value will always throw {@link SQLException}.
     * @return the database factory
     */
    public static @Nonnull PerItemDatabase find() {
        PerItemDatabase database = findOrNull();
        return database != null ? database : new PerItemDatabase() {
            @Override public DataSource getDataSource(TopLevelItem item) throws SQLException {
                throw new SQLException("No per-item database has been configured");
            }
        };
    }

}
