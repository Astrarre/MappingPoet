/*
 * Copyright (c) 2020 FabricMC, Astrarre
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.mappingpoet;

import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.FieldDef;
import net.fabricmc.mapping.tree.MethodDef;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;
import net.fabricmc.mappings.EntryTriple;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.github.astrarre.mappingpoet.ManifestRemapper;
import org.objectweb.asm.commons.Remapper;

//Taken from loom
//Modified by HalfOf2
public class RemappingMappingsStore implements MappingsStore {
	// todo reverse this
	public final Map<String, ClassDef> classes = new HashMap<>();
	public final Map<EntryTriple, FieldDef> fields = new HashMap<>();
	public final Map<EntryTriple, MethodDef> methods = new HashMap<>();
	private final Remapper remapper;
	private final String namespace = "named";

	public RemappingMappingsStore(TinyTree tree, Remapper remapper) {
		this.remapper = remapper;
		for (ClassDef classDef : tree.getClasses()) {
			final String className = classDef.getName(namespace);

			classes.put(className, classDef);

			for (FieldDef fieldDef : classDef.getFields()) {
				fields.put(new EntryTriple(className, fieldDef.getName(namespace), fieldDef.getDescriptor(namespace)), fieldDef);
			}

			for (MethodDef methodDef : classDef.getMethods()) {
				methods.put(new EntryTriple(className, methodDef.getName(namespace), methodDef.getDescriptor(namespace)), methodDef);
			}
		}
	}

	/**
	 * (map from abstracter output -> minecraft)
	 * @param remapper source -> mapping
	 */
	public RemappingMappingsStore(Path tinyFile, Remapper remapper) {
		this(readMappings(tinyFile), remapper);
	}

	public RemappingMappingsStore(Path tinyFile, Map<String, String> manifest) {
		this(tinyFile, new ManifestRemapper(manifest));
	}


	public RemappingMappingsStore(Path tinyFile, Path manifest) {
		this(tinyFile, new ManifestRemapper(manifest));
	}

	@Override
	public String getClassDoc(String className) {
		ClassDef classDef = classes.get(remapper.map(className));
		return classDef != null ? classDef.getComment() : null;
	}

	@Override
	public String getFieldDoc(EntryTriple fieldEntry) {
		FieldDef fieldDef = fields.get(new EntryTriple(remapper.map(fieldEntry.getOwner()), fieldEntry.getName(), remapper.mapDesc(fieldEntry.getDesc())));
		return fieldDef != null ? fieldDef.getComment() : null;
	}

	@Override
	public Map.Entry<String, String> getParamNameAndDoc(Function<String, Collection<String>> superGetters, EntryTriple methodEntry, int index) {
		MethodDef methodDef = searchMethod(superGetters, methodEntry);
		if (methodDef != null) {
			if (methodDef.getParameters().isEmpty()) {
				return null;
			}
			return methodDef.getParameters().stream()
					.filter(param -> param.getLocalVariableIndex() == index)
					.map(param -> new AbstractMap.SimpleImmutableEntry<>(param.getName(namespace), param.getComment()))
					.findFirst()
					.orElse(null);
		}
		return null;
	}

	@Override
	public String getMethodDoc(Function<String, Collection<String>> superGetters, EntryTriple methodEntry) {
		MethodDef methodDef = searchMethod(superGetters, methodEntry);

		if (methodDef != null) {
			return methodDef.getComment(); // comment doc handled separately by javapoet
		}

		return null;
	}

	private MethodDef searchMethod(Function<String, Collection<String>> superGetters, EntryTriple methodEntry) {
		String className = methodEntry.getOwner();
		if (!classes.containsKey(className)) {
			return null;
		}

		if (methods.containsKey(methodEntry)) {
			return methods.get(new EntryTriple(remapper.map(methodEntry.getOwner()), methodEntry.getName(), remapper.mapMethodDesc(methodEntry.getDesc()))); // Nullable!
		}

		for (String superName : superGetters.apply(className)) {
			EntryTriple triple = new EntryTriple(superName, methodEntry.getName(), methodEntry.getDesc());
			MethodDef ret = searchMethod(superGetters, triple);
			if (ret != null) {
				methods.put(triple, ret);
				return ret;
			}
		}

		methods.put(methodEntry, null);
		return null;
	}

	private static TinyTree readMappings(Path input) {
		try (BufferedReader reader = Files.newBufferedReader(input)) {
			return TinyMappingFactory.loadWithDetection(reader);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read mappings", e);
		}
	}
}
