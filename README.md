# Database Plugin

This library plugin defines a common abstraction to connect to
relational database. By doing so, it serves three purposes:

-   It allows database driver plugins (such as [H2 Database
    Plugin](https://plugins.jenkins.io/database-h2/))
    to be developed, improving the user experience for administrators.
-   It simplifies other plugins that use RDBMS by eliminating the need
    to code up a configuration UI to let administrators select database.

**This plugin is not meant to be used by end users by itself.** It's
supposed to be included through the dependencies of other plugins. If
you are wondering how to store your job configuration etc. in a
database, the answer is that you cannot.

## Usage

### Abstraction

The key class in this plugin is the
[Database](https://github.com/jenkinsci/database-plugin/blob/master/src/main/java/org/jenkinsci/plugins/database/Database.java)
class, which acts as a holder for JDBC `DataSource` instance. The
`Database` class is an extension point to be implemented by database
driver plugins.

The calling code can use this `DataSource` instance to obtain a
connection to the database.

### Use the Jenkins global database

This plugin adds a system configuration entry to let the administrator
configure the database used by Jenkins to store miscellaneous stuff. So
the easiest way for plugins to start storing data to the database is to
use this `Database` instance. This global database instance is kept in
the `GlobalDatabaseConfiguration` class. You can inject this via
`@Inject`, or you can call `GlobalDatabaseConfiguration.get()` to
retrieve it, and then use the `getDatabase()` method to obtain the
`Database` instance.

Because the database is shared by all the plugins, please use table
names that include your plugin name as a prefix to avoid collisions.

### Use the per-job database

In addition to the global database, this plugin also creates a database
local to
[TopLevelItem](https://javadoc.jenkins-ci.org/byShortName/TopLevelItem).
These databases are normally backed by embedded database that stores
data under `$JENKINS_HOME/jobs/NAME`, and this simplifies the backup,
copying, deletion of the data that's local to jobs (such as test
reports, coverage data, and so on.)

This information is kept in the `PerItemDatabase` class, which you can
obtain by `PerItemDatabaseConfiguration.findOrNull()`.

### Your own database

Sometimes it makes sense to store data to an entirely different
database. Users may already have a database with data in it, in which case he'd want to
just connect to that.

A plugin that wants to do this should define a field whose type is
`Database` (see
[example](https://github.com/jenkinsci/database-plugin/blob/master/src/test/java/org/jenkinsci/plugins/database/Sample.java)).
In the config.{groovy,jelly}, use the `f:dropdownDescriptorSelector` tag
to allow the user to select a database (see
[example](https://github.com/jenkinsci/database-plugin/blob/master/src/test/resources/org/jenkinsci/plugins/database/Sample/config.groovy)).

### JPA support

This plugin exposes it through [JPA 2.0
API](https://en.wikipedia.org/wiki/Java_Persistence_API) (internally, it
uses Hibernate but please do not rely on this fact if you can as it may
change.) The entry point to the JPA support is the `PersistenceService`
class, and this exposes methods for obtaining `EntityManagerFactory` for
both the global database as well as arbitrary per-item database of your
choice.

Because there are several different databases, involved, `@Entity`
annotation alone is not sufficient. For persisted classes meant for the
global database, please put `@GlobalTable` in addition to `@Entity`.

The following code shows how to use this to persiste a new row:

```java
public class Push {
    @Inject
    PersistenceService ps;

    public void go(int n) throws IOException, SQLException {
        Jenkins.getInstance().getInjector().injectMembers(this);
        EntityManager em = ps.getGlobalEntityManagerFactory().createEntityManager();
        em.getTransaction().begin();
        TestRow row = new TestRow();
        row.buildNumber = n;
        row.x = "foo";
        em.persist(row);
        em.getTransaction().commit();
        em.close();
    }
}

@GlobalTable
@Entity
public class TestRow {
    @Id
    @Column
    public int buildNumber;

    @Column
    public String x;
}
```

## Developing driver plugin

[MySQL Database
plugin](https://plugins.jenkins.io/database-mysql/)
and [PostgreSQL Database
plugin](https://plugins.jenkins.io/database-postgresql/)
are good examples of typical database driver plugins. For other "unusual"
drivers that doesn't use the canonical
host+database+username+password+properties combo, see [H2 Database
plugin](https://plugins.jenkins.io/database-h2/)
source code as an example.

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
