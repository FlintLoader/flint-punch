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
package net.flintloader.punch.impl.game.minecraft;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.flintloader.punch.api.ObjectShare;
import net.flintloader.punch.impl.FormattedException;
import net.flintloader.punch.impl.PunchLoaderImpl;
import net.flintloader.punch.impl.game.GameProvider;
import net.flintloader.punch.impl.game.GameProviderHelper;
import net.flintloader.punch.impl.game.LibClassifier;
import net.flintloader.punch.impl.game.minecraft.patch.BrandingPatch;
import net.flintloader.punch.impl.game.minecraft.patch.EntrypointPatch;
import net.flintloader.punch.impl.game.minecraft.patch.EntrypointPatchFML125;
import net.flintloader.punch.impl.game.minecraft.patch.TinyFDPatch;
import net.flintloader.punch.impl.game.patch.GameTransformer;
import net.flintloader.punch.impl.launch.PunchLauncher;
import net.flintloader.punch.impl.util.Arguments;
import net.flintloader.punch.impl.util.ExceptionUtil;
import net.flintloader.punch.impl.util.LoaderUtil;
import net.flintloader.punch.impl.util.SystemProperties;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogHandler;

public class MinecraftGameProvider implements GameProvider {
	private static final String[] ALLOWED_EARLY_CLASS_PREFIXES = { "org.apache.logging.log4j.", "com.mojang.util." };

	private static final Set<String> SENSITIVE_ARGS = new HashSet<>(Arrays.asList(
			// all lowercase without --
			"accesstoken",
			"clientid",
			"profileproperties",
			"proxypass",
			"proxyuser",
			"username",
			"userproperties",
			"uuid",
			"xuid"));

	private String entrypoint;
	private Arguments arguments;
	private final List<Path> gameJars = new ArrayList<>(2); // env game jar and potentially common game jar
	private Path realmsJar;
	private final Set<Path> logJars = new HashSet<>();
	private boolean log4jAvailable;
	private boolean slf4jAvailable;
	private final List<Path> miscGameLibraries = new ArrayList<>(); // libraries not relevant for loader's uses
	private Collection<Path> validParentClassPath; // computed parent class path restriction (loader+deps)
	private McVersion versionData;
	private boolean hasModLoader = false;

	private final GameTransformer transformer = new GameTransformer(
			new EntrypointPatch(this),
			new BrandingPatch(),
			new EntrypointPatchFML125(),
			new TinyFDPatch());

	@Override
	public String getGameId() {
		return "minecraft";
	}

	@Override
	public String getGameName() {
		return "Minecraft";
	}

	@Override
	public String getRawGameVersion() {
		return versionData.getRaw();
	}

	@Override
	public String getNormalizedGameVersion() {
		return versionData.getNormalized();
	}

	public Path getGameJar() {
		return gameJars.get(0);
	}

	@Override
	public String getEntrypoint() {
		return entrypoint;
	}

	@Override
	public Path getLaunchDirectory() {
		if (arguments == null) {
			return Paths.get(".");
		}

		return getLaunchDirectory(arguments);
	}

	@Override
	public boolean isObfuscated() {
		return true; // generally yes...
	}

	@Override
	public boolean requiresUrlClassLoader() {
		return hasModLoader;
	}

	@Override
	public boolean isEnabled() {
		return System.getProperty(SystemProperties.SKIP_MC_PROVIDER) == null;
	}

