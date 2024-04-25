/**
* Copyright 2016 FabricMC
* Copyright 2024 Flint Loader Contributors
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
package net.flintloader.punch.api;

import java.io.File;
import java.nio.file.Path;

import net.flintloader.punch.impl.PunchLoaderImpl;

/**
 * The public-facing PunchLoader instance.
 *
 * <p>To obtain a working instance, call {@link #getInstance()}.</p>
 *
 * @since 0.4.0
 */
public interface PunchLoader {
	/**
	 * Returns the public-facing Fabric Loader instance.
	 */
	static PunchLoader getInstance() {
		PunchLoader ret = PunchLoaderImpl.INSTANCE;

		if (ret == null) {
			throw new RuntimeException("Accessed PunchLoader too early!");
		}

		return ret;
	}

	/**
	 * Get the object share for inter-mod communication.
	 *
	 * <p>The share allows mods to exchange data without directly referencing each other. This makes simple interaction
	 * easier by eliminating any compile- or run-time dependencies if the shared value type is independent of the mod
	 * (only Java/game/Fabric types like collections, primitives, String, Consumer, Function, ...).
	 *
	 * <p>Active interaction is possible as well since the shared values can be arbitrary Java objects. For example
	 * exposing a {@code Runnable} or {@code Function} allows the "API" user to directly invoke some program logic.
	 *
	 * <p>It is required to prefix the share key with the mod id like {@code mymod:someProperty}. Mods should not
	 * modify entries by other mods. The share is thread safe.
	 *
	 * @return the global object share instance
	 * @since 0.12.0
	 */
	ObjectShare getObjectShare();

	/**
	 * Get the current mapping resolver.
	 *
	 * <p>When performing reflection, a mod should always query the mapping resolver for
	 * the remapped names of members than relying on other heuristics.</p>
	 *
	 * @return the current mapping resolver instance
	 * @since 0.4.1
	 */
	MappingResolver getMappingResolver();

	/**
	 * Checks if Fabric Loader is currently running in a "development"
	 * environment. Can be used for enabling debug mode or additional checks.
	 *
	 * <p>This should not be used to make assumptions on certain features,
	 * such as mappings, but as a toggle for certain functionalities.</p>
	 *
	 * @return whether or not Loader is currently in a "development"
	 * environment
	 */
	boolean isDevelopmentEnvironment();

	/**
	 * Get the current game instance. Can represent a game client or
	 * server object. As such, the exact return is dependent on the
	 * current environment type.
	 *
	 *
	 * @return A client or server instance object
	 * @deprecated This method is experimental and it's use is discouraged.
	 */
	/* @Nullable */
	@Deprecated
	Object getGameInstance();

	/**
	 * Get the current game working directory.
	 *
	 * @return the working directory
	 */
	Path getGameDir();

	@Deprecated
	File getGameDirectory();

	/**
	 * Get the current directory for game configuration files.
	 *
	 * @return the configuration directory
	 */
	Path getConfigDir();

	@Deprecated
	File getConfigDirectory();

	/**
	 * Gets the command line arguments used to launch the game.
	 *
	 * <p>The implementation will try to strip or obscure sensitive data like authentication tokens if {@code sanitize}
	 * is set to true. Callers are highly encouraged to enable sanitization as compromising the information can easily
	 * happen with logging, exceptions, serialization or other causes.
	 *
	 * <p>There is no guarantee that {@code sanitize} covers everything, so the launch arguments should still not be
	 * logged or otherwise exposed routinely even if the parameter is set to {@code true}. In particular it won't
	 * necessarily strip all information that can be used to identify the user.
	 *
	 * @param sanitize Whether to try to remove or obscure sensitive information
	 * @return the launch arguments for the game
	 */
	String[] getLaunchArguments(boolean sanitize);
}
