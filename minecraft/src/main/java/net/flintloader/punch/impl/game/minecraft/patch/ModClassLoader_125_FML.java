/**
* Copyright 2016 FabricMC
* Copyright 2023 HypherionSA and contributors
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
package net.flintloader.punch.impl.game.minecraft.patch;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import net.flintloader.punch.impl.PunchLoaderImpl;
import net.flintloader.punch.impl.game.minecraft.MinecraftGameProvider;
import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.util.UrlUtil;

/**
 * Wrapper class replacing pre-1.3 FML's ModClassLoader (which relies on
 * URLClassLoader implementation details - no longer applicable in Java 9+)
 * with an implementation effectively wrapping Punch.
 */
public class ModClassLoader_125_FML extends URLClassLoader {
	private URL[] localUrls;

	public ModClassLoader_125_FML() {
		super(new URL[0], PunchLauncherBase.getLauncher().getTargetClassLoader());
		localUrls = new URL[0];
	}

	@Override
	protected void addURL(URL url) {
		PunchLauncherBase.getLauncher().addToClassPath(UrlUtil.asPath(url));

		URL[] newLocalUrls = new URL[localUrls.length + 1];
		System.arraycopy(localUrls, 0, newLocalUrls, 0, localUrls.length);
		newLocalUrls[localUrls.length] = url;
		localUrls = newLocalUrls;
	}

	@Override
	public URL[] getURLs() {
		return localUrls;
	}

	@Override
	public URL findResource(final String name) {
		return getParent().getResource(name);
	}

	@Override
	public Enumeration<URL> findResources(final String name) throws IOException {
		return getParent().getResources(name);
	}

	/**
	 * This is used to add mods to the classpath.
	 * @param file The mod file.
	 * @throws MalformedURLException If the File->URL transformation fails.
	 */
	public void addFile(File file) throws MalformedURLException {
		try {
			addURL(UrlUtil.asUrl(file));
		} catch (MalformedURLException e) {
			throw new MalformedURLException(e.getMessage());
		}
	}

	/**
	 * This is used to find the Minecraft .JAR location.
	 *
	 * @return The "parent source" file.
	 */
	public File getParentSource() {
		return ((MinecraftGameProvider) PunchLoaderImpl.INSTANCE.getGameProvider()).getGameJar().toFile();
	}

	/**
	 * @return The "parent source" files array.
	 */
	public File[] getParentSources() {
		return new File[] { getParentSource() };
	}

	static {
		registerAsParallelCapable();
	}
}
