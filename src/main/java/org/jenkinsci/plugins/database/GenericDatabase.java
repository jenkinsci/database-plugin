package org.jenkinsci.plugins.database;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.tools.ant.AntClassLoader;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.File;
import java.sql.SQLException;
import org.kohsuke.stapler.verb.POST;

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
    public final int initialSize;
    public final int maxTotal;
    public final int maxIdle;
    public final int minIdle;

    private transient DataSource source;

    @DataBoundConstructor
    public GenericDatabase(String url, String driver, String username, Secret password,
                           int initialSize, int maxTotal, int maxIdle, int minIdle) {
        this.url = url;
        this.driver = driver;
        this.username = username;
        this.password = password;
        this.initialSize = initialSize;
        this.maxTotal = maxTotal;
        this.maxIdle = maxIdle;
        this.minIdle = minIdle;
    }

    @Override
    public synchronized DataSource getDataSource() throws SQLException {
        if (source==null) {
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
            this.source = source.createDataSource();
        }
        return source;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension(ordinal = -1000) // low priority because this is generic
    public static class DescriptorImpl extends DatabaseDescriptor {
        private transient AntClassLoader loader;

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
                                         @QueryParameter Secret password,
                                         @QueryParameter int initialSize,
                                         @QueryParameter int maxTotal,
                                         @QueryParameter int maxIdle,
                                         @QueryParameter int minIdle) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            
            try {
                new GenericDatabase(url,driver,username, password, initialSize, maxTotal, maxIdle, minIdle).getDataSource();
                // XXX what about the "SELECT 1" trick from AbstractRemoteDatabaseDescriptor?
                return FormValidation.ok("OK");
            } catch (SQLException e) {
                return FormValidation.error(e,"Failed to connect");
            }
        }
    }
}
