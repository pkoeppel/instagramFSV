package org.fsv.instagramuploader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.fsv.instagramuploader.men.MatchdayCreator;
import org.fsv.instagramuploader.men.ResultCreator;
import org.fsv.instagramuploader.model.GameModel;
import org.fsv.instagramuploader.model.ResultModel;
import org.fsv.instagramuploader.youth.MatchdaysCreator;
import org.fsv.instagramuploader.youth.ResultsCreator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
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
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class Controller {
 Logger logger = LoggerFactory.getLogger(Controller.class);
 
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
	 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/allMatches.json"), StandardCharsets.UTF_8);
	 JSONObject result = (JSONObject) new JSONParser().parse(reader);
	 logger.info("Load matches from allMatches.json");
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @GetMapping("/getNextMatches")
 public ResponseEntity<JSONObject> getNextMatches() {
	try {
	 logger.info("Update allMatches.json from Fußball.de ...");
	 Helper.updateNextMatchesFromFBDE();
	 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/allMatches.json"), StandardCharsets.UTF_8);
	 JSONObject result = (JSONObject) new JSONParser().parse(reader);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException | URISyntaxException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @GetMapping(value = "/download/{pathName}/{fileName:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
 public @ResponseBody byte[] downloadMenFile(@PathVariable String pathName, @PathVariable String fileName) throws IOException {
	Path filePath = Paths.get("src/main/resources/save", pathName, fileName);
	return Files.readAllBytes(filePath);
 }
 
 @GetMapping(value = "/download/youth/{pathName}/{fileName:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
 public @ResponseBody byte[] downloadYouthFile(@PathVariable String pathName, @PathVariable String fileName) throws IOException {
	Path filePath = Paths.get("src/main/resources/save/youth/", pathName, fileName);
	return Files.readAllBytes(filePath);
 }
 
 @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Directory is known to exist or handled elsewhere")
 @GetMapping(value = "/zip-download/{dir}", produces = "application/zip")
 public void zipDownload(@PathVariable String dir, HttpServletResponse res) throws IOException {
	logger.info("Zip Download started...");
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
			
			StreamUtils.copy(fsRes.getInputStream(), zipOS);
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
	 logger.info("Update club started...");
	 Map<String, Map<String, Object>> allClubs = new ObjectMapper().readValue(new File("src/main/resources/templates/clubs.json"), new TypeReference<>() {
	 });
	 Map<String, Object> clubData = allClubs.get(currentClub);
	 File clubLogo = new File("src/main/resources/pictures/teamlogos/" + clubData.get("fileName").toString() + ".png");
	 String newClubLogoName = newClub.replaceAll("\\W", "_");
	 Files.move(clubLogo.toPath(), new File("src/main/resources/pictures/teamlogos/" + newClubLogoName + ".png").toPath(), StandardCopyOption.REPLACE_EXISTING);
	 clubData.remove("fileName");
	 clubData.put("fileName", newClubLogoName);
	 allClubs.remove(currentClub);
	 allClubs.put(newClub, clubData);
	 new ObjectMapper().writeValue(new File("src/main/resources/templates/clubs.json"), allClubs);
	 return new ResponseEntity<>(HttpStatus.OK);
	} catch (IOException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @DeleteMapping("/deleteClub")
 public ResponseEntity<String> deleteClub(@RequestParam("club") String currentClub) {
	try {
	 logger.info("Delete club started...");
	 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/clubs.json"), StandardCharsets.UTF_8);
	 JSONObject allClubs = (JSONObject) new JSONParser().parse(reader);
	 JSONObject clubData = (JSONObject) allClubs.get(currentClub);
	 File clubLogo = new File("src/main/resources/pictures/teamlogos/" + clubData.get("fileName").toString() + ".png");
	 boolean deleteFile = clubLogo.delete();
	 allClubs.remove(currentClub);
	 boolean deleteEntry = allClubs.containsKey(currentClub);
	 if (deleteEntry && deleteFile) {
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("src/main/resources/templates/clubs.json"), StandardCharsets.UTF_8);
		writer.write(allClubs.toJSONString());
		writer.close();
		return new ResponseEntity<>(HttpStatus.OK);
	 }
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/postMatchMen")
 public ResponseEntity<String> postMatchMen(@RequestBody GameModel match) {
	try {
	 logger.info("Post match men started...");
	 String result = mc.createMatch(match);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @GetMapping("/getTeamData")
 public ResponseEntity<JSONObject> getTeamData() {
	try {
	 logger.info("Get team info ...");
	 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/teamInfo.json"), StandardCharsets.UTF_8);
	 JSONObject obj = (JSONObject) new JSONParser().parse(reader);
	 return new ResponseEntity<>(obj, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/updateTeamInfo")
 public ResponseEntity<String> updateTeamInfo(@RequestParam("newData") String newData) {
	logger.info("Update team info ...");
	try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("src/main/resources/templates/teamInfo.json"), StandardCharsets.UTF_8)) {
	 writer.write(newData);
	 writer.close();
	 return new ResponseEntity<>("Success", HttpStatus.OK);
	} catch (IOException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/getAllTeams")
 public ResponseEntity<ArrayList<String>> getAllTeams() {
	try {
	 logger.info("Get all saved teams from clubs.json ...");
	 ArrayList<String> result = new ArrayList<>();
	 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/clubs.json"), StandardCharsets.UTF_8);
	 JSONObject obj = (JSONObject) new JSONParser().parse(reader);
	 for (Object key : obj.keySet()) {
		result.add(key.toString());
	 }
	 Collections.sort(result);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @GetMapping("/getAllMenMatches")
 public ResponseEntity<JSONArray> getAllMenMatches() {
	try {
	 logger.info("Get all men matches from men-games.json ...");
	 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/men-games.json"), StandardCharsets.UTF_8);
	 JSONArray arr = (JSONArray) new JSONParser().parse(reader);
	 return new ResponseEntity<>(arr, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/deleteMatchEntry")
 public ResponseEntity<?> deleteYouthMatchEntry(@RequestParam("game") String match, @RequestParam("team") String team) {
	try {
	 logger.info("Delete match entry ...");
	 JSONObject m = Helper.parser(match);
	 Helper.deleteTempTxt(m, team + "-games");
	 return new ResponseEntity<>(HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/postMenMatchResult")
 public ResponseEntity<?> postMenMatchResult(@RequestParam("match") String match) {
	try {
	 logger.info("Post men match result ...");
	 JSONParser jp = new JSONParser();
	 JSONObject result = rc.createResult((JSONObject) jp.parse(match));
	 rc = new ResultCreator();
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/sendMenMatchPicture")
 public ResponseEntity<HttpStatus> sendMenMatchPicure(@RequestParam("coords") String coords, @RequestParam("file") MultipartFile file) {
	try {
	 logger.info("Send men match picture ...");
	 JSONParser jp = new JSONParser();
	 rc.savePicture((JSONObject) jp.parse(coords), file);
	 return new ResponseEntity<>(HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "Anonymous TypeReference is standard Jackson pattern")
 @RequestMapping("/postNewTeam")
 public ResponseEntity<String> postNewTeam(@RequestParam("club") String club, @RequestParam("place") String place, @RequestParam("insta1") String instaAcc1, @RequestParam("insta2") String instaAcc2, @RequestParam("file") MultipartFile clubLogo) {
	try {
	 logger.info("Post new team ...");
	 Map<String, Map<String, Object>> obj = new ObjectMapper().readValue(new File("src/main/resources/templates/clubs.json"), new TypeReference<>() {
	 });
	 for (Object key : obj.keySet()) {
		if (key.equals("club")) {
		 return new ResponseEntity<>("Object already found", HttpStatus.OK);
		}
	 }
	 Map<String, Object> newTeam = new HashMap<>();
	 newTeam.put("fileName", club.replaceAll(" ", "_"));
	 newTeam.put("place", place);
	 newTeam.put("insta_acc", instaAcc1);
	 newTeam.put("insta_acc2", instaAcc2);
	 obj.put(club, newTeam);
	 new ObjectMapper().writeValue(new File("src/main/resources/templates/clubs.json"), obj);
	 //save picture
	 BufferedImage img = ImageIO.read(clubLogo.getInputStream());
	 File saveLogo = new File("src/main/resources/pictures/teamlogos/" + club.replaceAll(" ", "_") + ".png");
	 ImageIO.write(img, "png", saveLogo);
	 return new ResponseEntity<>("Success", HttpStatus.OK);
	} catch (IOException e) {
	 return new ResponseEntity<>("Error", HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/postMatchFilesYouth")
 public ResponseEntity<?> postMatchFilesYouth(@RequestBody ArrayList<GameModel> mmArr) {
	try {
	 logger.info("Post match files youth ...");
	 Map<String, Integer> result = msc.createMatches(mmArr);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/getAllYouthMatches")
 public ResponseEntity<JSONArray> getAllYouthMatches() {
	try {
	 logger.info("Get all youth matches from youth-games.json ...");
	 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/youth-games.json"), StandardCharsets.UTF_8);
	 JSONArray arr = (JSONArray) new JSONParser().parse(reader);
	 return new ResponseEntity<>(arr, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/postYouthResults")
 public ResponseEntity<?> postYouthResult(@RequestBody ArrayList<ResultModel> rmArr) {
	try {
	 logger.info("Post youth results ...");
	 Map<String, Integer> result = rsc.createResults(rmArr);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (IOException | ParseException e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
}
