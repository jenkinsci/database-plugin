package org.jenkinsci.plugins.database.steps;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.util.ListBoxModel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jenkinsci.plugins.database.Database;
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration;
import org.jenkinsci.plugins.database.PerItemDatabase;
import org.jenkinsci.plugins.database.PerItemDatabaseConfiguration;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class DatabaseConnectionStep extends Step {

  @Extension(optional = true)
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  private static final Logger LOG = Logger.getLogger ( DatabaseConnectionStep.class.getName () );
  private DatabaseType type;
  private String id;

  @DataBoundConstructor
  public DatabaseConnectionStep () {
  }

  @Override
  public StepExecution start(StepContext context) throws Exception {
    return new Execution(this, context);
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

  public static class Execution extends StepExecution {

    private static final long serialVersionUID = 1L;

    private final transient DatabaseConnectionStep step;
    private transient Connection connection;
    private transient BodyExecution execution;

    public Execution(DatabaseConnectionStep step, @NonNull StepContext context) {
      super(context);
      this.step = step;
    }

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

    private void fetchDatabase () throws Exception {
      try {
        LOG.log ( Level.FINE, "Fetching database connection of type {0}", step.type );
        switch ( step.type ) {
          default:
          case GLOBAL:
            final GlobalDatabaseConfiguration globalDatabaseConfiguration = ExtensionList.lookupSingleton(GlobalDatabaseConfiguration.class);
            final Database database = globalDatabaseConfiguration.getDatabase();
            if (database != null) {
              connection = database.getDataSource().getConnection();
            }
            break;
          case PERITEM:
            if ( getContext().get(Run.class).getParent () instanceof TopLevelItem ) {
              final PerItemDatabaseConfiguration perItemDatabaseConfiguration = ExtensionList.lookupSingleton(PerItemDatabaseConfiguration.class);
              final PerItemDatabase perItemDatabase = perItemDatabaseConfiguration.getDatabase();
              if (perItemDatabase != null) {
                connection = perItemDatabase.getDataSource((TopLevelItem) getContext().get(Run.class).getParent())
                        .getConnection();
              }
            } else {
              throw new FailedToGetDatabaseException (
                  "Failed to get per item database build.getParent is not instance of TopLevelItem? " +
                      getContext().get(Run.class).getParent ().getClass () );
            }
            break;
        }
        LOG.log ( Level.FINE, "Got database connection" );
      } catch ( SQLException e ) {
        throw new FailedToGetDatabaseException ( "Failed to get database connection", e );
      }
    }

    private void closeConnection () throws Exception {
      if ( connection != null ) {
        try {
          LOG.log ( Level.FINE, "Closing database connection" );
          connection.close ();
          connection = null;
        } catch ( SQLException e ) {
          getContext().get(TaskListener.class).error ( "Failed to close connection %s", e );
        }
      }
    }

    @Override
    public void stop ( @NonNull Throwable cause ) throws Exception {
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

  public static class DescriptorImpl extends StepDescriptor {

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      Set<Class<?>> context = new HashSet<>();
      Collections.addAll(context, Run.class, TaskListener.class);
      return Collections.unmodifiableSet(context);
    }

    @Override
    public String getFunctionName () {
      return "getDatabaseConnection";
    }

    @NonNull
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
