package org.quickcached.protocol;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import junit.framework.TestCase;

import org.quickcached.client.*;

/**
 *
 * @author akshath
 */
public class ProtocolTest extends TestCase  {
	protected MemcachedClient c = null;
	public static String server = "127.0.0.1:11211"//"192.168.1.2:11211 192.168.1.2:11212";

	public ProtocolTest(String name) {
        super(name);
    }
	
	public void testGet() throws TimeoutException, MemcachedException {
		String readObject = null;
		String key = null;
		
		//1 - String
		key = "testget1";
		c.set(key, 3600, "World");
		readObject = (String) c.get(key);
		
		assertNotNull(readObject);
		assertEquals("World",  readObject);
        
        
		//3 - native obj
        Date value = new Date();
		key = "testget3";
		c.set(key, 3600, value);
		Date readObjectDate = (Date) c.get(key);

		assertNotNull(readObjectDate);
		assertEquals(value.getTime(),  readObjectDate.getTime());
		
		//3 - custom obj
        TestObject testObject = new TestObject();
		key = "testget4";
		testObject.setName("TestName");
		c.set(key, 3600, testObject);
		TestObject readTestObject = (TestObject) c.get(key);
		
		assertNotNull(readTestObject);
		assertEquals("TestName",  readTestObject.getName());
		
		//4 - no reply
		Object client = c.getBaseClient();
		key = "testget5";
        
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
			
			try {
				xmc.setWithNoReply(key, 3600, "World");			
			} catch (InterruptedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			readObject = (String) c.get(key);
			assertNotNull(readObject);
			assertEquals("World",  readObject);
		} else if (client instanceof net.spy.memcached.MemcachedClient) {
			net.spy.memcached.MemcachedClient smc = (net.spy.memcached.MemcachedClient) client;
			//does not support noreply
		}		
	}
	
	public void testGets() throws TimeoutException, MemcachedException {
		String readObject = null;
		String key = "testGetCAS";
		String value = "Value";
		
		long casVal = 1; //default cas value is 1, and increments on each set
		
		c.set(key, 3600, value);
		CASValue casResult = c.gets(key, 1000);
		
		assertNotNull(casResult);
		assertEquals(casResult.getValue(), value);
		assertEquals(casResult.getCas(), casVal);
		
		casVal++;
		value = "Value2";
		
		c.set(key, 3600, value);
		CASValue casResult2 = c.gets(key, 1000);
		
		assertNotNull(casResult2);
		assertEquals(casResult2.getValue(), value);
		assertEquals(casResult2.getCas(), casVal);
	}
	
	public void testCAS() throws TimeoutException, MemcachedException {
		String key = "testcas1";		
		
		Object client = c.getBaseClient();
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			//ERROR
			CASResponse testRslt1 = c.cas(key, "value", 3000, 1);
			assertNotNull(testRslt1);
			assertEquals(CASResponse.ERROR,  testRslt1);

			String result = (String)c.get(key);
			assertNull(result);
		} else if (client instanceof org.quickcached.client.impl.QuickCachedClientImpl) {			
			//NOT_FOUND
			CASResponse testRslt1 = c.cas(key, "value", 3000, 1);
			assertNotNull(testRslt1);
			assertEquals(CASResponse.NOT_FOUND,  testRslt1);

			String result = (String)c.get(key);
			assertNull(result);
		}
		
		c.set(key, 3000, "value");
		//Default value of cas is 1
		CASResponse testRslt2 = c.cas(key, "value2", 3000, 1);
		assertNotNull(testRslt2);
		assertEquals(CASResponse.OK,  testRslt2);
		
