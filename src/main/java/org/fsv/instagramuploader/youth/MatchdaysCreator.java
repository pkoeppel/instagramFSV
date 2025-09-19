package org.fsv.instagramuploader.youth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.GameModel;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Component("msc")
public class MatchdaysCreator {
 Logger logger = LoggerFactory.getLogger(MatchdaysCreator.class);
 private static final String tmpURL = "src/main/resources/pictures/template/youth/matchdayTemp.jpg";
 private List<LocalDate> matchDates;
 
 public Map<String, Integer> createMatches(ArrayList<GameModel> mmArr) throws IOException, ParseException {
	logger.info("Create {} matches", mmArr.size());
	matchDates = new ArrayList<>();
	BufferedImage background = ImageIO.read(new File(tmpURL));
	int blockStart = 530;
	int pageCount = 1;
	for (GameModel m : mmArr) {
	 if (blockStart > 1800) {
		blockStart = 530;
		String savePathPart = Helper.createMatchdaysHead(background, matchDates);
		String fileName = "Matchday" + pageCount;
		Helper.savePicture(background, "src/main/resources/save/youth/" + savePathPart, fileName);
		background = ImageIO.read(new File(tmpURL));
		pageCount++;
	 }
	 Graphics g = background.getGraphics();
	 g.setColor(Color.GRAY);
	 int[] polyX = {0, 250, 225, 0};
	 int[] polyY = {blockStart, blockStart, blockStart + 100, blockStart + 100};
	 g.fillPolygon(polyX, polyY, polyY.length);
	 checkMatchDate(m.getPrintDate());
	 
	 
	 if (m.getCompetition().contains("Kinder")) {
		emptyBlock(background, m, blockStart);
	 } else {
		filledBlock(background, m, blockStart);
	 }
	 Helper.writeOnPicture(background, m.getTeam() + "-Jugend", "team-name", FontClass.teamYouth, Color.BLACK, blockStart);
	 blockStart += 220;
	}
	String savePathPart = Helper.createMatchdaysHead(background, matchDates);
	String fileName = "Matchday" + pageCount;
	Helper.savePicture(background, "src/main/resources/save/youth/" + savePathPart, fileName);
	
	Map<String, Integer> result = new HashMap<>();
	result.put(savePathPart, pageCount);
	return result;
 }
 
