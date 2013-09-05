package org.jenkinsci.plugins.database.jpa;

import com.thoughtworks.xstream.core.util.ConcurrentWeakHashMap;
import hudson.Extension;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import org.hibernate.ejb.HibernatePersistence;
import org.jenkinsci.plugins.database.Database;
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration;
import org.jenkinsci.plugins.database.PerItemDatabase;
import org.jenkinsci.plugins.database.PerItemDatabaseConfiguration;
import org.jvnet.hudson.annotation_indexer.Index;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
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

    @Inject
    PerItemDatabaseConfiguration perItemDatabaseConfiguration;

    private final AtomicReference<EMFCache<Database>> globalCache = new AtomicReference<EMFCache<Database>>();
    private final ConcurrentMap<TopLevelItem,EMFCache<PerItemDatabase>> perItemCache =
            new ConcurrentWeakHashMap<TopLevelItem, EMFCache<PerItemDatabase>>();

    public EntityManagerFactory createEntityManagerFactory(DataSource dataSource, List<Class> classes) {
        return new HibernatePersistence().createContainerEntityManagerFactory(new PersistenceUnitInfoImpl(
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

    /**
     * Obtains a fully configured {@link EntityManagerFactory} for connecting
     * to {@linkplain PerItemDatabase per-item database} of the specific item.
     *
     * @return
     *      null if there's no global database configured yet.
     */
    @CheckForNull
    public EntityManagerFactory getPerItemEntityManagerFactory(TopLevelItem item) throws SQLException, IOException {
        PerItemDatabase db = perItemDatabaseConfiguration.getDatabase();
        if (db==null) {
            close(perItemCache.get(item));
            return null;
        }

        EMFCache<PerItemDatabase> c = perItemCache.get(item);
        if (c==null || c.cacheKey!=db) {
            List<Class> classes = new ArrayList<Class>();
            for (Class cls : Index.list(PerItemTable.class,jenkins.pluginManager.uberClassLoader,Class.class))
                classes.add(cls);
            // set the new one, and close the old one if any
            close(perItemCache.put(item, c = new EMFCache<PerItemDatabase>(
                    createEntityManagerFactory(db.getDataSource(item), classes), db)));
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
    class EMFCache<K> {
        final K cacheKey;
        final EntityManagerFactory factory;

        EMFCache(EntityManagerFactory factory, K cacheKey) {
            this.factory = factory;
            this.cacheKey = cacheKey;
        }
    }
}
