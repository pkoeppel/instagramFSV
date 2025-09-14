package org.fsv.instagramuploader.men;

import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.Controller;
import org.fsv.instagramuploader.FontClass;
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
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Component("kc")
public class KickoffCreator {
 Logger logger = LoggerFactory.getLogger(KickoffCreator.class);
 
 public String createKickoff(String match, MultipartFile file, JSONObject c) throws IOException, ParseException {
	logger.info("Creating Kickoff (Background = kickoffTemp.jpg)");
	BufferedImage background = ImageIO.read(new File("src/main/resources/pictures/template/men/kickoffTemp.jpg"));
	JSONObject m = Helper.parser(match);
	
	String competition = m.get("competition").toString();
	String opponent = m.get("homeTeam").toString();
	if (opponent.equals("FSV Treuen")) {
	 opponent = m.get("awayTeam").toString();
	}
	String mDate = m.get("gameDate").toString();
	String headline = "Testspiel";
	if (competition.contains("liga")) {
	 headline = "in der" + "\n" + "Vogtlandliga";
	} else if (competition.contains("pokal")) {
	 headline = "im" + "\n" + "Vogtlandpokal";
	}
	
	Helper.writeOnPicture(background, headline, "headline2-men", FontClass.headMen1, Color.WHITE, 83);
	
	ClubSelector getClub = new ClubSelector();
	ClubModel ownClub = getClub.getClubDetails("FSV Treuen");
	ClubModel oppClub = getClub.getClubDetails(opponent);
	
	//Logo, posX, posY, Höhe, Breite, null
	Helper.pictureOnPicture(background, oppClub.clubLogo(), "smallClub-men", 0);
	Helper.pictureOnPicture(background, ownClub.clubLogo(), "bigClub-men", 0);
	
	BufferedImage formattedImage = chanceFormat(c, ImageIO.read(file.getInputStream()));
	Helper.pictureOnPicture(background, formattedImage, "playerPic-men", 0);
	logger.info("Picture finished!");
	String saveName = getClub.getClubDetails(opponent).saveClubName();
	
	String savePath = mDate + "_" + competition + "_" + saveName;
	Helper.savePicture(background, "src/main/resources/save/" + savePath, "Kickoff");
	logger.info("Save picture finished (Path: +" + savePath + ")!");
	return savePath;
 }
 
 private BufferedImage chanceFormat(JSONObject c, BufferedImage image) {
	BufferedImage targetImg = new BufferedImage(670, 670, BufferedImage.TYPE_INT_ARGB);
	BufferedImage subImg = image.getSubimage(Helper.getC(c, "x"), Helper.getC(c, "y"), Helper.getC(c, "w"), Helper.getC(c, "h"));
	Graphics2D g2 = targetImg.createGraphics();
	RenderingHints qualityHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	qualityHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	g2.setRenderingHints(qualityHints);
	g2.setClip(new RoundRectangle2D.Double(0, 0, 670, 670, 670, 670));
	g2.drawImage(subImg, 0, 0, 670, 670, null);
	g2.dispose();
	return targetImg;
 }
 
}
