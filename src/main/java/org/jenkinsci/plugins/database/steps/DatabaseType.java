package org.jenkinsci.plugins.database.steps;

public enum DatabaseType {
  GLOBAL ( "Global" ),
  PERITEM ( "Per Item" );

  private final String displayName;

  DatabaseType ( String displayName ) {
    this.displayName = displayName;
  }

  public String getDisplayName () {
    return displayName;
  }
}
