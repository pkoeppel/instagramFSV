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
	private static final int CANVAS_WIDTH = 1080;
	private static final int CANVAS_HEIGHT = 1920;
	private static final int PHOTO_SIZE = 1080;
	
	public String createMatch(GameModel match, BufferedImage userImage) throws IOException, ParseException {
		return createMatch(match, userImage, null);
	}

	public String createMatch(GameModel match, BufferedImage userImage, String customLocation) throws IOException, ParseException {
		logger.info("Creating men matchday image: date={}, competition={}, team={}", match.getSaveGameDate(), match.getCompetition(), match.getTeam());
		BufferedImage background = composeBackground(userImage);
		ClubModel homeClub = ClubSelector.getClubDetails(match.getHomeTeam());
		ClubModel awayClub = ClubSelector.getClubDetails(match.getAwayTeam());

		if (homeClub != null && awayClub != null) {
			Helper.pictureOnPicture(background, ImageIO.read(new File(homeClub.getClubLogoDir())), "logo-left-men", Helper.isOwnClub(homeClub));
			Helper.pictureOnPicture(background, ImageIO.read(new File(awayClub.getClubLogoDir())), "logo-right-men", Helper.isOwnClub(awayClub));
			String homeClubName = (homeClub.getChangedName() != null) ? homeClub.getChangedName() : homeClub.getClubName();
			String awayClubName = (awayClub.getChangedName() != null) ? awayClub.getChangedName() : awayClub.getClubName();
			Helper.writeOnPicture(background, homeClubName + homeClub.getClubStats(), "homeclub-men", FontClass.clubMen, Color.WHITE, 0);
			Helper.writeOnPicture(background, awayClubName + awayClub.getClubStats(), "awayclub-men", FontClass.clubMen, Color.WHITE, 0);

			String gameTime = match.getGameTime() != null ? match.getGameTime() : "";
			String timePart = gameTime.isBlank() ? "" : " | " + gameTime + " Uhr";
			String dateTime = match.getFullMatchDate() + timePart;
			Helper.writeOnPicture(background, dateTime, "dateTime-men", FontClass.dateTimeMen, Color.WHITE, 0);
			Helper.writeOnPicture(background, resolveLocation(homeClub, customLocation), "location-men", FontClass.dateTimeMen, Color.WHITE, 0);

			String savePath = match.getSaveGameDate() + "_" + match.getCompetition() + "_" + homeClub.getSaveName() + "_" + awayClub.getSaveName();
			writeTempTxt(match, homeClub, awayClub, savePath, match.getTeam());

			Helper.savePicture(background, "src/main/resources/save/" + savePath, "Matchday");
			logger.info("Men matchday image saved: path='{}', home='{}', away='{}'", savePath, homeClubName, awayClubName);
			return savePath;
		}
		logger.error("Cannot create men matchday image because a club could not be resolved: date={}, competition={}", match.getSaveGameDate(), match.getCompetition());
		return null;
	}

	private BufferedImage composeBackground(BufferedImage userImage) throws IOException {
		BufferedImage rawTemplate = ImageIO.read(new File("src/main/resources/pictures/template/men/matchdayTemp.png"));

		BufferedImage background = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = background.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

			if (userImage != null) {
				BufferedImage square = cropToSquare(userImage);
				g.drawImage(square, 0, 0, PHOTO_SIZE, PHOTO_SIZE, null);
			}

			g.drawImage(rawTemplate, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, null);
		} finally {
			g.dispose();
		}
		return background;
	}

	private BufferedImage cropToSquare(BufferedImage image) {
		int size = Math.min(image.getWidth(), image.getHeight());
		int x = (image.getWidth() - size) / 2;
		int y = (image.getHeight() - size) / 2;
		return image.getSubimage(x, y, size, size);
	}

	private String buildLocation(ClubModel homeClub) {
		String place = homeClub.getClubPlace();
		if (place == null || place.isBlank()) {
			return "";
		}
		String lower = place.toLowerCase(Locale.ROOT);
		if (lower.contains("sport") || lower.contains("stadion") || lower.contains("platz")) {
			return place;
		}
		return "Sportplatz " + place;
	}

	private String resolveLocation(ClubModel homeClub, String customLocation) {
		if (homeClub == null) {
			return "";
		}
		if (homeClub.getClubName() != null && homeClub.getClubName().equals("FSV Treuen")) {
			if (customLocation != null && !customLocation.isBlank()) {
				return customLocation;
			}
			return "Friedrich-Ludwig-Jahn Stadion";
		}
		if (customLocation != null && !customLocation.isBlank()) {
			return customLocation;
		}
		return buildLocation(homeClub);
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
