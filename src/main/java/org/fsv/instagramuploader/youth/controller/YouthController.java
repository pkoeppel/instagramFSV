package org.fsv.instagramuploader.youth.controller;

import org.fsv.instagramuploader.model.MatchModel;
import org.fsv.instagramuploader.model.ResultModel;
import org.fsv.instagramuploader.youth.pictureCreator.MatchdaysCreator;
import org.fsv.instagramuploader.youth.pictureCreator.ResultsCreator;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@RestController
public class YouthController {
	final MatchdaysCreator msc;
	final ResultsCreator rsc;
	
	public YouthController(MatchdaysCreator msc, ResultsCreator rsc) {
		this.msc = msc;
		this.rsc = rsc;
	}

	@SuppressWarnings("resource")
	@GetMapping(value = "/download/youth/{pathName}/{fileName:.+}", produces= MediaType.IMAGE_JPEG_VALUE)
	public @ResponseBody byte[] downloadLocalFile(@PathVariable String pathName, @PathVariable String fileName) throws IOException {
		InputStream is = new FileInputStream("src/main/resources/save/youth/" + pathName + "/" + fileName);
		return Objects.requireNonNull(is).readAllBytes();
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
