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
package net.flintloader.punch.impl.launch.punch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.Objects;

import net.flintloader.punch.impl.game.GameProvider;
import net.flintloader.punch.impl.launch.punch.PunchClassDelegate.ClassLoaderAccess;

// class name referenced by string constant in util.impl.net.flintloader.punch.LoaderUtil.verifyNotInTargetCl
final class PunchClassLoader extends SecureClassLoader implements ClassLoaderAccess {
	private static final class DynamicURLClassLoader extends URLClassLoader {
		private DynamicURLClassLoader(URL[] urls) {
			super(urls, new DummyClassLoader());
		}

		@Override
		public void addURL(URL url) {
			super.addURL(url);
		}

		static {
			registerAsParallelCapable();
		}
	}

	private final DynamicURLClassLoader urlLoader;
	private final ClassLoader originalLoader;
	private final PunchClassDelegate<PunchClassLoader> delegate;

	PunchClassLoader(boolean isDevelopment, GameProvider provider) {
		super(new DynamicURLClassLoader(new URL[0]));
		this.originalLoader = getClass().getClassLoader();
		this.urlLoader = (DynamicURLClassLoader) getParent();
		this.delegate = new PunchClassDelegate<>(isDevelopment, this, originalLoader, provider);
	}

	PunchClassDelegate<?> getDelegate() {
		return delegate;
	}

	@Override
	public URL getResource(String name) {
		Objects.requireNonNull(name);

		URL url = urlLoader.getResource(name);

		if (url == null) {
			url = originalLoader.getResource(name);
		}

		return url;
	}

	@Override
	public URL findResource(String name) {
		Objects.requireNonNull(name);

		return urlLoader.findResource(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		Objects.requireNonNull(name);

		InputStream inputStream = urlLoader.getResourceAsStream(name);

		if (inputStream == null) {
			inputStream = originalLoader.getResourceAsStream(name);
		}

		return inputStream;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Objects.requireNonNull(name);

		final Enumeration<URL> resources = urlLoader.getResources(name);

		if (!resources.hasMoreElements()) {
			return originalLoader.getResources(name);
		}

		return resources;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return delegate.loadClass(name, resolve);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return delegate.tryLoadClass(name, false);
	}

	@Override
	public void addUrlFwd(URL url) {
		urlLoader.addURL(url);
	}

	@Override
	public URL findResourceFwd(String name) {
		return urlLoader.findResource(name);
	}

	@Override
	public Package getPackageFwd(String name) {
		return super.getPackage(name);
	}

	@Override
	public Package definePackageFwd(String name, String specTitle, String specVersion, String specVendor,
			String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
		return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
	}

	@Override
	public Object getClassLoadingLockFwd(String name) {
		return super.getClassLoadingLock(name);
	}

	@Override
	public Class<?> findLoadedClassFwd(String name) {
		return super.findLoadedClass(name);
	}

	@Override
	public Class<?> defineClassFwd(String name, byte[] b, int off, int len, CodeSource cs) {
		return super.defineClass(name, b, off, len, cs);
	}

	@Override
	public void resolveClassFwd(Class<?> cls) {
		super.resolveClass(cls);
	}

	static {
		registerAsParallelCapable();
	}
}
