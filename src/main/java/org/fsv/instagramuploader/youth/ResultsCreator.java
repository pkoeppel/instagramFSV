package org.fsv.instagramuploader.youth;

import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.men.MatchdayCreator;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.ResultModel;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Component("rsc")
public class ResultsCreator {
 Logger logger = LoggerFactory.getLogger(ResultsCreator.class);
 private List<LocalDate> matchDates;
 
 public Map<String, Integer> createResults(ArrayList<ResultModel> rmArr) throws IOException, ParseException {
	logger.info("Creating results for " + rmArr.size() + " games of youth");
	BufferedImage background = ImageIO.read(new File("src/main/resources/pictures/template/youth/resultTemp.jpg"));
	matchDates = new ArrayList<>();
	int blockStart = 500;
	int pageCount = 1;
	for (ResultModel rm : rmArr) {
	 if (blockStart > 1100) {
		blockStart = 500;
		String savePathPart = Helper.createMatchdaysHead(background, matchDates);
		Helper.savePicture(background, "src/main/resources/save/youth/" + savePathPart, "Result" + pageCount);
		background = ImageIO.read(new File("src/main/resources/pictures/template/youth/resultTemp.jpg"));
		pageCount++;
	 }
	 
	 Graphics g = background.getGraphics();
	 g.setColor(Color.GRAY);
	 int[] polyX = {0, 275, 250, 0};
	 int[] polyY = {blockStart, blockStart, blockStart + 100, blockStart + 100};
	 g.fillPolygon(polyX, polyY, polyY.length);
	 
	 Helper.writeOnPicture(background, rm.getValue("youth") + "-Jugend", "team-name", FontClass.teamYouth, Color.BLACK, blockStart);
	 
	 Helper.writeOnPicture(background, rm.getValue("matchType"), "match-type-short", FontClass.simpleYouth, Color.BLACK, blockStart);
	 
	 checkMatchDate(rm.getValue("date"));
	 String oppName = rm.getValue("oppName");
	 if (oppName.toLowerCase().contains("spg treuener land")) {
		oppName = "SpG Treuener Land";
	 }
	 ClubSelector getClub = new ClubSelector();
	 ClubModel ownClub = getClub.getClubDetails("SpG Treuener Land");
	 ClubModel oppClub = getClub.getClubDetails(oppName);
	 if (rm.text().equals("Abgesagt")) {
		logger.info("Game cancel!");
		Helper.pictureOnPicture(background, ownClub.clubLogo(), "logo-left-youth", blockStart);
		Helper.pictureOnPicture(background, oppClub.clubLogo(), "logo-right-youth", blockStart);
		Helper.writeOnPicture(background, "Abgesagt!", "center-point", FontClass.clubOwnYouth, Color.BLACK, blockStart);
	 } else {
		if (rm.getValue("matchType").toLowerCase().contains("kinder")) {
		 logger.info("Game is Kinderfest!");
		 Helper.pictureOnPicture(background, ownClub.clubLogo(), "logo-left-youth", blockStart);
		 Helper.writeOnPicture(background, "Kinderfest!", "center-point", FontClass.clubOwnYouth, Color.BLACK, blockStart);
		} else {
		 logger.info("Game normal!");
		 String homeTeamText = Helper.wrapString(rm.getValue("homeTeam"), 23);
		 String awayTeamText = Helper.wrapString(rm.getValue("awayTeam"), 23);
		 if (rm.getValue("matchType").toLowerCase().contains("liga") || rm.getValue("matchType").toLowerCase().contains("klasse")) {
			homeTeamText += "\n" + rm.homeStats();
			awayTeamText += "\n" + rm.awayStats();
		 }
		 Helper.writeOnPicture(background, homeTeamText, "club-name-home", FontClass.simpleYouth, Color.BLACK, blockStart);
		 Helper.writeOnPicture(background, awayTeamText, "club-name-away", FontClass.simpleYouth, Color.BLACK, blockStart);
		 if (Boolean.parseBoolean(rm.getValue("homeGame"))) {
			Helper.pictureOnPicture(background, ownClub.clubLogo(), "logo-left-youth", blockStart);
			Helper.pictureOnPicture(background, oppClub.clubLogo(), "logo-right-youth", blockStart);
		 } else {
			Helper.pictureOnPicture(background, oppClub.clubLogo(), "logo-left-youth", blockStart);
			Helper.pictureOnPicture(background, ownClub.clubLogo(), "logo-right-youth", blockStart);
		 }
		 Helper.writeOnPicture(background, rm.result(), "center-point", FontClass.resultYouth, Color.BLACK, blockStart);
		}
	 }
	 blockStart += 200;
	 Helper.deleteTempTxt(rm.id(), "youth-games");
	}
	String savePathPart = Helper.createMatchdaysHead(background, matchDates);
	Helper.savePicture(background, "src/main/resources/save/youth/" + savePathPart, "Result" + pageCount);
	
	Map<String, Integer> result = new HashMap<>();
	result.put(savePathPart, pageCount);
	logger.info("Return report");
	return result;
 }
 
 private void checkMatchDate(String date) {
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	LocalDate formattedDate = LocalDate.parse(date, dtf);
	if (!matchDates.contains(formattedDate)) {
	 matchDates.add(formattedDate);
	 Collections.sort(matchDates);
	}
 }
}