	@Override
	public boolean locateGame(PunchLauncher launcher, String[] args) {
		this.arguments = new Arguments();
		arguments.parse(args);

		try {
			LibClassifier<McLibrary> classifier = new LibClassifier<>(McLibrary.class,this);
			McLibrary envGameLib = McLibrary.MC_CLIENT;
			Path commonGameJar = GameProviderHelper.getCommonGameJar();
			Path envGameJar = GameProviderHelper.getEnvGameJar();
			boolean commonGameJarDeclared = commonGameJar != null;

			if (envGameJar != null) {
				classifier.process(envGameJar);
			}

			classifier.process(launcher.getClassPath());

			envGameJar = classifier.getOrigin(envGameLib);
			if (envGameJar == null) return false;

			gameJars.add(envGameJar);

			if (commonGameJar != null && !commonGameJar.equals(envGameJar)) {
				gameJars.add(commonGameJar);
			}

			entrypoint = classifier.getClassName(envGameLib);
			realmsJar = classifier.getOrigin(McLibrary.REALMS);
			hasModLoader = classifier.has(McLibrary.MODLOADER);
			log4jAvailable = classifier.has(McLibrary.LOG4J_API) && classifier.has(McLibrary.LOG4J_CORE);
			slf4jAvailable = classifier.has(McLibrary.SLF4J_API) && classifier.has(McLibrary.SLF4J_CORE);
			boolean hasLogLib = log4jAvailable || slf4jAvailable;

			Log.configureBuiltin(hasLogLib, !hasLogLib);

			for (McLibrary lib : McLibrary.LOGGING) {
				Path path = classifier.getOrigin(lib);

				if (path != null) {
					if (hasLogLib) {
						logJars.add(path);
					} else if (!gameJars.contains(path)) {
						miscGameLibraries.add(path);
					}
				}
			}

			miscGameLibraries.addAll(classifier.getUnmatchedOrigins());
			validParentClassPath = classifier.getSystemLibraries();
		} catch (IOException e) {
			throw ExceptionUtil.wrap(e);
		}

		// expose obfuscated jar locations for mods to more easily remap code from obfuscated to intermediary
		ObjectShare share = PunchLoaderImpl.INSTANCE.getObjectShare();
		share.put("flint-loader:inputGameJar", gameJars.get(0)); // deprecated
		share.put("flint-loader:inputGameJars", gameJars);
		if (realmsJar != null) share.put("flint-loader:inputRealmsJar", realmsJar);

		String version = arguments.remove(Arguments.GAME_VERSION);
		if (version == null) version = System.getProperty(SystemProperties.GAME_VERSION);
		versionData = McVersionLookup.getVersion(gameJars, entrypoint, version);

		processArgumentMap(arguments);

		return true;
	}

	private static void processArgumentMap(Arguments argMap) {
		if (!argMap.containsKey("accessToken")) {
			argMap.put("accessToken", "FlintMC");
		}

		if (!argMap.containsKey("version")) {
			argMap.put("version", "Flint");
		}

		String versionType = "";

		if (argMap.containsKey("versionType") && !argMap.get("versionType").equalsIgnoreCase("release")) {
			versionType = argMap.get("versionType") + "/";
		}

		argMap.put("versionType", versionType + "Flint");

		if (!argMap.containsKey("gameDir")) {
			argMap.put("gameDir", getLaunchDirectory(argMap).toAbsolutePath().normalize().toString());
		}
	}

	private static Path getLaunchDirectory(Arguments argMap) {
		return Paths.get(argMap.getOrDefault("gameDir", "."));
	}

	@Override
	public void initialize(PunchLauncher launcher) {
		launcher.setValidParentClassPath(validParentClassPath);

		if (isObfuscated()) {
			Map<String, Path> obfJars = new HashMap<>(3);
			String[] names = new String[gameJars.size()];

			for (int i = 0; i < gameJars.size(); i++) {
				String name;

				if (i == 0) {
					name = "client";
				} else if (i == 1) {
					name = "common";
				} else {
					name = String.format(Locale.ENGLISH, "extra-%d", i - 2);
				}

				obfJars.put(name, gameJars.get(i));
				names[i] = name;
			}

			if (realmsJar != null) {
				obfJars.put("realms", realmsJar);
			}

			obfJars = GameProviderHelper.deobfuscate(obfJars,
					getGameId(), getNormalizedGameVersion(),
					getLaunchDirectory(),
					launcher);

			for (int i = 0; i < gameJars.size(); i++) {
				Path newJar = obfJars.get(names[i]);
				Path oldJar = gameJars.set(i, newJar);

				if (logJars.remove(oldJar)) logJars.add(newJar);
			}

			realmsJar = obfJars.get("realms");
		}

		// Load the logger libraries on the platform CL when in a unit test
		if (!logJars.isEmpty() && !Boolean.getBoolean(SystemProperties.UNIT_TEST)) {
			for (Path jar : logJars) {
				if (gameJars.contains(jar)) {
					launcher.addToClassPath(jar, ALLOWED_EARLY_CLASS_PREFIXES);
				} else {
					launcher.addToClassPath(jar);
				}
			}
		}

		setupLogHandler(launcher, true);

		transformer.locateEntrypoints(launcher, gameJars);
	}

