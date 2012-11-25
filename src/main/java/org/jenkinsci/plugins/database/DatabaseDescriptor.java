package org.jenkinsci.plugins.database;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

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

    public static DescriptorExtensionList<Database,DatabaseDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(Database.class);
    }
}
