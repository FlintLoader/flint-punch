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
package net.flintloader.punch.impl.transformer;

import net.flintloader.punch.impl.PunchLoaderImpl;
import net.flintloader.punch.impl.launch.PunchLauncherBase;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import net.fabricmc.accesswidener.AccessWidenerClassVisitor;

public final class PunchTransformer {
	public static byte[] transform(boolean isDevelopment, String name, byte[] bytes) {
		boolean isMinecraftClass = name.startsWith("net.minecraft.") || name.startsWith("com.mojang.blaze3d.") || name.indexOf('.') < 0;
		boolean transformAccess = isMinecraftClass && PunchLauncherBase.getLauncher().getMappingConfiguration().requiresPackageAccessHack();
		boolean environmentStrip = !isMinecraftClass || isDevelopment;
		boolean applyAccessWidener = isMinecraftClass && PunchLoaderImpl.INSTANCE.getAccessWidener().getTargets().contains(name);

		if (!transformAccess && !environmentStrip && !applyAccessWidener) {
			return bytes;
		}

		ClassReader classReader = new ClassReader(bytes);
		ClassWriter classWriter = new ClassWriter(classReader, 0);
		ClassVisitor visitor = classWriter;
		int visitorCount = 0;

		if (applyAccessWidener) {
			visitor = AccessWidenerClassVisitor.createClassVisitor(PunchLoaderImpl.ASM_VERSION, visitor, PunchLoaderImpl.INSTANCE.getAccessWidener());
			visitorCount++;
		}

		if (transformAccess) {
			visitor = new PackageAccessFixer(PunchLoaderImpl.ASM_VERSION, visitor);
			visitorCount++;
		}

		if (visitorCount <= 0) {
			return bytes;
		}

		classReader.accept(visitor, 0);
		return classWriter.toByteArray();
	}
}
