package org.asf.connective.standalone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.ContentSource;
import org.asf.connective.handlers.HttpRequestHandler;
import org.asf.connective.headers.HttpHeader;
import org.asf.connective.standalone.configuration.ConfigExclude;
import org.asf.connective.standalone.configuration.ConnectiveConfiguration;
import org.asf.connective.standalone.configuration.HostEntry;
import org.asf.connective.standalone.logger.Log4jManagerImpl;
import org.asf.connective.standalone.modules.IConnectiveModule;
import org.asf.connective.standalone.modules.IMavenRepositoryProvider;
import org.asf.connective.standalone.modules.IModuleMavenDependencyProvider;
import org.asf.connective.standalone.modules.ModuleManager;
import org.asf.cyan.fluid.DynamicClassLoader;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.objectweb.asm.tree.ClassNode;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.util.GroovyScriptEngine;

public class ConnectiveStandaloneMain {

	private static Logger logger;
	private static GroovyScriptEngine groovyEngine;
	private static ConnectiveConfiguration config;
	private static ClassLoader moduleLoader;

	public static void main(String[] args)
			throws IOException, URISyntaxException, ClassNotFoundException, NoSuchAlgorithmException {
		// Setup logging
		if (System.getProperty("debugMode") != null) {
			System.setProperty("log4j2.configurationFile",
					ConnectiveStandaloneMain.class.getResource("/log4j2-ide.xml").toString());
		} else {
			System.setProperty("log4j2.configurationFile",
					ConnectiveStandaloneMain.class.getResource("/log4j2.xml").toString());
		}
		logger = LogManager.getLogger("CONNECTIVE STANDALONE");

		// Assign
		new Log4jManagerImpl().assignAsMain();

		// Log init
		logger.info("Preparing connective standalone server...");

		// Load modules
		File modulesDir = new File("modules");
		logger.info("Searching for modules...");
		modulesDir.mkdirs();
		logger.debug("Scanning modules folder...");
		FluidClassPool pool = FluidClassPool.create();
		DynamicClassLoader loader = new DynamicClassLoader();
		moduleLoader = loader;

		// Load current jar/class folder to class pool
		File source = new File(
				ConnectiveStandaloneMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		if (source.isDirectory()) {
			// Directory, likely debug, load all classes
			loadClassesFrom(source, pool, "");
		} else {
			// File, import archive
			FileInputStream fIn = new FileInputStream(source);
			ZipInputStream zIn = new ZipInputStream(fIn);
			pool.importArchive(zIn);
			zIn.close();
			fIn.close();
		}

		// Debug modules
		if (System.getProperty("debugMode") != null) {
			// Load all debug modules
			String moduleList = System.getProperty("debugModeLoadModules");
			if (moduleList != null) {
				for (String type : moduleList.split(":")) {
					// Load type
					logger.info("Loading debug type: " + type);
					pool.getClassNode(type);
				}
			}
		}

		// Scan modules
		for (File f : modulesDir.listFiles(t -> t.isFile())) {
			// Attempt to load it
			logger.info("Attempting to load module file: " + f.getName() + "...");
			try {
				// Import
				FileInputStream fIn = new FileInputStream(f);
				ZipInputStream zIn = new ZipInputStream(fIn);
				pool.importArchive(zIn);
				zIn.close();
				fIn.close();
				loader.addUrl(f.toURI().toURL());
			} catch (Exception e) {
				logger.error("Failed to load module file: " + f.getName() + ": an error occured while reading the file",
						e);
			}
		}

		// Load repos
		logger.info("Searching for dependency repository definitions...");
		ArrayList<IMavenRepositoryProvider> repos = new ArrayList<IMavenRepositoryProvider>();
		for (ClassNode node : pool.getLoadedClasses()) {
			if (nodeExtends(node, pool, IMavenRepositoryProvider.class) && !Modifier.isAbstract(node.access)
					&& !Modifier.isInterface(node.access)) {
				// Found a source
				try {
					logger.debug("Loading repository definition from: " + node.name.replace("/", ".") + "...");
					Class<?> repoCls = loader.loadClass(node.name.replace("/", "."));
					IMavenRepositoryProvider repoDef = (IMavenRepositoryProvider) repoCls.getConstructor()
							.newInstance();
					if (!repos.stream().anyMatch(t -> t.serverBaseURL().equals(repoDef.serverBaseURL())
							&& t.priority() >= repoDef.priority())) {
						if (repos.stream().anyMatch(t -> t.serverBaseURL().equals(repoDef.serverBaseURL()))) {
							repos.remove(repos.stream().filter(t -> t.serverBaseURL().equals(repoDef.serverBaseURL()))
									.findFirst().get());
						} else {
							logger.info("Added maven repository: " + repoDef.serverBaseURL());
						}
						repos.add(repoDef);
					}
				} catch (Exception e) {
					logger.error("Failed to load repository definition from " + node.name.replace("/", "."), e);
				}
			}
		}
		repos.sort((t1, t2) -> Integer.compare(t1.priority(), t2.priority()));

		// Load dependencies
		ArrayList<IModuleMavenDependencyProvider> deps = new ArrayList<IModuleMavenDependencyProvider>();
		logger.info("Scanning for dependency definitions...");
		for (ClassNode node : pool.getLoadedClasses()) {
			if (nodeExtends(node, pool, IModuleMavenDependencyProvider.class) && !Modifier.isAbstract(node.access)
					&& !Modifier.isInterface(node.access)) {
				// Found a dependency
				try {
					logger.debug("Loading dependency definition from: " + node.name.replace("/", ".") + "...");
					Class<?> depCls = loader.loadClass(node.name.replace("/", "."));
					IModuleMavenDependencyProvider depDef = (IModuleMavenDependencyProvider) depCls.getConstructor()
							.newInstance();

					// Check if its present
					if (!deps.stream().anyMatch(t -> t.group().equals(depDef.group()) && t.name().equals(depDef.name())
							&& !checkVersionGreaterThan(depDef.version(), t.version()))) {
						// Remove old if needed
						if (deps.stream()
								.anyMatch(t -> t.group().equals(depDef.group()) && t.name().equals(depDef.name())
										&& !checkVersionGreaterThan(depDef.version(), t.version()))) {
							deps.remove(deps.stream()
									.filter(t -> t.group().equals(depDef.group()) && t.name().equals(depDef.name())
											&& !checkVersionGreaterThan(depDef.version(), t.version()))
									.findFirst().get());
						} else
							logger.info("Found dependency: " + depDef.group() + ":" + depDef.name() + ":"
									+ depDef.version());
						deps.add(depDef);
					}
				} catch (Exception e) {
					logger.error("Failed to load repository definition from " + node.name.replace("/", "."), e);
				}
			}
		}

		// Download/update dependencies
		File depsDir = new File("libs");
		depsDir.mkdirs();
		boolean updatedDeps = false;
		logger.info("Verifying dependencies...");
		for (IModuleMavenDependencyProvider depDef : deps) {
			// Log
			logger.debug("Verifying dependency: " + depDef.group() + ":" + depDef.name() + ":" + depDef.version());

			// Load hash if possible
			String oldHash = "";
			String filePath = depDef.name() + (depDef.classifier() != null ? "-" + depDef.classifier() : "")
					+ depDef.extension();
			File file = new File(depsDir, filePath);
			if (file.exists()) {
				// Load hash
				FileInputStream strm = new FileInputStream(file);
				oldHash = hashFile(strm);
				strm.close();
			}

			// Read remote hash
			String url = null;
			String remoteHash = null;
			for (IMavenRepositoryProvider repo : repos) {
				try {
					// Build url
					String urlR = repo.serverBaseURL();
					if (!urlR.endsWith("/")) {
						urlR += "/";
					}
					urlR += depDef.group().replace(".", "/");
					urlR += "/";
					urlR += depDef.name();
					urlR += "/";
					urlR += depDef.version();
					urlR += "/";
					urlR += depDef.name();
					urlR += "-";
					urlR += depDef.version();
					if (depDef.classifier() != null) {
						urlR += "-";
						urlR += depDef.classifier();
					}
					urlR += depDef.extension();

					// Download hash
					URL u = new URL(urlR + ".sha1");
					InputStream strm = u.openStream();
					remoteHash = new String(strm.readAllBytes(), "UTF-8").replace("\r", "").replace("\n", "");
					url = urlR;
					strm.close();
					break;
				} catch (Exception e) {
				}
			}

			// Check
			if (url == null) {
				if (oldHash.isEmpty()) {
					logger.fatal("Unable to find a repository that contains dependency " + depDef.group() + ":"
							+ depDef.name() + ":" + depDef.version() + "!");
					System.exit(1);
				} else
					logger.warn("Unable to find a repository that contains dependency " + depDef.group() + ":"
							+ depDef.name() + ":" + depDef.version()
							+ ", unable to check for updates and cannot verify integrity!");
			} else {
				// Check integrity
				if (!oldHash.equals(remoteHash)) {
					// Update
					logger.info("Updating dependency " + depDef.group() + ":" + depDef.name() + ":" + depDef.version()
							+ "...");
					FileOutputStream fOut = new FileOutputStream(file);
					URL u = new URL(url);
					InputStream strm = u.openStream();
					strm.transferTo(fOut);
					strm.close();
					fOut.close();
					updatedDeps = true;
				}
			}
		}
		if (updatedDeps) {
			logger.info("Updated server dependencies, please restart the server.");
			System.exit(0);
		}

		// Load modules
		logger.info("Dependencies are up-to-date, loading modules...");
		ModuleManager.init(pool, loader, logger);

		// Clean up
		logger.info("Clearing module pool...");
		pool.close();
		pool = null;

		// Prepare server
		logger.info("Preparing server...");

		// Write default config if needed
		File configFile = new File("serverconfig.groovy");
		if (!configFile.exists()) {
			// Write default
			InputStream defConfig = ConnectiveStandaloneMain.class.getClassLoader()
					.getResourceAsStream("defaultserverconfig.groovy");
			FileOutputStream fOut = new FileOutputStream(configFile);
			defConfig.transferTo(fOut);
			defConfig.close();
			fOut.close();
		}

		// Load groovy script engine
		groovyEngine = new GroovyScriptEngine(new URL[] { new File(".").getCanonicalFile().toURI().toURL() });

		// Load configuration
		config = new ConnectiveConfiguration();
		loadConfiguration(configFile);
		lockConfig = true;
		for (IConnectiveModule module : ModuleManager.getLoadedModules()) {
			try {
				Map<String, String> conf = config.getModuleConfig(module.moduleID());
				if (conf != null)
					module.onLoadModuleConfig(conf);
			} catch (Exception e) {
				logger.error("Failed to configure module " + module.moduleID(), e);
			}
		}

		// Start and configure servers
		logger.info("Configuring hosts...");
		ArrayList<ConnectiveHttpServer> servers = new ArrayList<ConnectiveHttpServer>();
		for (HostEntry host : config.hosts.hosts) {
			if (host.adapterName == null) {
				logger.fatal("No adapter configured for host!");
				System.exit(1);
			}
			logger.info("Creating host " + host.adapterConfiguration + " with adapter " + host.adapterName + "...");

			// Create instance
			ConnectiveHttpServer server = ConnectiveHttpServer.create(host.adapterName, host.adapterConfiguration);

			// Configure server
			logger.info("Configuring content sources...");
			for (ContentSource src : host.contentSources) {
				logger.info("Registered content source: " + src.getClass().getTypeName());
				server.setContentSource(src);
			}
			logger.info("Configuring error page...");
			server.setErrorPageGenerator(host.errorGenerator);
			if (host.serverName != null) {
				logger.info("Setting server name to " + host.serverName + "...");
				server.setServerName(host.serverName);
			}
			logger.info("Setting default headers...");
			for (HttpHeader header : host.defaultHeaders.getHeaders()) {
				boolean first = true;
				for (String val : header.getValues()) {
					if (first)
						server.getDefaultHeaders().addHeader(header.getName(), val);
					else
						server.getDefaultHeaders().addHeader(header.getName(), val, true);
					first = false;
				}
			}
			logger.info("Adding handlers...");
			for (HttpRequestHandler proc : host.handlers) {
				logger.info("Registering handler: " + proc.getClass().getTypeName());
				server.registerHandler(proc);
			}

			// Run modules
			for (IConnectiveModule module : ModuleManager.getLoadedModules()) {
				try {
					module.onPrepareServer(server);
				} catch (Exception e) {
					logger.error("Failed to run module onPrepareServer " + module.moduleID(), e);
				}
			}

			// Add instance
			servers.add(server);
		}
		if (servers.size() == 0) {
			logger.fatal("No hosts have been configured, unable to start!");
			System.exit(1);
		}

		// Start servers
		logger.info("Starting servers...");
		for (ConnectiveHttpServer server : servers)
			server.start();

		// Post-init modules
		logger.info("Post-initializing modules...");
		for (IConnectiveModule module : ModuleManager.getLoadedModules()) {
			try {
				logger.info("Post-initializing module: " + module.moduleID() + "...");
				module.postInit();
			} catch (Exception e) {
				logger.error("Failed to post-initialize module " + module.moduleID(), e);
			}
		}

		// Wait for exit
		logger.info("Servers are running.");
		servers.forEach(t -> t.waitForExit());
	}

	private static boolean lockConfig = false;

	/**
	 * Retrieves the server configuration
	 * 
	 * @return Server configuration instance
	 */
	public static ConnectiveConfiguration getConfiguration() {
		return config;
	}

	/**
	 * Loads configuration files
	 * 
	 * @param configFile Config file to load
	 */
	public static void loadConfiguration(File configFile) {
		if (lockConfig)
			throw new IllegalStateException("Cannot load configurations after the server initialized");
		try {
			logger.info("Loading configuration from: " + configFile.getPath());
			if (!configFile.exists()) {
				logger.fatal("Failed to parse configuration file: " + configFile.getName() + ": file does not exist.");
				System.exit(1);
			}

			// Load the class
			Binding binding = new Binding();
			loadPropsInto(binding, config);

			// Run modules
			for (IConnectiveModule module : ModuleManager.getLoadedModules()) {
				try {
					module.onPrepareConfigBinding(binding, (source) -> loadPropsInto(binding, source));
				} catch (Exception e) {
					logger.error("Failed to run module onPrepareConfigBinding " + module.moduleID(), e);
				}
			}

			// Run script
			groovyEngine.createScript(configFile.getPath(), binding).evaluate(configFile);
		} catch (Exception e) {
			logger.fatal("Failed to parse configuration file: " + configFile.getName(), e);
			System.exit(1);
		}
	}

	private static void loadPropsInto(Binding binding, Object source) {
		// Load all methods into the groovy object
		for (Method meth : source.getClass().getMethods()) {
			if (meth.isAnnotationPresent(ConfigExclude.class))
				continue;
			if (!Modifier.isStatic(meth.getModifiers())) {
				// Non-static
				binding.setProperty(meth.getName(), new SpecialClosure(meth, source));
			} else {
				// Static
				binding.setProperty(meth.getName(), new SpecialClosure(meth, null));
			}
		}

		// Load all static fields into the groovy object
		for (Field f : source.getClass().getFields()) {
			if (f.isAnnotationPresent(ConfigExclude.class))
				continue;
			if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) {
				if (f.getType().getTypeName().equals(Class.class.getTypeName())) {
					// Check enum
					Class<?> cls = f.getType();
					if (cls.isEnum()) {
						// Add enum as map
						HashMap<String, Object> mp = new HashMap<String, Object>();
						Stream.of(cls.getFields()).filter(t -> t.getType().getTypeName().equals(cls.getTypeName()))
								.forEach(t -> {
									try {
										String nm = t.getName();
										if (nm.startsWith("__"))
											nm = nm.substring(2);
										mp.put(nm, t.get(null));
									} catch (IllegalArgumentException | IllegalAccessException e) {
									}
								});
						binding.setProperty(f.getName(), mp);
					} else {
						try {
							Object ob = f.get(null);
							if (ob != null)
								binding.setProperty(f.getName(), ob);
						} catch (IllegalArgumentException | IllegalAccessException e) {
						}
					}
				} else {
					try {
						Object ob = f.get(null);
						if (ob != null)
							binding.setProperty(f.getName(), ob);
					} catch (IllegalArgumentException | IllegalAccessException e) {
					}
				}
			}
		}
	}

