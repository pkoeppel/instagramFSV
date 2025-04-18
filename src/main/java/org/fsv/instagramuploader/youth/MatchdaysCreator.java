package org.fsv.instagramuploader.youth;

import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.men.MatchdayCreator;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Component("msc")
public class MatchdaysCreator {
 Logger logger = LoggerFactory.getLogger(MatchdaysCreator.class);
 private static final String tmpURL = "src/main/resources/pictures/template/youth/matchdayTemp.jpg";
 private static ClubSelector getClub;
 private static FileWriter fw;
 private List<LocalDate> matchDates;
 
 public Map<String, Integer> createMatches(ArrayList<GameModel> mmArr) throws IOException, ParseException {
	logger.info("Create "+ mmArr.size() + " matches");
	matchDates = new ArrayList<>();
	BufferedImage background = ImageIO.read(new File(tmpURL));
	getClub = new ClubSelector();
	int blockStart = 530;
	int pageCount = 1;
	for (GameModel m : mmArr) {
	 if (blockStart > 2100) {
		blockStart = 530;
		String savePathPart = Helper.createMatchdaysHead(background, matchDates);
		String fileName = "Matchday" + pageCount;
		Helper.savePicture(background, "src/main/resources/save/youth/" + savePathPart, fileName);
		background = ImageIO.read(new File(tmpURL));
		pageCount++;
	 }
	 Graphics g = background.getGraphics();
	 g.setColor(Color.GRAY);
	 int[] polyX = {0, 275, 250, 0};
	 int[] polyY = {blockStart, blockStart, blockStart + 100, blockStart + 100};
	 g.fillPolygon(polyX, polyY, polyY.length);
	 checkMatchDate(m.getPrintDate());
	 if (m.getCompetition() == null || m.getCompetition().contains("Kinder")) {
		emptyBlock(background, m, blockStart);
	 } else {
		filledBlock(background, m, blockStart);
	 }
	 Helper.writeOnPicture(background, m.getYouthTeam() + "-Jugend", "team-name", FontClass.teamYouth, Color.BLACK, blockStart);
	 blockStart += 220;
	}
	String savePathPart = Helper.createMatchdaysHead(background, matchDates);
	fw.close();
	String fileName = "Matchday" + pageCount;
	Helper.savePicture(background, "src/main/resources/save/youth/" + savePathPart, fileName);
	
	Map<String, Integer> result = new HashMap<>();
	result.put(savePathPart, pageCount);
	return result;
 }
 
 private void writeTempTxt(GameModel m) throws IOException {
	JSONParser jp = new JSONParser();
	JSONArray ja = new JSONArray();
	try {
	 ja = (JSONArray) jp.parse(new FileReader("src/main/resources/templates/youth-games.json"));
	 
	 JSONObject gameDetails = new JSONObject();
	 gameDetails.put("date", m.getSaveGameDate());
	 gameDetails.put("matchType", m.getCompetition());
	 gameDetails.put("youth", m.getYouthTeam());
	 gameDetails.put("homeGame", m.getHomeGame());
	 gameDetails.put("homeTeam", m.getHomeTeam());
	 gameDetails.put("awayTeam", m.getAwayTeam());
	 gameDetails.put("oppName", m.getChangeName());
	 Helper.saveTempTxt(ja, gameDetails);
	} catch (ParseException ignored) {
	}
	
	
	fw = new FileWriter("src/main/resources/templates/youth-games.json");
	fw.write(ja.toJSONString());
	fw.flush();
 }
 
