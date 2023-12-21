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
package net.flintloader.punch.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.flintloader.loader.api.FlintModuleContainer;
import net.flintloader.loader.core.PunchLauncherHooks;
import net.flintloader.loader.modules.ModuleList;
import net.flintloader.punch.PunchLoader;
import net.flintloader.punch.api.MappingResolver;
import net.flintloader.punch.api.ObjectShare;
import net.flintloader.punch.impl.game.GameProvider;
import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.launch.punch.Punch;
import net.flintloader.punch.impl.util.LoaderUtil;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;
import org.objectweb.asm.Opcodes;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;

@SuppressWarnings("deprecation")
public final class PunchLoaderImpl extends PunchLoader {
	public static final PunchLoaderImpl INSTANCE = InitHelper.get();

	public static final int ASM_VERSION = Opcodes.ASM9;

	public static final String VERSION = "0.0.4";

	public static final String CACHE_DIR_NAME = ".punch"; // relative to game dir
	public static final String REMAPPED_JARS_DIR_NAME = "remappedJars"; // relative to cache dir

	private final AccessWidener accessWidener = new AccessWidener();

	private final ObjectShare objectShare = new ObjectShareImpl();

	private boolean frozen = false;

	private Object gameInstance;

	private MappingResolver mappingResolver;
	private GameProvider provider;
	private Path gameDir;
	private Path configDir;

	private PunchLoaderImpl() { }

	/**
	 * Freeze the PunchLoader, preventing additional mods from being loaded.
	 */
	public void freeze() {
		if (frozen) {
			throw new IllegalStateException("Already frozen!");
		}

		frozen = true;
		PunchLauncherHooks.finishModuleSetup();
	}

	public GameProvider getGameProvider() {
		if (provider == null) throw new IllegalStateException("game provider not set (yet)");

		return provider;
	}

	public GameProvider tryGetGameProvider() {
		return provider;
	}

	public void setGameProvider(GameProvider provider) {
		this.provider = provider;

		setGameDir(provider.getLaunchDirectory());
	}

	private void setGameDir(Path gameDir) {
		this.gameDir = gameDir;
		this.configDir = gameDir.resolve("config");
	}

	@Override
	public Object getGameInstance() {
		return gameInstance;
	}

	/**
	 * @return The game instance's root directory.
	 */
	@Override
	public Path getGameDir() {
		if (gameDir == null) throw new IllegalStateException("invoked too early?");

		return gameDir;
	}

	@Override
	@Deprecated
	public File getGameDirectory() {
		return getGameDir().toFile();
	}

	/**
	 * @return The game instance's configuration directory.
	 */
	@Override
	public Path getConfigDir() {
		if (!Files.exists(configDir)) {
			try {
				Files.createDirectories(configDir);
			} catch (IOException e) {
				throw new RuntimeException("Creating config directory", e);
			}
		}

		return configDir;
	}

	@Override
	@Deprecated
	public File getConfigDirectory() {
		return getConfigDir().toFile();
	}

	public void load() {
		if (provider == null) throw new IllegalStateException("game provider not set");
		if (frozen) throw new IllegalStateException("Frozen - cannot load additional mods!");
		PunchLauncherHooks.discoverModules();
	}

	@Override
	public MappingResolver getMappingResolver() {
		if (mappingResolver == null) {
			mappingResolver = new MappingResolverImpl(
					PunchLauncherBase.getLauncher().getMappingConfiguration()::getMappings,
					PunchLauncherBase.getLauncher().getTargetNamespace()
					);
		}

		return mappingResolver;
	}

	@Override
	public ObjectShare getObjectShare() {
		return objectShare;
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return PunchLauncherBase.getLauncher().isDevelopment();
	}

