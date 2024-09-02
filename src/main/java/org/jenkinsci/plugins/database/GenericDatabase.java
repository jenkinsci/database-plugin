package org.jenkinsci.plugins.database;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import jenkins.model.Jenkins;
import org.apache.tools.ant.AntClassLoader;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.File;
import java.sql.SQLException;

/**
 * {@link Database} implementation that allows the user to specify arbitrary JDBC connection string.
 *
 * @author Kohsuke Kawaguchi
 */
public class GenericDatabase extends Database {

    public final String driver;
    public final String username;
    public final Secret password;
    public final String url;

    private Integer initialSize = DescriptorImpl.defaultInitialSize;
    private Integer maxTotal = DescriptorImpl.defaultMaxTotal;
    private Integer maxIdle = DescriptorImpl.defaultMaxIdle;
    private Integer minIdle = DescriptorImpl.defaultMinIdle;

    private transient DataSource dataSource;
    private transient DataSource instrumentedDataSource;

    @DataBoundConstructor
    public GenericDatabase(String url, String driver, String username, Secret password) {
        this.url = url;
        this.driver = driver;
        this.username = username;
        this.password = password;
    }

    @NonNull
    public Integer getInitialSize() {
        return initialSize;
    }

    @DataBoundSetter
    public void setInitialSize(final Integer initialSize) {
        this.initialSize = initialSize == null ? DescriptorImpl.defaultInitialSize : initialSize;
    }

    @NonNull
    public Integer getMaxTotal() {
        return maxTotal;
    }

    @DataBoundSetter
    public void setMaxTotal(final Integer maxTotal) {
        this.maxTotal = maxTotal == null ? DescriptorImpl.defaultMaxTotal : maxTotal;
    }

    @NonNull
    public Integer getMaxIdle() {
        return maxIdle;
    }

    @DataBoundSetter
    public void setMaxIdle(final Integer maxIdle) {
        this.maxIdle = maxIdle == null ? DescriptorImpl.defaultMaxIdle : maxIdle;
    }

    @NonNull
    public Integer getMinIdle() {
        return minIdle;
    }

    @DataBoundSetter
    public void setMinIdle(final Integer minIdle) {
        this.minIdle = minIdle == null ? DescriptorImpl.defaultMinIdle : minIdle;
    }

    @Override
    public synchronized DataSource getDataSource() throws SQLException {
        if (dataSource ==null) {
            BasicDataSource2 source = new BasicDataSource2();
            source.setDriverClassLoader(getDescriptor().getClassLoader());
            source.setDriverClassName(driver);
            source.setUrl(url);
            source.setUsername(username);
            source.setPassword(Secret.toString(password));
            source.setInitialSize(initialSize);
            source.setMaxTotal(maxTotal);
            source.setMaxIdle(maxIdle);
            source.setMinIdle(minIdle);
            this.dataSource = source.createDataSource();
        }
        if (otelJdbcInstrumentationEnabled.get() && instrumentedDataSource == null) {
            instrumentedDataSource = JdbcTelemetry.create(GlobalOpenTelemetry.get()).wrap(dataSource);
        }
        return otelJdbcInstrumentationEnabled.get() ? instrumentedDataSource : dataSource;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension(ordinal = -1000) // low priority because this is generic
    public static class DescriptorImpl extends DatabaseDescriptor {
        private transient AntClassLoader loader;

        public static final Integer defaultInitialSize = 0;
        public static final Integer defaultMaxTotal = 8;
        public static final Integer defaultMaxIdle = 8;
        public static final Integer defaultMinIdle = 0;

        @Override
        public String getDisplayName() {
            return "Generic";
        }

        /**
         * ClassLoader that loads generic JDBC drivers.
         *
         * This includes {@code $JENKINS_HOME/jdbc-drivers/*.jar}
         */
        private synchronized ClassLoader getClassLoader() {
            if (loader ==null) {
                // delegate to the container
                loader = new AntClassLoader(HttpServletRequest.class.getClassLoader(),true);
                File[] files = new File(Jenkins.getInstance().getRootDir(), "jdbc-drivers").listFiles();
                if (files!=null) {
                    for (File jar : files) {
                        if (jar.getName().endsWith(".jar"))
                            loader.addPathComponent(jar);
                    }
                }
            }
            return loader;
        }

        @POST
        public FormValidation doCheckDriver(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);

            if (value.length()==0)
                return FormValidation.ok(); // no value typed yet.

            try {
                getClassLoader().loadClass(value);
                return FormValidation.ok();
            } catch (ClassNotFoundException e) {
                return FormValidation.error("No such class: "+value);
            }
        }

        @POST
        public FormValidation doValidate(@QueryParameter String driver,
                                         @QueryParameter String url,
                                         @QueryParameter String username,
                                         @QueryParameter Secret password) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            
            try {
                new GenericDatabase(url,driver,username, password).getDataSource();
                // XXX what about the "SELECT 1" trick from AbstractRemoteDatabaseDescriptor?
                return FormValidation.ok("OK");
            } catch (SQLException e) {
                return FormValidation.error(e,"Failed to connect");
            }
        }
    }
}
