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

import net.flintloader.punch.impl.PunchLoaderImpl;
import net.flintloader.punch.impl.launch.PunchLauncherBase;
import net.flintloader.punch.impl.transformer.PunchTransformer;
import net.minecraft.launchwrapper.IClassTransformer;

public class PunchClassTransformer implements IClassTransformer {
	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		boolean isDevelopment = PunchLauncherBase.getLauncher().isDevelopment();
		byte[] input = PunchLoaderImpl.INSTANCE.getGameProvider().getEntrypointTransformer().transform(name);

		if (input != null) {
			return PunchTransformer.transform(isDevelopment, name, input);
		} else {
			if (bytes != null) {
				return PunchTransformer.transform(isDevelopment, name, bytes);
			} else {
				return null;
			}
		}
	}
}