 private void filledBlock(BufferedImage background, GameModel m, int startPoint) throws IOException, ParseException {
	logger.info("Normal game");
	boolean isHomeGame;
	String opponent;
	ClubModel homeClub, awayClub;
	if (m.getHomeTeam().contains("SpG Treuener Land")) {
	 m.setHomeTeam(m.getHomeTeam().replaceAll("\\s*\\([^)]*\\)", "").trim());
	 homeClub = getClub.getClubDetails("SpG Treuener Land");
	 awayClub = getClub.getClubDetails(m.getAwayTeam());
	 isHomeGame = true;
	 opponent = m.getAwayTeam();
	} else {
	 m.setAwayTeam(m.getAwayTeam().replaceAll("\\s*\\([^)]*\\)", "").trim());
	 homeClub = getClub.getClubDetails(m.getHomeTeam());
	 awayClub = getClub.getClubDetails("SpG Treuener Land");
	 isHomeGame = false;
	 opponent = m.getHomeTeam();
	}
	
	if (m.getChangeName() != null) {
	 String buffer;
	 if (isHomeGame) {
		buffer = m.getAwayTeam();
		m.setAwayTeam(m.getChangeName().replaceAll("\\s*\\([^)]*\\)", "").trim());
	 } else {
		buffer = m.getHomeTeam();
		m.setHomeTeam(m.getChangeName().replaceAll("\\s*\\([^)]*\\)", "").trim());
	 }
	 m.setChangeName(buffer);
	} else {
	 m.setChangeName(opponent);
	}
	String gamePlace = "Sportplatz ";
	if (isHomeGame) {
	 Helper.writeOnPicture(background, Helper.wrapString(m.getHomeTeam(), 23), "club-name-home", FontClass.clubOwnYouth, Color.BLACK, startPoint);
	 Helper.writeOnPicture(background, Helper.wrapString(m.getAwayTeam(), 23), "club-name-away", FontClass.simpleYouth, Color.BLACK, startPoint);
	 JSONObject obj = (JSONObject) new JSONParser().parse(new FileReader("src/main/resources/templates/teamInfo.json"));
	 JSONObject teamInfo = (JSONObject) obj.get(m.getYouthTeam());;
	 gamePlace += teamInfo.get("default-place");
	} else {
	 Helper.writeOnPicture(background, Helper.wrapString(m.getHomeTeam(), 23), "club-name-home", FontClass.simpleYouth, Color.BLACK, startPoint);
	 Helper.writeOnPicture(background, Helper.wrapString(m.getAwayTeam(), 23), "club-name-away", FontClass.clubOwnYouth, Color.BLACK, startPoint);
	 gamePlace += homeClub.clubPlace();
	}
	
	Helper.writeOnPicture(background, m.getCompetition(), "match-type-short", FontClass.simpleYouth, Color.BLACK, startPoint);
	
	Helper.pictureOnPicture(background, homeClub.clubLogo(), "logo-left-youth", startPoint);
	Helper.pictureOnPicture(background, awayClub.clubLogo(), "logo-right-youth", startPoint);
	
	String bottom = m.fullMatchDate() + " - " + m.getGameTime() + " Uhr" + "\n" + gamePlace;
	Helper.writeOnPicture(background, bottom, "bottom-center", FontClass.simpleYouth, Color.BLACK, startPoint);
	Helper.writeOnPicture(background, ":", "center-point", FontClass.simpleYouth, Color.BLACK, startPoint);
	writeTempTxt(m);
 }
 
 private void emptyBlock(BufferedImage background, GameModel m, int startPoint) throws IOException, ParseException {
	logger.info("Kinderfest");
	ClubModel homeClub;
	ClubModel ownClub = getClub.getClubDetails("SpG Treuener Land");
		String gamePlace = "Sportplatz ";
	if (!m.getHomeTeam().toLowerCase().contains("spg treuener land")) {
	 homeClub = getClub.getClubDetails(m.getHomeTeam());
	 gamePlace += homeClub.clubPlace();
	} else {
	 homeClub = ownClub;
	 JSONObject obj = (JSONObject) new JSONParser().parse(new FileReader("src/main/resources/templates/teamInfo.json"));
	 JSONObject teamInfo = (JSONObject) obj.get(m.getYouthTeam());;
	 gamePlace += teamInfo.get("default-place");
	}
	if (m.getChangeName() != null) {
	 String buffer;
	 if (m.getHomeGame()) {
		buffer = m.getAwayTeam();
		m.setAwayTeam(m.getChangeName().replaceAll("\\s*\\([^)]*\\)", "").trim());
	 } else {
		buffer = m.getHomeTeam();
		m.setHomeTeam(m.getChangeName().replaceAll("\\s*\\([^)]*\\)", "").trim());
	 }
	 m.setChangeName(buffer);
	} else {
	 m.setChangeName("SpG Treuener Land");
	}
	Helper.pictureOnPicture(background, ownClub.clubLogo(), "logo-right-youth", startPoint);
	Helper.pictureOnPicture(background, homeClub.clubLogo(), "logo-left-youth", startPoint);
	
	
	Helper.writeOnPicture(background, m.getCompetition(), "match-type-short", FontClass.simpleYouth, Color.BLACK, startPoint);
	Helper.writeOnPicture(background, m.getCompetition() + "!", "matchType", FontClass.mTypeYouth, Color.BLACK, startPoint);
	String bottom = m.fullMatchDate() + " - " + m.getGameTime() + " Uhr" + "\n" + gamePlace;
	Helper.writeOnPicture(background, bottom, "bottom-center", FontClass.simpleYouth, Color.BLACK, startPoint);
	writeTempTxt(m);
 }
 
 private void checkMatchDate(String date) {
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	LocalDate formattedDate = LocalDate.parse(date, dtf);
	if (!matchDates.contains(formattedDate)) {
	 matchDates.add(formattedDate);
	 Collections.sort(matchDates);
	}
 }
}
