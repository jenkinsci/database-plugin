package org.jenkinsci.plugins.database.steps;

import hudson.Extension;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLStep extends AbstractStepImpl {
  @Extension
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
  public StepDescriptor getDescriptor () {
    return DESCRIPTOR;
  }

  public static class Execution extends AbstractSynchronousNonBlockingStepExecution<List<Map<String, Object>>> {

    @StepContextParameter
    private transient Run build;
    @StepContextParameter
    private transient TaskListener taskListener;
    @StepContextParameter
    private transient DatabaseContext databaseContext;
    @Inject
    private transient SQLStep step;


    @Override
    protected List<Map<String, Object>> run () throws Exception {
      List<Map<String, Object>> rt = new ArrayList<Map<String, Object>> ();
      LOG.log ( Level.FINE, "Running SQL {0} with parameters {1} on connection {2}",
          new Object[]{ step.sql, step.parameters, step.connection } );
      Connection connection = databaseContext.getConnection ( step.connection );

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
                taskListener.error ( "Error closing resultset %s", e );
              }
            }
          }
        }
      } finally {
        if ( preparedStatement != null ) {
          try {
            preparedStatement.close ();
          } catch ( SQLException e ) {
            taskListener.error ( "Error closing statement %s", e );
          }
        }
      }

      return rt;
    }
  }

  public static class DescriptorImpl extends AbstractStepDescriptorImpl {
    public DescriptorImpl () {
      super ( Execution.class );
    }

    @Override
    public String getFunctionName () {
      return "sql";
    }

    @Nonnull
    @Override
    public String getDisplayName () {
      return "Run SQL";
    }

  }
}
