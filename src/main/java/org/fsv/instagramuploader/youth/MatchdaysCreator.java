package org.fsv.instagramuploader.youth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.GameModel;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class MatchdaysCreator {
 private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
 private static final String tmpURL = "src/main/resources/pictures/template/youth/matchdayTemp.jpg";
 private static final Logger logger = LoggerFactory.getLogger(MatchdaysCreator.class);
 
 @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "Anonymous TypeReference is standard Jackson pattern")
 public Map<String, Integer> createMatches(ArrayList<GameModel> mmArr) throws IOException, ParseException {
	logger.info("Creating youth matchday images for {} matches", mmArr.size());
	List<LocalDate> matchDates = new ArrayList<>();
	List<Map<String, Object>> games = OBJECT_MAPPER.readValue(new File("src/main/resources/templates/youth-games.json"), new TypeReference<>() {
	});
	Map<String, Map<String, String>> teamInfo = OBJECT_MAPPER.readValue(new File("src/main/resources/templates/teamInfo.json"), new TypeReference<>() {
	});
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
	 g.dispose();
	 checkMatchDate(matchDates, m.getPrintDate());
	 
	 
	 if (m.getCompetition().contains("Kinder")) {
		emptyBlock(background, m, blockStart, games, teamInfo);
	 } else {
		filledBlock(background, m, blockStart, games, teamInfo);
	 }
	 Helper.writeOnPicture(background, m.getTeam() + "-Jugend", "team-name", FontClass.teamYouth, Color.BLACK, blockStart);
	 blockStart += 220;
	}
	String savePathPart = Helper.createMatchdaysHead(background, matchDates);
	String fileName = "Matchday" + pageCount;
	Helper.savePicture(background, "src/main/resources/save/youth/" + savePathPart, fileName);
	OBJECT_MAPPER.writeValue(new File("src/main/resources/templates/youth-games.json"), games);

	Map<String, Integer> result = new HashMap<>();
	result.put(savePathPart, pageCount);
	logger.info("Youth matchday images created: path='{}', pages={}, queuedMatches={}", savePathPart, pageCount, games.size());
	return result;
 }
 
 private void addGame(List<Map<String, Object>> games, GameModel match) {
	Map<String, Object> gameDetails = new HashMap<>();
	gameDetails.put("date", match.getSaveGameDate());
	gameDetails.put("matchType", match.getCompetition());
	gameDetails.put("team", match.getTeam());
	gameDetails.put("homeTeam", match.getHomeTeam());
	gameDetails.put("awayTeam", match.getAwayTeam());
	if (!games.contains(gameDetails)) {
	 games.add(gameDetails);
	}
 }
 
 private void filledBlock(BufferedImage background, GameModel m, int startPoint, List<Map<String, Object>> games, Map<String, Map<String, String>> teamInfo) throws IOException, ParseException {
	logger.debug("Rendering youth match block: team={}, date={}, competition={}", m.getTeam(), m.getSaveGameDate(), m.getCompetition());
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
		gamePlace += getDefaultPlace(teamInfo, m.getTeam());
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
	addGame(games, m);
 }
 
 private void emptyBlock(BufferedImage background, GameModel m, int startPoint, List<Map<String, Object>> games, Map<String, Map<String, String>> teamInfo) throws IOException, ParseException {
	logger.debug("Rendering youth festival block: team={}, date={}", m.getTeam(), m.getSaveGameDate());
	String gamePlace = "Sportplatz ";
	String homeTeam = m.getHomeTeam().getClubName();
	String awayTeam = m.getAwayTeam().getClubName();
	
	ClubModel homeClub = ClubSelector.searchClubDetails(homeTeam.replaceAll("\\s*\\([^)]*\\)", "").trim());
	ClubModel awayClub = ClubSelector.searchClubDetails(awayTeam.replaceAll("\\s*\\([^)]*\\)", "").trim());
	if (homeClub != null && awayClub != null) {
	 homeClub.setChangedName(m.getHomeTeam().getChangedName());
	 awayClub.setChangedName(m.getAwayTeam().getChangedName());
	 
	 if (homeClub.getClubName().equals("SpG Treuener Land")) {
		gamePlace += getDefaultPlace(teamInfo, m.getTeam());
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
	addGame(games, m);
 }
 
 private String getDefaultPlace(Map<String, Map<String, String>> teamInfo, String team) {
	Map<String, String> data = teamInfo.get(team);
	return data != null ? data.getOrDefault("default-place", "") : "";
 }

 private void checkMatchDate(List<LocalDate> matchDates, String date) {
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	LocalDate formattedDate = LocalDate.parse(date, dtf);
	if (!matchDates.contains(formattedDate)) {
	 matchDates.add(formattedDate);
	 Collections.sort(matchDates);
	}
 }
}
