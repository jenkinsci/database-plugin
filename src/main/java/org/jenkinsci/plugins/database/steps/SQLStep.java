package org.jenkinsci.plugins.database.steps;

import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLStep extends Step {
  @Extension(optional = true)
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl ();
  private static final Logger LOG = Logger.getLogger ( SQLStep.class.getName () );
  private String sql;
  private String connection;
  private List<Object> parameters;

  @DataBoundConstructor
  public SQLStep ( String sql ) {
    this.sql = Util.fixEmptyAndTrim ( sql );
  }

  public String getSql () {
    return sql;
  }

  public String getConnection () {
    return connection;
  }

  @DataBoundSetter
  public void setConnection ( @CheckForNull String connection ) {
    this.connection = Util.fixEmptyAndTrim ( connection );
  }

  public List<Object> getParameters () {
    return parameters;
  }

  @DataBoundSetter
  public void setParameters ( List<Object> parameters ) {
    this.parameters = parameters;
  }

  @Override
  public StepExecution start(StepContext context) throws Exception {
    return new Execution(this, context);
  }

  @Override
  public StepDescriptor getDescriptor () {
    return DESCRIPTOR;
  }

  public static class Execution extends SynchronousNonBlockingStepExecution<List<Map<String, Object>>> {

    private static final long serialVersionUID = 1L;

    private final transient SQLStep step;

    protected Execution(SQLStep step, @NonNull StepContext context) {
      super(context);
      this.step = step;
    }


    @Override
    protected List<Map<String, Object>> run () throws Exception {
      List<Map<String, Object>> rt = new ArrayList<Map<String, Object>> ();
      LOG.log ( Level.FINE, "Running SQL {0} with parameters {1} on connection {2}",
          new Object[]{ step.sql, step.parameters, step.connection } );
      Connection connection = getContext().get(DatabaseContext.class).getConnection ( step.connection );

      PreparedStatement preparedStatement = connection.prepareStatement ( step.sql );
      try {
        if ( step.parameters != null && !step.parameters.isEmpty () ) {
          for ( int i = 0; i < step.parameters.size (); i++ ) {
            preparedStatement.setObject ( i + 1, step.parameters.get ( i ) );
          }
        }
        if ( preparedStatement.execute () ) {
          ResultSet set = preparedStatement.getResultSet ();
          try {
            while ( set.next () ) {
              Map<String, Object> row = new TreeMap<String, Object> ();
              for ( int i = 1; i <= set.getMetaData ().getColumnCount (); i++ ) {
                row.put ( set.getMetaData ().getColumnName ( i ), set.getObject ( i ) );
              }
              rt.add ( row );
            }
            LOG.log ( Level.FINE, "Got {0} rows", rt.size () );
          } finally {
            if ( set != null ) {
              try {
                set.close ();
              } catch ( SQLException e ) {
                getContext().get(TaskListener.class).error ( "Error closing resultset %s", e );
              }
            }
          }
        }
      } finally {
        if ( preparedStatement != null ) {
          try {
            preparedStatement.close ();
          } catch ( SQLException e ) {
            getContext().get(TaskListener.class).error ( "Error closing statement %s", e );
          }
        }
      }

      return rt;
    }
  }

  public static class DescriptorImpl extends StepDescriptor {

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      Set<Class<?>> context = new HashSet<>();
      Collections.addAll(context, DatabaseContext.class, TaskListener.class);
      return Collections.unmodifiableSet(context);
    }

    @Override
    public String getFunctionName () {
      return "sql";
    }

    @NonNull
    @Override
    public String getDisplayName () {
      return "Run SQL";
    }

  }
}
