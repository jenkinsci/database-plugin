package org.jenkinsci.plugins.database;

import org.jvnet.hudson.test.HudsonHomeLoader;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.MetaClass;
import org.mortbay.jetty.bio.SocketConnector;

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
        SocketConnector connector = new SocketConnector();
        connector.setPort(localPort=8888);
        connector.setHeaderBufferSize(12 * 1024); // use a bigger buffer as Stapler traces can get pretty large on deeply nested URL
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
