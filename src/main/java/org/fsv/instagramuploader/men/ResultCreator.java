package org.fsv.instagramuploader.men;

import org.fsv.instagramuploader.ClubSelector;
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Component("rc")
public class ResultCreator {
 Logger logger = LoggerFactory.getLogger(ResultCreator.class);
 
 final ArrayList<BufferedImage> allImg = new ArrayList<>();
 BufferedImage targetImg;
 
 public JSONObject createResult(JSONObject match) throws IOException, ParseException, GeneralSecurityException {
	logger.info("Creating result ...");
	JSONObject m = Helper.parser(match.get("match").toString());
	String mType = m.get("matchType").toString();
	String mOpp = m.get("oppTeamName").toString();
	String mDate = m.get("matchDate").toString();
	String mHome = m.get("homeGame").toString();
	String oppName = m.get("opponent").toString();
	ClubSelector getClub = new ClubSelector();
	ClubModel ownClub = getClub.getClubDetails("FSV Treuen");
	ClubModel oppClub = getClub.getClubDetails(mOpp);
	logger.info("Build text ...");
	String matchResult;
	if (Boolean.parseBoolean(mHome)) {
	 matchResult = "FSV Treuen : " + oppName + " (" + match.get("result") + ")\n\n";
	} else {
	 matchResult = oppName + " : FSV Treuen (" + match.get("result") + ")\n\n";
	}
		/*
		String oppRep = "";
		if (!match.get("reporterOpp").equals("") && !match.get("reportOpp").equals("")){
			oppRep = match.get("reporterOpp") + " (" + oppName + "):\n" + match.get("reportOpp") + "\n";
		}
		
		String ownRep = "";
		if (!match.get("reporterOwn").equals("") && !match.get("reportOwn").equals("")){
			ownRep = match.get("reporterOwn") + " (FSV Treuen):\n" + match.get("reportOwn") + "\n";
		}
		
		String fullReport = matchResult + match.get("headline") + "\uD83D\uDD34⚪ \n\n \uD83D\uDCF8: @arminvogtland @netti_1909 \n\n"
										+ match.get("report") + "\n\nStimmen zum Spiel:\n" + oppRep + ownRep + "\n" +match.get("future");
			*/
	String fullReport = matchResult + match.get("headline") + "\uD83D\uDD34⚪ \n\n \uD83D\uDCF8: @arminvogtland @netti_1909 \n\n"
					+ match.get("report") + "\n\n" + match.get("future");
	String saveName = getClub.getClubDetails(mOpp).saveClubName();
	int imgCount = 0;
	String dir = mDate + "_" + mType + "_" + saveName;
	File directory = new File("src/main/resources/save/" + dir + "/Bilder");
	if (!directory.exists()) {
	 //noinspection ResultOfMethodCallIgnored
	 directory.mkdir();
	}
	String formatedDate = DateTimeFormatter.ofPattern("yyyyMMdd").format(DateTimeFormatter.ofPattern("yyyy-MM-dd").parse(mDate));
	logger.info("Add logos to pictures ...");
	GoogleDriveService googleService = new GoogleDriveService(saveName);
	for (BufferedImage img : allImg) {
	 Helper.pictureOnPicture(img, oppClub.clubLogo(), "smallClubResult-men", 0);
	 Helper.pictureOnPicture(img, ownClub.clubLogo(), "bigClubResult-men", 0);
	 File fileToSave = new File(directory + "/" + formatedDate + "_" + imgCount + ".jpeg");
	 ImageIO.write(img, "jpeg", fileToSave);
	 googleService.uploadFileToFolder(fileToSave);
	 imgCount++;
	}
	BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/save/" + dir + "/report.txt"));
	writer.write(fullReport);
	writer.close();
	logger.info("Save report finished (Path: +{})!", dir);
	
	Helper.deleteTempTxt(m, "men-games");
	Helper.updateMatchdayValue(mType);
	JSONObject result = new JSONObject();
	result.put("fileDir", dir);
	result.put("caption", fullReport);
	logger.info("Return report");
	return result;
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
