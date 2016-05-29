package org.jenkinsci.plugins.database.steps;

public class FailedToGetDatabaseException extends Exception {
  public FailedToGetDatabaseException () {
  }

  public FailedToGetDatabaseException ( String message ) {
    super ( message );
  }

  public FailedToGetDatabaseException ( String message, Throwable cause ) {
    super ( message, cause );
  }
}
