package org.jenkinsci.plugins.database;

import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.jvnet.hudson.test.HudsonHomeLoader;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.MetaClass;

import java.io.File;

import static hudson.Main.*;

/**
 * Sample Main program. Run this and use http://localhost:8888/
 *
 * @author Kohsuke Kawaguchi
 */
public class Main extends HudsonTestCase {
    @Override
    protected void setUp() throws Exception {
        homeLoader = new HudsonHomeLoader() {
            public File allocate() throws Exception {
                return new File("./work");
            }
        };
        super.setUp();
    }

    public void test1() throws Exception {
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(localPort=8888);
        HttpConfiguration config = connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        // use a bigger buffer as Stapler traces can get pretty large on deeply nested URL
        config.setRequestHeaderSize(12 * 1024);

        server.addConnector(connector);
        connector.start();

        interactiveBreak();
    }

    public static void main(String[] args) {
        Main m = new Main();
        m.setName("test1");
        m.run();
    }

    static {
        System.setProperty("stapler.resourcePath","src/main/resources;src/test/resources");
        MetaClass.NO_CACHE = true;
        isUnitTest = false;
    }
}