 @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "Anonymous TypeReference is standard Jackson pattern")
 private void writeTempTxt(GameModel m) throws IOException {
	logger.info("Writing match to youth-games.json");
	List<Map<String, Object>> gamesArray = new ObjectMapper().readValue(new File("src/main/resources/templates/youth-games.json"), new TypeReference<>() {
	});
	
	Map<String, Object> gameDetails = new HashMap<>();
	gameDetails.put("date", m.getSaveGameDate());
	gameDetails.put("matchType", m.getCompetition());
	gameDetails.put("team", m.getTeam());
	gameDetails.put("homeTeam", m.getHomeTeam());
	gameDetails.put("awayTeam", m.getAwayTeam());
	if (!gamesArray.contains(gameDetails)) {
	 gamesArray.add(gameDetails);
	}
	new ObjectMapper().writeValue(new File("src/main/resources/templates/youth-games.json"), gamesArray);
 }
 
 private void filledBlock(BufferedImage background, GameModel m, int startPoint) throws IOException, ParseException {
	logger.info("Normal game");
	String gamePlace = "Sportplatz ";
	String homeTeam = m.getHomeTeam().getClubName();
	String awayTeam = m.getAwayTeam().getClubName();
	
	ClubModel homeClub = ClubSelector.searchClubDetails(homeTeam.replaceAll("\\s*\\([^)]*\\)", "").trim());
	ClubModel awayClub = ClubSelector.searchClubDetails(awayTeam.replaceAll("\\s*\\([^)]*\\)", "").trim());
	if (homeClub != null && awayClub != null) {
	 homeClub.setChangedName(m.getHomeTeam().getChangedName());
	 awayClub.setChangedName(m.getAwayTeam().getChangedName());
	 
	 homeTeam = (homeClub.getChangedName() != null) ? homeClub.getChangedName() : homeClub.getClubName();
	 awayTeam = (awayClub.getChangedName() != null) ? awayClub.getChangedName() : awayClub.getClubName();
	 if (homeClub.getClubName().equals("SpG Treuener Land")) {
		Helper.writeOnPicture(background, Helper.wrapString(homeTeam, 23), "club-name-home", FontClass.clubOwnYouth, Color.BLACK, startPoint);
		Helper.writeOnPicture(background, Helper.wrapString(awayTeam, 23), "club-name-away", FontClass.simpleYouth, Color.BLACK, startPoint);
		InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/teamInfo.json"), StandardCharsets.UTF_8);
		JSONObject obj = (JSONObject) new JSONParser().parse(reader);
		JSONObject teamInfo = (JSONObject) obj.get(m.getTeam());
		gamePlace += teamInfo.get("default-place");
	 } else {
		Helper.writeOnPicture(background, Helper.wrapString(homeTeam, 23), "club-name-home", FontClass.simpleYouth, Color.BLACK, startPoint);
		Helper.writeOnPicture(background, Helper.wrapString(awayTeam, 23), "club-name-away", FontClass.clubOwnYouth, Color.BLACK, startPoint);
		gamePlace += homeClub.getClubPlace();
	 }
	 Helper.writeOnPicture(background, m.getCompetition(), "match-type-short", FontClass.simpleYouth, Color.BLACK, startPoint);
	 Helper.pictureOnPicture(background, ImageIO.read(new File(homeClub.getClubLogoDir())), "logo-left-youth", startPoint);
	 Helper.pictureOnPicture(background, ImageIO.read(new File(awayClub.getClubLogoDir())), "logo-right-youth", startPoint);
	}
	String bottom = m.fullMatchDate() + " - " + m.getGameTime() + " Uhr" + "\n" + gamePlace;
	Helper.writeOnPicture(background, bottom, "bottom-center", FontClass.simpleYouth, Color.BLACK, startPoint);
	Helper.writeOnPicture(background, ":", "center-point", FontClass.simpleYouth, Color.BLACK, startPoint);
	writeTempTxt(m);
 }
 
 private void emptyBlock(BufferedImage background, GameModel m, int startPoint) throws IOException, ParseException {
	logger.info("Kinderfest");
	String gamePlace = "Sportplatz ";
	String homeTeam = m.getHomeTeam().getClubName();
	String awayTeam = m.getAwayTeam().getClubName();
	
	ClubModel homeClub = ClubSelector.searchClubDetails(homeTeam.replaceAll("\\s*\\([^)]*\\)", "").trim());
	ClubModel awayClub = ClubSelector.searchClubDetails(awayTeam.replaceAll("\\s*\\([^)]*\\)", "").trim());
	if (homeClub != null && awayClub != null) {
	 homeClub.setChangedName(m.getHomeTeam().getChangedName());
	 awayClub.setChangedName(m.getAwayTeam().getChangedName());
	 
	 if (homeClub.getClubName().equals("SpG Treuener Land")) {
		InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/teamInfo.json"), StandardCharsets.UTF_8);
		JSONObject obj = (JSONObject) new JSONParser().parse(reader);
		JSONObject teamInfo = (JSONObject) obj.get(m.getTeam());
		gamePlace += teamInfo.get("default-place");
	 } else {
		gamePlace += homeClub.getClubPlace();
	 }
	 Helper.writeOnPicture(background, m.getCompetition(), "match-type-short", FontClass.simpleYouth, Color.BLACK, startPoint);
	 Helper.pictureOnPicture(background, ImageIO.read(new File(homeClub.getClubLogoDir())), "logo-left-youth", startPoint);
	 Helper.pictureOnPicture(background, ImageIO.read(new File(awayClub.getClubLogoDir())), "logo-right-youth", startPoint);
	}
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
