package io.github.astrarre.mappingpoet.manifest;

import java.io.File;
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
