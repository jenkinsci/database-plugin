package org.jenkinsci.plugins.database;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * {@link Descriptor} for {@link PerItemDatabase}.
 */
public abstract class PerItemDatabaseDescriptor extends Descriptor<PerItemDatabase> {

    protected PerItemDatabaseDescriptor() {}

    protected PerItemDatabaseDescriptor(Class<? extends PerItemDatabase> clazz) {
        super(clazz);
    }

    public static DescriptorExtensionList<PerItemDatabase,PerItemDatabaseDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(PerItemDatabase.class);
    }

}