	private static class SpecialClosure extends Closure<Object> {

		private static final long serialVersionUID = 1L;
		private Object owner;
		private Method meth;

		public SpecialClosure(Method meth, Object owner) {
			super(owner);
			this.owner = owner;
			this.parameterTypes = meth.getParameterTypes();
			this.meth = meth;
			meth.setAccessible(true);
		}

		@Override
		public Object call(Object... args) {
			try {
				return meth.invoke(owner, args);
			} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
				if (e instanceof InvocationTargetException)
					throw new RuntimeException("Exception occured processing server configuration", e.getCause());
				throw new RuntimeException("Exception occured processing server configuration", e);
			}
		}

	}

	private static String hashFile(InputStream strm) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] hash = digest.digest(strm.readAllBytes());
		String hashTxt = "";
		for (byte b : hash)
			hashTxt += Integer.toString((b & 0xff) + 0x100, 16).substring(1);
		return hashTxt;
	}

	private static void loadClassesFrom(File source, FluidClassPool pool, String pref) throws ClassNotFoundException {
		for (File dir : source.listFiles(t -> t.isDirectory()))
			loadClassesFrom(dir, pool, pref + dir.getName() + ".");
		for (File file : source.listFiles(t -> !t.isDirectory() && t.getName().endsWith(".class")))
			pool.getClassNode(pref + file.getName().replace(".class", ""));
	}

	private static boolean checkVersionGreaterThan(String newversion, String version) {
		newversion = convertVerToVCheckString(newversion.replace("-", ".").replaceAll("[^0-9A-Za-z.]", ""));
		String oldver = convertVerToVCheckString(version.replace("-", ".").replaceAll("[^0-9A-Za-z.]", ""));

		int ind = 0;
		String[] old = oldver.split("\\.");
		for (String vn : newversion.split("\\.")) {
			if (ind < old.length) {
				String vnold = old[ind];
				if (Integer.valueOf(vn) > Integer.valueOf(vnold)) {
					return true;
				} else if (Integer.valueOf(vn) < Integer.valueOf(vnold)) {
					return false;
				}
				ind++;
			} else
				return false;
		}

		return false;
	}

	private static String convertVerToVCheckString(String version) {
		char[] ver = version.toCharArray();
		version = "";
		boolean lastWasAlpha = false;
		for (char ch : ver) {
			if (ch == '.') {
				version += ".";
			} else {
				if (Character.isAlphabetic(ch) && !lastWasAlpha && !version.endsWith(".")) {
					version += ".";
					lastWasAlpha = true;
				} else if (lastWasAlpha && !version.endsWith(".")) {
					version += ".";
					lastWasAlpha = false;
				} else {
					version += Integer.toString((int) ch);
				}
			}
		}
		return version;
	}

	private static boolean nodeExtends(ClassNode node, FluidClassPool pool, Class<?> target) {
		while (true) {
			// Check node
			if (node.name.equals(target.getTypeName().replace(".", "/")))
				return true;

			// Check interfaces
			if (node.interfaces != null) {
				for (String inter : node.interfaces) {
					try {
						if (nodeExtends(pool.getClassNode(inter), pool, target))
							return true;
					} catch (ClassNotFoundException e) {
					}
				}
			}

			// Check if end was reached
			if (node.superName == null || node.superName.equals("java/lang/Object"))
				break;
			try {
				node = pool.getClassNode(node.superName);
			} catch (ClassNotFoundException e) {
				break;
			}
		}
		return false;
	}

	/**
	 * Retrieves the module class loader
	 * 
	 * @return Module class loader instance
	 */
	public static ClassLoader getModuleClassLoader() {
		return moduleLoader;
	}

}
