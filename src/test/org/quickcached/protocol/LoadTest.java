package org.quickcached.protocol;

import java.io.IOException;
import java.util.logging.*;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.BinaryConnectionFactory;
/**
 *
 * @author akshath
 */
public class LoadTest {
	private MemcachedClient c = null;
	private int count;
	private String name;

	public static void main(String args[]) {
		int txn = 100000;
		int threads = 10;
		//doSingleThreadTest(txn);
		doMultiThreadTest(txn, threads);
    }

	public static void doSingleThreadTest(int txn) {
		LoadTest ltu = new LoadTest("Test1", txn);
		ltu.setUp();
		long stime = System.currentTimeMillis();
        ltu.test1();
		long etime = System.currentTimeMillis();

		float ttime = etime-stime;
		double atime = ttime/txn;
		ltu.tearDown();
		System.out.println("Total Time for "+txn+" txn was "+ttime);
		System.out.println("Avg Time for "+txn+" txn was "+atime);
	}

	public static void doMultiThreadTest(int txn, int threads) {
		int eachUnitCount = txn/threads;

		final LoadTest ltu[] = new LoadTest[threads];
		for(int i=0;i<threads;i++) {
			ltu[i] = new LoadTest("Test-"+i, eachUnitCount);
			ltu[i].setUp();
		}

		Thread threadPool[] = new Thread[threads];
		for(int i=0;i<threads;i++) {
			final int myi = i;
			threadPool[i] = new Thread() {
				public void run() {
					ltu[myi].test1();
				}
			};
		}

		System.out.println("Starting....");
		System.out.println("=============");
		long stime = System.currentTimeMillis();
		for(int i=0;i<threads;i++) {
			threadPool[i].start();
		}
		for(int i=0;i<threads;i++) {
			try {
				threadPool[i].join();
			} catch (InterruptedException ex) {
				Logger.getLogger(LoadTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		long etime = System.currentTimeMillis();

		float ttime = etime-stime;
		double atime = ttime/txn;

		System.out.println("Done....");
		System.out.println("=============");

		System.out.println("=============");
		System.out.println("Total Txn: "+txn);
		System.out.println("Total Threads: "+threads);
		System.out.println("Total Time: "+ttime+ " ms");
		System.out.println("Avg Time: "+atime+ " ms");
		System.out.println("=============");
		
		try {
			//wait for it to close connections.
			Thread.sleep(1000*10);
		} catch (InterruptedException ex) {
			Logger.getLogger(LoadTest.class.getName()).log(Level.SEVERE, null, ex);
		}

		for(int i=0;i<threads;i++) {
			ltu[i].tearDown();
		}
	}
	
	public LoadTest(String name, int count) {
        this.count = count;
		this.name = name;
    }

	public void setUp(){
		try {
			c = new MemcachedClient(new BinaryConnectionFactory(),
					AddrUtil.getAddresses("localhost:11211"));
		} catch (IOException ex) {
			Logger.getLogger(TextProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void tearDown(){
		if(c!=null) c.shutdown();
	}

	public void test1() {
		for(int i=0;i<count;i++) {
			doSet(i);
		}
		for(int i=0;i<count;i++) {
			doGet(i);
		}
		for(int i=0;i<count;i++) {
			doDelete(i);
		}
	}

    public void doSet(int i) {
		String key = name+"-"+i;
		String value = name+"-"+(i*2);
		c.set(key, 3600, value);
	}

	public void doGet(int i) {
		String key = name+"-"+i;
		Object readObject = (String) c.get(key);
		if(readObject==null) {
			System.out.println("get was null! for "+key);
		}
	}

	public void doDelete(int i) {
		String key = name+"-"+i;
		c.delete(key);
	}
}