	private void setupLogHandler(PunchLauncher launcher, boolean useTargetCl) {
		System.setProperty("log4j2.formatMsgNoLookups", "true"); // lookups are not used by mc and cause issues with older log4j2 versions

		try {
			final String logHandlerClsName;

			if (log4jAvailable) {
				logHandlerClsName = "net.flintloader.punch.impl.game.minecraft.Log4jLogHandler";
			} else if (slf4jAvailable) {
				logHandlerClsName = "net.flintloader.punch.impl.game.minecraft.Slf4jLogHandler";
			} else {
				return;
			}

			ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
			Class<?> logHandlerCls;

			if (useTargetCl) {
				Thread.currentThread().setContextClassLoader(launcher.getTargetClassLoader());
				logHandlerCls = launcher.loadIntoTarget(logHandlerClsName);
			} else {
				logHandlerCls = Class.forName(logHandlerClsName);
			}

			Log.init((LogHandler) logHandlerCls.getConstructor().newInstance());
			Thread.currentThread().setContextClassLoader(prevCl);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Arguments getArguments() {
		return arguments;
	}

	@Override
	public String[] getLaunchArguments(boolean sanitize) {
		if (arguments == null) return new String[0];

		String[] ret = arguments.toArray();
		if (!sanitize) return ret;

		int writeIdx = 0;

		for (int i = 0; i < ret.length; i++) {
			String arg = ret[i];

			if (i + 1 < ret.length
					&& arg.startsWith("--")
					&& SENSITIVE_ARGS.contains(arg.substring(2).toLowerCase(Locale.ENGLISH))) {
				i++; // skip value
			} else {
				ret[writeIdx++] = arg;
			}
		}

		if (writeIdx < ret.length) ret = Arrays.copyOf(ret, writeIdx);

		return ret;
	}

	@Override
	public GameTransformer getEntrypointTransformer() {
		return transformer;
	}

	@Override
	public boolean canOpenErrorGui() {
		if (arguments == null) {
			return true;
		}

		List<String> extras = arguments.getExtraArgs();
		return !extras.contains("nogui") && !extras.contains("--nogui");
	}

	@Override
	public boolean hasAwtSupport() {
		// MC always sets -XstartOnFirstThread for LWJGL
		return !LoaderUtil.hasMacOs();
	}

	@Override
	public void unlockClassPath(PunchLauncher launcher) {
		for (Path gameJar : gameJars) {
			if (logJars.contains(gameJar)) {
				launcher.setAllowedPrefixes(gameJar);
			} else {
				launcher.addToClassPath(gameJar);
			}
		}

		if (realmsJar != null) launcher.addToClassPath(realmsJar);

		for (Path lib : miscGameLibraries) {
			launcher.addToClassPath(lib);
		}
	}

	@Override
	public void launch(ClassLoader loader) {
		String targetClass = entrypoint;

		if (targetClass.contains("Applet")) {
			targetClass = "net.flintloader.punch.impl.game.minecraft.applet.AppletMain";
		}

		MethodHandle invoker;

		try {
			Class<?> c = loader.loadClass(targetClass);
			invoker = MethodHandles.lookup().findStatic(c, "main", MethodType.methodType(void.class, String[].class));
		} catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
			throw new FormattedException("Failed to start Minecraft", e);
		}

		try {
			invoker.invokeExact(arguments.toArray());
		} catch (Throwable t) {
			throw new FormattedException("Minecraft has crashed!", t);
		}
	}
}
