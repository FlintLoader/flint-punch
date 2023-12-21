/**
* Copyright 2016 FabricMC
* Copyright 2023 Flint Loader Contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/
package net.flintloader.loader.modules.entrypoint;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.util.LoaderUtil;

/**
 * @author HypherionSA
 * Utility class to help with entry point creation
 */
public final class EntryPointUtil {

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
	public static Class<?> getClass(String className) throws ClassNotFoundException, IOException {
		InputStream stream = PunchLauncherBase.getLauncher().getResourceAsStream(LoaderUtil.getClassFileName(className));
		if (stream == null) throw new ClassNotFoundException("Could not find or load class " + className);
		stream.close();
		return PunchLauncherBase.getClass(className);
	}

}
