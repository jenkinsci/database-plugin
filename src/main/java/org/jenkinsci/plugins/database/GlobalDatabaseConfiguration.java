package org.jenkinsci.plugins.database;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;

/**
 * Provides the system-wide {@link Database} that's open for plugins to store arbitrary information.
 *
 * <p>
 * If a plugin wishes to store some information globally to RDBMS, and if the data can co-exist with
 * the data from other plugins, use {@linkplain #getDatabase() this shared database instance}.
 * In cases where a plugin wants to store data in its own database separate from this shared instance,
 * you can do so by using {@link Database} directly.
 *
 * <p>
 * There's also {@link PerItemDatabaseConfiguration}, which provides separate database for information
 * local to a specific job.
 *
 * @author Kohsuke Kawaguchi
 * @see PerItemDatabaseConfiguration
 */
@Extension(ordinal=GlobalDatabaseConfiguration.ORDINAL)
public class GlobalDatabaseConfiguration extends GlobalConfiguration {
    private volatile Database database;

    public GlobalDatabaseConfiguration() {
        load();
    }

    /**
     * This is the configured {@link Database} instance, or null in case none is configured yet.
     */
    public @CheckForNull Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        if (database instanceof NoDatabase)
            database = null;
        this.database = database;
        save();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    public static GlobalDatabaseConfiguration get() {
        return all().get(GlobalDatabaseConfiguration.class);
    }

    /*package*/ static final double ORDINAL = -33;
}
