package org.fsv.instagramuploader;

import org.fsv.instagramuploader.model.ClubModel;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClubSelector {
 private static final Logger logger = LoggerFactory.getLogger(ClubSelector.class);
 private static final Path CLUBS_FILE = Paths.get("src/main/resources/templates/clubs.json");
 private static final String LOGO_DIRECTORY = "src/main/resources/pictures/teamlogos/";
 private static volatile JSONObject clubs;
 private static volatile long clubsLastModified = -1;

 private ClubSelector() {
 }

 public static ClubModel getClubDetails(ClubModel club) throws IOException, ParseException {
	JSONObject clubData = (JSONObject) loadClubs().get(club.getClubName());
	if (clubData == null) {
	 logger.error("Club '{}' not found", club.getClubName());
	 return null;
	}
	String saveName = clubData.get("fileName").toString();
	club.setClubPlace(clubData.get("place").toString());
	club.setClubLogoDir(LOGO_DIRECTORY + saveName + ".png");
	club.setSaveName(saveName);
	return club;
 }

 public static ClubModel searchClubDetails(String club) throws IOException, ParseException {
	return getClubDetails(new ClubModel(club, null, null, null, null, null));
 }

 private static JSONObject loadClubs() throws IOException, ParseException {
	long lastModified = Files.getLastModifiedTime(CLUBS_FILE).toMillis();
	JSONObject result = clubs;
	if (result == null || clubsLastModified != lastModified) {
	 synchronized (ClubSelector.class) {
		if (clubs == null || clubsLastModified != lastModified) {
		 try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(CLUBS_FILE), StandardCharsets.UTF_8)) {
			clubs = (JSONObject) new JSONParser().parse(reader);
			clubsLastModified = lastModified;
		 }
		}
		result = clubs;
	 }
	}
	return result;
 }
}
