package org.jenkinsci.plugins.database;

import hudson.ExtensionPoint;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TopLevelItem;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Similar to {@link Database} but assumes that there is a separate database per item.
 * Typically would be implemented by an embeddable database capable of loading from the {@linkplain TopLevelItem#getRootDir root directory}.
 * It is recommended, but not required, that implementations have a no-argument {@link DataBoundConstructor},
 * which permits them to be configured by default merely by installing a plugin.
 */
public abstract class PerItemDatabase extends AbstractDescribableImpl<PerItemDatabase> implements ExtensionPoint {

    /**
     * Loads the database for an item.
     * Should be created if not already present.
     * Since multiple plugins may be sharing this database, prefix any table names with the {@linkplain PluginWrapper#shortName plugin name}.
     * @param item a job, folder, etc.
     * @return a database connection specific to that item
     * @throws SQLException in case it is impossible to connect to the database
     */
    public abstract DataSource getDataSource(TopLevelItem item) throws SQLException;

    @Override public PerItemDatabaseDescriptor getDescriptor() {
        return (PerItemDatabaseDescriptor) super.getDescriptor();
    }

}
