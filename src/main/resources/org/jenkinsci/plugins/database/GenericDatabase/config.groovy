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
f.block() {
    f.validateButton(method:"validate",title:_("Test Connection"),with:"driver,url,username,password")
}