package com.playwire.file.mp3.infoeditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Logger;

@Component
public class AuthorAlbumFileReader {

	Logger logger = (Logger) LoggerFactory
			.getLogger(AuthorAlbumFileReader.class);

	private static final String fileLookupDirectory = "Author List";

	private Map<String, String> authorAlbumMap = new HashMap<>();

	public AuthorAlbumFileReader() {
		getAuthorAlbumMap();
	}

	public Map<String, String> getAuthorAlbumMap() {
		if (authorAlbumMap.size() == 0) {
			read();
		}
		return authorAlbumMap;
	}

	public void read() {
		List<File> files = fetchTextFiles(fileLookupDirectory);
		for (File file : files) {
			readAuthorOrAlbum(file);
		}
		
		logger.info("##############################################");
		logger.info("Author/Album - Language Mapped:- ");
		for (Map.Entry<String, String> entry : authorAlbumMap.entrySet()) {
			logger.info(entry.getKey() + " : " + entry.getValue());
		}
	}

	private void readAuthorOrAlbum(File authorAlbumFile) {
		String fileName = authorAlbumFile.getName();
		fileName = fileName.substring(0, fileName.lastIndexOf("."));
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(authorAlbumFile));
			String st;
			while ((st = br.readLine()) != null)
				authorAlbumMap.put(st.trim(), fileName);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("error", e);
		}
	}

	private List<File> fetchTextFiles(String txtDir) {

		List<File> authorAlbumFiles = new ArrayList<>();

		ClassLoader cl = this.getClass().getClassLoader();
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
				cl);
		try {
			Resource resources[] = resolver
					.getResources("classpath:" + fileLookupDirectory + "/*");
			for (Resource resource : resources) {
				File file = resource.getFile();
				if (file.isFile()) {
					String fileName = file.getName();
					int dotIdx = fileName.lastIndexOf(".");

					if (dotIdx > 0) {
						String extension = fileName.substring(dotIdx + 1);
						if ("txt".equalsIgnoreCase(extension)) {
							authorAlbumFiles.add(file);
							logger.trace(file.getAbsolutePath());
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("error", e);
		}

		return authorAlbumFiles;
	}

}