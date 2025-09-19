package org.fsv.instagramuploader;

import org.fsv.instagramuploader.model.ClubModel;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ClubSelector {
 static Logger logger = LoggerFactory.getLogger(ClubSelector.class);
 
 public static ClubModel getClubDetails(ClubModel club) throws IOException, ParseException {
	logger.info("Search Club by ClubModel");
	String fileSource = "src/main/resources/pictures/teamlogos/";
	InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/clubs.json"), StandardCharsets.UTF_8);
	JSONObject obj = (JSONObject) new JSONParser().parse(reader);
	JSONObject objClub = (JSONObject) obj.get(club.getClubName());
	if (objClub != null) {
	 club.setClubPlace(objClub.get("place").toString());
	 club.setClubLogoDir(fileSource + objClub.get("fileName").toString() + ".png");
	 club.setSaveName(objClub.get("fileName").toString());
	 return club;
	} else {
	 logger.error("Club not found by ClubModel!");
	 return null;
	}
 }
 
 public static ClubModel searchClubDetails(String club) throws IOException, ParseException {
	logger.info("Search Club by club name");
	String fileSource = "src/main/resources/pictures/teamlogos/";
	InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/clubs.json"), StandardCharsets.UTF_8);
	JSONObject obj = (JSONObject) new JSONParser().parse(reader);
	JSONObject objClub = (JSONObject) obj.get(club);
	if (objClub != null) {
	 String saveName = objClub.get("fileName").toString();
	 String fileDir = fileSource + saveName + ".png";
	 String clubPlace = objClub.get("place").toString();
	 return new ClubModel(club, null, clubPlace, fileDir, saveName, null);
	} else {
	 logger.error("Club not found by club name!");
	 return null;
	}
 }
}
