package org.jenkinsci.plugins.database;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TopLevelItem;
import hudson.model.TransientProjectActionFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;

public class DatabaseConsole implements Action {

    private final TopLevelItem p;

    private DatabaseConsole(TopLevelItem p) {
        this.p = p;
    }

    @Override public String getUrlName() {
        return enabled() ? "database-console" : null;
    }

    @Override public String getDisplayName() {
        return "Database Console";
    }

    @Override public String getIconFileName() {
        return enabled() ? "terminal.png" : null;
    }

    private boolean enabled() {
        return Jenkins.getInstance().hasPermission(Jenkins.RUN_SCRIPTS) && PerItemDatabaseConfiguration.findOrNull() != null;
    }

    public HttpResponse doExecute(@QueryParameter String sql) throws SQLException {
        Jenkins.getInstance().checkPermission(Jenkins.RUN_SCRIPTS);
        PerItemDatabase db = PerItemDatabaseConfiguration.find();
        Connection con = db.getDataSource(p).getConnection();
        try {
            Statement s = con.createStatement();
            try {
                if (s.execute(sql)) {
                    return HttpResponses.forwardToView(this,"index").with("r",s.getResultSet());
                } else {
                    return HttpResponses.forwardToView(this,"index").with("message","OK");
                }
            } finally {
                // XXX clone to r/o flyweight object and return instead: s.close();
            }
        } finally {
            // XXX con.close();
        }
    }

    @Extension public static class Factory extends TransientProjectActionFactory {

        @Override public Collection<? extends Action> createFor(AbstractProject p) {
            return p instanceof TopLevelItem ? Collections.singleton(new DatabaseConsole((TopLevelItem) p)) : Collections.<Action>emptySet();
        }

    }

}
