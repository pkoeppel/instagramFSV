package org.fsv.instagramuploader.men;

import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.model.ClubModel;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Component("kc")
public class KickoffCreator {
	public String createKickoff(String match, MultipartFile file, JSONObject c) throws IOException, ParseException {
		BufferedImage image = ImageIO.read(new File("src/main/resources/pictures/template/men/kickoffTemp.jpg"));
		Helper h = new Helper(image);
		JSONObject m = h.parser(match);
		
		String mType = m.get("matchType").toString();
		String mOpp = m.get("opponent").toString();
		String mDate = m.get("matchDate").toString();
		String date = DateTimeFormatter.ofPattern("yyyyMMdd").format(DateTimeFormatter.ofPattern("dd.MM.yyyy").parse(mDate));
		String headline = switch (mType) {
			case "Liga" -> "in der" + "\n" + "Vogtlandliga";
			case "Pokal" -> "im" + "\n" + "Vogtlandpokal";
			case "Testspiel" -> "Testspiel";
			default -> "";
		};
		h.writeOnPicture(headline, "headline2-men", FontClass.headMen1, Color.WHITE, 83);
		
		ClubSelector getClub = new ClubSelector();
		ClubModel ownClub = getClub.getClubDetails("FSV Treuen");
		ClubModel oppClub = getClub.getClubDetails(mOpp);
		
		//Logo, posX, posY, Höhe, Breite, null
		h.pictureOnPicture(oppClub.clubLogo(), "smallClub-men", 0);
		h.pictureOnPicture(ownClub.clubLogo(), "bigClub-men", 0);
		
		BufferedImage formattedImage = chanceFormat(c, ImageIO.read(file.getInputStream()));
		h.pictureOnPicture(formattedImage, "playerPic-men", 0);
		
		String saveName = getClub.getClubDetails(mOpp).saveClubName();
		
		String savePath = date + "_" + mType + "_" + saveName;
		h.savePicture("src/main/resources/save/" + savePath, image, "Kickoff");
		h.deleteTempTxt(m, "men-games-kickoff");
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
