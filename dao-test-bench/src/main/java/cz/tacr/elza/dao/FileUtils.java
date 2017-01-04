package cz.tacr.elza.dao;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.yaml.snakeyaml.Yaml;

public class FileUtils {

	private static final ThreadLocal<Yaml> YAML_INSTANCE = new ThreadLocal<Yaml>() {
		@Override
		protected Yaml initialValue() {
			return new Yaml();
		}
	};

	public static void createYamlFile(Path path, Object content) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.CREATE_NEW,
				StandardOpenOption.WRITE)) {
			YAML_INSTANCE.get().dump(content, bw);
		}
	}

	public static void createFile(Path path, String content) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.CREATE_NEW,
				StandardOpenOption.WRITE)) {
			bw.write(content);
		}
	}

	public static void saveYamlFile(Path path, Object content) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			YAML_INSTANCE.get().dump(content, bw);
		}
	}

	public static void saveFile(Path path, String content) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			bw.write(content);
		}
	}

	public static <T> T readYamlFile(Path path, Class<T> type) throws IOException {
		try (InputStream is = Files.newInputStream(path)) {
			return YAML_INSTANCE.get().loadAs(is, type);
		}
	}

	public static String readFirstLine(Path path) throws IOException {
		return Files.lines(path).findFirst().orElse(null);
	}
}
