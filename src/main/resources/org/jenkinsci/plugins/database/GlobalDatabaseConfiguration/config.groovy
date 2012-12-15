package org.jenkinsci.plugins.database.GlobalDatabaseConfiguration

import org.jenkinsci.plugins.database.NoDatabase

def f = namespace(lib.FormTagLib)

f.section(title:_("Global Database")) {
    f.dropdownDescriptorSelector(field:"database",title:_("Database"),descriptors:NoDatabase.allPlusNone())
}
