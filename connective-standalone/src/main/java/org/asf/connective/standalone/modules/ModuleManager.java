package org.asf.connective.standalone.modules;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.asf.cyan.fluid.DynamicClassLoader;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.objectweb.asm.tree.ClassNode;

/**
 * 
 * Connective module manager
 * 
 * @author Sky Swimmer
 *
 */
public class ModuleManager {

	private static ArrayList<IConnectiveModule> modules = new ArrayList<IConnectiveModule>();
	private static boolean inited = false;

	public static void init(FluidClassPool pool, DynamicClassLoader loader, Logger logger) {
		if (inited)
			throw new IllegalStateException("Already initialized");
		inited = true;

		// Find modules
		logger.debug("Scanning for module classes...");
		for (ClassNode node : pool.getLoadedClasses()) {
			if (nodeExtends(node, pool, IConnectiveModule.class) && !Modifier.isAbstract(node.access)
					&& !Modifier.isInterface(node.access)) {
				// Found a module
				try {
					// Load it
					logger.debug("Loading module class from: " + node.name.replace("/", ".") + "...");
					Class<?> modCls = loader.loadClass(node.name.replace("/", "."));
					try {
						IConnectiveModule modInst = (IConnectiveModule) modCls.getConstructor().newInstance();
						logger.info("Loading module " + modInst.moduleID() + " version " + modInst.version() + "...");
						if (modules.stream().anyMatch(t -> t.moduleID().equalsIgnoreCase(modInst.moduleID()))) {
							// Error: duplicate modules
							IConnectiveModule modInst2 = getLoadedModule(modInst.moduleID());
							logger.error("Duplicate module ID detected: " + modInst.moduleID() + "\n\n"
									+ modInst.moduleID() + " " + modInst.version() + ": "
									+ new File(modInst.getClass().getProtectionDomain().getCodeSource().getLocation()
											.toURI()).getName()
									+ "\n\n" + modInst2.moduleID() + " " + modInst2.version() + ": "
									+ new File(modInst2.getClass().getProtectionDomain().getCodeSource().getLocation()
											.toURI()).getName());
							System.exit(1);
						}

						// Add to loaded module list and pre-initialize it
						modules.add(modInst);
						try {
							modInst.preInit();
						} catch (Exception e) {
							logger.error("Failed to initialize module: " + modInst.moduleID() + ", module source file: "
									+ new File(modCls.getProtectionDomain().getCodeSource().getLocation().toURI())
											.getName(),
									e);
							modules.remove(modInst);
						}
					} catch (Exception e) {
						logger.error(
								"Failed to load module from: " + node.name.replace("/", ".") + ", module source file: "
										+ new File(modCls.getProtectionDomain().getCodeSource().getLocation().toURI())
												.getName(),
								e);
					}
				} catch (Exception e) {
					logger.error("Failed to load module from: " + node.name.replace("/", "."), e);
				}
			}
		}

		// Initialize modules
		logger.info("Initializing modules...");
		for (IConnectiveModule mod : ModuleManager.getLoadedModules()) {
			logger.info("Initializing module: " + mod.moduleID() + "...");
			try {
				mod.init();
			} catch (Exception e) {
				logger.error("Failed to initialize module " + mod.moduleID(), e);
				modules.remove(mod);
			}
		}
	}

	/**
	 * Retrieves modules by ID
	 * 
	 * @param id Module ID
	 * @return IConnectiveModule instance or null
	 */
	public static IConnectiveModule getLoadedModule(String id) {
		for (IConnectiveModule module : modules) {
			if (module.moduleID().equalsIgnoreCase(id))
				return module;
		}
		return null;
	}

	/**
	 * Retrieves modules by type
	 * 
	 * @param <T>  Module type
	 * @param type Module class
	 * @return Module instance or null
	 */
	@SuppressWarnings("unchecked")
	public static <T extends IConnectiveModule> T getLoadedModule(Class<T> type) {
		return (T) modules.stream().filter(t -> type.isAssignableFrom(t.getClass())).findFirst().orElseGet(() -> null);
	}

	/**
	 * Retrieves all loaded modules
	 * 
	 * @return Array of IConnectiveModule instancess
	 */
	public static IConnectiveModule[] getLoadedModules() {
		return modules.toArray(t -> new IConnectiveModule[t]);
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

}
