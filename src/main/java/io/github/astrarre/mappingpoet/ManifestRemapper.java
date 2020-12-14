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

package io.github.astrarre.mappingpoet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.objectweb.asm.commons.Remapper;

public class ManifestRemapper extends Remapper {
	private final Map<String, String> manifest;

	public ManifestRemapper(Path path) {
		try {
			Properties properties = new Properties();
			properties.load(Files.newBufferedReader(path));
			this.manifest = (Map) properties;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public ManifestRemapper(Properties properties) {this((Map) properties);}
	public ManifestRemapper(Map<String, String> manifest) {this.manifest = manifest;}


	@Override
	public String map(String internalName) {
		return this.manifest.getOrDefault(internalName, internalName);
	}
}
