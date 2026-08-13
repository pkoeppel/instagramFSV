package org.fsv.instagramuploader.men;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.GoogleDriveService;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.model.ClubModel;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResultCreator {
 private static final String STATIC_REPORT_TEXT = "🔴⚪\n\n📸: @arminvogtland @netti_1909";
 private static final Pattern GAME_URL_PATTERN = Pattern.compile("^https://www\\.fussball\\.de/spiel/.+/-/spiel/([A-Z0-9]+)(?:#!/)?$", Pattern.CASE_INSENSITIVE);
 private static final Pattern EVENT_PATTERN = Pattern.compile("<div class=\"row-event (event-left|event-right)[^\"]*\">(.*?)(?=<div class=\"row-event |<div class=\"row-time\")", Pattern.DOTALL);
 private static final Pattern MINUTE_PATTERN = Pattern.compile("<div class=\"valign-inner\">(.*?)</div>", Pattern.DOTALL);
 private static final Pattern PLAYER_URL_PATTERN = Pattern.compile("<a href=\"(https://www\\.fussball\\.de/spielerprofil/[^\"]+)\"", Pattern.DOTALL);
 private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
 private static final Logger logger = LoggerFactory.getLogger(ResultCreator.class);

 record GoalEvent(boolean homeTeam, String minute, String playerUrl, boolean ownGoal) {
 }

 private record Goal(String minute, String player) {
 }

 private record Scorers(List<Goal> home, List<Goal> away) {
 }

 private record MatchDetails(String result, String matchLine, String scorers) {
 }
 
 final ArrayList<BufferedImage> allImg = new ArrayList<>();
 BufferedImage targetImg;
	
 public JSONObject createResult(JSONObject match) throws IOException, ParseException {
	JSONObject m = Helper.parser(match.get("match").toString());
	String team = m.get("team").toString();
	ClubModel homeClub = ClubSelector.getClubDetails(new ClubModel((JSONObject) m.get("homeClub")));
	ClubModel awayClub = ClubSelector.getClubDetails(new ClubModel((JSONObject) m.get("awayClub")));
	String mDate = m.get("matchDate").toString();
	String savePath = m.get("savePath").toString();
	String competition = m.get("competition").toString();
	if (homeClub == null || awayClub == null) {
	 throw new IOException("Could not resolve clubs for men result");
	}
	logger.info("Creating men result: path='{}', date={}, team={}, images={}", savePath, mDate, team, allImg.size());
	boolean homeGame = homeClub.getClubName().equals("FSV Treuen");
	logger.debug("Building report text and loading match details for '{}'", savePath);
	String homeClubName = (homeClub.getChangedName() != null) ? homeClub.getChangedName() : homeClub.getClubName();
	String awayClubName = (awayClub.getChangedName() != null) ? awayClub.getChangedName() : awayClub.getClubName();
	MatchDetails matchDetails = loadMatchDetails((String) m.get("gameUrl"), homeClubName, awayClubName);
	String headline = m.get("headline") == null ? "" : m.get("headline").toString().trim();
	String report = m.get("report") == null ? "" : m.get("report").toString().trim();
	StringBuilder fullReportBuilder = new StringBuilder();
	fullReportBuilder.append(matchDetails.matchLine().trim());
	if (!headline.isEmpty()) {
	 fullReportBuilder.append("\n\n").append(headline);
	}
	if (!matchDetails.scorers().isEmpty()) {
	 fullReportBuilder.append("\n\n").append(matchDetails.scorers());
	}
	fullReportBuilder.append("\n\n").append(STATIC_REPORT_TEXT);
	if (!report.isEmpty()) {
	 fullReportBuilder.append("\n\n").append(report);
	}
	String fullReport = fullReportBuilder.toString().stripTrailing();
	int imgCount = 0;
	File directory = new File("src/main/resources/save/" + savePath + "/Bilder");
	if (!directory.exists()) {
	 if (!directory.mkdirs()) {
		throw new IOException("Could not create image directory " + directory.getAbsolutePath());
	 }
	}
	String formatedDate = DateTimeFormatter.ofPattern("yyyyMMdd").format(DateTimeFormatter.ofPattern("yyyy-MM-dd").parse(mDate));
	logger.debug("Applying club logos and result template to {} images", allImg.size());
	BufferedImage homeClubLogo = ImageIO.read(new File(homeClub.getClubLogoDir()));
	BufferedImage awayClubLogo = ImageIO.read(new File(awayClub.getClubLogoDir()));
	BufferedImage opponentClubLogo = homeGame ? awayClubLogo : homeClubLogo;
	BufferedImage ownClubLogo = homeGame ? homeClubLogo : awayClubLogo;
	String folderName = homeGame ? awayClub.getSaveName() : homeClub.getSaveName();
	GoogleDriveService googleService = new GoogleDriveService(folderName);
	if (!allImg.isEmpty()) {
	 for (BufferedImage image : allImg) {
		Helper.pictureOnPicture(image, opponentClubLogo, "smallClubResult-men", 0);
		Helper.pictureOnPicture(image, ownClubLogo, "bigClubResult-men", 0);
	 }
	 BufferedImage firstImg = allImg.get(0);
	 BufferedImage template = ImageIO.read(new File("src/main/resources/pictures/template/men/ResultTemplate.jpg"));
	 Helper.pictureOnPicture(firstImg, template, "template", 0);
	 Helper.pictureOnPicture(firstImg, homeClubLogo, "homeClubLogoResult-men", Helper.isOwnClub(homeClub));
	 Helper.pictureOnPicture(firstImg, awayClubLogo, "awayClubLogoResult-men", Helper.isOwnClub(awayClub));
	 String[] resultSplit = matchDetails.result().split(":");
	 int resultShift = Integer.compare(resultSplit[1].length(), resultSplit[0].length()) * 20;
	 Helper.writeOnPicture(firstImg, matchDetails.result(), "result", FontClass.resultMen, Color.BLACK, resultShift);
	 Helper.writeOnPicture(firstImg, Helper.wrapString(homeClubName, 23), "homeClubResult-men", FontClass.clubMenResult, Color.BLACK, 0);
	 Helper.writeOnPicture(firstImg, Helper.wrapString(awayClubName, 23), "awayClubResult-men", FontClass.clubMenResult, Color.BLACK, 0);
	 for (BufferedImage image : allImg) {
		File fileToSave = new File(directory + "/" + formatedDate + "_" + imgCount + ".jpeg");
		ImageIO.write(image, "jpeg", fileToSave);
		googleService.uploadFileToFolder(fileToSave);
		imgCount++;
	 }
	} else {
	 logger.warn("Men result '{}' contains no uploaded images; only report.txt will be created", savePath);
	}
	try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("src/main/resources/save/" + savePath + "/report.txt"), StandardCharsets.UTF_8)) {
	 writer.write(fullReport);
	}
	logger.debug("Report file written for '{}'", savePath);

	Helper.deleteTempTxt(m, "men-games");
	Helper.updateMatchdayValue(team, competition);
	Map<String, String> result = new HashMap<>();
	result.put("fileDir", savePath);
	result.put("caption", fullReport);
	logger.info("Men result completed: path='{}', result={}, images={}", savePath, matchDetails.result(), imgCount);
	return new JSONObject(result);
 }

 public JSONObject getMatchDetails(JSONObject match) throws IOException {
	ClubModel homeClub = new ClubModel((JSONObject) match.get("homeClub"));
	ClubModel awayClub = new ClubModel((JSONObject) match.get("awayClub"));
	String homeClubName = (homeClub.getChangedName() != null) ? homeClub.getChangedName() : homeClub.getClubName();
	String awayClubName = (awayClub.getChangedName() != null) ? awayClub.getChangedName() : awayClub.getClubName();
	MatchDetails details = loadMatchDetails((String) match.get("gameUrl"), homeClubName, awayClubName);
	Map<String, String> result = new HashMap<>();
	result.put("result", details.result());
	result.put("matchLine", details.matchLine());
	result.put("staticText", STATIC_REPORT_TEXT);
	result.put("scorers", details.scorers());
	return new JSONObject(result);
 }

 private MatchDetails loadMatchDetails(String gameUrl, String homeClubName, String awayClubName) throws IOException {
	Scorers scorers = loadScorers(gameUrl);
	String result = scorers.home().size() + ":" + scorers.away().size();
	String matchLine = homeClubName + " : " + awayClubName + " (" + result + ")";
	return new MatchDetails(result, matchLine, formatScorers(homeClubName, awayClubName, scorers));
 }

 private String formatScorers(String homeClubName, String awayClubName, Scorers scorers) {
	StringBuilder result = new StringBuilder();
	if (!scorers.home().isEmpty()) {
	 result.append(homeClubName).append(":\n");
	 for (Goal goal : scorers.home()) {
		result.append(formatMinute(goal.minute())).append("': ").append(goal.player()).append("\n");
	 }
	}
	if (!scorers.away().isEmpty()) {
	 if (!result.isEmpty()) {
		result.append("\n");
	 }
	 result.append(awayClubName).append(":\n");
	 for (Goal goal : scorers.away()) {
		result.append(formatMinute(goal.minute())).append("': ").append(goal.player()).append("\n");
	 }
	}
	return result.toString().stripTrailing();
 }

 private Scorers loadScorers(String gameUrl) throws IOException {
	Matcher gameUrlMatcher = GAME_URL_PATTERN.matcher(gameUrl == null ? "" : gameUrl);
	if (!gameUrlMatcher.matches()) {
	 throw new IOException("Invalid or missing Fussball.de game URL");
	}
	String gameId = gameUrlMatcher.group(1);
	String courseUrl = "https://www.fussball.de/ajax.match.course/-/mode/PAGE/spiel/" + gameId;
	logger.debug("Loading goal events from Fussball.de: gameId={}", gameId);
	List<Goal> homeScorers = new ArrayList<>();
	List<Goal> awayScorers = new ArrayList<>();
	Map<String, String> playerNames = new HashMap<>();
	try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
	 for (GoalEvent event : parseGoalEvents(loadPage(httpClient, courseUrl))) {
		String playerName = "Unbekannt";
		if (event.playerUrl() != null) {
		 playerName = playerNames.get(event.playerUrl());
		 if (playerName == null) {
			playerName = parsePlayerName(loadPage(httpClient, event.playerUrl()));
			playerNames.put(event.playerUrl(), playerName);
		 }
		}
		Goal goal = new Goal(event.minute(), playerName + (event.ownGoal() ? " (ET)" : ""));
		if (event.homeTeam()) {
		 homeScorers.add(goal);
		} else {
		 awayScorers.add(goal);
		}
	 }
	}
	logger.debug("Loaded goal events for gameId={}: homeGoals={}, awayGoals={}, playerProfiles={}", gameId, homeScorers.size(), awayScorers.size(), playerNames.size());
	return new Scorers(homeScorers, awayScorers);
 }

 static List<GoalEvent> parseGoalEvents(String html) {
	List<GoalEvent> result = new ArrayList<>();
	Matcher eventMatcher = EVENT_PATTERN.matcher(html);
	while (eventMatcher.find()) {
	 String eventHtml = eventMatcher.group(2);
	 if (!eventHtml.contains("class=\"score-left\"")) {
		continue;
	 }
	 Matcher minuteMatcher = MINUTE_PATTERN.matcher(eventHtml);
	 if (minuteMatcher.find()) {
		Matcher playerUrlMatcher = PLAYER_URL_PATTERN.matcher(eventHtml);
		String playerUrl = playerUrlMatcher.find() ? playerUrlMatcher.group(1) : null;
		String minute = StringEscapeUtils.unescapeHtml4(minuteMatcher.group(1).replaceAll("<[^>]+>", ""))
						.replace("’", "")
						.replace("'", "")
						.replaceAll("\\s+", "");
		result.add(new GoalEvent("event-left".equals(eventMatcher.group(1)), minute, playerUrl, eventMatcher.group().contains("own-goal")));
	 }
	}
	return result;
 }

 static String parsePlayerName(String html) throws IOException {
	Matcher titleMatcher = TITLE_PATTERN.matcher(html);
	if (!titleMatcher.find()) {
	 throw new IOException("Player name is missing on Fussball.de profile");
	}
	String title = StringEscapeUtils.unescapeHtml4(titleMatcher.group(1)).replaceAll("\\s+", " ").trim();
	int profileSuffix = title.indexOf(" Spielerprofil |");
	if (profileSuffix < 0) {
	 profileSuffix = title.indexOf(" Basisprofil |");
	}
	if (profileSuffix < 0) {
	 throw new IOException("Unexpected Fussball.de player profile title");
	}
	String player = title.substring(0, profileSuffix);
	int clubStart = player.lastIndexOf(" (");
	return clubStart < 0 ? player : player.substring(0, clubStart);
 }

 private String loadPage(CloseableHttpClient httpClient, String url) throws IOException {
	URI uri = URI.create(url);
	HttpGet request = new HttpGet(uri);
	HttpHost host = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
	try (ClassicHttpResponse response = httpClient.executeOpen(host, request, HttpClientContext.create())) {
	 if (response.getCode() != 200 || response.getEntity() == null) {
		throw new IOException("Fussball.de request failed with status " + response.getCode());
	 }
	 try {
		return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
	 } catch (org.apache.hc.core5.http.ParseException e) {
		throw new IOException("Could not read Fussball.de response", e);
	 }
	}
 }

 private String formatMinute(String minute) {
	return minute.matches("\\d") ? "0" + minute : minute;
 }

 public void savePicture(JSONObject c, MultipartFile file) throws IOException {
	 targetImg = new BufferedImage(1080, 1350, BufferedImage.TYPE_INT_RGB);
	 BufferedImage image = ImageIO.read(file.getInputStream());
	 BufferedImage subImg = image.getSubimage(Helper.getC(c, "x"), Helper.getC(c, "y"), Helper.getC(c, "w"), Helper.getC(c, "h"));
	 Graphics2D g2 = targetImg.createGraphics();
	 g2.drawImage(subImg, 0, 0, 1080, 1350, null);
	 allImg.add(targetImg);
 }
}
