package org.jenkinsci.plugins.database.AbstractRemoteDatabase;

def f = namespace(lib.FormTagLib)

f.entry(field:"hostname", title:_("Host Name")) {
    f.textbox()
}
f.entry(field:"database", title:_("Database")) {
    f.textbox()
}
f.entry(field:"username", title:_("Username")) {
    f.textbox()
}
f.entry(field:"password", title:_("Password")) {
    f.password()
}
f.advanced {
    f.entry(field:"properties", title:_("Additional Properties")) {
        f.textarea()
    }
}
f.block {
    f.validateButton(method:"validate",title:_("Test Connection"),with:"hostname,database,username,password,properties")
}