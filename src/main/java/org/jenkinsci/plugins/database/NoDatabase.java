package org.jenkinsci.plugins.database;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pseudo instance in case the configuration of {@link Database} needs a "(none)" option.
 *
 * You can define your <tt>config.groovy</tt> as follows to have "(none)" appear as the default.
 * Also, see {@link GlobalDatabaseConfiguration#setDatabase(Database)} for the necessary fix up
 * on the setter (or in the constructor.)
 *
 * <pre>
 *     f.dropdownDescriptorSelector(field:"database",title:_("Database"),descriptors:NoDatabase.allPlusNone())
 * </pre>
 *
 * @author Kohsuke Kawaguchi
 */
public class NoDatabase extends Database {
    @DataBoundConstructor
    public NoDatabase() {
    }

    @Override
    public DataSource getDataSource() throws SQLException {
        return null;
    }

    // this is a pseudo instance
    // @Extension
    public static class DescriptorImpl extends DatabaseDescriptor {
        @Override
        public String getDisplayName() {
            return "(None)";
        }
    }

    public static List<DatabaseDescriptor> allPlusNone() {
        List<DatabaseDescriptor> r = new ArrayList<DatabaseDescriptor>();
        r.add(new DescriptorImpl());
        r.addAll(DatabaseDescriptor.all());
        return r;
    }
}
