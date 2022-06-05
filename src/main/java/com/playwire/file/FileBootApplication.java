package com.playwire.file;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.playwire.file.image.infoeditor.ImageFileReader;
import com.playwire.file.mp3.infoeditor.Mp3FileNameChanger;
import com.playwire.file.mp3.infoeditor.ReadMp3Description;

import ch.qos.logback.classic.Logger;

@SpringBootApplication
public class FileBootApplication implements CommandLineRunner {

	Logger logger = (Logger) LoggerFactory.getLogger(FileBootApplication.class);

	@Autowired
	private Mp3FileNameChanger mp3FileNameChanger;

	@Autowired
	private ReadMp3Description readMp3Description;

	@Autowired
	private ImageFileReader imageFileReader;

	public static void main(String[] args) {
		SpringApplication.run(FileBootApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
//		 readMp3Description.read();
		mp3FileNameChanger.process();
		// imageFileReader.read();
	}
}
