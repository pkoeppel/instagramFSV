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
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component("mc")
public class MatchdayCreator {
 Logger logger = LoggerFactory.getLogger(MatchdayCreator.class);
 
 public String createMatch(GameModel match) throws IOException, ParseException {
	logger.info("Creating Matchday file.");
	logger.info("Load background file.");
	BufferedImage background = ImageIO.read(new File("src/main/resources/pictures/template/men/matchdayTemp.jpg"));
	String headline = createHeadline(match.getMatchDay(), match.getCompetition());
	ClubModel homeClub = ClubSelector.getClubDetails(match.getHomeTeam());
	ClubModel awayClub = ClubSelector.getClubDetails(match.getAwayTeam());
	
	if (homeClub != null && awayClub != null) {
	 Helper.writeOnPicture(background, headline, "headline-men", FontClass.headMen, Color.WHITE, 125);
	 String dateTime = match.getPrintDate() + "\n" + match.getGameTime() + " Uhr";
	 Helper.writeOnPicture(background, dateTime, "dateTime-men", FontClass.dateTimeMen, Color.WHITE, 520);
	 Helper.pictureOnPicture(background, ImageIO.read(new File(homeClub.getClubLogoDir())), "logo-left-men", Helper.isOwnClub(homeClub));
	 Helper.pictureOnPicture(background, ImageIO.read(new File(awayClub.getClubLogoDir())), "logo-right-men", Helper.isOwnClub(awayClub));
	 String homeClubName = (homeClub.getChangedName() != null) ? homeClub.getChangedName() : homeClub.getClubName();
	 String awayClubName = (awayClub.getChangedName() != null) ? awayClub.getChangedName() : awayClub.getClubName();
	 Helper.writeOnPicture(background, homeClubName + homeClub.getClubStats(), "homeclub-men", FontClass.clubMen, Color.WHITE, 1675);
	 Helper.writeOnPicture(background, awayClubName + awayClub.getClubStats(), "awayclub-men", FontClass.clubMen, Color.WHITE, 1675);
	 
	 String savePath = match.getSaveGameDate() + "_" + match.getCompetition() + "_" + homeClub.getSaveName() + "_" + awayClub.getSaveName();
	 writeTempTxt(match, homeClub, awayClub, savePath, match.getTeam());
	 
	 logger.info("Picture finished!");
	 Helper.savePicture(background, "src/main/resources/save/" + savePath, "Matchday");
	 logger.info("Save picture finished (Path: +{})!", savePath);
	 return savePath;
	}
	return null;
 }
 
 private String createHeadline(String matchDay, String competition) {
	String headline = "Testspiel";
	if (competition.contains("liga") || competition.contains("klasse")) {
	 logger.info("League match");
	 headline = matchDay + ". Spieltag";
	}
	if (competition.contains("pokal")) {
	 logger.info("Cup match");
	 headline = matchDay;
	 if (Helper.isNumeric(matchDay)) {
		headline += ". Pokal-\nrunde";
	 }
	}
	if (competition.contains("freundschaft")) {
	 logger.info("Friend match");
	}
	return headline;
 }
 
 @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "Anonymous TypeReference is standard Jackson pattern")
 private void writeTempTxt(GameModel m, ClubModel homeClub, ClubModel awayClub, String savePath, String team) throws IOException {
	logger.info("Writing match to men-games.json");
	List<Map<String, Object>> gamesArray = new ObjectMapper().readValue(new File("src/main/resources/templates/men-games.json"), new TypeReference<>() {
	});
	
	Map<String, Object> gameDetails = new HashMap<>();
	gameDetails.put("team", team);
	gameDetails.put("homeClub", homeClub);
	gameDetails.put("awayClub", awayClub);
	gameDetails.put("savePath", savePath);
	gameDetails.put("matchDate", m.getSaveGameDate());
	gameDetails.put("competition", m.getCompetition());
	
	if (!gamesArray.contains(gameDetails)) {
	 gamesArray.add(gameDetails);
	}
	new ObjectMapper().writeValue(new File("src/main/resources/templates/men-games.json"), gamesArray);
 }

//  private BufferedImage chanceFormat(JSONObject c, BufferedImage image) {
//    BufferedImage targetImg = new BufferedImage(670, 1300, BufferedImage.TYPE_INT_ARGB);
//    BufferedImage subImg = image.getSubimage(Helper.getC(c, "x"), Helper.getC(c, "y"), Helper.getC(c, "w"), Helper.getC(c, "h"));
//    Graphics2D g2 = targetImg.createGraphics();
//    RenderingHints qualityHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//    qualityHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//    g2.setRenderingHints(qualityHints);
//    g2.drawImage(subImg, 0, 0, 670, 1300, null);
//    g2.dispose();
//    return targetImg;
//  }
}
