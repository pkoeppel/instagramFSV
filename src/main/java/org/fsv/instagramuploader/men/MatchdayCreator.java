package org.fsv.instagramuploader.men;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MatchdayCreator {
 private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
 private static final Logger logger = LoggerFactory.getLogger(MatchdayCreator.class);
 
 public String createMatch(GameModel match) throws IOException, ParseException {
	logger.info("Creating men matchday image: date={}, competition={}, team={}", match.getSaveGameDate(), match.getCompetition(), match.getTeam());
	logger.debug("Loading men matchday background template");
	BufferedImage background = ImageIO.read(new File("src/main/resources/pictures/template/men/matchdayTemp.jpg"));
	String headline = createHeadline(match.getMatchDay(), match.getCompetition());
	ClubModel homeClub = ClubSelector.getClubDetails(match.getHomeTeam());
	ClubModel awayClub = ClubSelector.getClubDetails(match.getAwayTeam());
	
	if (homeClub != null && awayClub != null) {
	 Helper.writeOnPicture(background, headline, "headline-men", FontClass.headMen, Color.WHITE, 0);
	 String dateTime = match.getPrintDate() + "\n" + match.getGameTime() + " Uhr";
	 Helper.writeOnPicture(background, dateTime, "dateTime-men", FontClass.dateTimeMen, Color.WHITE, 0);
	 Helper.pictureOnPicture(background, ImageIO.read(new File(homeClub.getClubLogoDir())), "logo-left-men", Helper.isOwnClub(homeClub));
	 Helper.pictureOnPicture(background, ImageIO.read(new File(awayClub.getClubLogoDir())), "logo-right-men", Helper.isOwnClub(awayClub));
	 String homeClubName = (homeClub.getChangedName() != null) ? homeClub.getChangedName() : homeClub.getClubName();
	 String awayClubName = (awayClub.getChangedName() != null) ? awayClub.getChangedName() : awayClub.getClubName();
	 Helper.writeOnPicture(background, homeClubName + homeClub.getClubStats(), "homeclub-men", FontClass.clubMen, Color.WHITE, 0);
	 Helper.writeOnPicture(background, awayClubName + awayClub.getClubStats(), "awayclub-men", FontClass.clubMen, Color.WHITE, 0);
	 
	 String savePath = match.getSaveGameDate() + "_" + match.getCompetition() + "_" + homeClub.getSaveName() + "_" + awayClub.getSaveName();
	 writeTempTxt(match, homeClub, awayClub, savePath, match.getTeam());
	 
	 Helper.savePicture(background, "src/main/resources/save/" + savePath, "Matchday");
	 logger.info("Men matchday image saved: path='{}', home='{}', away='{}'", savePath, homeClubName, awayClubName);
	 return savePath;
	}
	logger.error("Cannot create men matchday image because a club could not be resolved: date={}, competition={}", match.getSaveGameDate(), match.getCompetition());
	return null;
 }
 
 private String createHeadline(String matchDay, String competition) {
	String headline = "Testspiel";
	String competitionLower = competition.toLowerCase(Locale.ROOT);
	if (competitionLower.contains("liga") || competitionLower.contains("klasse")) {
	 logger.debug("Using league headline for competition '{}'", competition);
	 headline = matchDay + ". Spieltag";
	}
	if (competitionLower.contains("pokal")) {
	 logger.debug("Using cup headline for competition '{}'", competition);
	 headline = matchDay;
	 if (Helper.isNumeric(matchDay)) {
		headline += ". Pokal-\nrunde";
	 }
	}
	return headline;
 }
 
 @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "Anonymous TypeReference is standard Jackson pattern")
 private void writeTempTxt(GameModel m, ClubModel homeClub, ClubModel awayClub, String savePath, String team) throws IOException {
	logger.debug("Adding match preview to men-games.json: path='{}'", savePath);
	List<Map<String, Object>> gamesArray = OBJECT_MAPPER.readValue(new File("src/main/resources/templates/men-games.json"), new TypeReference<>() {
	});
	
	Map<String, Object> gameDetails = new HashMap<>();
	gameDetails.put("team", team);
	gameDetails.put("homeClub", createClubDetails(homeClub));
	gameDetails.put("awayClub", createClubDetails(awayClub));
	gameDetails.put("savePath", savePath);
	gameDetails.put("matchDate", m.getSaveGameDate());
	gameDetails.put("competition", m.getCompetition());
	gameDetails.put("gameUrl", m.getGameUrl());
	
	if (!gamesArray.contains(gameDetails)) {
	 gamesArray.add(gameDetails);
	}
	OBJECT_MAPPER.writeValue(new File("src/main/resources/templates/men-games.json"), gamesArray);
 }

 private Map<String, String> createClubDetails(ClubModel club) {
	Map<String, String> clubDetails = new HashMap<>();
	clubDetails.put("clubName", club.getClubName());
	clubDetails.put("changedName", club.getChangedName());
	return clubDetails;
 }
}
