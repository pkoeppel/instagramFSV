package org.fsv.instagramuploader.men;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontRegistry;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.GameModel;
import org.fsv.instagramuploader.model.PlayerModel;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LineupCreator {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(LineupCreator.class);
	private static final int CANVAS_WIDTH = 1080;
	private static final int CANVAS_HEIGHT = 1920;
	private static final int MIN_BANK_FONT_SIZE = 10;

	public String createLineup(String matchJson, String playersJson, String trainer) throws IOException, ParseException {
		logger.info("Creating men lineup image");
		JSONObject match = (JSONObject) new JSONParser().parse(matchJson);
		List<PlayerModel> players = parsePlayers(playersJson);
		GameModel game = buildMatch(match);

		validateLineup(players);

		BufferedImage background = composeBackground();
		addClubLogos(background, game);
		drawStart11(background, players);
		drawBankAndTrainer(background, players, trainer);

		String savePath = game.getSavePath() != null ? game.getSavePath() : buildFallbackSavePath(game);
		Helper.savePicture(background, "src/main/resources/save/" + savePath, "Lineup");
		logger.info("Created men lineup image at {}", savePath);
		return savePath;
	}

	@SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "Anonymous TypeReference is standard Jackson pattern")
	private List<PlayerModel> parsePlayers(String playersJson) throws IOException {
		return OBJECT_MAPPER.readValue(playersJson, new TypeReference<>() {
		});
	}

	private void validateLineup(List<PlayerModel> players) {
		List<PlayerModel> starters = players.stream()
				.filter(p -> "start".equalsIgnoreCase(p.getRole()))
				.toList();

		if (starters.size() > 11) {
			logger.warn("Mehr als 11 Spieler für die Startelf gewählt: {}", starters.size());
		}

		boolean hasGoalkeeper = starters.stream().anyMatch(PlayerModel::isGoalkeeper);
		boolean hasCaptain = starters.stream().anyMatch(PlayerModel::isCaptain);

		if (!hasGoalkeeper) {
			logger.warn("Der Torwart fehlt in der Startaufstellung.");
		}

		if (!hasCaptain) {
			logger.warn("Der Kapitän fehlt in der Startaufstellung.");
		}
	}

	private GameModel buildMatch(JSONObject match) {
		String competition = match.get("competition") != null ? match.get("competition").toString() : "";
		String dateStr = match.get("matchDate") != null ? match.get("matchDate").toString() : null;
		LocalDate gameDate = dateStr != null ? LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;
		String team = match.get("team") != null ? match.get("team").toString() : "1";
		GameModel game = new GameModel(competition, gameDate, null, team);
		game.setSavePath(match.get("savePath") != null ? match.get("savePath").toString() : null);
		Object homeClub = match.get("homeClub");
		if (homeClub instanceof Map<?, ?> homeMap) {
			game.setHomeTeam(new ClubModel(homeMap));
		}
		Object awayClub = match.get("awayClub");
		if (awayClub instanceof Map<?, ?> awayMap) {
			game.setAwayTeam(new ClubModel(awayMap));
		}
		return game;
	}

	private String buildFallbackSavePath(GameModel game) {
		String home = game.getHomeTeam() != null ? game.getHomeTeam().getClubName() : "Heim";
		String away = game.getAwayTeam() != null ? game.getAwayTeam().getClubName() : "Gast";
		String date = game.getSaveGameDate() != null ? game.getSaveGameDate() : "unknown";
		return date + "_" + game.getCompetition() + "_" + home + "_" + away;
	}

	private BufferedImage composeBackground() throws IOException {
		BufferedImage rawTemplate = ImageIO.read(new File("src/main/resources/pictures/template/men/teamTemp.png"));
		BufferedImage background = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = background.createGraphics();
		try {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(rawTemplate, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, null);
		} finally {
			g.dispose();
		}
		return background;
	}

	private void addClubLogos(BufferedImage background, GameModel game) throws IOException, ParseException {
		if (game.getHomeTeam() == null || game.getAwayTeam() == null) {
			return;
		}
		ClubModel homeClub = resolveClub(game.getHomeTeam());
		ClubModel awayClub = resolveClub(game.getAwayTeam());
		ClubModel own = "FSV Treuen".equals(homeClub.getClubName()) ? homeClub
				: ("FSV Treuen".equals(awayClub.getClubName()) ? awayClub : homeClub);
		ClubModel opponent = (own == homeClub) ? awayClub : homeClub;
		drawLogo(background, own, "lineup-logo-home");
		drawLogo(background, opponent, "lineup-logo-away");
	}

	private ClubModel resolveClub(ClubModel club) throws IOException, ParseException {
		ClubModel details = ClubSelector.getClubDetails(club);
		return details != null ? details : club;
	}

	private void drawLogo(BufferedImage background, ClubModel club, String position) throws IOException {
		String logoPath = club.getClubLogoDir();
		if (logoPath == null || !Files.exists(Paths.get(logoPath))) {
			logger.warn("Club logo for '{}' not found at {}", club.getClubName(), logoPath);
			return;
		}
		BufferedImage logo = ImageIO.read(new File(logoPath));
		if (logo == null) {
			logger.warn("Could not read club logo for '{}'", club.getClubName());
			return;
		}
		Helper.pictureOnPicture(background, logo, position, Helper.isOwnClub(club));
	}

	private void drawStart11(BufferedImage background, List<PlayerModel> players) {
		List<PlayerModel> starters = players.stream()
				.filter(p -> "start".equalsIgnoreCase(p.getRole()))
				.sorted(this::playerDisplayOrder)
				.limit(11)
				.toList();

		Font fallbackFont = new Font(Font.SANS_SERIF, Font.BOLD, 42);
		Color fallbackColor = Color.WHITE;
		for (int i = 0; i < starters.size(); i++) {
			PlayerModel player = starters.get(i);
			String numberText = String.valueOf(player.getNumber());
			String nameText = player.getName() + buildSuffix(player);
			Helper.writeOnPicture(background, numberText, "lineup-start-" + (i + 1) + "-number", fallbackFont, fallbackColor, 0);
			Helper.writeOnPicture(background, nameText, "lineup-start-" + (i + 1) + "-name", fallbackFont, fallbackColor, 0);
		}
	}

	private void drawBankAndTrainer(BufferedImage background, List<PlayerModel> players, String trainer) {
		List<PlayerModel> bench = players.stream()
				.filter(p -> "bench".equalsIgnoreCase(p.getRole()))
				.sorted(this::playerDisplayOrder)
				.toList();
		drawBank(background, bench);
		Font fallbackFont = new Font(Font.SANS_SERIF, Font.BOLD, 30);
		Color fallbackColor = Color.WHITE;
		String trainerText = resolveTrainer(players, trainer);
		Helper.writeOnPicture(background, trainerText, "lineup-trainer", fallbackFont, fallbackColor, 0);
	}

	String resolveTrainer(List<PlayerModel> players, String trainer) {
		if (trainer != null && !trainer.isBlank()) {
			return trainer;
		}
		return players.stream()
				.filter(p -> "trainer".equalsIgnoreCase(p.getRole()))
				.map(PlayerModel::getName)
				.findFirst()
				.orElse("Toni Seidel");
	}

	private void drawBank(BufferedImage background, List<PlayerModel> bench) {
		Map<String, Object> block = Helper.getCoordinateBlock("lineup-bank");
		if (block == null) {
			String bankString = bench.isEmpty()
					? "keine"
					: bench.stream()
							.map(p -> p.getNumber() + " | " + p.getName() + buildSuffix(p))
							.collect(Collectors.joining(", "));
			Helper.writeOnPicture(background, bankString, "lineup-bank", new Font(Font.SANS_SERIF, Font.BOLD, 30), Color.WHITE, 0);
			return;
		}
		if (bench.isEmpty()) {
			Helper.writeOnPicture(background, "keine", "lineup-bank", new Font(Font.SANS_SERIF, Font.BOLD, 30), Color.WHITE, 0);
			return;
		}

		List<String> entries = bench.stream()
				.map(p -> p.getNumber() + " | " + p.getName() + buildSuffix(p))
				.toList();

		Graphics2D graphics = background.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			Color color = decodeColor(block.getOrDefault("textColor", "#ffffff"));
			graphics.setColor(color);

			int posX = ((Number) block.get("posX")).intValue();
			int posY = ((Number) block.get("posY")).intValue();
			int sizeX = ((Number) block.get("sizeX")).intValue();
			int sizeY = ((Number) block.get("sizeY")).intValue();
			String alignment = String.valueOf(block.getOrDefault("alignment", "left"));
			String verticalAlignment = String.valueOf(block.getOrDefault("verticalAlignment", "top"));

			Font baseFont = createFont(block, new Font(Font.SANS_SERIF, Font.BOLD, 30));
			List<String> lines;
			int fontSize = baseFont.getSize();
			while (true) {
				Font font = baseFont.deriveFont((float) fontSize);
				graphics.setFont(font);
				FontMetrics metrics = graphics.getFontMetrics();
				lines = wrapBankEntries(entries, metrics, sizeX);
				int totalHeight = lines.size() * metrics.getHeight();
				if (totalHeight <= sizeY || fontSize <= MIN_BANK_FONT_SIZE) {
					break;
				}
				fontSize--;
			}

			FontMetrics metrics = graphics.getFontMetrics();
			int baseline;
			switch (verticalAlignment) {
				case "top" -> baseline = posY + metrics.getAscent();
				case "bottom" -> baseline = posY + sizeY - lines.size() * metrics.getHeight() + metrics.getAscent();
				default -> baseline = posY + (sizeY - lines.size() * metrics.getHeight()) / 2 + metrics.getAscent();
			}

			for (String line : lines) {
				int textWidth = metrics.stringWidth(line);
				int x = switch (alignment) {
					case "center" -> posX + (sizeX - textWidth) / 2;
					case "right" -> posX + sizeX - textWidth;
					default -> posX;
				};
				graphics.drawString(line, x, baseline);
				baseline += metrics.getHeight();
			}
		} finally {
			graphics.dispose();
		}
	}

	private List<String> wrapBankEntries(List<String> entries, FontMetrics metrics, int maxWidth) {
		int separatorWidth = metrics.stringWidth(", ");
		List<String> lines = new ArrayList<>();
		StringBuilder currentLine = new StringBuilder();
		int currentWidth = 0;
		for (int i = 0; i < entries.size(); i++) {
			String entry = entries.get(i);
			int entryWidth = metrics.stringWidth(entry);
			if (i > 0 && currentWidth + separatorWidth + entryWidth > maxWidth) {
				lines.add(currentLine.toString());
				currentLine = new StringBuilder(entry);
				currentWidth = entryWidth;
			} else {
				if (i > 0) {
					currentLine.append(", ");
					currentWidth += separatorWidth;
				}
				currentLine.append(entry);
				currentWidth += entryWidth;
			}
		}
		if (!currentLine.isEmpty()) {
			lines.add(currentLine.toString());
		}
		return lines;
	}

	private Color decodeColor(Object value) {
		try {
			return Color.decode(String.valueOf(value));
		} catch (NumberFormatException e) {
			return Color.WHITE;
		}
	}

	private int playerDisplayOrder(PlayerModel a, PlayerModel b) {
		if (a.isGoalkeeper() && !b.isGoalkeeper()) {
			return -1;
		}
		if (b.isGoalkeeper() && !a.isGoalkeeper()) {
			return 1;
		}
		return Integer.compare(a.getNumber(), b.getNumber());
	}

	private String buildSuffix(PlayerModel player) {
		List<String> tags = new ArrayList<>();
		if (player.isGoalkeeper()) {
			tags.add("TW");
		}
		if (player.isCaptain()) {
			tags.add("C");
		}
		if (tags.isEmpty()) {
			return "";
		}
		return " (" + String.join(", ", tags) + ")";
	}

	private Font createFont(Map<String, Object> block, Font fallbackFont) {
		String fontFamily = String.valueOf(block.getOrDefault("fontFamily", fallbackFont.getFamily()));
		String fontStyle = String.valueOf(block.getOrDefault("fontStyle", "plain"));
		int style = switch (fontStyle) {
			case "bold" -> Font.BOLD;
			case "italic" -> Font.ITALIC;
			case "bold-italic" -> Font.BOLD | Font.ITALIC;
			default -> Font.PLAIN;
		};
		int fontSize = ((Number) block.getOrDefault("fontSize", fallbackFont.getSize())).intValue();
		return FontRegistry.createFont(fontFamily, style, fontSize, fallbackFont);
	}

}
