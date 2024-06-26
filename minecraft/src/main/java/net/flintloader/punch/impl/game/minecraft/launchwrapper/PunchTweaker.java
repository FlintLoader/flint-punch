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
package net.flintloader.punch.impl.game.minecraft.launchwrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import net.flintloader.loader.api.FlintModule;
import net.flintloader.loader.core.PunchLauncherHooks;
import net.flintloader.loader.core.entrypoints.FlintEntryPoints;
import net.flintloader.punch.impl.FormattedException;
import net.flintloader.punch.impl.PunchLoaderImpl;
import net.flintloader.punch.impl.game.GameProvider;
import net.flintloader.punch.impl.game.minecraft.MinecraftGameProvider;
import net.flintloader.punch.impl.launch.FlintMixinBootstrap;
import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.util.Arguments;
import net.flintloader.punch.impl.util.FileSystemUtil;
import net.flintloader.punch.impl.util.LoaderUtil;
import net.flintloader.punch.impl.util.ManifestUtil;
import net.flintloader.punch.impl.util.SystemProperties;
import net.flintloader.punch.impl.util.UrlUtil;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.Proxy;

public abstract class PunchTweaker extends PunchLauncherBase implements ITweaker {
	private static final LogCategory LOG_CATEGORY = LogCategory.create("GameProvider", "Tweaker");
	protected Arguments arguments;
	private LaunchClassLoader launchClassLoader;
	private final List<Path> classPath = new ArrayList<>();
	private boolean isDevelopment;

	@SuppressWarnings("unchecked")
	private final boolean isPrimaryTweaker = ((List<ITweaker>) Launch.blackboard.get("Tweaks")).isEmpty();

	@Override
	public String getEntrypoint() {
		return getLaunchTarget();
	}

	@Override
	public String getTargetNamespace() {
		// TODO: Won't work outside of Yarn
		return isDevelopment ? "named" : "intermediary";
	}

	@Override
	public void acceptOptions(List<String> localArgs, File gameDir, File assetsDir, String profile) {
		arguments = new Arguments();
		arguments.parse(localArgs);

		if (!arguments.containsKey("gameDir") && gameDir != null) {
			arguments.put("gameDir", gameDir.getAbsolutePath());
		}

		if (!arguments.containsKey("assetsDir") && assetsDir != null) {
			arguments.put("assetsDir", assetsDir.getAbsolutePath());
		}
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader launchClassLoader) {
		isDevelopment = Boolean.parseBoolean(System.getProperty(SystemProperties.DEVELOPMENT, "false"));
		Launch.blackboard.put(SystemProperties.DEVELOPMENT, isDevelopment);
		setProperties(Launch.blackboard);

		this.launchClassLoader = launchClassLoader;
		launchClassLoader.addClassLoaderExclusion("org.objectweb.asm.");
		launchClassLoader.addClassLoaderExclusion("org.spongepowered.asm.");
		launchClassLoader.addClassLoaderExclusion("net.flintloader.punch.");
		launchClassLoader.addClassLoaderExclusion("org.slf4j.");
		launchClassLoader.addClassLoaderExclusion("org.apache.logging.log4j.");

		launchClassLoader.addClassLoaderExclusion("net.flintloader.api.Environment");
		launchClassLoader.addClassLoaderExclusion("net.flintloader.api.EnvType");
		launchClassLoader.addClassLoaderExclusion("net.flintloader.api.ModInitializer");
		launchClassLoader.addClassLoaderExclusion("net.flintloader.api.ClientModInitializer");
		launchClassLoader.addClassLoaderExclusion("net.flintloader.api.DedicatedServerModInitializer");

		try {
			init();
		} catch (FormattedException e) {
			handleFormattedException(e);
		}
	}

	private void init() {
		setupUncaughtExceptionHandler();

		classPath.clear();

		for (URL url : launchClassLoader.getSources()) {
			Path path = UrlUtil.asPath(url);
			if (!Files.exists(path)) continue;

			classPath.add(LoaderUtil.normalizeExistingPath(path));
		}

		GameProvider provider = new MinecraftGameProvider();

		if (!provider.isEnabled()
				|| !provider.locateGame(this, arguments.toArray())) {
			throw new RuntimeException("Could not locate Minecraft: provider locate failed");
		}

		Log.finishBuiltinConfig();

		arguments = null;

		provider.initialize(this);

		PunchLoaderImpl loader = PunchLoaderImpl.INSTANCE;
		loader.setGameProvider(provider);
		loader.load();
		loader.freeze();

		launchClassLoader.registerTransformer(PunchClassTransformer.class.getName());
		PunchLoaderImpl.INSTANCE.loadAccessWideners();

		// Setup Mixin environment
		MixinBootstrap.init();
		FlintMixinBootstrap.init(PunchLoaderImpl.INSTANCE);
		MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);

