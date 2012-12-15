package org.jenkinsci.plugins.database;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;

/**
 * Provides the system-wide {@link Database} that's open for plugins to store arbitrary information.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension(ordinal=GlobalDatabaseConfiguration.ORDINAL)
public class GlobalDatabaseConfiguration extends GlobalConfiguration {
    private volatile Database database;

    public GlobalDatabaseConfiguration() {
        load();
    }

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

    /*package*/ static final double ORDINAL = -33;
}
