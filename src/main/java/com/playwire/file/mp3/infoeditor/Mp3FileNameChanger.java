package com.playwire.file.mp3.infoeditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.qos.logback.classic.Logger;

@Component
public class Mp3FileNameChanger {

	Logger logger = (Logger) LoggerFactory.getLogger(Mp3FileNameChanger.class);

	public static final String MP3_ROOT_DIR = "D:\\MusicTest";

	@Autowired
	private AuthorAlbumFileReader authorAlbumFileReader;

	private Map<String, String> replaceNameMap = new HashMap<>();

	public Mp3FileNameChanger() {
		if (replaceNameMap.size() == 0) {
			readReplaceFile();
		}

		logger.info("##############################################");
		logger.info("Author/Album - Text to replace:- ");
		for (Map.Entry<String, String> entry : replaceNameMap.entrySet()) {
			logger.info(entry.getKey() + " : " + entry.getValue());
		}
	}

	public void process() {

		// this audio file has metadata embedded in xmp (extensible metadata
		// platform) standard
		// created by adobe systems inc. xmp standardizes the definition,
		// creation, and
		// processing of extensible metadata.

		logger.info("##############################################");

		List<File> mp3Files = new ArrayList<>();

		List<File> nonMp3Files = new ArrayList<>();

		fetchMp3Files(MP3_ROOT_DIR, mp3Files, nonMp3Files);

		int nonMp3FileCount = 0;

		for (File file : nonMp3Files) {
			logger.info(++nonMp3FileCount + " --- " + file.getAbsolutePath());
		}

		List<File> unProcessedMp3Files = new ArrayList<>();

		int fileCount = 0;

		for (File mp3File : mp3Files) {

			logger.trace(mp3File.getName());

			try {

				Parser parser = new Mp3Parser();
				Metadata metadata = new Metadata();
				InputStream input = TikaInputStream
						.get(new FileInputStream(mp3File));
				ContentHandler handler = new DefaultHandler();
				ParseContext parsectx = new ParseContext();
				parser.parse(input, handler, metadata, parsectx);
				input.close();

				String[] metadatanames = metadata.names();

				for (String name : metadatanames) {
					logger.trace(name + ": " + metadata.get(name));
				}

				// retrieve the necessary info from metadata
				// names - title, xmpdm:artist etc. - mentioned below may differ
				// based
				// on the standard used for processing and storing standardized
				// and/or
				// proprietary information relating to the contents of a file.

				String title = metadata.get("title");
				String album = metadata.get("xmpDM:album");
				String author = metadata.get("Author");

				logger.trace("title--: " + title);
				logger.trace("album--: " + album);
				logger.trace("author--: " + author);

				if (title != null && !title.isEmpty()) {

					// album = formatText(album);

					String newFileName = getSuitableFileName(album, author);

					if (newFileName == null) {
						unProcessedMp3Files.add(mp3File);
						continue;
					}

					logger.info(String.valueOf(++fileCount));
					logger.info("Old Name: " + mp3File.getName());

					title = formatText(title);

					newFileName = newFileName + " - " + title + ".mp3";

					logger.info("New Name: " + newFileName);

					String absoluteFilePath = mp3File.getAbsolutePath();
					int lastPathIdx = absoluteFilePath
							.lastIndexOf(File.separator);
					String filePath = absoluteFilePath
							.substring(0, lastPathIdx + 1).trim();

					// mp3File.renameTo(
					// new File(filePath + File.separator + newFileName));

					Path path;
					try {
						path = Files.move(Paths.get(mp3File.getAbsolutePath()),
								Paths.get(filePath + File.separator
										+ newFileName));
						if (path != null) {
							logger.info(
									"Renamed successfully to " + newFileName);
						} else {
							unProcessedMp3Files.add(mp3File);
							logger.info("Renamed failed");
						}
					} catch (IOException e) {
						unProcessedMp3Files.add(mp3File);
						e.printStackTrace();
						logger.error("Renamed failed", e);// TODO rename
															// duplicate file
															// and try to save
															// again
					}

				} else {
					unProcessedMp3Files.add(mp3File);// display reason for not
														// processing
				}
			} catch (IOException | SAXException | TikaException
					| NegativeArraySizeException e) {
				unProcessedMp3Files.add(mp3File);
				e.printStackTrace();
				logger.error("error", e);
			}

			logger.info("------------------------------");
		}

		logger.info("Total number of mp3 files: " + mp3Files.size());
		logger.info("Total number of non mp3 files: " + nonMp3FileCount);
		logger.info("Total number of non processed mp3 files: "
				+ unProcessedMp3Files.size());

		File file = new File(MP3_ROOT_DIR + File.separator + "UnProcessed");
		boolean fileCreated = file.mkdir();
		if (fileCreated) {
			int count = 0;
			for (File mp3File : unProcessedMp3Files) {
				logger.info(++count + ". un processed mp3 file: "
						+ mp3File.getAbsolutePath());
				Path path;
				try {
					path = Files.move(Paths.get(mp3File.getAbsolutePath()),
							Paths.get(file.getAbsolutePath() + File.separator
									+ mp3File.getName()));
					if (path != null) {
						logger.info("Moved successfully to " + file.getName()
								+ " directory");
					} else {
						logger.info("Failed to move the file");
					}
				} catch (IOException e) {
					e.printStackTrace();
					logger.error("error", e);
				}
			}

		}
	}

