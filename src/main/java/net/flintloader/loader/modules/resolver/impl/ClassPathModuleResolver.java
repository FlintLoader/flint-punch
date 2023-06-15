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
package net.flintloader.loader.modules.resolver.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import net.flintloader.loader.modules.FlintModuleMetadata;
import net.flintloader.loader.modules.resolver.IModuleResolver;
import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.util.UrlUtil;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;

public final class ClassPathModuleResolver implements IModuleResolver {

	@Override
	public void resolve(Map<String, FlintModuleMetadata> outList) {
		if (PunchLauncherBase.getLauncher().isDevelopment()) {
			Log.info(LogCategory.DISCOVERY, "Discovering Modules in Classpath...");

			try {
				Enumeration<URL> urlEnumeration = PunchLauncherBase.getLauncher().getTargetClassLoader().getResources("flintmodule.json");
				while (urlEnumeration.hasMoreElements()) {
					URL url = urlEnumeration.nextElement();
					InputStream in = url.openStream();

					switch (url.getProtocol()) {
						case "jar":
							String spec = url.getFile();
							int separator = spec.indexOf("!/");
							if (separator == -1) {
								throw new MalformedURLException("no !/ found in url spec:" + spec);
							}
							url = new URL(spec.substring(0, separator));
							readModuleJson(outList, in, UrlUtil.asPath(url));
							break;
						case "file":
							readModuleJson(outList, in, new File(url.toURI()).getParentFile().toPath());
							break;

						default:
							throw new RuntimeException("Unsupported protocol: " + url);
					}
				}

			} catch (IOException | URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