		provider.unlockClassPath(this);
		FlintEntryPoints.invoke("early", FlintModule.class, FlintModule::earlyInitialization);
	}

	@Override
	public String[] getLaunchArguments() {
		return isPrimaryTweaker ? PunchLoaderImpl.INSTANCE.getGameProvider().getLaunchArguments(false) : new String[0];
	}

	@Override
	public void addToClassPath(Path path, String... allowedPrefixes) {
		try {
			launchClassLoader.addURL(UrlUtil.asUrl(path));
			// allowedPrefixes handling is not implemented (no-op)
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setAllowedPrefixes(Path path, String... prefixes) {
		// not implemented (no-op)
	}

	@Override
	public void setValidParentClassPath(Collection<Path> paths) {
		// not implemented (no-op)
	}

	@Override
	public List<Path> getClassPath() {
		return classPath;
	}

	@Override
	public boolean isClassLoaded(String name) {
		throw new RuntimeException("TODO isClassLoaded/launchwrapper");
	}

	@Override
	public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
		return launchClassLoader.loadClass(name); // TODO: implement properly, this may load the class into the system class loader
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return launchClassLoader.getResourceAsStream(name);
	}

	@Override
	public ClassLoader getTargetClassLoader() {
		return launchClassLoader;
	}

	@Override
	public byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
		String transformedName = name.replace('/', '.');
		byte[] classBytes = launchClassLoader.getClassBytes(name);

		if (runTransformers) {
			for (IClassTransformer transformer : launchClassLoader.getTransformers()) {
				if (transformer instanceof Proxy) {
					continue; // skip mixin as per method contract
				}

				classBytes = transformer.transform(name, transformedName, classBytes);
			}
		}

		return classBytes;
	}

	@Override
	public Manifest getManifest(Path originPath) {
		try {
			if (Files.isDirectory(originPath)) {
				return ManifestUtil.readManifest(originPath);
			} else {
				try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(originPath, false)) {
					return ManifestUtil.readManifest(jarFs.get().getRootDirectories().iterator().next());
				}
			}
		} catch (IOException e) {
			Log.warn(LOG_CATEGORY, "Error reading Manifest", e);
			return null;
		}
	}

	// By default the remapped jar will be on the classpath after the obfuscated one.
	// This will lead to us finding and the launching the obfuscated one when we search
	// for the entrypoint.
	// To work around that, we pre-popuplate the LaunchClassLoader's resource cache,
	// which will then cause it to use the one we need it to.
	@SuppressWarnings("unchecked")
	private void preloadRemappedJar(Path remappedJarFile) throws IOException {
		Map<String, byte[]> resourceCache = null;

		try {
			Field f = LaunchClassLoader.class.getDeclaredField("resourceCache");
			f.setAccessible(true);
			resourceCache = (Map<String, byte[]>) f.get(launchClassLoader);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (resourceCache == null) {
			Log.warn(LOG_CATEGORY, "Resource cache not pre-populated - this will probably cause issues...");
			return;
		}

		try (FileInputStream jarFileStream = new FileInputStream(remappedJarFile.toFile());
				JarInputStream jarStream = new JarInputStream(jarFileStream)) {
			JarEntry entry;

			while ((entry = jarStream.getNextJarEntry()) != null) {
				if (entry.getName().startsWith("net/minecraft/class_") || !entry.getName().endsWith(".class")) {
					// These will never be in the obfuscated jar, so we can safely skip them
					continue;
				}

				String className = entry.getName();
				className = className.substring(0, className.length() - 6).replace('/', '.');
				Log.debug(LOG_CATEGORY, "Appending %s to resource cache...", className);
				resourceCache.put(className, toByteArray(jarStream));
			}
		}
	}

	private byte[] toByteArray(InputStream inputStream) throws IOException {
		int estimate = inputStream.available();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(estimate < 32 ? 32768 : estimate);
		byte[] buffer = new byte[8192];
		int len;

		while ((len = inputStream.read(buffer)) > 0) {
			outputStream.write(buffer, 0, len);
		}

		return outputStream.toByteArray();
	}

	@Override
	public boolean isDevelopment() {
		return isDevelopment;
	}
}
