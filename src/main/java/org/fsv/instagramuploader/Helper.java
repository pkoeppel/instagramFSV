package org.fsv.instagramuploader;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.fsv.instagramuploader.model.GameModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {
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
	
	int sizeX = 0, sizeY = 0, posX = 0, posY = 0;
	
	switch (pos) {
	 case "logo-left-youth" -> {
		sizeX = 142;
		sizeY = 142;
		posX = 280;
		posY = fac - 1;
	 }
	 case "logo-right-youth" -> {
		sizeX = 142;
		sizeY = 142;
		posX = 1092;
		posY = fac - 1;
	 }
	 case "logo-left-men" -> {
		sizeX = 345 - fac;
		sizeY = 345 - fac;
		posX = (backX / 2 - (345 - fac)) / 2;
		posY = 655 + fac / 2;
	 }
	 case "logo-right-men" -> {
		sizeX = 345 - fac;
		sizeY = 345 - fac;
		posX = (backX / 2 - (345 - fac)) / 2 + backX / 2;
		posY = 655 + fac / 2;
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
	}
	g.drawImage(image, posX, posY, sizeX, sizeY, null);
 }
 
 public static void writeOnPicture(BufferedImage background, String text, String pos, Font font, Color color, int yStart) {
	Graphics g = background.getGraphics();
	g.setColor(color);
	g.setFont(font);
	int width = background.getWidth();
	
	FontMetrics fm = g.getFontMetrics();
	int x = 0, y = 0;
	
	int splitCount = text.split("\n").length - 1;
	int textSize = fm.getFont().getSize() + 5;
	int textPos = splitCount * (textSize / -2);
	for (String line : text.split("\n")) {
	 switch (pos) {
		//Matchday men
		case "headline2-men" -> {
		 x = (width - fm.stringWidth(line)) / 2;
		 y = fm.getHeight();
		 yStart = (yStart + fm.getHeight()) - 20;
		}
		case "homeclub-men" -> {
		 x = (width / 2 - fm.stringWidth(line)) / 2;
		 yStart += fm.getHeight();
		}
		case "awayclub-men" -> {
		 x = (width / 2 - fm.stringWidth(line)) / 2 + width / 2;
		 yStart += fm.getHeight();
		}
		case "bottom-men" -> {
		 x = (width - fm.stringWidth(line)) / 2;
		 y = fm.getHeight();
		 yStart += fm.getHeight();
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
		 x = 757 - (fm.stringWidth(line) / 2);
		 y = textSize + 50;
		}
		case "club-name-home" -> {
		 x = 580 - (fm.stringWidth(line) / 2);
		 y = 80 + textPos;
		 textPos += textSize;
		}
		case "club-name-away" -> {
		 x = 923 - (fm.stringWidth(line) / 2);
		 y = 80 + textPos;
		 textPos += textSize;
		}
		case "bottom-center" -> {
		 x = 757 - (fm.stringWidth(line) / 2);
		 y = 175 - textSize + textPos;
		 textPos += textSize;
		}
		case "center-point" -> {
		 x = 757 - (fm.stringWidth(line) / 2);
		 y = 80 + textPos;
		 textPos += textSize;
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
	JSONParser jp = new JSONParser();
	JSONArray ja = new JSONArray();
	JSONObject curObj;
	try {
	 ja = (JSONArray) jp.parse(new FileReader("src/main/resources/templates/" + file + ".json"));
	 for (int i = 0; i < ja.size(); i++) {
		curObj = (JSONObject) ja.get(i);
		if (curObj.get("game").equals(delMatch)) {
		 ja.remove(i);
		 break;
		}
	 }
	 ja.remove(delMatch);
	} catch (ParseException ignored) {
	}
	FileWriter fw = new FileWriter("src/main/resources/templates/" + file + ".json");
	fw.write(ja.toJSONString());
	fw.flush();
	fw.close();
 }
 
 public static void saveTempTxt(JSONArray ja, JSONObject gameDetails) {
	JSONObject game = new JSONObject();
	game.put("game", gameDetails);
	
	boolean exists = false;
	for (Object oldGame : ja) {
	 if (oldGame.equals(game)) {
		exists = true;
		break;
	 }
	}
	if (!exists) {
	 ja.add(game);
	}
 }
 
 public static JSONObject parser(String match) throws ParseException {
	JSONParser jp = new JSONParser();
	return (JSONObject) jp.parse(match);
 }
 
 public static void savePicture(BufferedImage img, String pathURL, String fileName) throws IOException {
	File dir = new File(pathURL);
	if (!dir.exists()) {
	 //noinspection ResultOfMethodCallIgnored
	 dir.mkdir();
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
 
 public static void updateNextMatchesFromFBDE() throws IOException, ParseException {
	JSONObject allGames = new JSONObject();
	JSONObject teams = (JSONObject) new JSONParser()
					.parse(new FileReader("src/main/resources/templates/teamInfo.json"));
	teams.keySet().forEach(key -> {
	 JSONArray games = new JSONArray();
	 JSONObject team = (JSONObject) teams.get(key);
	 String teamId = team.get("club-id").toString();
	 try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
		HttpGet getMatches = new HttpGet("https://www.fussball.de/ajax.team.next.games/-/mode/PAGE/team-id/" + teamId);
		try (CloseableHttpResponse response = httpClient.execute(getMatches)) {
		 if (response.getCode() == 200) {
			if (response.getEntity() != null) {
			 String html = EntityUtils.toString(response.getEntity());
			 games = parseGames(html);
			} else {
			 System.err.println("Request failed with status code: " + response.getCode());
			}
		 } else {
			return;
		 }
		}
	 } catch (java.text.ParseException | org.apache.hc.core5.http.ParseException | ParseException | IOException e) {
		throw new RuntimeException(e);
	 }
	 allGames.put(key.toString(), games);
	});
	JSONObject jo = new JSONObject(allGames);
	FileWriter fw = new FileWriter("src/main/resources/templates/allMatches.json");
	
	fw.write(jo.toJSONString());
	fw.flush();
	fw.close();
 }
 
 private static JSONArray parseGames(String html) throws IOException, ParseException, java.text.ParseException {
	JSONObject matchdays = (JSONObject) new JSONParser().parse(new FileReader("src/main/resources/templates/data.json"));
	Integer leagueMatchday = Integer.valueOf((String) matchdays.get("lastLeagueMatchday"));
	Integer cupMatchday = Integer.valueOf((String) matchdays.get("lastCupMatchday"));
	
	JSONArray result = new JSONArray();
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
		String team1 = StringEscapeUtils.unescapeHtml4(gameMatcher.group(4)).replace("\u200B", "").trim();
		
		String team2 = StringEscapeUtils.unescapeHtml4(gameMatcher.group(5)).replace("\u200B", "").trim();
		String gameId;
		if (competition.contains("Kinder")) {
		 gameId = gameMatcher.group(6).trim().split("/-/staffel/")[1];
		} else {
		 gameId = gameMatcher.group(6).trim().split("/-/spiel/")[1];
		}
		GameModel newGame = new GameModel(competition, team1, null, team2, null, date, time, null);
		if (competition.contains("liga")) {
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
			
			String homeStats = "\nPlatz " + homePlace + " (" + homePoints + " / " + homeScore + ")\nTrend: " + homeTrend;
			String awayStats = "\nPlatz " + awayPlace + " (" + awayPoints + " / " + awayScore + ")\nTrend: " + awayTrend;
			newGame.setHomeStats(homeStats);
			newGame.setAwayStats(awayStats);
			
			newGame.setMatchDay(String.valueOf(leagueMatchday));
			leagueMatchday++;
		 }
		}
		if (competition.contains("pokal")) {
		 newGame.setMatchDay(String.valueOf(cupMatchday));
		 cupMatchday++;
		}
		result.add(newGame.toJSON());
	 }
	}
	return result;
 }
 
 private static LocalDate parseDate(String dateStr) {
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	return LocalDate.parse(dateStr, formatter);
 }
 
 private static Matcher teamStats(String gameId) throws IOException {
	try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
	 HttpGet getMatches = new org.apache.hc.client5.http.classic.methods.HttpGet("https://www.fussball.de/ajax.season.stats/-/mode/PAGE/spiel/" + gameId);
	 try (CloseableHttpResponse response = httpClient.execute(getMatches)) {
		if (response.getCode() == 200) {
		 if (response.getEntity() != null) {
			String html = EntityUtils.toString(response.getEntity());
			
			String statsRegex = "<td.*?>(.*?)</td>.*?<td>Aktuelle Platzierung</td>.*?<td.*?>(.*?)</td>.*?<td.*?>(.*?)</td>.*?<td>Aktuelle Punktzahl</td>.*?<td.*?>(.*?)</td>.*?<td.*?>(.*?)</td>.*?<td>Aktuelles Torverhältnis</td>.*?<td.*?>(.*?)</td>.*?<td.*?><span.*?>(.*?)</span>.*?</td>.*?<td>Aktueller Trend</td>.*?<td.*?>.*?<span.*?>(.*?)</span>.*?</td>";
			Matcher gamesMatcher = Pattern.compile(statsRegex, Pattern.DOTALL).matcher(html);
			if (gamesMatcher.find()) {
			 return gamesMatcher;
			}
		 } else {
			System.err.println("Request failed with status code: " + response.getCode());
		 }
		}
	 } catch (org.apache.hc.core5.http.ParseException e) {
		throw new RuntimeException(e);
	 }
	}
	return null;
 }
 
 public static void updateMatchdayValue(String matchType) throws IOException, ParseException {
	JSONObject matchdays = (JSONObject) new JSONParser().parse(new FileReader("src/main/resources/templates/data.json"));
	Long leagueMatchday = (Long) matchdays.get("lastLeagueMatchday");
	Long cupMatchday = (Long) matchdays.get("lastCupMatchday");
	if (matchType.toLowerCase().contains("pokal")) {
	 matchdays.remove("lastCupMatchday");
	 matchdays.put("lastCupMatchday", cupMatchday + 1);
	}
	if (matchType.toLowerCase().contains("liga") || matchType.toLowerCase().contains("klasse")) {
	 matchdays.remove("lastLeagueMatchday");
	 matchdays.put("lastLeagueMatchday", leagueMatchday + 1);
	}
	FileWriter fw = new FileWriter("src/main/resources/templates/data.json");
	fw.write(matchdays.toJSONString());
	fw.flush();
	fw.close();
 }
}
