package com.ld.utils;

import io.spider.pojo.GlobalConfig;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 仅为兼容老MQ RPC使用
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
@Deprecated
public class ClassScanUtil
{
	private static final String CLASS_FILE_TAILS = ".class";

	private static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);

	private static Map<String, Set<Class<?>>> package2Classes = new ConcurrentHashMap<String, Set<Class<?>>>();

	public static Set<Class<?>> getClassesByPackageName(String packageName, ClassFilter filter)
			throws IOException
	{
		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		if (StringUtils.isEmpty(packageName)) {
			return classes;
		}
		if (package2Classes.containsKey(packageName)) {
			Set<Class<?>> result = (Set<Class<?>>)package2Classes.get(packageName);
			if (result != null) {
				return result;
			}
		}
		getClassesByPackageName(classes, packageName, filter);
		package2Classes.put(packageName, classes);
		return classes;
	}

	public static void clearCache()
	{
		package2Classes = new ConcurrentHashMap<String, Set<Class<?>>>();
	}

	private static void getClassesByPackageName(Set<Class<?>> classes, String packageName, ClassFilter filter) throws IOException
	{
		boolean recursive = true;
		String packageDirName = packageName.replace('.', '/');

		Enumeration<?> dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);

		if (filter == null) {
			filter = new ClassFilter() {
				public boolean accept(Class<?> clz) {
					return true;
				}
			};
		}

		while (dirs.hasMoreElements()) {
			URL url = (URL)dirs.nextElement();
			String protocol = url.getProtocol();
			if ("file".equals(protocol)) {
				String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
				findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes, filter);
			}
			else if ("jar".equals(protocol)) {
				JarFile jar = ((JarURLConnection)url.openConnection()).getJarFile();
				Enumeration<?> entries = jar.entries();

				while (entries.hasMoreElements()) {
					JarEntry entry = (JarEntry)entries.nextElement();
					String name = entry.getName();
					if (name.charAt(0) == '/') {
						name = name.substring(1);
					}
					if (name.startsWith(packageDirName)) {
						int idx = name.lastIndexOf('/');
						if (idx != -1) {
							packageName = name.substring(0, idx).replace('/', '.');
						}

						if (((idx != -1) || (recursive)) && 
								(name.endsWith(CLASS_FILE_TAILS)) && (!entry.isDirectory()))
						{
							String className = name.substring(packageName.length() + 1, name.length() - 6);

							tryAddClass(classes, packageName + '.' + className, filter);
						}
					}
				}
			}
		}
	}

	private static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes, ClassFilter filter)
	{
		File dir = new File(packagePath);
		try {
			if ((!dir.exists()) || (!dir.isDirectory()) || !dir.canRead()) {
				return;
			}
			File[] dirfiles = dir.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return (recursive && file.isDirectory()) || (file.getName().endsWith(CLASS_FILE_TAILS));
				}
			});
			for (File file : dirfiles){
				if (file.isDirectory()) {
					findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes, filter);
				}
				else
				{
					String className = file.getName().substring(0, file.getName().length() - 6);

					tryAddClass(classes, packageName + '.' + className, filter);
				}
			}
		} catch (Exception e) {
			return;
		}
		
		
		
	}

	private static void tryAddClass(Set<Class<?>> classes, String className, ClassFilter filter)
	{
		try
		{
			Class<?> clz = Thread.currentThread().getContextClassLoader().loadClass(className);
			if (filter.accept(clz)) {
				classes.add(clz);
			}
		}
		catch (Throwable e)
		{
			logger.warn("Failed to load class[" + className + "]");
		}
	}

	public static abstract interface ClassFilter
	{
		public abstract boolean accept(Class<?> paramClass);
	}
}