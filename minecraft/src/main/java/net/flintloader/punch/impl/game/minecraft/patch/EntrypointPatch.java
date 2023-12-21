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
package net.flintloader.punch.impl.game.minecraft.patch;

import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

import net.flintloader.punch.api.Version;
import net.flintloader.punch.api.VersionParsingException;
import net.flintloader.punch.api.metadata.version.VersionPredicate;
import net.flintloader.punch.impl.game.minecraft.Hooks;
import net.flintloader.punch.impl.game.minecraft.MinecraftGameProvider;
import net.flintloader.punch.impl.game.patch.GamePatch;
import net.flintloader.punch.impl.launch.PunchLauncher;
import net.flintloader.punch.impl.util.log.Log;
import net.flintloader.punch.impl.util.log.LogCategory;
import net.flintloader.punch.impl.util.version.VersionParser;
import net.flintloader.punch.impl.util.version.VersionPredicateParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class EntrypointPatch extends GamePatch {

	private static final VersionPredicate VERSION_1_19_4 = createVersionPredicate(">=1.19.4-");
	private final MinecraftGameProvider gameProvider;

	public EntrypointPatch(MinecraftGameProvider gameProvider) {
		this.gameProvider = gameProvider;
	}

	private void finishEntrypoint(ListIterator<AbstractInsnNode> it) {
		String methodName = String.format("start%s", "Client");
		it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Hooks.INTERNAL_NAME, methodName, "(Ljava/io/File;Ljava/lang/Object;)V", false));
	}

	@Override
	public void process(PunchLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
		String entrypoint = launcher.getEntrypoint();
		Version gameVersion = getGameVersion();

		if (!entrypoint.startsWith("net.minecraft.") && !entrypoint.startsWith("com.mojang.")) {
			return;
		}

		String gameEntrypoint = null;
		boolean serverHasFile = true;
		boolean isApplet = entrypoint.contains("Applet");
		ClassNode mainClass = classSource.apply(entrypoint);

		if (mainClass == null) {
			throw new RuntimeException("Could not load main class " + entrypoint + "!");
		}

		// Main -> Game entrypoint search
		//
		// -- CLIENT --
		// pre-1.6 (seems to hold to 0.0.11!): find the only non-static non-java-packaged Object field
		// 1.6.1+: [client].start() [INVOKEVIRTUAL]
		// 19w04a: [client].<init> [INVOKESPECIAL] -> Thread.start()
		// -- SERVER --
		// (1.5-1.7?)-:     Just find it instantiating itself.
		// (1.6-1.8?)+:     an <init> starting with java.io.File can be assumed to be definite
		// (20w20b-20w21a): Now has its own main class, that constructs the server class. Find a specific regex string in the class.
		// (20w22a)+:       Datapacks are now reloaded in main. To ensure that mods load correctly, inject into Main after --safeMode check.

		boolean is20w22aServerOrHigher = false;

		// pre-1.6 route
		List<FieldNode> newGameFields = findFields(mainClass,
				(f) -> !isStatic(f.access) && f.desc.startsWith("L") && !f.desc.startsWith("Ljava/")
		);

		if (newGameFields.size() == 1) {
			gameEntrypoint = Type.getType(newGameFields.get(0).desc).getClassName();
		}

		if (gameEntrypoint == null) {
			// main method searches
			MethodNode mainMethod = findMethod(mainClass, (method) -> method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V") && isPublicStatic(method.access));

			if (mainMethod == null) {
				throw new RuntimeException("Could not find main method in " + entrypoint + "!");
			}

			if (mainMethod.instructions.size() < 10) {
				// 22w24+ forwards to another method in the same class instead of processing in main() directly, use that other method instead if that's the case
				MethodInsnNode invocation = null;

				for (AbstractInsnNode insn : mainMethod.instructions) {
					MethodInsnNode methodInsn;

					if (invocation == null
							&& insn.getType() == AbstractInsnNode.METHOD_INSN
							&& (methodInsn = (MethodInsnNode) insn).owner.equals(mainClass.name)) {
						// capture first method insn to the same class
						invocation = methodInsn;
					} else if (insn.getOpcode() > Opcodes.ALOAD // ignore constant and variable loads as well as NOP, labels and line numbers
							&& insn.getOpcode() != Opcodes.RETURN) { // and RETURN
						// found unexpected insn for a simple forwarding method
						invocation = null;
						break;
					}
				}

				if (invocation != null) { // simple forwarder confirmed, use its target for further processing
					final MethodInsnNode reqMethod = invocation;
					mainMethod = findMethod(mainClass, m -> m.name.equals(reqMethod.name) && m.desc.equals(reqMethod.desc));
				}
			}

			if (gameEntrypoint == null) {
				// modern method search routes
				MethodInsnNode newGameInsn = (MethodInsnNode) findInsn(mainMethod,
						(insn) -> (insn.getOpcode() == Opcodes.INVOKESPECIAL || insn.getOpcode() == Opcodes.INVOKEVIRTUAL) && !((MethodInsnNode) insn).owner.startsWith("java/"),
								true
						);

				if (newGameInsn != null) {
					gameEntrypoint = newGameInsn.owner.replace('/', '.');
					serverHasFile = newGameInsn.desc.startsWith("(Ljava/io/File;");
				}
			}
		}

		if (gameEntrypoint == null) {
			throw new RuntimeException("Could not find game constructor in " + entrypoint + "!");
		}

		Log.debug(LogCategory.GAME_PATCH, "Found game constructor: %s -> %s", entrypoint, gameEntrypoint);
		ClassNode gameClass;

		if (gameEntrypoint.equals(entrypoint) || is20w22aServerOrHigher) {
			gameClass = mainClass;
		} else {
			gameClass = classSource.apply(gameEntrypoint);
			if (gameClass == null) throw new RuntimeException("Could not load game class " + gameEntrypoint + "!");
		}

		MethodNode gameMethod = null;
		MethodNode gameConstructor = null;
		AbstractInsnNode lwjglLogNode = null;
		AbstractInsnNode currentThreadNode = null;
		int gameMethodQuality = 0;

		if (!is20w22aServerOrHigher) {
			for (MethodNode gmCandidate : gameClass.methods) {
				if (gmCandidate.name.equals("<init>")) {
					gameConstructor = gmCandidate;

					if (gameMethodQuality < 1) {
						gameMethod = gmCandidate;
						gameMethodQuality = 1;
					}
				}

				if (!isApplet && gameMethodQuality < 2) {
					// Try to find a method with an LDC string "LWJGL Version: ".
					// This is the "init()" method, or as of 19w38a is the constructor, or called somewhere in that vicinity,
					// and is by far superior in hooking into for a well-off mod start.
					// Also try and find a Thread.currentThread() call before the LWJGL version print.

					int qual = 2;
					boolean hasLwjglLog = false;

					for (AbstractInsnNode insn : gmCandidate.instructions) {
						if (insn.getOpcode() == Opcodes.INVOKESTATIC && insn instanceof MethodInsnNode) {
							final MethodInsnNode methodInsn = (MethodInsnNode) insn;

							if ("currentThread".equals(methodInsn.name) && "java/lang/Thread".equals(methodInsn.owner) && "()Ljava/lang/Thread;".equals(methodInsn.desc)) {
								currentThreadNode = methodInsn;
							}
						} else if (insn instanceof LdcInsnNode) {
							Object cst = ((LdcInsnNode) insn).cst;

							if (cst instanceof String) {
								String s = (String) cst;

								//This log output was renamed to Backend library in 19w34a
								if (s.startsWith("LWJGL Version: ") || s.startsWith("Backend library: ")) {
									hasLwjglLog = true;

									if ("LWJGL Version: ".equals(s) || "LWJGL Version: {}".equals(s) || "Backend library: {}".equals(s)) {
										qual = 3;
										lwjglLogNode = insn;
									}

									break;
								}
							}
						}
					}

					if (hasLwjglLog) {
						gameMethod = gmCandidate;
						gameMethodQuality = qual;
					}
				}
			}
		} else {
			gameMethod = findMethod(mainClass, (method) -> method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V") && isPublicStatic(method.access));
		}

		if (gameMethod == null) {
			throw new RuntimeException("Could not find game constructor method in " + gameClass.name + "!");
		}

		boolean patched = false;
		Log.debug(LogCategory.GAME_PATCH, "Patching game constructor %s%s", gameMethod.name, gameMethod.desc);

		if (isApplet) {
			// Applet-side: field is private static File, run at end
			// At the beginning, set file field (hook)
			FieldNode runDirectory = findField(gameClass, (f) -> isStatic(f.access) && f.desc.equals("Ljava/io/File;"));

			if (runDirectory == null) {
				// TODO: Handle pre-indev versions.
				//
				// Classic has no agreed-upon run directory.
				// - level.dat is always stored in CWD. We can assume CWD is set, launchers generally adhere to that.
				// - options.txt in newer Classic versions is stored in user.home/.minecraft/. This is not currently handled,
				// but as these versions are relatively low on options this is not a huge concern.
				Log.warn(LogCategory.GAME_PATCH, "Could not find applet run directory! (If you're running pre-late-indev versions, this is fine.)");

				ListIterator<AbstractInsnNode> it = gameMethod.instructions.iterator();

				if (gameConstructor == gameMethod) {
					moveBefore(it, Opcodes.RETURN);
				}

				/*it.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
					it.add(new InsnNode(Opcodes.DUP));
					it.add(new LdcInsnNode("."));
					it.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false)); */
				it.add(new InsnNode(Opcodes.ACONST_NULL));
				it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/flintloader/punch/impl/game/minecraft/applet/AppletMain", "hookGameDir", "(Ljava/io/File;)Ljava/io/File;", false));
				it.add(new VarInsnNode(Opcodes.ALOAD, 0));
				finishEntrypoint(it);
			} else {
				// Indev and above.
				ListIterator<AbstractInsnNode> it = gameConstructor.instructions.iterator();
				moveAfter(it, Opcodes.INVOKESPECIAL); /* Object.init */
				it.add(new FieldInsnNode(Opcodes.GETSTATIC, gameClass.name, runDirectory.name, runDirectory.desc));
				it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/flintloader/punch/impl/game/minecraft/applet/AppletMain", "hookGameDir", "(Ljava/io/File;)Ljava/io/File;", false));
				it.add(new FieldInsnNode(Opcodes.PUTSTATIC, gameClass.name, runDirectory.name, runDirectory.desc));

				it = gameMethod.instructions.iterator();

				if (gameConstructor == gameMethod) {
					moveBefore(it, Opcodes.RETURN);
				}

				it.add(new FieldInsnNode(Opcodes.GETSTATIC, gameClass.name, runDirectory.name, runDirectory.desc));
				it.add(new VarInsnNode(Opcodes.ALOAD, 0));
				finishEntrypoint(it);
			}

			patched = true;
		} else {
			// Client-side:
			// - if constructor, identify runDirectory field + location, run immediately after
			// - if non-constructor (init method), head

			if (gameConstructor == null) {
				throw new RuntimeException("Non-applet client-side, but could not find constructor?");
			}

			ListIterator<AbstractInsnNode> consIt = gameConstructor.instructions.iterator();

			while (consIt.hasNext()) {
				AbstractInsnNode insn = consIt.next();
				if (insn.getOpcode() == Opcodes.PUTFIELD
						&& ((FieldInsnNode) insn).desc.equals("Ljava/io/File;")) {
					Log.debug(LogCategory.GAME_PATCH, "Run directory field is thought to be %s/%s", ((FieldInsnNode) insn).owner, ((FieldInsnNode) insn).name);

					ListIterator<AbstractInsnNode> it;

					if (gameMethod == gameConstructor) {
						it = consIt;
					} else {
						it = gameMethod.instructions.iterator();
					}

					// Add the hook just before the Thread.currentThread() call for 1.19.4 or later
					// If older 4 method insn's before the lwjgl log
					if (currentThreadNode != null && VERSION_1_19_4.test(gameVersion)) {
						moveBefore(it, currentThreadNode);
					} else if (lwjglLogNode != null) {
						moveBefore(it, lwjglLogNode);

						for (int i = 0; i < 4; i++) {
							moveBeforeType(it, AbstractInsnNode.METHOD_INSN);
						}
					}

					it.add(new VarInsnNode(Opcodes.ALOAD, 0));
					it.add(new FieldInsnNode(Opcodes.GETFIELD, ((FieldInsnNode) insn).owner, ((FieldInsnNode) insn).name, ((FieldInsnNode) insn).desc));
					it.add(new VarInsnNode(Opcodes.ALOAD, 0));
					finishEntrypoint(it);

					patched = true;
					break;
				}
			}
		}

		if (!patched) {
			throw new RuntimeException("Game constructor patch not applied!");
		}

		if (gameClass != mainClass) {
			classEmitter.accept(gameClass);
		} else {
			classEmitter.accept(mainClass);
		}

		if (isApplet) {
			Hooks.appletMainClass = entrypoint;
		}
	}

	private boolean hasSuperClass(String cls, String superCls, Function<String, ClassNode> classSource) {
		if (cls.contains("$") || (!cls.startsWith("net/minecraft") && cls.contains("/"))) {
			return false;
		}

		ClassNode classNode = classSource.apply(cls);

		return classNode != null && classNode.superName.equals(superCls);
	}

	private boolean hasStrInMethod(String cls, String methodName, String methodDesc, String str, Function<String, ClassNode> classSource) {
		if (cls.contains("$") || (!cls.startsWith("net/minecraft") && cls.contains("/"))) {
			return false;
		}

		ClassNode node = classSource.apply(cls);
		if (node == null) return false;

		for (MethodNode method : node.methods) {
			if (method.name.equals(methodName) && method.desc.equals(methodDesc)) {
				for (AbstractInsnNode insn : method.instructions) {
					if (insn instanceof LdcInsnNode) {
						Object cst = ((LdcInsnNode) insn).cst;

						if (cst instanceof String) {
							if (cst.equals(str)) {
								return true;
							}
						}
					}
				}

				break;
			}
		}

		return false;
	}

	private Version getGameVersion() {
		try {
			return VersionParser.parseSemantic(gameProvider.getNormalizedGameVersion());
		} catch (VersionParsingException e) {
			throw new RuntimeException(e);
		}
	}

	private static VersionPredicate createVersionPredicate(String predicate) {
		try {
			return VersionPredicateParser.parse(predicate);
		} catch (VersionParsingException e) {
			throw new RuntimeException(e);
		}
	}
}