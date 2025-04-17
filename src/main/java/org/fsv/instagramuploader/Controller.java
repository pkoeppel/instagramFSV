package org.fsv.instagramuploader;

import jakarta.servlet.http.HttpServletResponse;
import org.fsv.instagramuploader.men.KickoffCreator;
import org.fsv.instagramuploader.men.MatchdayCreator;
import org.fsv.instagramuploader.men.ResultCreator;
import org.fsv.instagramuploader.model.GameModel;
import org.fsv.instagramuploader.model.ResultModel;
import org.fsv.instagramuploader.youth.MatchdaysCreator;
import org.fsv.instagramuploader.youth.ResultsCreator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class Controller {
 Logger logger = LoggerFactory.getLogger(Controller.class);
 
 MatchdaysCreator msc;
 ResultsCreator rsc;
 MatchdayCreator mc;
 KickoffCreator kc;
 ResultCreator rc;
 
 public Controller(MatchdaysCreator msc, ResultsCreator rsc, MatchdayCreator mc, KickoffCreator kc, ResultCreator rc) {
	this.msc = msc;
	this.rsc = rsc;
	this.mc = mc;
	this.kc = kc;
	this.rc = rc;
 }
 
 @GetMapping("/getMatches")
 public ResponseEntity<JSONObject> getMatches() {
	try {
	 JSONObject result = (JSONObject) new JSONParser().parse(new FileReader("src/main/resources/templates/allMatches.json"));
	 logger.info("Load matches from allMatches.json");
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @GetMapping("/getNextMatches")
 public ResponseEntity<JSONObject> getNextMatches() {
	try {
	 logger.info("Update allMatches.json from Fußball.de ...");
	 Helper.updateNextMatchesFromFBDE();
	 JSONObject result = (JSONObject) new JSONParser().parse(new FileReader("src/main/resources/templates/allMatches.json"));
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @SuppressWarnings("resource")
 @GetMapping(value = "/download/{pathName}/{fileName:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
 public @ResponseBody byte[] downloadMenFile(@PathVariable String pathName, @PathVariable String fileName) throws IOException {
	InputStream is = new FileInputStream("src/main/resources/save/" + pathName + "/" + fileName);
	return Objects.requireNonNull(is).readAllBytes();
 }
 
 @SuppressWarnings("resource")
 @GetMapping(value = "/download/youth/{pathName}/{fileName:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
 public @ResponseBody byte[] downloadYouthFile(@PathVariable String pathName, @PathVariable String fileName) throws IOException {
	InputStream is = new FileInputStream("src/main/resources/save/youth/" + pathName + "/" + fileName);
	return Objects.requireNonNull(is).readAllBytes();
 }
 
 @GetMapping(value = "/zip-download/{dir}", produces = "application/zip")
 public void zipDownload(@PathVariable String dir, HttpServletResponse res) throws IOException {
	logger.info("Zip Download started...");
	File directory = new File("src/main/resources/save/" + dir + "/Bilder/");
	ZipOutputStream zipOS = new ZipOutputStream(res.getOutputStream());
	for (String fn : Objects.requireNonNull(directory.list())) {
	 FileSystemResource fsRes = new FileSystemResource("src/main/resources/save/" + dir + "/Bilder/" + fn);
	 ZipEntry zip = new ZipEntry(Objects.requireNonNull(fsRes.getFilename()));
	 zip.setSize(fsRes.contentLength());
	 zipOS.putNextEntry(zip);
	 StreamUtils.copy(fsRes.getInputStream(), zipOS);
	 zipOS.closeEntry();
	}
	zipOS.finish();
	zipOS.close();
	res.setStatus(HttpServletResponse.SC_OK);
	res.addHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
					.filename("download.zip", StandardCharsets.UTF_8)
					.build()
					.toString());
 }
 
 @RequestMapping("/updateClub")
 public ResponseEntity<String> updateClub(@RequestParam("club") String currentClub, @RequestParam("newClubName") String newClub) {
	try {
	 logger.info("Update club started...");
	 JSONObject allClubs = (JSONObject) new JSONParser()
					 .parse(new FileReader("src/main/resources/templates/clubs.json"));
	 JSONObject clubData = (JSONObject) allClubs.get(currentClub);
	 File clubLogo = new File("src/main/resources/pictures/teamlogos/" + clubData.get("fileName").toString() + ".png");
	 String newClubLogoName = newClub.replaceAll("\\W", "_");
	 clubLogo.renameTo(new File("src/main/resources/pictures/teamlogos/" + newClubLogoName + ".png"));
	 clubData.remove("fileName");
	 clubData.put("fileName", newClubLogoName);
	 allClubs.remove(currentClub);
	 allClubs.put(newClub, clubData);
	 FileWriter fw = new FileWriter("src/main/resources/templates/clubs.json");
	 fw.write(allClubs.toJSONString());
	 fw.flush();
	 fw.close();
	 return new ResponseEntity<>(HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
 }
 
 @DeleteMapping("/deleteClub")
 public ResponseEntity<String> deleteClub(@RequestParam("club") String currentClub) {
	try {
	 logger.info("Delete club started...");
	 JSONObject allClubs = (JSONObject) new JSONParser()
					 .parse(new FileReader("src/main/resources/templates/clubs.json"));
	 JSONObject clubData = (JSONObject) allClubs.get(currentClub);
	 File clubLogo = new File("src/main/resources/pictures/teamlogos/" + clubData.get("fileName").toString() + ".png");
	 boolean deleteFile = clubLogo.delete();
	 allClubs.remove(currentClub);
	 boolean deleteEntry = allClubs.containsKey(currentClub);
	 if (deleteEntry && deleteFile) {
		FileWriter fw = new FileWriter("src/main/resources/templates/clubs.json");
		fw.write(allClubs.toJSONString());
		fw.flush();
		fw.close();
		return new ResponseEntity<>(HttpStatus.OK);
	 }
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	} catch (Exception e) {
	 return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/createKickoffMen")
 public ResponseEntity<String> createKickoffFile(@RequestParam("match") String match, @RequestParam("file") MultipartFile playerPic, @RequestParam("coords") String coords) {
	try {
	 logger.info("Create kickoff started...");
	 JSONParser jp = new JSONParser();
	 String result = kc.createKickoff(match, playerPic, (JSONObject) jp.parse(coords));
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/postMatchMen")
 public ResponseEntity<String> postMatchFile(@RequestParam("game") GameModel match) {
	try {
	 logger.info("Post match men started...");
	 String result = mc.createMatch(match);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @GetMapping("/getTeamIds")
 public ResponseEntity<JSONObject> getTeamIds() {
	try {
	 logger.info("Get team info ...");
	 JSONObject obj = (JSONObject) new JSONParser()
					 .parse(new FileReader("src/main/resources/templates/teamInfo.json"));
	 return new ResponseEntity<>(obj, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @GetMapping("/getSavedMatchdays")
 public ResponseEntity<JSONObject> getSavedMatchdays() {
	try {
	 logger.info("Get saved matchdays ...");
	 JSONObject obj = (JSONObject) new JSONParser()
					 .parse(new FileReader("src/main/resources/templates/data.json"));
	 return new ResponseEntity<>(obj, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/updateTeamInfo")
 public ResponseEntity<String> getTeamIds(@RequestParam("newData") String newData){
	try{
	 logger.info("Update team info ...");
	 FileWriter fw = new FileWriter("src/main/resources/templates/teamInfo.json");
	 fw.write(newData);
	 fw.flush();
	 fw.close();
	 return new ResponseEntity<>("Success", HttpStatus.OK);
	}catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/updateMatchdaysInfo")
 public ResponseEntity<String> updateMatchdaysInfo(@RequestParam("leagueMatchday") String leagueMatchday, @RequestParam("cupMatchday") String cupMatchday){
	try{
	 logger.info("Update matchdays info ...");
	 JSONObject jo = new JSONObject();
	 jo.put("lastLeagueMatchday", leagueMatchday);
	 jo.put("lastCupMatchday", cupMatchday);
	 FileWriter fw = new FileWriter("src/main/resources/templates/data.json");
	 fw.write(jo.toJSONString());
	 fw.flush();
	 fw.close();
	 return new ResponseEntity<>("Success", HttpStatus.OK);
	}catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/getAllTeams")
 public ResponseEntity<ArrayList<String>> getAllTeams() {
	try {
	 logger.info("Get all saved teams from clubs.json ...");
	 ArrayList<String> result = new ArrayList<>();
	 JSONObject obj = (JSONObject) new JSONParser()
					 .parse(new FileReader("src/main/resources/templates/clubs.json"));
	 for (Object key : obj.keySet()) {
		result.add(key.toString());
	 }
	 Collections.sort(result);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @GetMapping("/getAllMenMatches")
 public ResponseEntity<JSONArray> getAllMenMatches() {
	try {
	 logger.info("Get all men matches from men-games.json ...");
	 JSONArray arr = (JSONArray) new JSONParser()
					 .parse(new FileReader("src/main/resources/templates/men-games.json"));
	 return new ResponseEntity<>(arr, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/deleteMatchEntry")
 public ResponseEntity<?> deleteYouthMatchEntry(@RequestParam("game") String match, @RequestParam("team") String team) {
	try {
	 logger.info("Delete match entry ...");
	 JSONObject m = Helper.parser(match);
	 Helper.deleteTempTxt(m, team + "-games");
	 return new ResponseEntity<>(HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
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
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/sendMenMatchPicture")
 public ResponseEntity<HttpStatus> sendMenMatchPicure(@RequestParam("coords") String coords, @RequestParam("file") MultipartFile file) {
	try {
	 logger.info("Send men match picture ...");
	 JSONParser jp = new JSONParser();
	 rc.savePicture((JSONObject) jp.parse(coords), file);
	 return new ResponseEntity<>(HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
 }
 
 @RequestMapping("/postNewTeam")
 public ResponseEntity<String> postNewTeam(@RequestParam("club") String club, @RequestParam("place") String place, @RequestParam("insta1") String instaAcc1, @RequestParam("insta2") String instaAcc2, @RequestParam("file") MultipartFile clubLogo) {
	try {
	 logger.info("Post new team ...");
	 JSONObject obj = (JSONObject) new JSONParser()
					 .parse(new FileReader("src/main/resources/templates/clubs.json"));
	 for (Object key : obj.keySet()) {
		if (key.equals("club")) {
		 return new ResponseEntity<>("Object already found", HttpStatus.OK);
		}
	 }
	 JSONObject newTeam = new JSONObject();
	 newTeam.put("fileName", club.replaceAll(" ", "_"));
	 newTeam.put("place", place);
	 newTeam.put("insta_acc", instaAcc1);
	 newTeam.put("insta_acc2", instaAcc2);
	 obj.put(club, newTeam);
	 
	 FileWriter fw = new FileWriter("src/main/resources/templates/clubs.json");
	 fw.write(obj.toJSONString());
	 fw.flush();
	 fw.close();
	 //save picture
	 BufferedImage img = ImageIO.read(clubLogo.getInputStream());
	 File saveLogo = new File("src/main/resources/pictures/teamlogos/" + club.replaceAll(" ", "_") + ".png");
	 ImageIO.write(img, "png", saveLogo);
	 return new ResponseEntity<>("Success", HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);
	}
 }
 
 @RequestMapping("/postMatchFilesYouth")
 public ResponseEntity<?> postMatchFilesYouth(@RequestParam("allGames") ArrayList<GameModel> mmArr) {
	try {
	 logger.info("Post match files youth ...");
	 Map<String, Integer> result = msc.createMatches(mmArr);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/getAllYouthMatches")
 public ResponseEntity<JSONArray> getAllYouthMatches() {
	try {
	 logger.info("Get all youth matches from youth-games.json ...");
	 JSONArray arr = (JSONArray) new JSONParser()
					 .parse(new FileReader("src/main/resources/templates/youth-games.json"));
	 return new ResponseEntity<>(arr, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
 
 @RequestMapping("/postYouthResults")
 public ResponseEntity<?> postYouthResult(@RequestParam("allResults") ArrayList<ResultModel> rmArr) {
	try {
	 logger.info("Post youth results ...");
	 Map<String, Integer> result = rsc.createResults(rmArr);
	 return new ResponseEntity<>(result, HttpStatus.OK);
	} catch (Exception e) {
	 return new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY);
	}
 }
}
