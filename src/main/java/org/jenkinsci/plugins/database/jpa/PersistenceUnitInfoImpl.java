package org.jenkinsci.plugins.database.jpa;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Kohsuke Kawaguchi
 */
class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

    private DataSource dataSource;

    private List<Class> classes;
    private ClassLoader classLoader;

    public PersistenceUnitInfoImpl(DataSource dataSource, List<Class> classes, ClassLoader classLoader) {
        this.dataSource = dataSource;
        this.classes = classes;
        this.classLoader = classLoader;
    }

    public String getPersistenceUnitName() {
        return "TODO";  // ???
    }

    public String getPersistenceProviderClassName() {
        return null; // ???
    }

    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }

    public DataSource getJtaDataSource() {
        return null;
    }

    public DataSource getNonJtaDataSource() {
        return dataSource;
    }

    public List<String> getMappingFileNames() {
        return Collections.emptyList();
    }

    public List<URL> getJarFileUrls() {
        return Collections.emptyList();
    }

    public URL getPersistenceUnitRootUrl() {
        return null;    // prevent Hibernate from trying to auto discover classes as it won't work

//        try {
//            return new URL("file:///tmp/no-such-file");
//        } catch (MalformedURLException e) {
//            throw new Error(e);
//        }
    }

    public List<String> getManagedClassNames() {
        return new AbstractList<String>() {
            @Override
            public String get(int index) {
                return classes.get(index).getName();
            }

            @Override
            public int size() {
                return classes.size();
            }
        };
    }

    public boolean excludeUnlistedClasses() {
        return true;
    }

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.hbm2ddl.auto","update");
        return properties;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void addTransformer(ClassTransformer transformer) {
        throw new UnsupportedOperationException();
    }

    public ClassLoader getNewTempClassLoader() {
        return classLoader;
    }

    public SharedCacheMode getSharedCacheMode() {
        return SharedCacheMode.UNSPECIFIED;
    }

    public ValidationMode getValidationMode() {
        return ValidationMode.AUTO;
    }

    public String getPersistenceXMLSchemaVersion() {
        return null;
    }
}
