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
 static Logger logger = LoggerFactory.getLogger(Helper.class);
 
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
	switch (pos) {
	 case "logo-left-youth" -> {
		sizeX = 128;
		sizeY = 128;
		posX = 245;
		posY = fac - 1;
	 }
	 case "logo-right-youth" -> {
		sizeX = 128;
		sizeY = 128;
		posX = 940;
		posY = fac - 1;
	 }
	 case "logo-left-men" -> {
		sizeX = 345 - fac;
		sizeY = 345 - fac;
		posX = (backX / 2 - (190 - fac)) / 2;
		posY = 1310 + fac / 2;
	 }
	 case "logo-right-men" -> {
		sizeX = 345 - fac;
		sizeY = 345 - fac;
		posX = (backX / 2 - (500 - fac)) / 2 + backX / 2;
		posY = 1310 + fac / 2;
	 }
	 case "homeClubResult-men" -> {
		sizeX = 280 - (fac / 2);
		sizeY = 280 - (fac / 2);
		posX = (backX / 2 - (210 - fac / 2)) / 2;
		posY = 720 + fac / 2;
	 }
	 case "awayClubResult-men" -> {
		sizeX = 280 - (fac / 2);
		sizeY = 280 - (fac / 2);
		posX = (backX / 2 - (350 - fac / 2)) / 2 + backX / 2;
		posY = 720 + fac / 2;
	 }
	 case "sponsor-men" -> {
		sizeX = 300;
		sizeY = 150;
		posX = backX - 300;
		posY = backY - 150;
	 }
	 case "bigClub-men" -> {
		sizeX = 400;
		sizeY = 400;
		posX = 4;
		posY = 715;
	 }
	 case "smallClub-men" -> {
		sizeX = 260;
		sizeY = 260;
		posX = 260;
		posY = 915;
	 }
	 case "bigClubResult-men" -> {
		sizeX = 180;
		sizeY = 180;
		posX = 5;
		posY = 5;
	 }
	 case "smallClubResult-men" -> {
		sizeX = 115;
		sizeY = 115;
		posX = 105;
		posY = 90;
	 }
	 case "playerPic-men" -> {
		sizeX = 560;
		sizeY = 560;
		posX = 500;
		posY = 720;
	 }
	 case "template" -> {
		sizeX = 1080;
		sizeY = 538;
		posX = 0;
		posY = 1350 - sizeY;
	 }
	 default -> {
		sizeX = 0;
		sizeY = 0;
		posX = 0;
		posY = 0;
	 }
	}
	g.drawImage(image, posX, posY, sizeX, sizeY, null);
 }
 
 public static void writeOnPicture(BufferedImage background, String text, String pos, Font font, Color color, int yStart) {
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
	int textPosUp = 0;
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
		 x = (width - fm.stringWidth(line)) / 2;
		 y = 990;
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
		case "homeScorer" -> {
		 x = width - fm.stringWidth(line) - (width / 2 + 50);
		 y = 1120 + textPosUp;
		 textPosUp += textSize;
		}
		case "awayScorer" -> {
		 x = width / 2 + 50;
		 y = 1120 + textPosUp;
		 textPosUp += textSize;
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
	JSONArray ja = new JSONArray();
	JSONObject curObj;
	try {
	 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/" + file + ".json"), StandardCharsets.UTF_8);
	 ja = (JSONArray) new JSONParser().parse(reader);
	 for (int i = 0; i < ja.size(); i++) {
		curObj = (JSONObject) ja.get(i);
		if (curObj.equals(delMatch)) {
		 ja.remove(i);
		 break;
		}
	 }
	 ja.remove(delMatch);
	} catch (ParseException ignored) {
	}
	try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("src/main/resources/templates/" + file + ".json"), StandardCharsets.UTF_8)) {
	 writer.write(ja.toJSONString());
	}
 }
 
 public static JSONObject parser(String match) throws ParseException {
	JSONParser jp = new JSONParser();
	return (JSONObject) jp.parse(match);
 }
 
 public static void savePicture(BufferedImage img, String pathURL, String fileName) throws IOException {
	File dir = new File(pathURL);
	if (!dir.exists()) {
	 if (!dir.mkdirs()) {
		logger.error("Error creating directory: {}", dir.getAbsolutePath());
	 }
	}
	File fileToSafe = new File(dir + "/" + fileName + ".jpeg");
	ImageIO.write(img, "jpeg", fileToSafe);
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
	Map<String, Map<String, String>> teams = new ObjectMapper().readValue(new File("src/main/resources/templates/teamInfo.json"), new TypeReference<>() {
	});
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
			games = parseGames(html, team);
		 } else {
			logger.error("Request failed with status code: {}", response.getCode());
		 }
		} catch (java.text.ParseException | org.apache.hc.core5.http.ParseException | ParseException | IOException e) {
		 throw new RuntimeException(e);
		}
		allGames.put(key, games);
	 }
	}
	JSONObject jo = new JSONObject(allGames);
	
	try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("src/main/resources/templates/allMatches.json"), StandardCharsets.UTF_8)) {
	 writer.write(jo.toJSONString());
	}
 }
 
 private static List<JSONObject> parseGames(String html, Map<String, String> team) throws IOException, ParseException, java.text.ParseException, URISyntaxException {
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
	
	while (gamesMatcher.find()) {
	 
	 String gameRegex = "<td colspan=\"6\">.*?, (.*?) - (.*?) Uhr \\| (.*?)</td>.*?<td class=\"column-club\">.*?<div class=\"club-name\">(.*?)</div>.*?<div class=\"club-name\">(.*?)</div>.*?<td class=\"column-detail\">.*?<a href=\"(.*?)\">.*?</td>";
	 Matcher gameMatcher = Pattern.compile(gameRegex, Pattern.DOTALL).matcher(gamesMatcher.group(0));
	 
	 while (gameMatcher.find()) {
		String dateStr = gameMatcher.group(1);
		LocalDate date = parseDate(dateStr);
		String time = gameMatcher.group(2);
		String competition = gameMatcher.group(3);
		String homeTeam = StringEscapeUtils.unescapeHtml4(gameMatcher.group(4)).replace("\u200B", "").trim();
		String awayTeam = StringEscapeUtils.unescapeHtml4(gameMatcher.group(5)).replace("\u200B", "").trim();
		
		String gameId;
		if (competition.contains("Kinder")) {
		 gameId = gameMatcher.group(6).trim().split("/-/staffel/")[1];
		 homeTeam = homeTeam.replace(" - Kinderfestival", "");
		} else {
		 gameId = gameMatcher.group(6).trim().split("/-/spiel/")[1];
		}
		GameModel newGame = new GameModel(competition, date, time, null);
		ClubModel homeClub = checkForOwnClub(homeTeam);
		ClubModel awayClub = checkForOwnClub(awayTeam);
		if (competition.contains("liga") || competition.contains("klasse")) {
		 Matcher stats = teamStats(gameId);
		 
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
			if (leagueMatchday != null) {
			 newGame.setMatchDay(String.valueOf(leagueMatchday));
			 leagueMatchday++;
			}
		 }
		}
		if (competition.contains("pokal") && cupMatchday != null) {
		 newGame.setMatchDay(String.valueOf(cupMatchday));
		 cupMatchday++;
		}
		newGame.setHomeTeam(homeClub);
		newGame.setAwayTeam(awayClub);
		result.add(newGame.toJSON());
	 }
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
 
 private static Matcher teamStats(String gameId) throws IOException, URISyntaxException {
	HttpGet getMatches = new HttpGet("https://www.fussball.de/ajax.season.stats/-/mode/PAGE/spiel/" + gameId);
	try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
	 URI uri = getMatches.getUri();
	 HttpHost host = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
	 try (ClassicHttpResponse response = httpClient.executeOpen(host, getMatches, HttpClientContext.create())) {
		if (response.getCode() == 200) {
		 if (response.getEntity() != null) {
			String html = EntityUtils.toString(response.getEntity());
			
			String statsRegex = "<td.*?>(.*?)</td>.*?<td>Aktuelle Platzierung</td>.*?<td.*?>(.*?)</td>.*?<td.*?>(.*?)</td>.*?<td>Aktuelle Punktzahl</td>.*?<td.*?>(.*?)</td>.*?<td.*?>(.*?)</td>.*?<td>Aktuelles Torverhältnis</td>.*?<td.*?>(.*?)</td>.*?<td.*?><span.*?>(.*?)</span>.*?</td>.*?<td>Aktueller Trend</td>.*?<td.*?>.*?<span.*?>(.*?)</span>.*?</td>";
			Matcher gamesMatcher = Pattern.compile(statsRegex, Pattern.DOTALL).matcher(html);
			if (gamesMatcher.find()) {
			 return gamesMatcher;
			}
		 } else {
			logger.error("Request failed with status code: {}", response.getCode());
		 }
		}
	 } catch (org.apache.hc.core5.http.ParseException e) {
		throw new RuntimeException(e);
	 }
	}
	return null;
 }
 
 public static void updateMatchdayValue(String team, String matchType) throws IOException {
	Map<String, Map<String, String>> allTeams = new ObjectMapper().readValue(new File("src/main/resources/templates/teamInfo.json"), new TypeReference<>() {
	});
	Map<String, String> teamData = allTeams.get(team);
	long leagueMatchday = Long.parseLong(teamData.get("lastLeagueMatchday"));
	long cupMatchday = Long.parseLong(teamData.get("lastCupMatchday"));
	
	String matchTypeLower = matchType.toLowerCase(Locale.ROOT);
	
	if (matchTypeLower.contains("pokal")) {
	 teamData.remove("lastCupMatchday");
	 teamData.put("lastCupMatchday", String.valueOf(cupMatchday + 1));
	}
	if (matchTypeLower.contains("liga") || matchTypeLower.contains("klasse")) {
	 teamData.remove("lastLeagueMatchday");
	 teamData.put("lastLeagueMatchday", String.valueOf(leagueMatchday + 1));
	}
	new ObjectMapper().writeValue(new File("src/main/resources/templates/teamInfo.json"), allTeams);
 }
 
 public static int isOwnClub(ClubModel club) {
	if (club.getClubName().equals("FSV Treuen")) {
	 return 0;
	}
	return 80;
 }
}
