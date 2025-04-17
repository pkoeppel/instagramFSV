package org.fsv.instagramuploader;

import org.fsv.instagramuploader.men.MatchdayCreator;
import org.fsv.instagramuploader.model.ClubModel;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ClubSelector {
 Logger logger = LoggerFactory.getLogger(ClubSelector.class);
 public ClubModel getClubDetails(String club) throws IOException, ParseException {
	logger.info("Search Club ...");
	String fileSource = "src/main/resources/pictures/teamlogos/";
	JSONObject obj = (JSONObject) new JSONParser().parse(new FileReader("src/main/resources/templates/clubs.json"));
	JSONObject objClub = (JSONObject) obj.get(club);
	if (objClub != null) {
	 File logo = new File(fileSource + objClub.get("fileName").toString() + ".png");
	 String clubPlace = objClub.get("place").toString();
	 String clubName = objClub.get("fileName").toString();
	 return new ClubModel(club, clubPlace, ImageIO.read(logo), clubName);
	} else {
	 logger.error("Club not found!");
	 return null;
	}
 }
}