	private String getSuitableFileName(String mp3Album, String mp3Author) {
		String fileName = null;
		Map<String, String> authorAlbumMap = authorAlbumFileReader
				.getAuthorAlbumMap();
		String albumLanguage = null;
		String authorLanguage = null;
		String language = null;
		if (mp3Album != null) {
			mp3Album = mp3Album.trim();
			albumLanguage = authorAlbumMap.get(mp3Album);
		}
		if (mp3Author != null) {
			mp3Author = mp3Author.trim();
			authorLanguage = authorAlbumMap.get(mp3Author);
		}
		if (albumLanguage != null && authorLanguage != null) {
			// do not process
		} else if (albumLanguage != null) {
			language = albumLanguage;
		} else if (authorLanguage != null) {
			language = authorLanguage;
		} else {
			// do not process
		}
		String tempName = null;
		if (language != null) {
			if ("english".equalsIgnoreCase(language)) {
				fileName = mp3Author;
				tempName = mp3Album;
			} else if ("hindi".equalsIgnoreCase(language)
					|| "tamil".equalsIgnoreCase(language)
					|| "malayalam".equalsIgnoreCase(language)) {
				fileName = mp3Album;
				tempName = mp3Author;
			}
		}

		if (fileName == null) {
			fileName = tempName;
		}
		fileName = replacer(fileName);

		return fileName;
	}

	private String replacer(String fileName) {
		String replacer = replaceNameMap.get(fileName);
		if (replacer != null) {
			fileName = replacer;
		}
		return fileName;
	}

	private void readReplaceFile() {
		ClassLoader cl = this.getClass().getClassLoader();
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
				cl);
		try {
			Resource resources[] = resolver
					.getResources("classpath:Replace.txt");
			for (Resource resource : resources) {
				File file = resource.getFile();
				if (file.isFile()) {
					String fileName = file.getName();
					int dotIdx = fileName.lastIndexOf(".");

					if (dotIdx > 0) {
						String extension = fileName.substring(dotIdx + 1);
						if ("txt".equalsIgnoreCase(extension)) {
							logger.trace(file.getAbsolutePath());
							BufferedReader br;
							try {
								br = new BufferedReader(new FileReader(file));
								String st;
								while ((st = br.readLine()) != null) {
									String replaceText[] = st.trim().split("-:-");
									replaceNameMap.put(replaceText[0].trim(),
											replaceText[1].trim());
								}
							} catch (IOException e) {
								e.printStackTrace();
								logger.error("error", e);
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("error", e);
		}
	}

	private String formatText(String text) {
		// text = text.replaceAll("[^a-zA-Z0-9]", " ").trim();
		text = text.replaceAll("\\s+", " ").trim();
		// windows unsupported characters
		text = text.replace("^\\.+", "").replaceAll("[\\\\/:*?\"<>|]", "");
		return text;
	}

	private void fetchMp3Files(String mp3RootDir, List<File> mp3Files,
			List<File> nonMp3Files) {

		File directory = new File(mp3RootDir);

		File[] fList = directory.listFiles();

		for (File file : fList) {

			if (file.isFile()) {
				String fileName = file.getName();
				int dotIdx = fileName.lastIndexOf(".");

				if (dotIdx > 0) {
					String extension = fileName.substring(dotIdx + 1);
					if ("mp3".equalsIgnoreCase(extension)) {
						mp3Files.add(file);
					} else {
						nonMp3Files.add(file);
					}
				} else {
					nonMp3Files.add(file);
				}
			} else if (file.isDirectory()) {
				fetchMp3Files(file.getAbsolutePath(), mp3Files, nonMp3Files);
			}

		}
	}
}