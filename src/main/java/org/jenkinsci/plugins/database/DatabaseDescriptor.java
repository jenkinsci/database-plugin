package org.jenkinsci.plugins.database;

import hudson.model.Descriptor;

/**
 * {@link Descriptor} for {@link Database}
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class DatabaseDescriptor extends Descriptor<Database> {
    protected DatabaseDescriptor() {
    }

    protected DatabaseDescriptor(Class<? extends Database> clazz) {
        super(clazz);
    }
}