		//Increment the cas, it should match the current cas, so saving current CAS
		CASResponse testRslt3 = c.cas(key, "value2", 3000, 2);
		assertNotNull(testRslt3);
		assertEquals(CASResponse.OK,  testRslt3);
		
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			//Increment the cas, passing wrong CAS value, expecting rslt as "ERROR"
			CASResponse testRslt4 = c.cas(key, "value3", 3000, 6);
			assertNotNull(testRslt4);
			assertEquals(CASResponse.ERROR,  testRslt4);
		} else if (client instanceof org.quickcached.client.impl.QuickCachedClientImpl) {			
			//Increment the cas, passing wrong CAS value, expecting rslt as "EXISTS"
			CASResponse testRslt4 = c.cas(key, "value3", 3000, 6);
			assertNotNull(testRslt4);
			assertEquals(CASResponse.EXISTS,  testRslt4);
		}
	}
	
	public void testAppend() throws TimeoutException, MemcachedException {
		String readObject = null;
		String key = null;
		
		//1
		key = "testapp1";
		c.set(key, 3600, "ABCD");

		c.append(key, "EFGH");
		readObject = (String) c.get(key);

		assertNotNull(readObject);
		assertEquals("ABCDEFGH", readObject);
		
		//2
		Object client = c.getBaseClient();
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
			key = "testapp1";
			try {
				xmc.appendWithNoReply(key,"XYZ");				
			} catch (InterruptedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			readObject = (String) c.get(key);
			assertNotNull(readObject);
			assertEquals("ABCDEFGHXYZ", readObject);
		} else if (client instanceof net.spy.memcached.MemcachedClient) {			
			//does not support noreply			
		}		
	}

	public void testPrepend() throws TimeoutException, MemcachedException {
		String readObject = null;
		String key = null;
		String value = null;
		
		key = "testpre1";

		//1
		c.set(key, 3600, "ABCD");
		c.prepend(key, "EFGH");
		readObject = (String) c.get(key);

		assertNotNull(readObject);
		assertEquals("EFGHABCD", readObject);
		
		//2
		Object client = c.getBaseClient();
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
			key = "testpre1";
			try {
				xmc.prependWithNoReply(key,"XYZ");				
			} catch (InterruptedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			readObject = (String) c.get(key);
			assertNotNull(readObject);
			assertEquals("XYZEFGHABCD", readObject);
		} else if (client instanceof net.spy.memcached.MemcachedClient) {			
			//does not support noreply			
		}
	}

	public void testAdd() throws TimeoutException, MemcachedException {
		String readObject = null;
		String key = null;
		String value = null;
		
		//1
        value = "ABCD";
		key = "testadd1";

		c.delete(key);
		boolean flag = c.add(key, 3600, value);
		assertTrue(flag);
		
		readObject = (String) c.get(key);
		assertNotNull(readObject);
		assertEquals("ABCD", readObject);

		
		flag = c.add(key, 3600, "XYZ");
		assertFalse(flag);
		
		//read old value
		readObject = (String) c.get(key);
		assertNotNull(readObject);
		assertEquals("ABCD", readObject);
		
		//2
		key = "testadd2";
		c.delete(key);
		Object client = c.getBaseClient();
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
			try {
				xmc.addWithNoReply(key, 3600, value);
			} catch (InterruptedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			readObject = (String) c.get(key);
			assertNotNull(readObject);
			assertEquals("ABCD", readObject);

			flag = c.add(key, 3600, "XYZ");
			assertFalse(flag);

			//read old value
			readObject = (String) c.get(key);
			assertNotNull(readObject);
			assertEquals("ABCD", readObject);
		} else if (client instanceof net.spy.memcached.MemcachedClient) {			
			//does not support noreply			
		}
		
	}

	public void testReplace() throws TimeoutException, MemcachedException {
		String readObject = null;
		String key = null;
		String value = null;
		
		//1
        value = "ABCD";
		key = "testrep1";

		c.set(key, 3600, "World");

		boolean flag = c.replace(key, 3600, value);
		assertTrue(flag);
		readObject = (String) c.get(key);
		assertNotNull(readObject);
		assertEquals("ABCD", readObject);

		c.delete(key);
		
		flag = c.replace(key, 3600, "XYZ");
		assertFalse(flag);
		
		//read old value i.e. no value
		readObject = (String) c.get(key);
		assertNull(readObject);
		
		//2
		key = "testrep2";
		c.set(key, 3600, "World");
		
		Object client = c.getBaseClient();
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
			try {
				xmc.replaceWithNoReply(key, 3600, value);
			} catch (InterruptedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			readObject = (String) c.get(key);
			assertNotNull(readObject);
			assertEquals("ABCD", readObject);
		} else if (client instanceof net.spy.memcached.MemcachedClient) {			
			//does not support noreply			
		}		
		
		c.delete(key);		
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
			try {
				xmc.replaceWithNoReply(key, 3600, "XYZ");
			} catch (InterruptedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			//read old value i.e. no value
			readObject = (String) c.get(key);
			assertNull(readObject);	
		} else if (client instanceof net.spy.memcached.MemcachedClient) {			
			//does not support noreply			
		}			
	}

	public void testIncrement() throws TimeoutException, org.quickcached.client.MemcachedException {
		String readObject = null;
		String key = null;
		String randomKey = null;
		String value = null;
		Long currentValue = null;
		Long defaultValue = 410L;
	
		key = "testinc3";
        value = "10";				
		
		c.set(key, 3600, value);
		c.increment(key, 10);
		
		readObject = (String) c.get(key);
		assertNotNull(readObject);
		assertEquals("20", readObject);

		c.increment(key, 1);
		readObject = (String) c.get(key);
		assertNotNull(readObject);
		assertEquals("21", readObject);
		
		//2
		Object client = c.getBaseClient();
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient){
			net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
			try {
				xmc.incrWithNoReply(key, 4);
			} catch (InterruptedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			readObject = (String) c.get(key);
			assertNotNull(readObject);
			assertEquals("25", readObject);
		}else if (client instanceof net.spy.memcached.MemcachedClient){
			//does not support noreply
		}
		
	}

	public void testDecrement() throws TimeoutException, org.quickcached.client.MemcachedException {
		String readObject = null;
		String key = null;
		String value = null;
		
        //1
		key = "testdec1";
        value = "25";		

		c.set(key, 3600, value);
		c.decrement(key, 10);

		readObject = (String) c.get(key);
		assertNotNull(readObject);
		assertEquals("15", readObject);

		c.decrement(key, 1);
		readObject = (String) c.get(key);
		assertNotNull(readObject);
		assertEquals("14", readObject);
		
		//2
		Object client = c.getBaseClient();
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
			try {
				xmc.decrWithNoReply(key, 4);
			} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			} catch (InterruptedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			readObject = (String) c.get(key);
			assertNotNull(readObject);
			assertEquals("10", readObject);
		} else if (client instanceof net.spy.memcached.MemcachedClient) {			
			//does not support noreply
		}		
	}

	public void testDelete() throws TimeoutException, MemcachedException {
		String readObject = null;
		String key = null;
		String value = null;
		
		//1
		key = "testdel1";
		
		c.set(key, 3600, "World");
		readObject = (String) c.get(key);

		assertNotNull(readObject);
		assertEquals("World",  readObject);

		c.delete(key);
		readObject = (String) c.get(key);
		assertNull(readObject);
		
		//2
		c.set(key, 3600, "World");
		readObject = (String) c.get(key);

		assertNotNull(readObject);
		assertEquals("World",  readObject);
		Object client = c.getBaseClient();
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
			try {
				xmc.deleteWithNoReply(key);
			} catch (InterruptedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			readObject = (String) c.get(key);
			assertNull(readObject);
		} else if (client instanceof net.spy.memcached.MemcachedClient) {			
			//does not support noreply
		}
		
	}

	public void testVersion() throws TimeoutException {
		Map ver = c.getVersions();
		assertNotNull(ver);
		//System.out.println("ver: "+ver);
		Iterator iterator = ver.keySet().iterator();
		InetSocketAddress key = null;
		while(iterator.hasNext()) {
			key = (InetSocketAddress) iterator.next();
			assertNotNull(key);
			//assertEquals("1.4.5",  (String) ver.get(key));
		}
	}

	public void testStats() throws Exception {
		Map stats = c.getStats();
		assertNotNull(stats);

		Iterator iterator = stats.keySet().iterator();
		InetSocketAddress key = null;
		while(iterator.hasNext()) {
			key = (InetSocketAddress) iterator.next();
			assertNotNull(key);
			//System.out.println("Stat for "+key+" " +stats.get(key));
		}
	}
	
	public void testTouch() throws TimeoutException {
		Object client = c.getBaseClient();
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			//Not supported touch in text protocol
		} else if (client instanceof org.quickcached.client.impl.QuickCachedClientImpl) {			
			String key = null;
			String value = null;

			//1
			value = "ABCD";
			key = "testtouch";

			c.set(key, 3600, "World");

			boolean flag = c.touch(key, 3600);
			assertTrue(flag);
		}
	}

	public void testFlush() throws TimeoutException, MemcachedException {
		String readObject = null;
		String key = null;
		String value = null;
		
		//1
		key = "testflush1";
		c.set(key, 3600, "World");
		readObject = (String) c.get(key);

		assertNotNull(readObject);
		assertEquals("World",  readObject);

		c.flushAll();

		readObject = (String) c.get(key);
		assertNull(readObject);
		
		//2
		c.set(key, 3600, "World");
		readObject = (String) c.get(key);

		assertNotNull(readObject);
		assertEquals("World",  readObject);
		
		Object client = c.getBaseClient();
		if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
			net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
			try {
				xmc.flushAllWithNoReply();
			} catch (InterruptedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
				Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			readObject = (String) c.get(key);
			assertNull(readObject);
		} else if (client instanceof net.spy.memcached.MemcachedClient) {			
			//does not support noreply
		}
		
	}
	
	public void testDoubleSet1() throws TimeoutException {
		try {
			String readObject = null;
			String key = null;
			String value = null;

			//1
			key = "testdset1";
			value = "v1";
			c.set(key, 3600, value);
			readObject = (String) c.get(key);

			assertNotNull(readObject);
			assertEquals("v1",  readObject);

			value = "v2";
			c.set(key, 3600, value);
			readObject = (String) c.get(key);

			assertNotNull(readObject);
			assertEquals("v2",  readObject);

			//2
			key = "testdset2";
			Map valuemap = new HashMap();
			valuemap.put("key1", "v1");

			c.set(key, 3600, valuemap);
			Map readObjectMap = (Map) c.get(key);

			assertNotNull(readObjectMap);
			assertEquals(valuemap,  readObjectMap);

			valuemap.put("key2", "v2");
			c.set(key, 3600, valuemap);
			readObjectMap = (Map) c.get(key);

			assertNotNull(readObjectMap);
			assertEquals(valuemap,  readObjectMap);

			//3
			valuemap.put("key2", "v3");
			Object client = c.getBaseClient();
			if(client instanceof net.rubyeye.xmemcached.MemcachedClient) {
				net.rubyeye.xmemcached.MemcachedClient xmc = (net.rubyeye.xmemcached.MemcachedClient) client;
				try {
					xmc.setWithNoReply(key, 3600, valuemap);
				} catch (InterruptedException ex) {
					Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
				} catch (net.rubyeye.xmemcached.exception.MemcachedException ex) {
					Logger.getLogger(ProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
				}

				readObjectMap = (Map) c.get(key);

				assertNotNull(readObjectMap);
				assertEquals(valuemap,  readObjectMap);
			} else if (client instanceof net.spy.memcached.MemcachedClient) {			
				//does not support noreply
			}
		} catch (MemcachedException e) {
			throw new TimeoutException("Memcached exception");
		}
		
		
	}

	
}
