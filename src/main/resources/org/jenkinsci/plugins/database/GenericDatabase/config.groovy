package org.jenkinsci.plugins.database.GenericDatabase

def f = namespace(lib.FormTagLib)

f.entry(field:"driver",title:_("JDBC Driver Class")) {
    f.textbox()
}
f.entry(field:"url",title:_("JDBC Connection URL")) {
    f.textbox()
}
f.entry(field:"username",title:_("User Name")) {
    f.textbox()
}
f.entry(field:"password",title:_("Password")) {
    f.password()
}
f.advanced {
    f.entry(field: "initialSize", title: _("Initial Size")) {
        f.number(clazz: "number", min: 0, max: 65535, step: 1, default: "${descriptor.defaultInitialSize}")
    }
    f.entry(field: "maxTotal", title: _("Max Total")) {
        f.number(clazz: "number", min: -1, max: 65535, step: 1, default: "${descriptor.defaultMaxTotal}")
    }
    f.entry(field: "maxIdle", title: _("Max Idle")) {
        f.number(clazz: "number", min: -1, max: 65535, step: 1, default: "${descriptor.defaultMaxIdle}")
    }
    f.entry(field: "minIdle", title: _("Min Idle")) {
        f.number(clazz: "number", min: 0, max: 65535, step: 1, default: "${descriptor.defaultMinIdle}")
    }
}
f.block() {
    f.validateButton(method:"validate",title:_("Test Connection"),with:"driver,url,username,password")
}