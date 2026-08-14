package org.fsv.instagramuploader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletResponse;
import org.fsv.instagramuploader.men.MatchdayCreator;
import org.fsv.instagramuploader.men.ResultCreator;
import org.fsv.instagramuploader.model.GameModel;
import org.fsv.instagramuploader.model.ResultModel;
import org.fsv.instagramuploader.youth.MatchdaysCreator;
import org.fsv.instagramuploader.youth.ResultsCreator;
import org.fsv.instagramuploader.bot.TelegramBot;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
public class Controller {
 private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
 private static final Logger logger = LoggerFactory.getLogger(Controller.class);

 @Autowired
 private TelegramBot telegramBot;

 MatchdaysCreator msc;
 ResultsCreator rsc;
 MatchdayCreator mc;
 ResultCreator rc;

 public Controller() {
	this.msc = new MatchdaysCreator();
	this.rsc = new ResultsCreator();
	this.mc = new MatchdayCreator();
	this.rc = new ResultCreator();
 }

 @GetMapping("/getMatches")
 public ResponseEntity<JSONObject> getMatches() {
	try {
	 JSONObject result = (JSONObject) readJsonFile("allMatches.json");
	 logger.debug("Loaded schedules for {} teams from allMatches.json", result.size());
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not load allMatches.json", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Berlin")
 public void updateNextMatchesScheduled() {
	long startedAt = System.currentTimeMillis();
	try {
	 logger.info("Scheduled Fussball.de match update started");
	 Helper.updateNextMatchesFromFBDE();
	 logger.info("Scheduled Fussball.de match update completed in {} ms", System.currentTimeMillis() - startedAt);
	 sendAutomaticHerrenMatchHints();
	} catch (IOException | URISyntaxException e) {
	 logger.error("Scheduled update of allMatches.json failed", e);
	}
 }

 private void sendAutomaticHerrenMatchHints() {
	try {
	 JSONObject allMatches = (JSONObject) readJsonFile("allMatches.json");
	 ZoneId berlin = ZoneId.of("Europe/Berlin");
	 LocalDate previewDate = LocalDate.now(berlin).plusDays(2);
	 DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	 int sentHints = 0;
	 for (Object teamObj : allMatches.keySet()) {
		String team = teamObj.toString();
		Object gamesObj = allMatches.get(teamObj);
		if (!(gamesObj instanceof JSONArray games)) {
		 continue;
		}
		for (Object gameObj : games) {
		 if (!(gameObj instanceof JSONObject game)) {
			continue;
		 }
		 Object gameDateObj = game.get("gameDate");
		 if (gameDateObj == null) {
			continue;
		 }
		 try {
			LocalDate gameDate = LocalDate.parse(gameDateObj.toString());
			if (previewDate.equals(gameDate)) {
			 String home = "?";
			 Object homeTeamObj = game.get("homeTeam");
			 if (homeTeamObj instanceof Map<?, ?> homeMap) {
				Object homeName = homeMap.get("clubName");
				home = homeName != null ? homeName.toString() : "?";
			 }
			 String away = "?";
			 Object awayTeamObj = game.get("awayTeam");
			 if (awayTeamObj instanceof Map<?, ?> awayMap) {
				Object awayName = awayMap.get("clubName");
				away = awayName != null ? awayName.toString() : "?";
			 }
			 Object timeObj = game.get("gameTime");
			 String time = timeObj != null ? timeObj.toString() : "";
			 Object competitionObj = game.get("competition");
			 String competition = competitionObj != null ? competitionObj.toString() : "";
			 String message = String.format("Erinnerung: In 2 Tagen findet ein Herren-Spiel statt:%n%n%s | %s Uhr | %s%n%s - %s",
					 gameDate.format(dateFormatter), time, competition, home, away);
			 if (telegramBot != null) {
				telegramBot.sendAutomaticMatchHint(message);
				sentHints++;
			 } else {
				logger.warn("TelegramBot not available; cannot send automatic match hint");
			 }
			}
		 } catch (Exception e) {
			logger.error("Could not process game for team '{}' during automatic match hint check", team, e);
		 }
		}
	 }
	 logger.info("Sent {} automatic Herren match hint(s)", sentHints);
	} catch (Exception e) {
	 logger.error("Automatic Herren match hint check failed", e);
	}
 }

 @GetMapping("/getNextMatches")
 public ResponseEntity<JSONObject> getNextMatches() {
	long startedAt = System.currentTimeMillis();
	try {
	 logger.info("Manual Fussball.de match update started");
	 Helper.updateNextMatchesFromFBDE();
	 JSONObject result = (JSONObject) readJsonFile("allMatches.json");
	 logger.info("Manual Fussball.de match update completed for {} teams in {} ms", result.size(), System.currentTimeMillis() - startedAt);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException | URISyntaxException e) {
	 logger.error("Manual Fussball.de match update failed after {} ms", System.currentTimeMillis() - startedAt, e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @GetMapping(value = "/download/{pathName}/{fileName:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
 public @ResponseBody byte[] downloadMenFile(@PathVariable String pathName, @PathVariable String fileName) throws IOException {
	Path filePath = Paths.get("src/main/resources/save", pathName, fileName);
	logger.debug("Downloading men file '{}'", filePath);
	return Files.readAllBytes(filePath);
 }

 @GetMapping(value = "/download/youth/{pathName}/{fileName:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
 public @ResponseBody byte[] downloadYouthFile(@PathVariable String pathName, @PathVariable String fileName) throws IOException {
	Path filePath = Paths.get("src/main/resources/save/youth/", pathName, fileName);
	logger.debug("Downloading youth file '{}'", filePath);
	return Files.readAllBytes(filePath);
 }

 @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Directory is known to exist or handled elsewhere")
 @GetMapping(value = "/zip-download/{dir}", produces = "application/zip")
 public void zipDownload(@PathVariable String dir, HttpServletResponse res) throws IOException {
	logger.info("ZIP download requested for result directory '{}'", dir);
	File directory = new File("src/main/resources/save/" + dir + "/Bilder/");
	if (directory.exists() && directory.isDirectory()) {
	 String[] fileList = directory.list();
	 if (fileList != null && fileList.length != 0) {
		res.setStatus(HttpServletResponse.SC_OK);
		res.addHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(dir + ".zip", StandardCharsets.UTF_8)
						.build()
						.toString());
		try (ZipOutputStream zipOS = new ZipOutputStream(res.getOutputStream())) {
		 for (String fn : fileList) {
			FileSystemResource fsRes = new FileSystemResource(directory.getPath() + "/" + fn);
			
			ZipEntry zip = new ZipEntry(fsRes.getFilename());
			zip.setSize(fsRes.contentLength());
			zipOS.putNextEntry(zip);
			
			try (InputStream input = fsRes.getInputStream()) {
			 StreamUtils.copy(input, zipOS);
			}
			zipOS.closeEntry();
		 }
		 zipOS.finish();
		}
	 }
	}
 }

 @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "Anonymous TypeReference is standard Jackson pattern")
 @RequestMapping("/updateClub")
 public ResponseEntity<String> updateClub(@RequestParam("club") String currentClub, @RequestParam("newClubName") String newClub) {
	try {
	 logger.info("Renaming club '{}' to '{}'", currentClub, newClub);
	 Map<String, Map<String, Object>> allClubs = OBJECT_MAPPER.readValue(new File("src/main/resources/templates/clubs.json"), new TypeReference<>() {
	 });
	 Map<String, Object> clubData = allClubs.get(currentClub);
	 if (clubData == null) {
		logger.warn("Cannot rename unknown club '{}'", currentClub);
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	 }
	 if (!currentClub.equals(newClub) && allClubs.containsKey(newClub)) {
		logger.warn("Cannot rename club '{}' because '{}' already exists", currentClub, newClub);
		return new ResponseEntity<>(HttpStatus.CONFLICT);
	 }
	 File clubLogo = new File("src/main/resources/pictures/teamlogos/" + clubData.get("fileName").toString() + ".png");
	 String newClubLogoName = newClub.replaceAll("\\W", "_");
	 Files.move(clubLogo.toPath(), new File("src/main/resources/pictures/teamlogos/" + newClubLogoName + ".png").toPath(), StandardCopyOption.REPLACE_EXISTING);
	 clubData.put("fileName", newClubLogoName);
	 allClubs.remove(currentClub);
	 allClubs.put(newClub, clubData);
	 OBJECT_MAPPER.writeValue(new File("src/main/resources/templates/clubs.json"), allClubs);
	 logger.info("Club '{}' renamed to '{}'", currentClub, newClub);
	 return new ResponseEntity<>(HttpStatus.OK);
	} catch (IOException e) {
	 logger.error("Could not rename club '{}' to '{}'", currentClub, newClub, e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @DeleteMapping("/deleteClub")
 public ResponseEntity<String> deleteClub(@RequestParam("club") String currentClub) {
	try {
	 logger.info("Deleting club '{}'", currentClub);
	 File clubsFile = new File("src/main/resources/templates/clubs.json");
	 Map<String, Map<String, Object>> allClubs = OBJECT_MAPPER.readValue(clubsFile, new TypeReference<>() {
	 });
	 Map<String, Object> removedClub = allClubs.remove(currentClub);
	 if (removedClub == null) {
		logger.warn("Cannot delete unknown club '{}'", currentClub);
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	 }
	 OBJECT_MAPPER.writeValue(clubsFile, allClubs);
	 Path clubLogo = Paths.get("src/main/resources/pictures/teamlogos", removedClub.get("fileName") + ".png");
	 boolean logoDeleted = Files.deleteIfExists(clubLogo);
	 logger.info("Club '{}' deleted; logoDeleted={}", currentClub, logoDeleted);
	 return new ResponseEntity<>(HttpStatus.OK);
	} catch (IOException e) {
	 logger.error("Could not delete club '{}'", currentClub, e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @PostMapping("/postMatchMen")
 public ResponseEntity<String> postMatchMen(
		 @RequestParam("match") String matchJson,
		 @RequestParam(value = "image", required = false) MultipartFile image,
		 @RequestParam(value = "venue", required = false) String venue) {
	try {
	 JSONObject matchData = (JSONObject) new JSONParser().parse(matchJson);
	 GameModel match = new GameModel(matchData, stringValue(matchData.get("team")));
	 logger.info("Creating men matchday preview: team={}, date={}, competition={}", match.getTeam(), match.getSaveGameDate(), match.getCompetition());
	 BufferedImage userImage = null;
	 if (image != null && !image.isEmpty()) {
		 userImage = ImageIO.read(image.getInputStream());
	 }
	 String result = mc.createMatch(match, userImage, venue);
	 logger.info("Men matchday preview created at '{}'", result);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not create men matchday preview", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @GetMapping("/getTeamData")
 public ResponseEntity<JSONObject> getTeamData() {
	try {
	 JSONObject obj = (JSONObject) readJsonFile("teamInfo.json");
	 logger.debug("Loaded configuration for {} teams", obj.size());
	 return new ResponseEntity<>(obj, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not load teamInfo.json", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @PostMapping("/updateTeamInfo")
 public ResponseEntity<String> updateTeamInfo(@RequestBody Map<String, Map<String, String>> teamData) {
	logger.info("Updating configuration for {} teams", teamData != null ? teamData.size() : 0);
	if (!isValidTeamData(teamData)) {
	 logger.warn("Rejected invalid team configuration");
	 return new ResponseEntity<>("Invalid team data", HttpStatus.BAD_REQUEST);
	}
	try {
	 OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File("src/main/resources/templates/teamInfo.json"), teamData);
	 logger.info("Team configuration updated successfully");
	 return new ResponseEntity<>("Success", HttpStatus.OK);
	} catch (IOException e) {
	 logger.error("Could not update teamInfo.json", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 private boolean isValidTeamData(Map<String, Map<String, String>> teamData) {
	if (teamData == null || teamData.isEmpty()) {
	 return false;
	}
	for (Map.Entry<String, Map<String, String>> entry : teamData.entrySet()) {
	 Map<String, String> team = entry.getValue();
	 if (!entry.getKey().matches("[A-Za-z0-9_-]{1,10}") || team == null
					 || team.get("club-id") == null || team.get("club-id").isBlank()
					 || team.get("default-place") == null || team.get("default-place").isBlank()
					 || team.get("category") != null && !team.get("category").matches("men|youth")
					 || isPositiveNumberOrMissing(team.get("lastLeagueMatchday"))
					 || isPositiveNumberOrMissing(team.get("lastCupMatchday"))) {
		return false;
	 }
	}
	return true;
 }

 private boolean isPositiveNumberOrMissing(String value) {
	if (value == null) {
	 return false;
	}
	try {
	 return Long.parseLong(value) <= 0;
	} catch (NumberFormatException e) {
	 return true;
	}
 }

 @RequestMapping("/getAllTeams")
 public ResponseEntity<ArrayList<String>> getAllTeams() {
	try {
	 ArrayList<String> result = new ArrayList<>();
	 JSONObject obj = (JSONObject) readJsonFile("clubs.json");
	 for (Object key : obj.keySet()) {
		result.add(key.toString());
	 }
	 Collections.sort(result);
	 logger.debug("Loaded {} configured clubs", result.size());
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not load clubs.json", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @GetMapping("/getAllMenMatches")
 public ResponseEntity<JSONArray> getAllMenMatches() {
	try {
	 JSONArray arr = (JSONArray) readJsonFile("men-games.json");
	 logger.debug("Loaded {} pending men matches", arr.size());
	 return new ResponseEntity<>(arr, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not load men-games.json", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @RequestMapping("/deleteMatchEntry")
 public ResponseEntity<?> deleteYouthMatchEntry(@RequestParam("game") String match, @RequestParam("team") String team) {
	try {
	 logger.info("Deleting pending match from '{}-games.json'", team);
	 JSONObject m = Helper.parser(match);
	 Helper.deleteTempTxt(m, team + "-games");
	 logger.info("Pending match deleted from '{}-games.json'", team);
	 return new ResponseEntity<>(HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not delete pending match from '{}-games.json'", team, e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @GetMapping("/getMenMatchDetails")
 public ResponseEntity<JSONObject> getMenMatchDetails(@RequestParam("match") String match) {
	try {
	 JSONObject game = (JSONObject) new JSONParser().parse(match);
	 logger.debug("Loading Fussball.de details for game URL '{}'", game.get("gameUrl"));
	 return new ResponseEntity<>(rc.getMatchDetails(game), HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not load men match details", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @RequestMapping("/postMenMatchResult")
 public ResponseEntity<?> postMenMatchResult(@RequestParam("match") String match) {
	try {
	 logger.info("Creating men result package");
	 JSONParser jp = new JSONParser();
	 JSONObject result = rc.createResult((JSONObject) jp.parse(match));
	 logger.info("Men result package created at '{}'", result.get("fileDir"));
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not create men result package", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	} finally {
	 rc = new ResultCreator();
	}
 }

 @RequestMapping("/sendMenMatchPicture")
 public ResponseEntity<HttpStatus> sendMenMatchPicure(@RequestParam("coords") String coords, @RequestParam("file") MultipartFile file) {
	try {
	 logger.info("Processing men result image upload: size={} bytes, contentType={}", file.getSize(), file.getContentType());
	 JSONParser jp = new JSONParser();
	 rc.savePicture((JSONObject) jp.parse(coords), file);
	 logger.debug("Men result image upload processed successfully");
	 return new ResponseEntity<>(HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not process men result image upload", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "Anonymous TypeReference is standard Jackson pattern")
 @RequestMapping("/postNewTeam")
 public ResponseEntity<String> postNewTeam(@RequestParam("club") String club, @RequestParam("place") String place, @RequestParam("insta1") String instaAcc1, @RequestParam("insta2") String instaAcc2, @RequestParam("file") MultipartFile clubLogo) {
	try {
	 logger.info("Creating club '{}': place={}, logoSize={} bytes", club, place, clubLogo.getSize());
	 Map<String, Map<String, Object>> obj = OBJECT_MAPPER.readValue(new File("src/main/resources/templates/clubs.json"), new TypeReference<>() {
	 });
	 if (obj.containsKey(club)) {
		logger.warn("Cannot create club '{}' because it already exists", club);
		return new ResponseEntity<>("Object already found", HttpStatus.CONFLICT);
	 }
	 String logoName = club.replaceAll("\\W", "_");
	 Map<String, Object> newTeam = new HashMap<>();
	 newTeam.put("fileName", logoName);
	 newTeam.put("place", place);
	 newTeam.put("insta_acc", instaAcc1);
	 newTeam.put("insta_acc2", instaAcc2);
	 obj.put(club, newTeam);
	 BufferedImage img;
	 try (InputStream input = clubLogo.getInputStream()) {
		img = ImageIO.read(input);
	 }
	 if (img == null) {
		logger.warn("Rejected unsupported logo image for club '{}'", club);
		return new ResponseEntity<>("Invalid image", HttpStatus.BAD_REQUEST);
	 }
	 File saveLogo = new File("src/main/resources/pictures/teamlogos/" + logoName + ".png");
	 ImageIO.write(img, "png", saveLogo);
	 OBJECT_MAPPER.writeValue(new File("src/main/resources/templates/clubs.json"), obj);
	 logger.info("Club '{}' created with logo '{}'", club, saveLogo.getName());
	 return new ResponseEntity<>("Success", HttpStatus.OK);
	} catch (IOException e) {
	 logger.error("Could not create club '{}'", club, e);
	 return new ResponseEntity<>("Error", HttpStatus.BAD_GATEWAY);
	}
 }

 @RequestMapping("/postMatchFilesYouth")
 public ResponseEntity<?> postMatchFilesYouth(@RequestBody List<Map<String, Object>> matchList) {
	try {
	 ArrayList<GameModel> mmArr = new ArrayList<>();
	 for (Map<String, Object> m : matchList) {
		mmArr.add(new GameModel(new JSONObject(m), stringValue(m.get("team"))));
	 }
	 logger.info("Creating youth matchday package for {} matches", mmArr.size());
	 Map<String, Integer> result = msc.createMatches(mmArr);
	 logger.info("Youth matchday package created: {}", result);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not create youth matchday package for {} matches", matchList != null ? matchList.size() : 0, e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @RequestMapping("/getAllYouthMatches")
 public ResponseEntity<JSONArray> getAllYouthMatches() {
	try {
	 JSONArray arr = (JSONArray) readJsonFile("youth-games.json");
	 logger.debug("Loaded {} pending youth matches", arr.size());
	 return new ResponseEntity<>(arr, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not load youth-games.json", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @RequestMapping("/postYouthResults")
 public ResponseEntity<?> postYouthResult(@RequestBody List<Map<String, Object>> resultList) {
	try {
	 ArrayList<ResultModel> rmArr = new ArrayList<>();
	 for (Map<String, Object> m : resultList) {
		Object idValue = m.get("id");
		JSONObject id = idValue instanceof Map ? new JSONObject((Map<?, ?>) idValue) : new JSONObject();
		rmArr.add(new ResultModel(id,
				stringValue(m.get("result")),
				stringValue(m.get("homeStats")),
				stringValue(m.get("awayStats")),
				stringValue(m.get("text"))));
	 }
	 logger.info("Creating youth result package for {} games", rmArr.size());
	 Map<String, Integer> result = rsc.createResults(rmArr);
	 logger.info("Youth result package created: {}", result);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not create youth result package for {} games", resultList != null ? resultList.size() : 0, e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 private static String stringValue(Object value) {
	return value != null ? value.toString() : null;
 }

 private Object readJsonFile(String fileName) throws IOException, ParseException {
	Path path = Paths.get("src/main/resources/templates", fileName);
	try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
	 return new JSONParser().parse(reader);
	}
 }

 @GetMapping("/getFonts")
 public ResponseEntity<List<String>> getFonts() {
	List<String> fonts = FontRegistry.refreshAvailableFamilies();
	logger.debug("Loaded {} available font families", fonts.size());
	return new ResponseEntity<>(fonts, HttpStatus.OK);
 }

 @GetMapping(value = "/getFontPreview", produces = MediaType.IMAGE_PNG_VALUE)
 public ResponseEntity<byte[]> getFontPreview(@RequestParam("family") String family,
					@RequestParam(value = "text", required = false) String text,
					@RequestParam(value = "style", defaultValue = "plain") String fontStyle) {
	try {
	 String previewText = text != null && text.length() > 80 ? text.substring(0, 80) : text;
	 int style = switch (fontStyle) {
		case "bold" -> Font.BOLD;
		case "italic" -> Font.ITALIC;
		case "bold-italic" -> Font.BOLD | Font.ITALIC;
		default -> Font.PLAIN;
	 };
	 return new ResponseEntity<>(FontRegistry.createPreview(family, previewText, style), HttpStatus.OK);
	} catch (IOException e) {
	 logger.error("Could not render font preview: family={}, style={}", family, fontStyle, e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @GetMapping("/getCoordinates")
 public ResponseEntity<JSONObject> getCoordinates() {
	try {
	 JSONObject obj = (JSONObject) readJsonFile("coordinates.json");
	 Object blocks = obj.get("coordinates");
	 logger.debug("Loaded coordinate configuration with {} blocks", blocks instanceof Map<?, ?> map ? map.size() : 0);
	 return new ResponseEntity<>(obj, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 logger.error("Could not load coordinates.json", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 @PostMapping("/updateCoordinates")
 public ResponseEntity<String> updateCoordinates(@RequestBody Map<String, Object> coordinatesData) {
	logger.info("Updating coordinate configuration");
	if (!isValidCoordinateConfiguration(coordinatesData)) {
	 logger.warn("Rejected invalid coordinate configuration");
	 return new ResponseEntity<>("Invalid coordinate configuration", HttpStatus.BAD_REQUEST);
	}
	try {
	 OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File("src/main/resources/templates/coordinates.json"), coordinatesData);
	 Helper.reloadCoordinates();
	 Map<?, ?> blocks = (Map<?, ?>) coordinatesData.get("coordinates");
	 logger.info("Coordinate configuration updated with {} blocks", blocks.size());
	 return new ResponseEntity<>("Success", HttpStatus.OK);
	} catch (IOException e) {
	 logger.error("Could not update coordinate configuration", e);
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }

 private boolean isValidCoordinateConfiguration(Map<String, Object> configuration) {
	if (!(configuration.get("templates") instanceof Map<?, ?> templates) || templates.isEmpty()
					|| !(configuration.get("coordinates") instanceof Map<?, ?> blocks) || blocks.isEmpty()) {
	 return false;
	}
	for (Map.Entry<?, ?> entry : blocks.entrySet()) {
	 if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof Map<?, ?> block)
					 || !(block.get("label") instanceof String) || !(block.get("type") instanceof String type)
					 || !(block.get("templates") instanceof List<?>) || !(block.get("posX") instanceof Number)
					 || !(block.get("posY") instanceof Number) || !(block.get("sizeX") instanceof Number sizeX)
					 || !(block.get("sizeY") instanceof Number sizeY) || sizeX.intValue() <= 0 || sizeY.intValue() <= 0) {
		return false;
	 }
	 if ("text".equals(type) && (!(block.get("fontFamily") instanceof String)
					 || !(block.get("fontStyle") instanceof String) || !(block.get("fontSize") instanceof Number fontSize)
					 || fontSize.intValue() <= 0 || !(block.get("textColor") instanceof String))) {
		return false;
	 }
	 if ("image".equals(type) && block.get("opacity") instanceof Number opacity
					 && (opacity.doubleValue() < 0.0 || opacity.doubleValue() > 1.0)) {
		return false;
	 }
	}
	return true;
 }

 @GetMapping("/getBackgroundImage")
 public ResponseEntity<byte[]> getBackgroundImage(@RequestParam(value = "template", required = false) String template,
					@RequestParam(value = "type", required = false) String type) throws IOException {
	String selectedTemplate = template != null ? template : type;
	if (selectedTemplate == null) {
	 logger.warn("Background image request rejected because template/type is missing");
	 return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
	String imagePath = switch (selectedTemplate) {
	 case "men", "men-matchday" -> "src/main/resources/pictures/template/men/matchdayTemp.png";
	 case "men-result" -> "src/main/resources/pictures/template/men/ResultTemplate.jpg";
	 case "youth", "youth-matchday" -> "src/main/resources/pictures/template/youth/matchdayTemp.jpg";
	 case "youth-result" -> "src/main/resources/pictures/template/youth/resultTemp.jpg";
	 default -> null;
	};
	if (imagePath == null) {
	 logger.warn("Unknown background template requested: '{}'", selectedTemplate);
	 return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
	Path filePath = Paths.get(imagePath);
	if (Files.exists(filePath)) {
	 return new ResponseEntity<>(Files.readAllBytes(filePath), HttpStatus.OK);
	}
	return new ResponseEntity<>(HttpStatus.NOT_FOUND);
 }
}
