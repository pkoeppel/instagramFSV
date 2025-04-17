package org.fsv.instagramuploader.men;

import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.GameModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


@Component("mc")
public class MatchdayCreator {
 Logger logger = LoggerFactory.getLogger(MatchdayCreator.class);
 private GameModel match;
 
 public String createMatch(GameModel matchInfo) throws IOException, ParseException {
	logger.info("Creating Matchday (Background = MatchdayTemp.jpg)");
	match = matchInfo;
	ClubSelector getClub = new ClubSelector();
	ClubModel homeClub = getClub.getClubDetails(match.getHomeTeam());
	ClubModel awayClub = getClub.getClubDetails(match.getAwayTeam());
	
	if (homeClub != null && awayClub != null) {
	 //select template
	 BufferedImage background = ImageIO.read(new File("src/main/resources/pictures/template/men/matchdayTemp.jpg"));
	 int blockHeight = 0;
	 String matchCase = matchInfo.getCompetition();
	 String headline = "Testspiel";
	 if (matchCase.contains("liga")) {
		logger.info("League match (Sponsor = Sparkasse)");
		headline = "Spieltag " + match.getMatchDay() + "\n" + "Vogtlandliga";
		Helper.pictureOnPicture(background, ImageIO.read(new File("src/main/resources/pictures/sponsor/Sparkasse_Vogtland.png")), "sponsor-men", 0);
		blockHeight = 1045;
	 }
	 if (matchCase.contains("pokal")) {
		logger.info("Cup match (Sponsor = Sternquell)");
		headline = match.getMatchDay();
		if (Helper.isNumeric(match.getMatchDay())) {
		 headline += ". Pokalrunde";
		}
		Helper.pictureOnPicture(background, ImageIO.read(new File("src/main/resources/pictures/sponsor/Sternquell.png")), "sponsor-men", 0);
		Helper.writeOnPicture(background, "Sternquell Vogtlandpokal", "headline2-men", FontClass.headMen3, Color.WHITE, 345);
		blockHeight = 1075;
	 }
	 if (matchCase.contains("freundschaft")) {
		logger.info("Friend match");
		blockHeight = 1075;
	 }
	 Helper.writeOnPicture(background, headline, "headline2-men", FontClass.headMen2, Color.WHITE, 160);
	 
	 buildBlocks(background, blockHeight, homeClub, awayClub);
	 
	 writeTempTxt(matchInfo);
	 
	 String opponent;
	 if (match.getHomeGame()) {
		opponent = awayClub.saveClubName();
	 } else {
		opponent = homeClub.saveClubName();
	 }
	 logger.info("Picture finished!");
	 String savePath = match.getSaveGameDate() + "_" + match.getCompetition() + "_" + opponent;
	 Helper.savePicture(background, "src/main/resources/save/" + savePath, "Matchday");
	 logger.info("Save picture finished (Path: +" + savePath + ")!");
	 return savePath;
	}
	return null;
 }
 
 private int isOwnClub(ClubModel club) {
	if (club.clubName().equals("FSV Treuen")) {
	 return 0;
	}
	return 60;
 }
 
 private void buildBlocks(BufferedImage background, Integer startBox, ClubModel homeClub, ClubModel awayClub) {
	
	String gamePlace = "Sportplatz " + homeClub.clubPlace();
	Helper.pictureOnPicture(background, homeClub.clubLogo(), "logo-left-men", isOwnClub(homeClub));
	Helper.pictureOnPicture(background, awayClub.clubLogo(), "logo-right-men", isOwnClub(awayClub));
	
	match.setHomeGame(homeClub.clubName().equals("FSV Treuen"));
	
	if (match.getChangeName() != null) {
	 if (homeClub.clubName().equals("FSV Treuen"))
		match.setAwayTeam(match.getChangeName());
	 else
		match.setHomeTeam(match.getChangeName());
	}
	
	if (match.getHomeStats() == null || match.getAwayStats() == null) {
	 match.setHomeStats("");
	 match.setAwayStats("");
	}
	
	Helper.writeOnPicture(background, match.getHomeTeam() + match.getHomeStats(), "homeclub-men", FontClass.clubMen, Color.BLACK, startBox);
	Helper.writeOnPicture(background, match.getAwayTeam() + match.getAwayStats(), "awayclub-men", FontClass.clubMen, Color.BLACK, startBox);
	
	String bottomBox = match.getPrintDate() + " | " + match.getGameTime() + " Uhr" + "\n" + Helper.wrapString(gamePlace, 30);
	Helper.writeOnPicture(background, bottomBox, "bottom-men", FontClass.bottomMen, Color.BLACK, 1278);
 }
 
 private void writeTempTxt(GameModel m) throws IOException {
	logger.info("Writing match to men-games.json");
	JSONParser jp = new JSONParser();
	JSONArray ja = new JSONArray();
	try {
	 ja = (JSONArray) jp.parse(new FileReader("src/main/resources/templates/men-games.json"));
	 
	 String opponent = m.getHomeTeam();
	 
	 if (m.getHomeTeam().equals("FSV Treuen")) {
		opponent = m.getAwayTeam();
	 }
	 
	 String oppTeamName = opponent;
	 
	 if (m.getChangeName() != null) {
		oppTeamName = m.getChangeName();
	 }
	 
	 JSONObject gameDetails = new JSONObject();
	 gameDetails.put("matchType", m.getCompetition());
	 gameDetails.put("opponent", opponent);
	 gameDetails.put("matchDate", m.getSaveGameDate());
	 
	 gameDetails.put("homeGame", m.getHomeGame());
	 gameDetails.put("oppTeamName", oppTeamName);
	 
	 Helper.saveTempTxt(ja, gameDetails);
	} catch (ParseException ignored) {
	}
	
	
	FileWriter fw = new FileWriter("src/main/resources/templates/men-games.json");
	fw.write(ja.toJSONString());
	fw.flush();
	fw.close();
 }
}
