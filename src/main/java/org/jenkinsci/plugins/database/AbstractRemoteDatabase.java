package org.jenkinsci.plugins.database;

import hudson.Util;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;

/**
 * Partial default implementation for typical JDBC connector that talks to a remote server
 * via host, database, username, and password parameters.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractRemoteDatabase extends Database {
  /**
   * Host name + optional port (in the "host[:port]" format)
   */
  public final String hostname;
  public final String database;
  public final String username;
  public final Secret password;
  public String validationQuery;

  public final String properties;

  private transient DataSource source;

  @DataBoundConstructor
    public AbstractRemoteDatabase(String hostname, String database, String username, Secret password, String properties) {
    this.hostname = hostname;
    this.database = database;
    this.username = username;
    this.password = password;
    this.properties = properties;
  }

  @DataBoundSetter
  public void setValidationQuery ( @QueryParameter String validationQuery ) {
    this.validationQuery = validationQuery;
  }

  public String getValidationQuery () {
    return validationQuery;
  }

  protected abstract Class<? extends Driver> getDriverClass ();

  protected abstract String getJdbcUrl ();

  @Override
  public synchronized DataSource getDataSource () throws SQLException {
    if ( source == null ) {
      BasicDataSource2 fac = new BasicDataSource2 ();
      fac.setDriverClass ( getDriverClass () );
      fac.setUrl ( getJdbcUrl () );
      fac.setUsername ( username );
      fac.setPassword ( Secret.toString ( password ) );
      fac.setValidationQuery ( validationQuery );

      try {
                for (Map.Entry e : Util.loadProperties(Util.fixNull(properties)).entrySet()) {
                    fac.addConnectionProperty(e.getKey().toString(), e.getValue().toString());
        }
      } catch ( IOException e ) {
        throw new SQLException ( "Invalid properties", e );
      }

      source = fac.createDataSource ();
    }
    return source;
  }
}
