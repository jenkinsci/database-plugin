package org.jenkinsci.plugins.database.jpa;

import hudson.Extension;
import jenkins.model.Jenkins;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.jenkinsci.plugins.database.Database;
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration;
import org.jvnet.hudson.annotation_indexer.Index;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Creates {@link EntityManagerFactory}.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class PersistenceService {
    @Inject
    private Jenkins jenkins;

    @Inject
    GlobalDatabaseConfiguration globalDatabaseConfiguration;

    private final AtomicReference<EMFCache<Database>> globalCache = new AtomicReference<>();

    public EntityManagerFactory createEntityManagerFactory(DataSource dataSource, List<Class> classes) {
        return new HibernatePersistenceProvider().createContainerEntityManagerFactory(new PersistenceUnitInfoImpl(
                dataSource, classes, jenkins.pluginManager.uberClassLoader), null);
    }

    /**
     * Obtains a fully configured {@link EntityManagerFactory} for connecting
     * to {@linkplain GlobalDatabaseConfiguration global database}.
     *
     * @return
     *      null if there's no global database configured yet.
     */
    @CheckForNull
    public EntityManagerFactory getGlobalEntityManagerFactory() throws SQLException, IOException {
        Database db = globalDatabaseConfiguration.getDatabase();
        if (db==null) {
            close(globalCache.getAndSet(null));
            return null;
        }

        EMFCache<Database> c = globalCache.get();
        if (c==null || c.cacheKey !=db) {
            List<Class> classes = new ArrayList<Class>();
            for (Class cls : Index.list(GlobalTable.class,jenkins.pluginManager.uberClassLoader,Class.class))
                classes.add(cls);
            // set the new one, and close the old one if any
            close(globalCache.getAndSet(c = new EMFCache<Database>(createEntityManagerFactory(db.getDataSource(), classes), db)));
        }
        return c.factory;
    }

    private void close(EMFCache<?> v) {
        if (v!=null)
            v.factory.close();
    }

    /**
     * For atomic update for {@link EntityManagerFactory}.
     */
    static class EMFCache<K> {
        final K cacheKey;
        final EntityManagerFactory factory;

        EMFCache(EntityManagerFactory factory, K cacheKey) {
            this.factory = factory;
            this.cacheKey = cacheKey;
        }
    }
}
