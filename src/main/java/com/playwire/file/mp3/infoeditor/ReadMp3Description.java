package com.playwire.file.mp3.infoeditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.qos.logback.classic.Logger;

/**
 * Get mp3 details and write only author, album and title details into log file
 * 
 * @author SkyLake
 *
 */
@Component
public class ReadMp3Description {

	Logger logger = (Logger) LoggerFactory.getLogger(ReadMp3Description.class);

	public void read() {

		// this audio file has metadata embedded in xmp (extensible metadata
		// platform) standard
		// created by adobe systems inc. xmp standardizes the definition,
		// creation, and
		// processing of extensible metadata.

		logger.info("##############################################");

		List<File> mp3Files = new ArrayList<>();

		List<File> nonMp3Files = new ArrayList<>();

		fetchMp3Files(Mp3FileNameChanger.MP3_ROOT_DIR, mp3Files, nonMp3Files);

		Set<String> authorAlbumTitleSet = new HashSet<>();

		int nonMp3FileCount = 0;

		for (File file : nonMp3Files) {
			logger.info(++nonMp3FileCount + " --- " + file.getAbsolutePath());
		}

		List<File> unProcessedMp3Files = new ArrayList<>();

		for (File mp3File : mp3Files) {

			logger.trace(mp3File.getName());

			try {

				Parser parser = new Mp3Parser();
				InputStream input = TikaInputStream
						.get(new FileInputStream(mp3File));
				ContentHandler handler = new DefaultHandler();
				Metadata metadata = new Metadata();
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
				logger.trace("album--: " + album); // for hindi, tamil,
													// malayalam
				logger.trace("author--: " + author); // for english

				if ((author != null && !author.isEmpty())
						|| (album != null && !album.isEmpty())) {
					authorAlbumTitleSet.add(
							author + " -#-#- " + album + " -#-#- " + title);
				} else {
					unProcessedMp3Files.add(mp3File);
				}
			} catch (IOException | SAXException | TikaException
					| NegativeArraySizeException e) {
				unProcessedMp3Files.add(mp3File);
				e.printStackTrace();
				logger.error("error", e);
			}
		}

		logger.info("Authors -#-#- Albums -#-#- Title - List:-");
		int count = 0;
		authorAlbumTitleSet = new TreeSet<>(authorAlbumTitleSet);
		for (String desc : authorAlbumTitleSet) {
			logger.info(++count + ". " + desc);
		}

		logger.info("Total number of mp3 files: " + mp3Files.size());
		logger.info("Total number of non mp3 files: " + nonMp3FileCount);
		logger.info("Total number of non processed mp3 files: "
				+ unProcessedMp3Files.size());

		count = 0;
		for (File file : unProcessedMp3Files) {
			logger.info(++count + ". un processed mp3 file: "
					+ file.getAbsolutePath());
		}

	}

	private List<File> fetchMp3Files(String mp3RootDir, List<File> mp3Files,
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

		return mp3Files;
	}
}