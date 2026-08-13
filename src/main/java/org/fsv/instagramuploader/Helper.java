package org.fsv.instagramuploader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.GameModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {
 private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
 private static final Logger logger = LoggerFactory.getLogger(Helper.class);
 private static Map<String, Map<String, Object>> coordinates;

 static {
	try {
	 loadCoordinates();
	} catch (IOException e) {
	 logger.error("Failed to load coordinates", e);
	 coordinates = new HashMap<>();
	}
 }

 private static void loadCoordinates() throws IOException {
   File file = new File("src/main/resources/templates/coordinates.json");
	if (file.exists()) {
	 Map<String, Object> data = OBJECT_MAPPER.readValue(file, new TypeReference<>() {});
	 coordinates = (Map<String, Map<String, Object>>) data.get("coordinates");
	} else {
	 logger.warn("Coordinates file not found, using empty coordinates");
	 coordinates = new HashMap<>();
	}
 }

 public static void reloadCoordinates() throws IOException {
	loadCoordinates();
 }

 public static int getC(JSONObject c, String val) {
	try {
	 double coord = (double) c.get(val);
	 return Double.valueOf(coord).intValue();
	} catch (ClassCastException e) {
	 long coord = (long) c.get(val);
	 return Long.valueOf(coord).intValue();
	}
 }

 public static String wrapString(String string, int charWrap) {
	int lastBreak = 0;
	int nextBreak = charWrap;
	if (string.length() > charWrap) {
	 StringBuilder setString = new StringBuilder();
	 do {
		while (string.charAt(nextBreak) != '/' && string.charAt(nextBreak) != '-' && string.charAt(nextBreak) != ' ' && nextBreak > lastBreak) {
		 nextBreak--;
		}
		if (nextBreak == lastBreak) {
		 nextBreak = lastBreak + charWrap;
		}
		setString.append(string.substring(lastBreak, nextBreak + 1).trim()).append("\n");
		lastBreak = nextBreak + 1;
		nextBreak += charWrap;

	 } while (nextBreak < string.length());
	 setString.append(string.substring(lastBreak).trim());
	 return setString.toString();
	} else {
	 return string;
	}
 }

 public static void pictureOnPicture(BufferedImage background, BufferedImage image, String pos, int fac) {
	Graphics g = background.getGraphics();
	int backX = background.getWidth();
	int backY = background.getHeight();

	int sizeX, sizeY, posX, posY;

	if (coordinates != null && coordinates.containsKey(pos)) {
	 Map<String, Object> coord = coordinates.get(pos);
	 sizeX = ((Number) coord.get("sizeX")).intValue();
	 sizeY = ((Number) coord.get("sizeY")).intValue();
	 posX = ((Number) coord.get("posX")).intValue();
	 posY = ((Number) coord.get("posY")).intValue();
	 String alignment = coord.containsKey("alignment") ? (String) coord.get("alignment") : "none";

	 String offsetAxis = coord.containsKey("offsetAxis") ? (String) coord.get("offsetAxis") : "";
	 if ("x".equals(offsetAxis)) {
		posX += fac;
	 } else if ("y".equals(offsetAxis)) {
		posY += fac;
	 }

	 // Apply alignment overrides
	 switch (alignment) {
		 case "center" -> {
			 posX = (backX - sizeX) / 2;
			 posY = (backY - sizeY) / 2;
		 }
		 case "center-horizontal" -> posX = (backX - sizeX) / 2;
		 case "center-vertical" -> posY = (backY - sizeY) / 2;
		 case "top-left" -> {
			 posX = 0;
			 posY = 0;
		 }
		 case "top-right" -> {
			 posX = backX - sizeX;
			 posY = 0;
		 }
		 case "bottom-left" -> {
			 posX = 0;
			 posY = backY - sizeY;
		 }
		 case "bottom-right" -> {
			 posX = backX - sizeX;
			 posY = backY - sizeY;
		 }
		 case "bottom" -> posY = backY - sizeY;
		 case "top" -> posY = 0;
		 case "left" -> posX = 0;
		 case "right" -> posX = backX - sizeX;
	 }

	 // If fac is not used as an axis offset, treat it as a shrink factor
	 // and keep the image centered inside the configured area.
	 if ("".equals(offsetAxis) && fac != 0) {
		posX += fac / 2;
		posY += fac / 2;
		sizeX = Math.max(1, sizeX - fac);
		sizeY = Math.max(1, sizeY - fac);
	 }
	} else {
	 // Fallback to default values if position not found in coordinates
	 sizeX = 0;
	 sizeY = 0;
	 posX = 0;
	 posY = 0;
	 logger.warn("Position '{}' not found in coordinates", pos);
	}

	float opacity = 1.0f;
	if (coordinates != null && coordinates.containsKey(pos)) {
	 Object opacityValue = coordinates.get(pos).get("opacity");
	 if (opacityValue instanceof Number number) {
		opacity = number.floatValue();
	 }
	 opacity = Math.max(0.0f, Math.min(1.0f, opacity));
	}

	if (opacity < 1.0f && g instanceof Graphics2D g2d) {
	 Composite oldComposite = g2d.getComposite();
	 g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
	 g2d.drawImage(image, posX, posY, sizeX, sizeY, null);
	 g2d.setComposite(oldComposite);
	} else {
	 g.drawImage(image, posX, posY, sizeX, sizeY, null);
	}
	g.dispose();
 }

 public static void writeOnPicture(BufferedImage background, String text, String pos, Font font, Color color, int yStart) {
	if (coordinates != null && coordinates.containsKey(pos) && "text".equals(coordinates.get(pos).get("type"))) {
	 drawConfiguredText(background, text, coordinates.get(pos), font, color, yStart);
	 return;
	}
	Graphics g = background.getGraphics();
	g.setColor(color);
	g.setFont(font);
	int width = background.getWidth();

	FontMetrics fm = g.getFontMetrics();
	int x, y;
	int border = 35;
	int splitCount = text.split("\n").length - 1;
	int textSize = fm.getFont().getSize() + 5;
	int textPos = splitCount * (textSize / -2);
	for (String line : text.split("\n")) {
	 switch (pos) {
		//Matchday men
		case "headline-men" -> {
		 x = (width - fm.stringWidth(line)) / 2;
		 y = 50;
		 yStart += fm.getHeight() - 50;
		}
		case "homeclub-men" -> {
		 x = (width + 4 * border - 2 * fm.stringWidth(line)) / 4;
		 y = 0;
		 yStart += fm.getHeight();
		}
		case "awayclub-men" -> {
		 x = (3 * width - 4 * border - 2 * fm.stringWidth(line)) / 4;
		 y = 0;
		 yStart += fm.getHeight();
		}
		case "dateTime-men" -> {
		 x = (790 - fm.stringWidth(line) / 2);
		 y = fm.getHeight();
		 yStart += fm.getHeight();
		}
		case "result" -> {
		 x = (width - fm.stringWidth(line)) / 2 + yStart;
		 y = 990 - yStart;
		}
		case "homeClubResult-men" -> {
		 x = (width + 4 * border - 2 * fm.stringWidth(line)) / 4;
		 y = 1040 + textPos;
		 textPos += textSize;
		}
		case "awayClubResult-men" -> {
		 x = (3 * width - 4 * border - 2 * fm.stringWidth(line)) / 4;
		 y = 1040 + textPos;
		 textPos += textSize;
		}
		case "head" -> {
		 x = (width - fm.stringWidth(line)) / 2;
		 y = 400;
		}
		case "team-name" -> {
		 x = 26;
		 y = textSize - 5;
		}
		case "match-type-short" -> {
		 x = 26;
		 y = textSize + 45;
		}
		case "matchType" -> {
		 x = 655 - (fm.stringWidth(line) / 2);
		 y = textSize + 40;
		}
		case "club-name-home" -> {
		 x = 500 - (fm.stringWidth(line) / 2);
		 y = 60 + textPos;
		 textPos += textSize;
		}
		case "club-name-away" -> {
		 x = 800 - (fm.stringWidth(line) / 2);
		 y = 60 + textPos;
		 textPos += textSize;
		}
		case "club-name-stats-home" -> {
		 x = 480 - (fm.stringWidth(line) / 2);
		 y = 80 + textPos;
		 textPos += textSize;
		}
		case "club-name-stats-away" -> {
		 x = 815 - (fm.stringWidth(line) / 2);
		 y = 80 + textPos;
		 textPos += textSize;
		}
		case "bottom-center" -> {
		 x = 652 - (fm.stringWidth(line) / 2);
		 y = 165 - textSize + textPos;
		 textPos += textSize;
		}
		case "center-point" -> {
		 x = 655 - (fm.stringWidth(line) / 2);
		 y = 60 + textPos;
		 textPos += textSize;
		}
		case "center-point-stats" -> {
		 x = 652 - (fm.stringWidth(line) / 2);
		 y = 80 + textPos;
		 textPos += textSize;
		}
		default -> {
		 y = 0;
		 x = 0;
		 yStart = 0;
		}
	 }
	 g.drawString(line, x, y + yStart);
	}
	g.dispose();
 }

 private static void drawConfiguredText(BufferedImage background, String text, Map<String, Object> block, Font fallbackFont, Color fallbackColor, int offset) {
	if (text == null) {
	 text = "";
	}
	Graphics2D graphics = background.createGraphics();
	String fontFamily = String.valueOf(block.getOrDefault("fontFamily", fallbackFont.getFamily()));
	String fontStyle = String.valueOf(block.getOrDefault("fontStyle", "plain"));
	int style = switch (fontStyle) {
	 case "bold" -> Font.BOLD;
	 case "italic" -> Font.ITALIC;
	 case "bold-italic" -> Font.BOLD | Font.ITALIC;
	 default -> Font.PLAIN;
	};
	int fontSize = ((Number) block.getOrDefault("fontSize", fallbackFont.getSize())).intValue();
	Font font = FontRegistry.createFont(fontFamily, style, fontSize, fallbackFont);
	Color color = fallbackColor;
	try {
	 color = Color.decode(String.valueOf(block.getOrDefault("textColor", "#000000")));
	} catch (NumberFormatException e) {
	 logger.warn("Invalid text color '{}'", block.get("textColor"));
	}
	graphics.setFont(font);
	graphics.setColor(color);
	graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	FontMetrics metrics = graphics.getFontMetrics();
	int posX = ((Number) block.get("posX")).intValue();
	int posY = ((Number) block.get("posY")).intValue();
	int sizeX = ((Number) block.get("sizeX")).intValue();
	int sizeY = ((Number) block.get("sizeY")).intValue();
	if ("x".equals(block.get("offsetAxis"))) {
	 posX += offset;
	} else if ("y".equals(block.get("offsetAxis"))) {
	 posY += offset;
	}
	String[] lines = text.split("\\n", -1);
	int textHeight = lines.length * metrics.getHeight();
	String verticalAlignment = String.valueOf(block.getOrDefault("verticalAlignment", "middle"));
	int baseline = switch (verticalAlignment) {
	 case "top" -> posY + metrics.getAscent();
	 case "bottom" -> posY + sizeY - textHeight + metrics.getAscent();
	 default -> posY + (sizeY - textHeight) / 2 + metrics.getAscent();
	};
	String alignment = String.valueOf(block.getOrDefault("alignment", "center"));
	for (String line : lines) {
	 int textWidth = metrics.stringWidth(line);
	 int x = switch (alignment) {
		case "left" -> posX;
		case "right" -> posX + sizeX - textWidth;
		default -> posX + (sizeX - textWidth) / 2;
	 };
	 graphics.drawString(line, x, baseline);
	 baseline += metrics.getHeight();
	}
	graphics.dispose();
 }

 public static String createMatchdaysHead(BufferedImage background, List<LocalDate> md) {
	LocalDate lastThu = md.get(0);
	if (!lastThu.getDayOfWeek().equals(DayOfWeek.THURSDAY)) {
	 lastThu = lastThu.with(TemporalAdjusters.previous(DayOfWeek.THURSDAY));
	}
	LocalDate nextWed = md.get(md.size() - 1);
	if (!nextWed.getDayOfWeek().equals(DayOfWeek.WEDNESDAY)) {
	 nextWed = nextWed.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
	}
	String headDay;
	if (nextWed.getYear() == lastThu.getYear()) {
	 headDay = lastThu.format(DateTimeFormatter.ofPattern("dd.MM.")) + " - " + nextWed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
	} else {
	 headDay = lastThu.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " - " + nextWed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
	}
	writeOnPicture(background, headDay, "head", FontClass.headYouth1, Color.BLACK, 0);
	return lastThu.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
 }

 public static void deleteTempTxt(JSONObject delMatch, String file) throws IOException {
	String filePath = "src/main/resources/templates/" + file + ".json";
	JSONArray matches;
	try (InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
	 matches = (JSONArray) new JSONParser().parse(reader);
	} catch (ParseException e) {
	 throw new IOException("Could not parse " + filePath, e);
	}
	matches.remove(delMatch);
	try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
	 writer.write(matches.toJSONString());
	}
 }

 public static JSONObject parser(String match) throws ParseException {
	JSONParser jp = new JSONParser();
	return (JSONObject) jp.parse(match);
 }

 public static void savePicture(BufferedImage img, String pathURL, String fileName) throws IOException {
	File dir = new File(pathURL);
	if (!dir.exists() && !dir.mkdirs()) {
	 throw new IOException("Could not create directory: " + dir.getAbsolutePath());
	}
	File fileToSafe = new File(dir, fileName + ".jpeg");
	ImageIO.write(img, "jpeg", fileToSafe);
	logger.debug("Saved picture '{}'", fileToSafe.getAbsolutePath());
 }

 public static boolean isNumeric(String str) {
	try {
	 Double.parseDouble(str);
	 return true;
	} catch (NumberFormatException e) {
	 return false;
	}
 }

 public static void updateNextMatchesFromFBDE() throws IOException, URISyntaxException {
	Map<String, List<JSONObject>> allGames = new HashMap<>();
	Map<String, Map<String, String>> teams = OBJECT_MAPPER.readValue(new File("src/main/resources/templates/teamInfo.json"), new TypeReference<>() {
	});
	logger.info("Updating next matches from Fussball.de for {} teams", teams.size());
	try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
	 for (Map.Entry<String, Map<String, String>> entry : teams.entrySet()) {
		String key = entry.getKey();
		Map<String, String> team = entry.getValue();
		List<JSONObject> games = new ArrayList<>();
		String teamId = team.get("club-id");
		HttpGet getMatches = new HttpGet("https://www.fussball.de/ajax.team.next.games/-/mode/PAGE/team-id/" + teamId);
		URI uri = getMatches.getUri();
		HttpHost host = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
		try (ClassicHttpResponse response = httpClient.executeOpen(host, getMatches, HttpClientContext.create())) {
		 int statusCode = response.getCode();
		 if (statusCode == 200 && response.getEntity() != null) {
			String html = EntityUtils.toString(response.getEntity());
			games = parseGames(html, team, httpClient);
		 } else {
			logger.warn("Fussball.de match list request for team '{}' (id={}) failed with status {}", key, teamId, statusCode);
		 }
		} catch (java.text.ParseException | org.apache.hc.core5.http.ParseException | ParseException e) {
		 throw new IOException("Could not parse matches for team " + key, e);
		}
		allGames.put(key, games);
	 }
	}
	JSONObject jo = new JSONObject(allGames);
	logger.info("Persisting {} team schedules to allMatches.json", allGames.size());
	try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("src/main/resources/templates/allMatches.json"), StandardCharsets.UTF_8)) {
	 writer.write(jo.toJSONString());
	}
 }

 private static List<JSONObject> parseGames(String html, Map<String, String> team, CloseableHttpClient httpClient) throws IOException, ParseException, java.text.ParseException, URISyntaxException {
	Long leagueMatchday = null, cupMatchday = null;
	if (team.get("lastLeagueMatchday") != null) {
	 leagueMatchday = Long.valueOf(team.get("lastLeagueMatchday"));
	}
	if (team.get("lastCupMatchday") != null) {
	 cupMatchday = Long.valueOf(team.get("lastCupMatchday"));
	}

	List<JSONObject> result = new ArrayList<>();
	String gamesRegex = "<tr class=\"row-headline visible-small\">.*?</tr>.*?<tr class=\"odd row-competition hidden-small\">.*?</tr>.*?<tr class=\"odd\">.*?</tr>";
	Matcher gamesMatcher = Pattern.compile(gamesRegex, Pattern.DOTALL).matcher(html);
	int gameCount = 1;
	while (gamesMatcher.find()) {

	 String gameRegex = "<td colspan=\"6\">.*?, (.*?) - (.*?) Uhr \\| (.*?)</td>.*?<td class=\"column-club\">.*?<div class=\"club-name\">(.*?)</div>.*?<div class=\"club-name\">(.*?)</div>.*?<td class=\"column-detail\">.*?<a href=\"(.*?)\">.*?</td>";
	 Matcher gameMatcher = Pattern.compile(gameRegex, Pattern.DOTALL).matcher(gamesMatcher.group(0));

	 while (gameMatcher.find()) {
		String dateStr = gameMatcher.group(1);
		LocalDate date = parseDate(dateStr);
		String time = gameMatcher.group(2);
		String competition = gameMatcher.group(3);
		String competitionLower = competition.toLowerCase(Locale.ROOT);
		String homeTeam = StringEscapeUtils.unescapeHtml4(gameMatcher.group(4)).replace("\u200B", "").trim();
		String awayTeam = StringEscapeUtils.unescapeHtml4(gameMatcher.group(5)).replace("\u200B", "").trim();

		String gameId;
		if (competition.contains("Kinder")) {
		 gameId = gameMatcher.group(6).trim().split("/-/staffel/")[1];
		 homeTeam = homeTeam.replace(" - Kinderfestival", "");
		} else {
		 gameId = gameMatcher.group(6).trim().split("/-/spiel/")[1];
		}
		GameModel newGame = new GameModel(String.valueOf(gameCount), competition, date, time, null);
		newGame.setGameUrl(createGameUrl(gameMatcher.group(6)));
		ClubModel homeClub = checkForOwnClub(homeTeam);
		ClubModel awayClub = checkForOwnClub(awayTeam);
		if (competitionLower.contains("liga") || competitionLower.contains("klasse")) {
		 leagueMatchday = setMatchDay(newGame, leagueMatchday);
		 Matcher stats = teamStats(gameId, httpClient);

		 if (stats != null) {
			String homePlace = stats.group(1);
			String awayPlace = stats.group(2);
			String homePoints = stats.group(3);
			String awayPoints = stats.group(4);
			String homeScore = stats.group(5);
			String awayScore = stats.group(6);
			String homeTrend = stats.group(7);
			String awayTrend = stats.group(8);

			homeClub.setClubStats("\nPlatz " + homePlace + " (" + homePoints + " / " + homeScore + ")\nTrend: " + homeTrend);
			awayClub.setClubStats("\nPlatz " + awayPlace + " (" + awayPoints + " / " + awayScore + ")\nTrend: " + awayTrend);
		 }
		}
		if (competitionLower.contains("pokal")) {
		 cupMatchday = setMatchDay(newGame, cupMatchday);
		}
		newGame.setHomeTeam(homeClub);
		newGame.setAwayTeam(awayClub);
		result.add(newGame.toJSON());
	 }
	}
	return result;
 }

 static Long setMatchDay(GameModel game, Long matchDay) {
	if (matchDay == null) {
	 return null;
	}
	game.setMatchDay(String.valueOf(matchDay));
	return matchDay + 1;
 }

 private static String createGameUrl(String gameUrl) {
	String result = StringEscapeUtils.unescapeHtml4(gameUrl.trim());
	if (result.startsWith("//")) {
	 return "https:" + result;
	}
	if (result.startsWith("/")) {
	 return "https://www.fussball.de" + result;
	}
	if (result.startsWith("http://www.fussball.de/")) {
	 return "https://" + result.substring("http://".length());
	}
	return result;
 }

 private static LocalDate parseDate(String dateStr) {
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	return LocalDate.parse(dateStr, formatter);
 }

 private static ClubModel checkForOwnClub(String teamName) {
	if (!teamName.equals("FSV Treuen") && teamName.contains("FSV Treuen")) {
	 return new ClubModel("FSV Treuen", null, null, null, null, teamName);
	}
	if (!teamName.equals("SpG Treuener Land") && teamName.contains("SpG Treuener Land")) {
	 return new ClubModel("SpG Treuener Land", null, null, null, null, teamName);
	}
	return new ClubModel(teamName, null, null, null, null, null);
 }

 private static Matcher teamStats(String gameId, CloseableHttpClient httpClient) throws IOException, URISyntaxException {
	HttpGet getMatches = new HttpGet("https://www.fussball.de/ajax.season.stats/-/mode/PAGE/spiel/" + gameId);
	URI uri = getMatches.getUri();
	HttpHost host = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
	try (ClassicHttpResponse response = httpClient.executeOpen(host, getMatches, HttpClientContext.create())) {
	 if (response.getCode() == 200 && response.getEntity() != null) {
		try {
		 String html = EntityUtils.toString(response.getEntity());
		 String statsRegex = "<td.*?>(.*?)</td>.*?<td>Aktuelle Platzierung</td>.*?<td.*?>(.*?)</td>.*?<td.*?>(.*?)</td>.*?<td>Aktuelle Punktzahl</td>.*?<td.*?>(.*?)</td>.*?<td.*?>(.*?)</td>.*?<td>Aktuelles Torverhältnis</td>.*?<td.*?>(.*?)</td>.*?<td.*?><span.*?>(.*?)</span>.*?</td>.*?<td>Aktueller Trend</td>.*?<td.*?>.*?<span.*?>(.*?)</span>.*?</td>";
		 Matcher gamesMatcher = Pattern.compile(statsRegex, Pattern.DOTALL).matcher(html);
		 return gamesMatcher.find() ? gamesMatcher : null;
		} catch (org.apache.hc.core5.http.ParseException e) {
		 throw new IOException("Could not parse team statistics", e);
		}
	 }
	 logger.warn("Fussball.de statistics request for gameId '{}' failed with status {}", gameId, response.getCode());
	 return null;
	}
 }

 public static void updateMatchdayValue(String team, String matchType) throws IOException {
	Map<String, Map<String, String>> allTeams = OBJECT_MAPPER.readValue(new File("src/main/resources/templates/teamInfo.json"), new TypeReference<>() {
	});
	Map<String, String> teamData = allTeams.get(team);
	if (teamData == null) {
		logger.warn("Cannot update matchday because team '{}' does not exist", team);
		return;
	}

	String matchTypeLower = matchType.toLowerCase(Locale.ROOT);

	if (matchTypeLower.contains("pokal")) {
		String cupMatchday = teamData.get("lastCupMatchday");
		if (cupMatchday == null || cupMatchday.isBlank()) {
			logger.warn("Cannot increment cup matchday for team '{}'; value is missing", team);
		} else {
			teamData.put("lastCupMatchday", String.valueOf(Long.parseLong(cupMatchday) + 1));
			logger.debug("Incremented cup matchday for team '{}' to {}", team, teamData.get("lastCupMatchday"));
		}
	}
	if (matchTypeLower.contains("liga") || matchTypeLower.contains("klasse")) {
		String leagueMatchday = teamData.get("lastLeagueMatchday");
		if (leagueMatchday == null || leagueMatchday.isBlank()) {
			logger.warn("Cannot increment league matchday for team '{}'; value is missing", team);
		} else {
			teamData.put("lastLeagueMatchday", String.valueOf(Long.parseLong(leagueMatchday) + 1));
			logger.debug("Incremented league matchday for team '{}' to {}", team, teamData.get("lastLeagueMatchday"));
		}
	}
	OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File("src/main/resources/templates/teamInfo.json"), allTeams);
	logger.info("Updated matchday counters for team '{}' (type='{}')", team, matchType);
 }

 public static int isOwnClub(ClubModel club) {
	if (club.getClubName().equals("FSV Treuen")) {
	 return 0;
	}
	return 80;
 }
}
