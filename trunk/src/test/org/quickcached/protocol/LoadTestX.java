package org.quickcached.protocol;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.logging.*;
import net.rubyeye.xmemcached.MemcachedClient;

import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;
/**
 *
 * @author akshath
 */
public class LoadTestX {
	private MemcachedClient c = null;
	private int count;
	private String name;
	private String hostList;
	private int timeouts;

	public static void main(String args[]) {
		String mode = "s";
		int threads = 10;
		String host = "192.168.1.10:11211";
		int txn = 10;

		if(args.length==4) {
			mode = args[0];
			threads = Integer.parseInt(args[1]);
			host = args[2];
			txn = Integer.parseInt(args[3]);
		}
		if(mode.equals("s")) {
			doSingleThreadTest(host, txn);
		} else {
			doMultiThreadTest(host, txn, threads);
		}
    }

	public static void doSingleThreadTest(String host, int txn) {
		LoadTestX ltu = new LoadTestX("Test1", host, txn);
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

	public static void doMultiThreadTest(String host, int txn, int threads) {
		int eachUnitCount = txn/threads;

		final LoadTestX ltu[] = new LoadTestX[threads];
		for(int i=0;i<threads;i++) {
			ltu[i] = new LoadTestX("Test-"+i, host, eachUnitCount);
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

		long timeoutCount = 0;
		for(int i=0;i<threads;i++) {
			try {
				threadPool[i].join();
			} catch (InterruptedException ex) {
				Logger.getLogger(LoadTestX.class.getName()).log(Level.SEVERE, null, ex);
			}
			timeoutCount = timeoutCount + ltu[i].timeouts;
		}
		long etime = System.currentTimeMillis();

		float ttime = etime-stime;
		double atime = ttime/txn;

		System.out.println("Done....");
		System.out.println("=============");

		System.out.println("=============");
		System.out.println("Host List: "+host);
		System.out.println("Total Txn: "+txn);
		System.out.println("Total Threads: "+threads);
		System.out.println("Total Time: "+ttime+ " ms");
		System.out.println("Timeouts: "+timeoutCount);
		System.out.println("Avg Time: "+atime+ " ms");
		System.out.println("=============");
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			Logger.getLogger(LoadTestX.class.getName()).log(Level.SEVERE, null, ex);
		}

		for(int i=0;i<threads;i++) {
			ltu[i].tearDown();
		}
	}
	
	public LoadTestX(String name, String host, int count) {
        this.count = count;
		this.name = name;
		this.hostList = host;
    }

	public void setUp(){
		try {
			MemcachedClientBuilder builder = new XMemcachedClientBuilder(
					AddrUtil.getAddresses(hostList));
			builder.setCommandFactory(new BinaryCommandFactory());//use binary protocol 
			c = builder.build();
   	} catch (IOException ex) {
			Logger.getLogger(TextProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void tearDown(){
		if(c!=null) try {
			c.shutdown();
		} catch (IOException ex) {
			Logger.getLogger(LoadTestX.class.getName()).log(Level.SEVERE, null, ex);
		}
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

	private String largeData = null;
    public void doSet(int i) {
		String key = name+"-"+i;
		if(largeData==null) {
			StringBuilder sb = new StringBuilder();
			Random r = new Random();
			char c = '0';
			for(int k=0;k<4000;k++) {
				c = (char)(r.nextInt(26) + 'a');
				sb.append(c);
			}
			largeData = sb.toString();
		}
		String value = name+"-"+(i*2)+"-"+largeData;
		try {
			c.set(key, 3600, value);
		} catch (TimeoutException ex) {
			Logger.getLogger(LoadTestX.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(LoadTestX.class.getName()).log(Level.SEVERE, null, ex);
		} catch (MemcachedException ex) {
			Logger.getLogger(LoadTestX.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void doGet(int i) {
		String key = name+"-"+i;
		try {
			Object readObject = (String) c.get(key);
			if(readObject==null) {
				System.out.println("get was null! for "+key);
			}
		} catch(TimeoutException e) {
			timeouts++;
			System.out.println("Timeout: "+e+" for "+key);
		} catch(InterruptedException e) {
			timeouts++;
			System.out.println("Timeout: "+e+" for "+key);
		} catch(MemcachedException e) {
			timeouts++;
			System.out.println("MemcachedException: "+e+" for "+key);
		}
	}

	public void doDelete(int i) {
		String key = name+"-"+i;
		try {
			c.delete(key);
		} catch (TimeoutException ex) {
			Logger.getLogger(LoadTestX.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(LoadTestX.class.getName()).log(Level.SEVERE, null, ex);
		} catch (MemcachedException ex) {
			Logger.getLogger(LoadTestX.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
