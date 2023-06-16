package net.flintloader.loader.modules.entrypoint;

import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.util.LoaderUtil;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author HypherionSA
 * Utility class to help with entry point creation
 */
public class EntryPointUtil {

	/**
	 * Create a new instance of the Module Entrypoint class
	 * @param modClass The declared entry point class
	 * @return The initiated class, ready to be used
	 */
	public static Object createInstance(Class<?> modClass) throws Exception {
		try {
			Constructor<?> constructor = modClass.getDeclaredConstructor();
			return constructor.newInstance();
		} catch (NoSuchMethodException e) {
			throw new Exception("Could not find constructor for class " + modClass.getName() + "!", e);
		} catch (IllegalAccessException e) {
			throw new Exception("Could not access constructor of class " + modClass.getName() + "!", e);
		} catch (InvocationTargetException | IllegalArgumentException | InstantiationException e) {
			throw new Exception("Could not instantiate class " + modClass.getName() + "!", e);
		}
	}

	/**
	 * Get a class from the classpath
	 * @param className The name of the class, for example "com.minecraft.Main"
	 * @return The class as loaded from the classpath
	 */
	public static Class<?> getClass(String className) throws ClassNotFoundException {
		InputStream stream = PunchLauncherBase.getLauncher().getResourceAsStream(LoaderUtil.getClassFileName(className));
		if (stream == null) throw new ClassNotFoundException("Could not find or load class " + className);
		return PunchLauncherBase.getClass(className);
	}

}
