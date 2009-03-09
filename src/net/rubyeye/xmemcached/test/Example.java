package net.rubyeye.xmemcached.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.io.IOException;
import java.io.Serializable;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

class Name implements Serializable {
	String firstName;
	String lastName;
	int age;
	int money;

	public Name(String firstName, String lastName, int age, int money) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
		this.age = age;
		this.money = money;
	}

	public String toString() {
		return "[" + firstName + " " + lastName + ",age=" + age + ",money="
				+ money + "]";
	}

}

public class Example {
	public static void main(String[] args) {
		try {
			String ip = "192.168.222.100";

			int port = 11211;
			XMemcachedClient client = new XMemcachedClient();
			// client.setOptimiezeSet(true);
			// 添加server
			client.addServer(ip, port);

			if (!client.set("hello", 0, "dennis")) {
				System.err.println("set error");
			}
			client.add("hello", 0, "dennis");
			client.replace("hello", 0, "dennis");
			client.append("hello", 0, " good");
			client.prepend("hello", 0, "hello ");
			String name = (String) client.get("hello");
			System.out.println(name);

			List<String> keys = new ArrayList<String>();
			keys.add("hello");
			keys.add("test");
			Map<String, Object> map = client.get(keys);
			System.out.println("map size:" + map.size());

			if (!client.delete("hello", 1000)) {
				System.err.println("delete error");
			}

			client.incr("a", 4);
			client.decr("a", 4);

			String version = client.version();
			System.out.println(version);
			Name dennis = new Name("dennis", "zhuang", 26, -1);
			System.out.println("dennis:" + dennis);
			client.set("dennis", 0, dennis);

			Name cachedPerson = (Name) client.get("dennis");
			System.out.println("cachedPerson:" + cachedPerson);
			cachedPerson.money = -10000;

			client.replace("dennis", 0, cachedPerson);
			Name cachedPerson2 = (Name) client.get("dennis");
			System.out.println("cachedPerson2:" + cachedPerson2);

			client.delete("dennis");
			System.out.println("after delete:" + client.get("dennis"));

			map = new HashMap();
			for (int i = 0; i < 1000; i++)
				map.put(String.valueOf(i), i);
			if (!client.set("map", 0, map, 10000)) {
				System.err.println("set map error");
			}
			HashMap cachedMap = (HashMap) client.get("map");
			if (cachedMap.size() != 1000)
				System.err.println("get map error");
			for (Object key : cachedMap.keySet()) {
				if (!cachedMap.get(key).equals(Integer.parseInt((String) key))) {
					System.err.println("get map error");
				}
			}
			for (int i = 0; i < 100; i++)
				if (client.get("hello__" + i) != null)
					System.err.println("get error");
			for (int i = 0; i < 100; i++)
				if (client.delete("hello__" + i))
					System.err.println("get error");

			long start = System.currentTimeMillis();
			for (int i = 0; i < 200; i++)
				if (!client.set("test", 0, i))
					System.out.println("set error");
			System.out.println(System.currentTimeMillis() - start);

			// 测试cas
			client.set("a", 0, 1);
			GetsResponse result = client.gets("a");
			long cas = result.getCas();
			if ((Integer) result.getValue() != 1)
				System.err.println("gets error");
			System.out.println("cas value:" + cas);
			if (!client.cas("a", 0, 2, cas)) {
				System.err.println("cas error");
			}
			result = client.gets("a");
			if ((Integer) result.getValue() != 2)
				System.err.println("cas error");

			/**
			 * 合并gets和cas，利用CASOperation
			 */
			client.cas("a", 0, new CASOperation() {

				@Override
				public int getMaxTries() {
					return 1;
				}

				@Override
				public Object getNewValue(long currentCAS, Object currentValue) {
					System.out.println("current value " + currentValue);
					return 3;
				}

			});
			result = client.gets("a");
			if ((Integer) result.getValue() != 3)
				System.err.println("cas error");
			keys.add("a");
			System.out.println(client.gets(keys).get("a").getValue());
			client.shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
			// 超时
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (MemcachedException e) {
			e.printStackTrace();
			// 执行异常
		}

	}

}
