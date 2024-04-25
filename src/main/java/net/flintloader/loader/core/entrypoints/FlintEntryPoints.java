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
package net.flintloader.loader.core.entrypoints;

import net.flintloader.loader.modules.FlintModuleMetadata;
import net.flintloader.loader.modules.ModuleList;
import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.util.ExceptionUtil;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

public class FlintEntryPoints {

	private static final Map<String, List<Entry>> entryPoints = new HashMap<>();

	private static List<Entry> getOrCreate(String key) {
		return entryPoints.computeIfAbsent(key, z -> new ArrayList<>());
	}

	public static boolean hasEntryPoints(String key) {
		return entryPoints.containsKey(key);
	}

	public static void add(FlintModuleMetadata metadata, String key, String path) {
		Log.debug(LogCategory.ENTRYPOINT, "Registering entry point %s for %s", key, metadata.getId());
		getOrCreate(key).add(new FlintEntry(metadata, path));
	}

	public static <T> List<T> getEntrypoints(String key, Class<T> type) {
		List<Entry> entries = entryPoints.get(key);
		if (entries == null) return Collections.emptyList();

		EntrypointException exception = null;
		List<T> results = new ArrayList<>(entries.size());

		for (Entry entry : entries) {
			try {
				T result = entry.getOrCreate(type);

				if (result != null) {
					results.add(result);
				}
			} catch (Throwable t) {
				if (exception == null) {
					exception = new EntrypointException(key, entry.getModuleContainer().getId(), t);
				} else {
					exception.addSuppressed(t);
				}
			}
		}

		if (exception != null) {
			throw exception;
		}

		return results;
	}

	public static <T> List<EntryPointHolder<T>> getEntrypointContainers(String key, Class<T> type) {
		List<Entry> entries = entryPoints.get(key);
		if (entries == null) return Collections.emptyList();

		List<EntryPointHolder<T>> results = new ArrayList<>(entries.size());

		for (Entry entry : entries) {
			EntryPointContainer<T> container = new EntryPointContainer<>(key, type, entry);
			results.add(container);
		}

		return results;
	}

	public static <T> void invoke(String name, Class<T> type, Consumer<? super T> invoker) {
		if (!hasEntryPoints(name)) {
			Log.debug(LogCategory.ENTRYPOINT, "No subscribers for entrypoint '%s'", name);
			return;
		}
		invokeInternal(name, type, invoker);
	}

	private static <T> void invokeInternal(String name, Class<T> type, Consumer<? super T> invoker) {
		RuntimeException exception = null;
		Collection<EntryPointHolder<T>> entryPoints = getEntrypointContainers(name, type);

		Log.debug(LogCategory.ENTRYPOINT, "Iterating over entrypoint '%s'", name);

		for (EntryPointHolder<T> container : entryPoints) {
			try {
				invoker.accept(container.getEntryPoint());
			} catch (Throwable t) {
				exception = ExceptionUtil.gatherExceptions(t,
						exception,
						exc -> new RuntimeException(String.format("Could not execute entrypoint stage '%s' due to errors, provided by '%s'!",
								name, container.getProvider().getId()),
								exc));
			}
		}

		if (exception != null) {
			throw exception;
		}
	}

	private static <T> T create(FlintModuleMetadata mod, String value, Class<T> type) throws Exception {
		String[] methodSplit = value.split("::");

		if (methodSplit.length >= 3) {
			throw new Exception("Invalid handle format: " + value);
		}

		Class<?> c;

		try {
			c = Class.forName(methodSplit[0], true, PunchLauncherBase.getLauncher().getTargetClassLoader());
		} catch (ClassNotFoundException e) {
			throw new Exception(e);
		}

		if (methodSplit.length == 1) {
			if (type.isAssignableFrom(c)) {
				try {
					return (T) c.getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					throw new Exception(e);
				}
			} else {
				throw new Exception("Class " + c.getName() + " cannot be cast to " + type.getName() + "!");
			}
		} else /* length == 2 */ {
			List<Method> methodList = new ArrayList<>();

			for (Method m : c.getDeclaredMethods()) {
				if (!(m.getName().equals(methodSplit[1]))) {
					continue;
				}

				methodList.add(m);
			}

			try {
				Field field = c.getDeclaredField(methodSplit[1]);
				Class<?> fType = field.getType();

				if ((field.getModifiers() & Modifier.STATIC) == 0) {
					throw new Exception("Field " + value + " must be static!");
				}

				if (!methodList.isEmpty()) {
					throw new Exception("Ambiguous " + value + " - refers to both field and method!");
				}

				if (!type.isAssignableFrom(fType)) {
					throw new Exception("Field " + value + " cannot be cast to " + type.getName() + "!");
				}

				return (T) field.get(null);
			} catch (NoSuchFieldException e) {
				// ignore
			} catch (IllegalAccessException e) {
				throw new Exception("Field " + value + " cannot be accessed!", e);
			}

			if (!type.isInterface()) {
				throw new Exception("Cannot proxy method " + value + " to non-interface type " + type.getName() + "!");
			}

			if (methodList.isEmpty()) {
				throw new Exception("Could not find " + value + "!");
			} else if (methodList.size() >= 2) {
				throw new Exception("Found multiple method entries of name " + value + "!");
			}

			final Method targetMethod = methodList.get(0);
			Object object = null;

			if ((targetMethod.getModifiers() & Modifier.STATIC) == 0) {
				try {
					object = c.getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					throw new Exception(e);
				}
			}

			MethodHandle handle;

			try {
				handle = MethodHandles.lookup()
						.unreflect(targetMethod);
			} catch (Exception ex) {
				throw new Exception(ex);
			}

			if (object != null) {
				handle = handle.bindTo(object);
			}

			// uses proxy as well, but this handles default and object methods
			try {
				return MethodHandleProxies.asInterfaceInstance(type, handle);
			} catch (Exception ex) {
				throw new Exception(ex);
			}
		}
	}

	interface Entry {
		<T> T getOrCreate(Class<T> type) throws Exception;
		boolean isOptional();
		FlintModuleMetadata getModuleContainer();
	}

	static class FlintEntry implements Entry {

		private final FlintModuleMetadata mod;
		private final String value;
		private final Map<Class<?>, Object> instanceMap;

		public FlintEntry(FlintModuleMetadata metadata, String value) {
			this.mod = metadata;
			this.value = value;
			this.instanceMap = new IdentityHashMap<>(1);
		}

		@Override
		public String toString() {
			return mod.getId() + "->(0.3.x)" + value;
		}

		@SuppressWarnings("unchecked")
		@Override
		public synchronized <T> T getOrCreate(Class<T> type) throws Exception {
			// this impl allows reentrancy (unlike computeIfAbsent)
			T ret = (T) instanceMap.get(type);

			if (ret == null) {
				ret = create(mod, value, type);
				assert ret != null;
				T prev = (T) instanceMap.putIfAbsent(type, ret);
				if (prev != null) ret = prev;
			}

			return ret;
		}

		@Override
		public boolean isOptional() {
			return false;
		}

		@Override
		public FlintModuleMetadata getModuleContainer() {
			return mod;
		}
	}

}
