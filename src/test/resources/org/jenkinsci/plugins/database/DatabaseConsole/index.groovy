package org.jenkinsci.plugins.database.DatabaseConsole

import java.sql.ResultSet;

def f = namespace(lib.FormTagLib)
def l = namespace(lib.LayoutTagLib)

l.layout{
    l.main_panel {
        form(method:"post",action:"execute") {
            raw("""
<p>
    Go to <a href="../configure">the system config page</a> and configure
    a valid database connection. Also try installing some database driver plugins,
    such as <tt>database-h2</tt>.
</p><p>
    Then come back to this console and execute arbitrary SQL against the configured database.
</p>
<h2>SQL</h2>
<textarea name=sql style='width:100%; height:5em'></textarea>
            """)
            div {
                f.submit(value:"Execute")
            }
        }

        if (request.getAttribute("message")!=null) {
            p(message)
        }

        if (request.getAttribute("r")!=null) {
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

