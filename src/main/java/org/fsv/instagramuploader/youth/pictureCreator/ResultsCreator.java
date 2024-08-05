package org.fsv.instagramuploader.youth.pictureCreator;

import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.ResultModel;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

@Component("rsc")
public class ResultsCreator {
	private List<LocalDate> matchDates;
	
	public Map<String, Integer> createResults(ArrayList<ResultModel> rmArr) throws IOException, ParseException {
		BufferedImage background = ImageIO.read(new File("src/main/resources/pictures/template/youth/resultTemp.jpg"));
		matchDates = new ArrayList<>();
		Helper h = new Helper(background);
		int blockStart = 500;
		int pageCount = 1;
		for (ResultModel rm : rmArr) {
			if (blockStart > 1100) {
				blockStart = 500;
				String savePathPart = h.createMatchdaysHead(matchDates);
				h.savePicture("src/main/resources/save/youth/" + savePathPart, background, "Result" + pageCount);
				background = ImageIO.read(new File("src/main/resources/pictures/template/youth/resultTemp.jpg"));
				h = new Helper(background);
				pageCount++;
			}
			
			Graphics g = background.getGraphics();
			g.setColor(Color.GRAY);
			int[] polyX = {0, 275, 250, 0};
			int[] polyY = {blockStart, blockStart, blockStart + 100, blockStart + 100};
			g.fillPolygon(polyX, polyY, polyY.length);
			
			h.writeOnPicture(rm.getValue("team"), "team-name", FontClass.teamYouth, Color.BLACK, blockStart);
			
			h.writeOnPicture("(" + rm.getMatchType() + ")", "match-type-short", FontClass.simpleYouth, Color.BLACK, blockStart);
			
			checkMatchDate(rm.getValue("date"));
			
			ClubSelector getClub = new ClubSelector();
			ClubModel ownClub = getClub.getClubDetails("SpG Treuener Land");
			ClubModel oppClub = getClub.getClubDetails(rm.getValue("opponent"));
			if (rm.text().equals("Abgesagt")) {
				h.pictureOnPicture(ownClub.clubLogo(), "logo-left-youth", blockStart);
				h.pictureOnPicture(oppClub.clubLogo(), "logo-right-youth", blockStart);
				h.writeOnPicture("Abgesagt!", "center-point", FontClass.clubOwnYouth, Color.BLACK, blockStart);
			} else {
				if (rm.getValue("matchType").equals("Kinderfest")) {
					h.pictureOnPicture(ownClub.clubLogo(), "logo-left-youth", blockStart);
					h.writeOnPicture("Kinderfest!", "center-point", FontClass.clubOwnYouth, Color.BLACK, blockStart);
				} else {
					String ownTeamText = "SpG Treuener Land";
					String oppTeamText = Helper.wrapString(rm.getValue("oppName"), 23);
					if (rm.getValue("matchType").equals("Liga")) {
						ownTeamText += "\n" + rm.ownStats();
						oppTeamText += "\n" + rm.oppStats();
					}
					if (Boolean.parseBoolean(rm.getValue("homeGame"))) {
						h.writeOnPicture(ownTeamText, "club-name-home", FontClass.simpleYouth, Color.BLACK, blockStart);
						h.writeOnPicture(oppTeamText, "club-name-away", FontClass.simpleYouth, Color.BLACK, blockStart);
						h.pictureOnPicture(ownClub.clubLogo(), "logo-left-youth", blockStart);
						h.pictureOnPicture(oppClub.clubLogo(), "logo-right-youth", blockStart);
					} else {
						h.writeOnPicture(ownTeamText, "club-name-away", FontClass.simpleYouth, Color.BLACK, blockStart);
						h.writeOnPicture(oppTeamText, "club-name-home", FontClass.simpleYouth, Color.BLACK, blockStart);
						h.pictureOnPicture(oppClub.clubLogo(), "logo-left-youth", blockStart);
						h.pictureOnPicture(ownClub.clubLogo(), "logo-right-youth", blockStart);
					}
					h.writeOnPicture(rm.result(), "center-point", FontClass.resultYouth, Color.BLACK, blockStart);
				}
			}
			blockStart += 200;
			h.deleteTempTxt(rm.id(), "youth-games");
		}
		String savePathPart = h.createMatchdaysHead(matchDates);
		h.savePicture("src/main/resources/save/youth/" + savePathPart, background, "Result" + pageCount);
		
		Map<String, Integer> result = new HashMap<>();
		result.put(savePathPart, pageCount);
		
		return result;
	}
	
	private void checkMatchDate(String date) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate formattedDate = LocalDate.parse(date, dtf);
		if (!matchDates.contains(formattedDate)) {
			matchDates.add(formattedDate);
			Collections.sort(matchDates);
		}
	}
}
