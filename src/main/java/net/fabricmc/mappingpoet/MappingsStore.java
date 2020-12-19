package net.fabricmc.mappingpoet;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import net.fabricmc.mappings.EntryTriple;

public interface MappingsStore {
	Map.Entry<String, String> getParamNameAndDoc(Function<String, Collection<String>> superGetters, EntryTriple methodEntry, int index);
	String getMethodDoc(Function<String, Collection<String>> superGetters, EntryTriple methodEntry);
	String getFieldDoc(EntryTriple fieldEntry);
	String getClassDoc(String className);
}
