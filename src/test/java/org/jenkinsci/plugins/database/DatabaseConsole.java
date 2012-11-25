package org.jenkinsci.plugins.database;

import hudson.Extension;
import hudson.model.RootAction;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class DatabaseConsole implements RootAction {
    @Inject
    Sample sample;

    public String getDisplayName() {
        return "Database Console";
    }

    public String getIconFileName() {
        return "terminal.png";
    }

    public String getUrlName() {
        return "database-console";
    }

    public HttpResponse doExecute(@QueryParameter String sql) throws SQLException {
        Database db = sample.getDatabase();
        if (db==null)
            throw new IllegalArgumentException("Database isn't configured yet");

        Connection con = db.getDataSource().getConnection();
        Statement s = con.createStatement();
        if (s.execute(sql)) {
            return HttpResponses.forwardToView(this,"index").with("r",s.getResultSet());
        } else {
            return HttpResponses.forwardToView(this,"index").with("message","OK");
        }
    }
}
