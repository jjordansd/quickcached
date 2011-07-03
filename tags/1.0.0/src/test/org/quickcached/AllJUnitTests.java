package org.quickcached;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.quickcached.protocol.*;

/**
 * Simple class to build a TestSuite out of the individual test classes.
 */
public class AllJUnitTests extends TestCase {

    public AllJUnitTests(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(SkeletonTest.class);

		//suite.addTest(new TestSuite(StatVerTest.class));
        suite.addTest(new TestSuite(TextProtocolTest.class));
		suite.addTest(new TestSuite(BinaryProtocolTest.class));
        return suite;
   }
}