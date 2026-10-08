package org.jenkinsci.plugins.database;

import hudson.util.Secret;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractRemoteDatabaseTest {

    private H2Database database;

    @AfterEach
    void tearDown() throws SQLException {
        if (database != null) {
            database.setMaxIdleConnections(0);
        }
    }

    @Test
    void setMaxIdleConnectionsClosesAlreadyIdleConnections() throws SQLException {
        database = new H2Database();

        // open and return several connections to the pool so they sit there idle
        // open 5 connections concurrently so the pool actually creates 5 distinct physical
        // connections, then return them all so they sit idle
        Connection[] connections = new Connection[5];
        for (int i = 0; i < connections.length; i++) {
            connections[i] = database.getDataSource().getConnection();
            assertTrue(connections[i].isValid(1));
        }
        for (Connection c : connections) {
            c.close();
        }
        assertEquals(5, database.dataSourceFactory.getNumIdle());

        database.setMaxIdleConnections(1);

        // clear() closes every connection that was already idle under the old limit, immediately
        assertEquals(0, database.dataSourceFactory.getNumIdle());
        assertEquals(1, database.dataSourceFactory.getMaxIdle());

        // going forward only 1 connection is kept idle, even if more are returned at once
        Connection[] moreConnections = new Connection[3];
        for (int i = 0; i < moreConnections.length; i++) {
            moreConnections[i] = database.getDataSource().getConnection();
        }
        for (Connection c : moreConnections) {
            c.close();
        }
        assertEquals(1, database.dataSourceFactory.getNumIdle());
    }

    @Test
    void setMaxIdleConnectionsIsNoOpOnDefaultDatabaseImplementation() throws SQLException {
        // the base Database no-op must not throw, even though it has no pool to configure
        Database noPoolDatabase = new Database() {
            @Override
            public javax.sql.DataSource getDataSource() {
                return null;
            }
        };
        noPoolDatabase.setMaxIdleConnections(0);
    }

    /**
     * Minimal concrete {@link AbstractRemoteDatabase} backed by an isolated in-memory H2 database,
     * purely so the pool-management behavior in the abstract base class can be exercised directly.
     */
    private static class H2Database extends AbstractRemoteDatabase {
        private final String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

        H2Database() {
            super("localhost", "test", "sa", Secret.fromString(""), null);
        }

        @Override
        protected Class<? extends Driver> getDriverClass() {
            return org.h2.Driver.class;
        }

        @Override
        protected String getJdbcUrl() {
            return jdbcUrl;
        }
    }
}
