package org.fsv.instagramuploader;

import jakarta.servlet.http.HttpServletResponse;
import org.fsv.instagramuploader.men.KickoffCreator;
import org.fsv.instagramuploader.men.MatchdayCreator;
import org.fsv.instagramuploader.men.ResultCreator;
import org.fsv.instagramuploader.model.MatchModel;
import org.fsv.instagramuploader.model.ResultModel;
import org.fsv.instagramuploader.youth.MatchdaysCreator;
import org.fsv.instagramuploader.youth.ResultsCreator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
	@SuppressWarnings("resource")
	@GetMapping(value = "/download/{pathName}/{fileName:.+}", produces= MediaType.IMAGE_JPEG_VALUE)
	public @ResponseBody byte[] downloadMenFile(@PathVariable String pathName, @PathVariable String fileName) throws IOException {
		InputStream is = new FileInputStream("src/main/resources/save/" + pathName + "/" + fileName);
		return Objects.requireNonNull(is).readAllBytes();
	}
	
	@SuppressWarnings("resource")
	@GetMapping(value = "/download/youth/{pathName}/{fileName:.+}", produces= MediaType.IMAGE_JPEG_VALUE)
	public @ResponseBody byte[] downloadYouthFile(@PathVariable String pathName, @PathVariable String fileName) throws IOException {
		InputStream is = new FileInputStream("src/main/resources/save/youth/" + pathName + "/" + fileName);
		return Objects.requireNonNull(is).readAllBytes();
	}
	
	@GetMapping(value = "/zip-download/{dir}", produces = "application/zip")
	public void zipDownload(@PathVariable String dir, HttpServletResponse res) throws IOException {
		File directory = new File("src/main/resources/save/" + dir + "/Bilder/");
		ZipOutputStream zipOS = new ZipOutputStream(res.getOutputStream());
		for (String fn : Objects.requireNonNull(directory.list())){
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
	
	@RequestMapping("/createKickoffMen")
	public ResponseEntity<String> createKickoffFile(@RequestParam("match") String match, @RequestParam("file") MultipartFile playerPic, @RequestParam("coords") String coords)
									throws IOException, ParseException {
		JSONParser jp = new JSONParser();
		String result = kc.createKickoff(match, playerPic, (JSONObject) jp.parse(coords));
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@RequestMapping("/createMatchMen")
	public ResponseEntity<String> createMatchFile(@RequestBody MatchModel match)
									throws IOException, ParseException {
		String result = mc.createMatch(match);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@RequestMapping("/getAllTeams")
	public ResponseEntity<ArrayList<String>> getAllTeams() throws IOException, ParseException {
		ArrayList<String> result = new ArrayList<>();
		JSONObject obj = (JSONObject) new JSONParser()
										.parse(new FileReader("src/main/resources/templates/clubs.json"));
		for (Object key : obj.keySet()) {
			result.add(key.toString());
		}
		Collections.sort(result);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequestMapping("/getYouthTeams")
	public ResponseEntity<JSONObject> getYouthTeams() throws IOException, ParseException {
		JSONObject obj = (JSONObject) new JSONParser().parse(new FileReader("src/main/resources/templates/youthTeams.json"));
		return new ResponseEntity<>(obj, HttpStatus.OK);
	}
	
	@RequestMapping("/getAllMenMatches")
	public ResponseEntity<JSONArray> getAllMenMatches(@RequestBody String target) throws IOException, ParseException {
		JSONArray arr = (JSONArray) new JSONParser()
										.parse(new FileReader("src/main/resources/templates/men-games-"+ target + ".json"));
		return new ResponseEntity<>(arr, HttpStatus.OK);
	}
	
	@RequestMapping("/sendMenMatchResult")
	public ResponseEntity<?> sendMenMatchResult(@RequestBody String match) throws ParseException, IOException {
		JSONParser jp = new JSONParser();
		JSONObject result = rc.addLogos((JSONObject) jp.parse(match));
		rc = new ResultCreator();
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@RequestMapping("/sendMenMatchPicture")
	public ResponseEntity<HttpStatus> sendMenMatchPicure(@RequestParam("coords") String coords, @RequestParam("file") MultipartFile file) throws ParseException, IOException {
		JSONParser jp = new JSONParser();
		rc.savePicture((JSONObject) jp.parse(coords), file);
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping("/addNewTeam")
	public ResponseEntity<String> addNewTeam(@RequestParam("club") String club, @RequestParam("place") String place, @RequestParam("insta1") String instaAcc1, @RequestParam("insta2") String instaAcc2, @RequestParam("file")MultipartFile clubLogo) throws IOException, ParseException {
		//save JSON-file
		JSONObject obj = (JSONObject) new JSONParser()
										.parse(new FileReader("src/main/resources/templates/clubs.json"));
		for (Object key : obj.keySet()) {
			if (key.equals("club")) {
				return new ResponseEntity<>("Object already found", HttpStatus.OK);
			}
		}
		JSONObject newTeam = new JSONObject();
		newTeam.put("fileName", club.replaceAll(" ","_"));
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
		File saveLogo = new File("src/main/resources/pictures/teamlogos/" + club.replaceAll(" ","_") + ".png");
		ImageIO.write(img, "png", saveLogo);
		return new ResponseEntity<>("Success", HttpStatus.OK);
	}
	@RequestMapping("/createMatchFilesYouth")
	public ResponseEntity<?> createMatchFilesYouth(@RequestBody ArrayList<MatchModel> mmArr) throws IOException, ParseException {
		Map<String, Integer> result = msc.createMatches(mmArr);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@RequestMapping("/getAllYouthMatches")
	public ResponseEntity<JSONArray> getAllYouthMatches() throws IOException, ParseException {
		JSONArray arr = (JSONArray) new JSONParser()
										.parse(new FileReader("src/main/resources/templates/youth-games.json"));
		return new ResponseEntity<>(arr, HttpStatus.OK);
	}
	
	@RequestMapping("/createYouthResults")
	public ResponseEntity<?> createYouthResult(@RequestBody ArrayList<ResultModel> rmArr) throws IOException, ParseException {
		Map<String, Integer> result = rsc.createResults(rmArr);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
