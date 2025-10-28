package org.fsv.instagramuploader.men;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.GoogleDriveService;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.model.ClubModel;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component("rc")
public class ResultCreator {
 Logger logger = LoggerFactory.getLogger(ResultCreator.class);
 
 final ArrayList<BufferedImage> allImg = new ArrayList<>();
 BufferedImage targetImg;
 
 public JSONObject createResult(JSONObject match) throws IOException, ParseException {
	logger.info("Creating result ...");
	JSONObject m = Helper.parser(match.get("match").toString());
	String team = m.get("team").toString();
	ClubModel homeClub = new ClubModel((JSONObject) m.get("homeClub"));
	ClubModel awayClub = new ClubModel((JSONObject) m.get("awayClub"));
	String mDate = m.get("matchDate").toString();
	String savePath = m.get("savePath").toString();
	String competition = m.get("competition").toString();
	boolean homeGame = homeClub.getClubName().equals("FSV Treuen");
	logger.info("Build text ...");
	String homeClubName = (homeClub.getChangedName() != null) ? homeClub.getChangedName() : homeClub.getClubName();
	String awayClubName = (awayClub.getChangedName() != null) ? awayClub.getChangedName() : awayClub.getClubName();
	String matchResult = homeClubName + " : " + awayClubName + " (" + match.get("result") + ")\n\n";
	
	String fullReport = matchResult + match.get("headline") + "\uD83D\uDD34⚪ \n\n \uD83D\uDCF8: @arminvogtland @netti_1909 \n\n"
					+ match.get("report") + "\n\n" + match.get("future");
	
	ArrayNode homeScorer = (ArrayNode) new ObjectMapper().readTree(match.get("scorerHome").toString());
	StringBuilder homeScore = new StringBuilder();
	for (JsonNode scoreNode : homeScorer) {
	 scoreNode.fields().forEachRemaining(entry -> {
		String minute = String.format("%02d", Integer.parseInt(entry.getKey()));
		homeScore.append(entry.getValue().asText()).append(" ").append(minute).append("'\n");
	 });
	}
	ArrayNode awayScorer = (ArrayNode) new ObjectMapper().readTree(match.get("scorerAway").toString());
	StringBuilder awayScore = new StringBuilder();
	for (JsonNode scoreNode : awayScorer) {
	 scoreNode.fields().forEachRemaining(entry -> {
		String minute = String.format("%02d", Integer.parseInt(entry.getKey()));
		awayScore.append(minute).append("' ").append(entry.getValue().asText()).append("\n");
	 });
	}
	int imgCount = 0;
	File directory = new File("src/main/resources/save/" + savePath + "/Bilder");
	if (!directory.exists()) {
	 if (!directory.mkdirs()) {
		logger.error("Error creating directory: {}", directory.getAbsolutePath());
	 }
	}
	String formatedDate = DateTimeFormatter.ofPattern("yyyyMMdd").format(DateTimeFormatter.ofPattern("yyyy-MM-dd").parse(mDate));
	logger.info("Add logos to pictures ...");
	BufferedImage opponentClubLogo, ownClubLogo;
	String folderName;
	if (!homeGame) {
	 opponentClubLogo = ImageIO.read(new File(homeClub.getClubLogoDir()));
	 ownClubLogo = ImageIO.read(new File(awayClub.getClubLogoDir()));
	 folderName = homeClub.getSaveName();
	} else {
	 ownClubLogo = ImageIO.read(new File(homeClub.getClubLogoDir()));
	 opponentClubLogo = ImageIO.read(new File(awayClub.getClubLogoDir()));
	 folderName = awayClub.getSaveName();
	}
	GoogleDriveService googleService = new GoogleDriveService(folderName);
	if (!allImg.isEmpty()) {
	 for (BufferedImage img : allImg) {
		Helper.pictureOnPicture(img, opponentClubLogo, "smallClubResult-men", 0);
		Helper.pictureOnPicture(img, ownClubLogo, "bigClubResult-men", 0);
		File fileToSave = new File(directory + "/" + formatedDate + "_" + imgCount + ".jpeg");
		ImageIO.write(img, "jpeg", fileToSave);
	 googleService.uploadFileToFolder(fileToSave);
		imgCount++;
	 }
	 BufferedImage firstImg = allImg.get(0);
	 BufferedImage template = ImageIO.read(new File("src/main/resources/pictures/template/men/ResultTemplate.jpg"));
	 Helper.pictureOnPicture(firstImg, template, "template", 0);
	 
	 Helper.pictureOnPicture(firstImg, ImageIO.read(new File(homeClub.getClubLogoDir())), "homeClubResult-men", Helper.isOwnClub(homeClub));
	 Helper.pictureOnPicture(firstImg, ImageIO.read(new File(awayClub.getClubLogoDir())), "awayClubResult-men", Helper.isOwnClub(awayClub));
	 String result = match.get("result").toString();
	 String[] resultSplit = result.split(":");
	 int resultShift;
	 if (resultSplit[0].length() == resultSplit[1].length()	) {
		resultShift = 0;
	 } else if (resultSplit[0].length() > resultSplit[1].length()) {
		resultShift = -20;
	 } else {
		resultShift = 20;
	 }
	 Helper.writeOnPicture(firstImg, result, "result", FontClass.resultMen, Color.BLACK, resultShift);
	 Helper.writeOnPicture(firstImg, Helper.wrapString(homeClubName, 23), "homeClubResult-men", FontClass.clubMenResult, Color.BLACK, 0);
	 Helper.writeOnPicture(firstImg, Helper.wrapString(awayClubName, 23), "awayClubResult-men", FontClass.clubMenResult, Color.BLACK, 0);
	 Helper.writeOnPicture(firstImg, homeScore.toString(), "homeScorer", FontClass.scorer, Color.WHITE, 0);
	 Helper.writeOnPicture(firstImg, awayScore.toString(), "awayScorer", FontClass.scorer, Color.WHITE, 0);
	 File fileToSave = new File(directory + "/" + formatedDate + "_0.jpeg");
	 ImageIO.write(firstImg, "jpeg", fileToSave);
	 
	}
	try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("src/main/resources/save/" + savePath + "/report.txt"), StandardCharsets.UTF_8)) {
	 writer.write(fullReport);
	}
	logger.info("Save report finished (Path: +{})!", savePath);
	
	Helper.deleteTempTxt(m, "men-games");
	Helper.updateMatchdayValue(team, competition);
	Map<String, String> result = new HashMap<>();
	result.put("fileDir", savePath);
	result.put("caption", fullReport);
	logger.info("Return report");
	return new JSONObject(result);
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
