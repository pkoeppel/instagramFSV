package org.fsv.instagramuploader.men.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.fsv.instagramuploader.men.pictureCreator.KickoffCreator;
import org.fsv.instagramuploader.men.pictureCreator.MatchdayCreator;
import org.fsv.instagramuploader.men.pictureCreator.ResultCreator;
import org.fsv.instagramuploader.model.MatchModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class MenController {
	
	final
	MatchdayCreator mc;
	final
	KickoffCreator kc;
	ResultCreator rc;
	
	public MenController(MatchdayCreator mc, KickoffCreator kc, ResultCreator rc) {
		this.mc = mc;
		this.kc = kc;
		this.rc = rc;
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

	@SuppressWarnings("resource")
	@GetMapping(value = "/download/{pathName}/{fileName:.+}", produces= MediaType.IMAGE_JPEG_VALUE)
	public @ResponseBody byte[] downloadLocalFile(@PathVariable String pathName, @PathVariable String fileName) throws IOException {
		InputStream is = new FileInputStream("src/main/resources/save/" + pathName + "/" + fileName);
		return Objects.requireNonNull(is).readAllBytes();
	}
	
	@RequestMapping("/createMatchMen")
	public ResponseEntity<String> createMatchFile(@RequestBody MatchModel match)
									throws IOException, ParseException {
		String result = mc.createMatch(match);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@RequestMapping("/createKickoffMen")
	public ResponseEntity<String> createKickoffFile(@RequestParam("match") String match, @RequestParam("file") MultipartFile playerPic, @RequestParam("coords") String coords)
									throws IOException, ParseException {
		JSONParser jp = new JSONParser();
		String result = kc.createKickoff(match, playerPic, (JSONObject) jp.parse(coords));
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
}
