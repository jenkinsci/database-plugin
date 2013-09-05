package org.jenkinsci.plugins.database.jpa;

import hudson.model.TopLevelItem;
import org.jenkinsci.plugins.database.GlobalDatabaseConfiguration;
import org.jvnet.hudson.annotation_indexer.Indexed;

import javax.persistence.Entity;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks JPA {@link Entity} classes that are targeted for
 * {@linkplain GlobalDatabaseConfiguration Jenkins global database}.
 *
 * @author Kohsuke Kawaguchi
 * @see PersistenceService#getPerItemEntityManagerFactory(TopLevelItem)
 */
@Indexed
@Retention(RUNTIME)
@Target({TYPE})
@Documented
public @interface PerItemTable {
}
