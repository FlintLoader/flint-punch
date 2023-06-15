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

import java.util.function.Consumer;
import java.util.function.Function;

import net.flintloader.punch.impl.game.patch.GamePatch;
import net.flintloader.punch.impl.launch.PunchLauncher;
import net.flintloader.punch.impl.launch.punch.Punch;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

public class EntrypointPatchFML125 extends GamePatch {
	private static final String FROM = ModClassLoader_125_FML.class.getName();
	private static final String TO = "cpw.mods.fml.common.ModClassLoader";
	private static final String FROM_INTERNAL = FROM.replace('.', '/');
	private static final String TO_INTERNAL = "cpw/mods/fml/common/ModClassLoader";

	@Override
	public void process(PunchLauncher launcher, Function<String, ClassReader> classSource, Consumer<ClassNode> classEmitter) {
		if (classSource.apply(TO) != null
				&& classSource.apply("cpw.mods.fml.relauncher.FMLRelauncher") == null) {
			if (!(launcher instanceof Punch)) {
				throw new RuntimeException("1.2.5 FML patch only supported on Punch!");
			}

			Log.debug(LogCategory.GAME_PATCH, "Detected 1.2.5 FML - Punching ModClassLoader...");

			ClassNode patchedClassLoader = readClass(classSource.apply(FROM));
			ClassNode remappedClassLoader = new ClassNode();

			patchedClassLoader.accept(new ClassRemapper(remappedClassLoader, new Remapper() {
				@Override
				public String map(String internalName) {
					return FROM_INTERNAL.equals(internalName) ? TO_INTERNAL : internalName;
				}
			}));

			classEmitter.accept(remappedClassLoader);
		}
	}
}
