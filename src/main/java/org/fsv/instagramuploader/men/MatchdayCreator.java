package org.fsv.instagramuploader.men;

import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.MatchModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


@Component("mc")
public class MatchdayCreator {
	
	private static Helper h;
	private static FileWriter fw;
	private MatchModel match;
	
	private static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public String createMatch(MatchModel matchInfo) throws IOException, ParseException {
		match = matchInfo;
		//select template
		BufferedImage image = ImageIO.read(new File("src/main/resources/pictures/template/men/matchdayTemp.jpg"));
		h = new Helper(image);
		int headCase, blockHeight, sponsorCase;
		String matchCase = matchInfo.matchType();
		switch (matchCase) {
			case "leagueMatch":
				headCase = 1;
				blockHeight = 1045;
				sponsorCase = 1;
				break;
			case "cupMatch":
				headCase = 2;
				blockHeight = 1075;
				sponsorCase = 2;
				//Zeile muss seperat geschrieben werden, da sie einen andere Schriftgröße hat
				h.writeOnPicture("Sternquell Vogtlandpokal", "headline2-men", FontClass.headMen3, Color.WHITE, 345);
				break;
			case "friendMatch":
				headCase = 3;
				blockHeight = 1075;
				sponsorCase = 0;
				break;
			default:
				headCase = 0;
				blockHeight = 0;
				sponsorCase = 0;
				break;
		}
		buildHeadline(headCase);
		buildBlocks(blockHeight);
		buildSponsor(sponsorCase);
		writeTempTxt(matchInfo, "kickoff");
		writeTempTxt(matchInfo, "result");
		fw.close();
		ClubSelector getClub = new ClubSelector();
		String saveName = getClub.getClubDetails(match.opponent()).saveClubName();
		String savePath = match.getSaveMatchDate() + "_" + match.getMatchType() + "_" + saveName;
		h.savePicture("src/main/resources/save/" + savePath, image, "Matchday");
		return savePath;
	}
	
	private void buildSponsor(Integer i) throws IOException {
		BufferedImage sponsor = switch (i) {
			case 1 -> ImageIO.read(new File("src/main/resources/pictures/sponsor/Sparkasse_Vogtland.png"));
			case 2 -> ImageIO.read(new File("src/main/resources/pictures/sponsor/Sternquell.png"));
			default -> null;
		};
		h.pictureOnPicture(sponsor, "sponsor-men", 0);
	}
	
	private void buildHeadline(Integer opt) {
		String headline;
		switch (opt) {
			case 1:
				headline = "Spieltag " + match.matchDay() + "\n" + "Vogtlandliga";
				break;
			case 2:
				headline = match.matchDay();
				if (isNumeric(match.matchDay())) {
					headline += ". Pokalrunde";
				}
				break;
			case 3:
				headline = "Testspiel";
				break;
			default:
				headline = "";
				break;
		}
		h.writeOnPicture(headline, "headline2-men", FontClass.headMen2, Color.WHITE, 160);
	}
	
	private void buildBlocks(Integer startBox) throws IOException, ParseException {
		ClubSelector getClub = new ClubSelector();
		ClubModel ownClub = getClub.getClubDetails("FSV Treuen");
		ClubModel oppClub = getClub.getClubDetails(match.opponent());
		
		String homeTeam, guestTeam;
		String gamePlace = "Sportplatz ";
		
		String oppTeamName = match.opponent();
		if (!match.oppName().isEmpty()){
			oppTeamName = match.oppName();
		}
		
		if (Boolean.parseBoolean(match.homeGame())) {
			gamePlace += "Treuen";
			homeTeam = ownClub.clubName() + match.ownStats();
			guestTeam = oppTeamName + match.oppStats();
			h.pictureOnPicture(ownClub.clubLogo(), "logo-left-men", 0);
			h.pictureOnPicture(oppClub.clubLogo(), "logo-right-men", 60);
		} else {
			gamePlace += oppClub.clubPlace();
			homeTeam = oppTeamName + match.oppStats();
			guestTeam = ownClub.clubName() + match.ownStats();
			h.pictureOnPicture(ownClub.clubLogo(), "logo-right-men", 0);
			h.pictureOnPicture(oppClub.clubLogo(), "logo-left-men", 60);
		}
		
		h.writeOnPicture(homeTeam, "homeclub-men", FontClass.clubMen, Color.BLACK, startBox);
		h.writeOnPicture(guestTeam, "awayclub-men", FontClass.clubMen, Color.BLACK, startBox);
		
		String bottomBox = match.matchDate() + " | " + match.matchTime() + " Uhr" + "\n" + Helper.wrapString(gamePlace, 30);
		h.writeOnPicture(bottomBox, "bottom-men", FontClass.bottomMen, Color.BLACK, 1278);
	}
	
	@SuppressWarnings("unchecked")
	private void writeTempTxt(MatchModel m, String part) throws IOException {
		JSONParser jp = new JSONParser();
		JSONArray ja = new JSONArray();
		try {
			ja = (JSONArray) jp.parse(new FileReader("src/main/resources/templates/men-games-" + part + ".json"));
			
			String oppTeamName = m.opponent();
                	if (!m.oppName().isEmpty()){
                        	oppTeamName = m.oppName();
                	}
			
			JSONObject gameDetails = new JSONObject();
			gameDetails.put("matchType", m.getMatchType());
			gameDetails.put("opponent", m.opponent());
			gameDetails.put("matchDate", m.matchDate());
			gameDetails.put("homeGame",m.homeGame());
			gameDetails.put("oppTeamName",oppTeamName);
			h.saveTempTxt(ja, gameDetails);
		} catch (ParseException ignored) {
		}
		
		
		fw = new FileWriter("src/main/resources/templates/men-games-" + part + ".json");
		fw.write(ja.toJSONString());
		fw.flush();
	}
}
