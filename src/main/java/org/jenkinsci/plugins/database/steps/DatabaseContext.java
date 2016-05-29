package org.jenkinsci.plugins.database.steps;

import java.io.Serializable;
import java.sql.Connection;
import java.text.MessageFormat;

public class DatabaseContext implements Serializable {

  private final transient DatabaseContext parent;
  private final transient Connection connection;
  private final String id;

  public DatabaseContext ( DatabaseContext parent, Connection connection, String id ) {
    this.parent = parent;
    this.connection = connection;
    this.id = id;
  }

  public Connection getConnection ( String id ) throws FailedToGetDatabaseException {
    if ( id == null || id.equals ( this.id ) ) {
      return connection;
    } else if ( parent != null ) {
      return parent.getConnection ( id );
    } else {
      throw new FailedToGetDatabaseException (
          MessageFormat.format ( "Failed to get connection to database {0}", id ) );
    }
  }
}