	public void loadAccessWideners() {
		AccessWidenerReader accessWidenerReader = new AccessWidenerReader(accessWidener);

		for (FlintModuleContainer modContainer : ModuleList.getInstance().allModules()) {
			if (modContainer.getMetadata().isBuiltIn()) continue;

			String accessWidener = modContainer.getMetadata().getAccessWidener();
			if (accessWidener == null || accessWidener.isEmpty()) continue;

			Path path = modContainer.findPath(accessWidener).orElse(null);
			if (path == null) throw new RuntimeException(String.format("Missing accessWidener file %s from module %s", accessWidener, modContainer.getMetadata().getId()));

			try (BufferedReader reader = Files.newBufferedReader(path)) {
				accessWidenerReader.read(reader, getMappingResolver().getCurrentRuntimeNamespace());
			} catch (Exception e) {
				throw new RuntimeException("Failed to read accessWidener file from mod " + modContainer.getMetadata().getId(), e);
			}
		}
	}

	public void prepareModuleInit(Path newRunDir, Object gameInstance) {
		if (!frozen) {
			throw new RuntimeException("Cannot instantiate mods when not frozen!");
		}

		if (gameInstance != null && PunchLauncherBase.getLauncher() instanceof Punch) {
			ClassLoader gameClassLoader = gameInstance.getClass().getClassLoader();
			ClassLoader targetClassLoader = PunchLauncherBase.getLauncher().getTargetClassLoader();
			boolean matchesKnot = (gameClassLoader == targetClassLoader);
			boolean containsKnot = false;

			if (matchesKnot) {
				containsKnot = true;
			} else {
				gameClassLoader = gameClassLoader.getParent();

				while (gameClassLoader != null && gameClassLoader.getParent() != gameClassLoader) {
					if (gameClassLoader == targetClassLoader) {
						containsKnot = true;
					}

					gameClassLoader = gameClassLoader.getParent();
				}
			}

			if (!matchesKnot) {
				if (containsKnot) {
					Log.info(LogCategory.KNOT, "Environment: Target class loader is parent of game class loader.");
				} else {
					Log.warn(LogCategory.KNOT, "\n\n* CLASS LOADER MISMATCH! THIS IS VERY BAD AND WILL PROBABLY CAUSE WEIRD ISSUES! *\n"
							+ " - Expected game class loader: %s\n"
							+ " - Actual game class loader: %s\n"
							+ "Could not find the expected class loader in game class loader parents!\n",
							PunchLauncherBase.getLauncher().getTargetClassLoader(), gameClassLoader);
				}
			}
		}

		this.gameInstance = gameInstance;

		if (gameDir != null) {
			try {
				if (!gameDir.toRealPath().equals(newRunDir.toRealPath())) {
					Log.warn(LogCategory.GENERAL, "Inconsistent game execution directories: engine says %s, while initializer says %s...",
							newRunDir.toRealPath(), gameDir.toRealPath());
					setGameDir(newRunDir);
				}
			} catch (IOException e) {
				Log.warn(LogCategory.GENERAL, "Exception while checking game execution directory consistency!", e);
			}
		} else {
			setGameDir(newRunDir);
		}

		PunchLauncherHooks.gatherEntryPoints();
	}

	public AccessWidener getAccessWidener() {
		return accessWidener;
	}

	/**
	 * Sets the game instance. This is only used in 20w22a+ by the dedicated server and should not be called by anything else.
	 */
	public void setGameInstance(Object gameInstance) {
		if (this.gameInstance != null) {
			throw new UnsupportedOperationException("Cannot overwrite current game instance!");
		}

		this.gameInstance = gameInstance;
	}

	@Override
	public String[] getLaunchArguments(boolean sanitize) {
		return getGameProvider().getLaunchArguments(sanitize);
	}

	/**
	 * Provides singleton for static init assignment regardless of load order.
	 */
	public static class InitHelper {
		private static PunchLoaderImpl instance;

		public static PunchLoaderImpl get() {
			if (instance == null) instance = new PunchLoaderImpl();

			return instance;
		}
	}

	static {
		LoaderUtil.verifyNotInTargetCl(PunchLoaderImpl.class);
	}
}
