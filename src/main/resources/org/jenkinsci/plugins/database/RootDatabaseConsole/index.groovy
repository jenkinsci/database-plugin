package org.jenkinsci.plugins.database.RootDatabaseConsole

import java.sql.ResultSet;

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)

// XXX require RUN_SCRIPTS
l.layout{
    l.main_panel {
        form(method:"post",action:"execute") {
            raw("""
<h2>SQL</h2>
<textarea name=sql style='width:100%; height:5em'></textarea>
            """)
            div {
                f.submit(value:"Execute")
            }
        }

        if (request2.getAttribute("message")!=null) {
            p(message)
        }

        if (request2.getAttribute("r")!=null) {
            // renders the result
            h2("Result")
            table {
                ResultSet rs = r;
                int count = rs.metaData.columnCount;

                tr {
                    for (int i=1; i<=count; i++) {
                        th { rs.metaData.getColumnLabel(i) }
                    }
                }

                while (rs.next()) {
                    tr {
                        for (int i=1; i<=count; i++) {
                            td(rs.getString(i))
                        }
                    }
                }
            }
        }
    }
}
