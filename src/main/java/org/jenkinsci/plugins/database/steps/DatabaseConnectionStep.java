package org.jenkinsci.plugins.database.steps;

import hudson.Extension;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.util.ListBoxModel;
import java.util.Objects;
import org.jenkinsci.plugins.database.Database;
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration;
import org.jenkinsci.plugins.database.PerItemDatabase;
import org.jenkinsci.plugins.database.PerItemDatabaseConfiguration;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class DatabaseConnectionStep extends AbstractStepImpl {

  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  private static final Logger LOG = Logger.getLogger ( DatabaseConnectionStep.class.getName () );
  private DatabaseType type;
  private String id;

  @DataBoundConstructor
  public DatabaseConnectionStep () {
  }

  public DatabaseType getType () {
    return type;
  }

  @DataBoundSetter
  public void setType ( DatabaseType type ) {
    this.type = type;
  }

  public String getId () {
    return id;
  }

  @DataBoundSetter
  public void setId ( @CheckForNull String id ) {
    this.id = Util.fixEmptyAndTrim ( id );
  }

  @Override
  public StepDescriptor getDescriptor () {
    return DESCRIPTOR;
  }

  public static class Execution extends AbstractStepExecutionImpl {

    private static final long serialVersionUID = 1L;

    @StepContextParameter
    private transient Run<?, ?> build;
    @StepContextParameter
    private transient TaskListener taskListener;
    @Inject
    private transient DatabaseConnectionStep step;
    @Inject
    private transient GlobalDatabaseConfiguration globalDatabaseConfiguration;
    @Inject
    private transient PerItemDatabaseConfiguration perItemDatabaseConfiguration;
    private transient Connection connection;
    private transient BodyExecution execution;

    @Override
    public void onResume () {
      throw new RuntimeException ( "Database operations can't be resumed" );
    }

    @Override
    public boolean start () throws Exception {
      fetchDatabase ();
      try {
        DatabaseContext dbContext =
            new DatabaseContext ( getContext ().get ( DatabaseContext.class ), connection, step.id );
        execution = getContext ()
            .newBodyInvoker ()
            .withContext ( dbContext )
            .withCallback ( new BodyExecutionCallback.TailCall () {
              @Override
              protected void finished ( StepContext context ) throws Exception {
                closeConnection ();
              }
            } )
            .start ();
        return false;
      } catch ( Exception e ) {
        closeConnection ();
        getContext ().onFailure ( e );
        return true;
      }
    }

    private void fetchDatabase () throws FailedToGetDatabaseException {
      try {
        LOG.log ( Level.FINE, "Fetching database connection of type {0}", step.type );
        switch ( step.type ) {
          default:
          case GLOBAL:
            final Database database = globalDatabaseConfiguration.getDatabase();
            if (database != null) {
              connection = database.getDataSource().getConnection();
            }
            break;
          case PERITEM:
            if ( build.getParent () instanceof TopLevelItem ) {
              final PerItemDatabase perItemDatabase = perItemDatabaseConfiguration.getDatabase();
              if (perItemDatabase != null) {
                connection = perItemDatabase.getDataSource((TopLevelItem) build.getParent())
                        .getConnection();
              }
            } else {
              throw new FailedToGetDatabaseException (
                  "Failed to get per item database build.getParent is not instance of TopLevelItem? " +
                      build.getParent ().getClass () );
            }
            break;
        }
        LOG.log ( Level.FINE, "Got database connection" );
      } catch ( SQLException e ) {
        throw new FailedToGetDatabaseException ( "Failed to get database connection", e );
      }
    }

    private void closeConnection () {
      if ( connection != null ) {
        try {
          LOG.log ( Level.FINE, "Closing database connection" );
          connection.close ();
          connection = null;
        } catch ( SQLException e ) {
          taskListener.error ( "Failed to close connection %s", e );
        }
      }
    }

    @Override
    public void stop ( @Nonnull Throwable cause ) throws Exception {
      LOG.log ( Level.FINE, "Recived stop request", cause );
      try {
        if ( execution != null ) {
          execution.cancel ( cause );
          execution = null;
        }
      } finally {
        closeConnection ();
      }
    }

  }

  public static class DescriptorImpl extends AbstractStepDescriptorImpl {
    public DescriptorImpl () {
      super ( Execution.class );
    }

    @Override
    public String getFunctionName () {
      return "getDatabaseConnection";
    }

    @Nonnull
    @Override
    public String getDisplayName () {
      return "Get Database Connection";
    }

    @Override
    public boolean takesImplicitBlockArgument () {
      return true;
    }

    public ListBoxModel doFillTypeItems () {
      ListBoxModel items = new ListBoxModel ();
      for ( DatabaseType dataType : DatabaseType.values () ) {
        items.add ( dataType.getDisplayName (), dataType.name () );
      }
      return items;
    }
  }
}
