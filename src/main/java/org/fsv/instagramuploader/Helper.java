package org.fsv.instagramuploader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

public class Helper {
	final Graphics g;
	final BufferedImage background;
	final int backX, backY;
	
	public Helper(BufferedImage image) {
		this.background = image;
		g = background.getGraphics();
		backX = background.getWidth();
		backY = background.getHeight();
	}
	
	public static int getC(JSONObject c, String val) {
		try {
			double coord = (double) c.get(val);
			return Double.valueOf(coord).intValue();
		}
		catch (ClassCastException e) {
			long coord = (long) c.get(val);
			return Long.valueOf(coord).intValue();
		}
	}

	public static String wrapString(String string, int charWrap) {
		int lastBreak = 0;
		int nextBreak = charWrap;
		if (string.length() > charWrap) {
			StringBuilder setString = new StringBuilder();
			do {
				while (string.charAt(nextBreak) != '/' && string.charAt(nextBreak) != '-' && string.charAt(nextBreak) != ' ' && nextBreak > lastBreak) {
					nextBreak--;
				}
				if (nextBreak == lastBreak) {
					nextBreak = lastBreak + charWrap;
				}
				setString.append(string.substring(lastBreak, nextBreak + 1).trim()).append("\n");
				lastBreak = nextBreak + 1;
				nextBreak += charWrap;
				
			} while (nextBreak < string.length());
			setString.append(string.substring(lastBreak).trim());
			return setString.toString();
		} else {
			return string;
		}
	}
	
	public void pictureOnPicture(BufferedImage image, String pos, int fac) {
		int sizeX = 0, sizeY = 0, posX = 0, posY = 0;
		
		switch (pos) {
			case "logo-left-youth" -> {
				sizeX = 142;
				sizeY = 142;
				posX = 280;
				posY = fac - 1;
			}
			case "logo-right-youth" -> {
				sizeX = 142;
				sizeY = 142;
				posX = 1092;
				posY = fac - 1;
			}
			case "logo-left-men" -> {
				sizeX = 345 - fac;
				sizeY = 345 - fac;
				posX = (backX / 2 - (345 - fac)) / 2;
				posY = 655 + fac / 2;
			}
			case "logo-right-men" -> {
				sizeX = 345 - fac;
				sizeY = 345 - fac;
				posX = (backX / 2 - (345 - fac)) / 2 + backX / 2;
				posY = 655 + fac / 2;
			}
			case "sponsor-men" -> {
				sizeX = 300;
				sizeY = 150;
				posX = backX - 300;
				posY = backY - 150;
			}
			case "bigClub-men" -> {
				sizeX = 400;
				sizeY = 400;
				posX = 4;
				posY = 715;
			}
			case "smallClub-men" -> {
				sizeX = 260;
				sizeY = 260;
				posX = 260;
				posY = 915;
			}
			case "bigClubResult-men" -> {
				sizeX = 180;
				sizeY = 180;
				posX = 5;
				posY = 5;
			}
			case "smallClubResult-men" -> {
				sizeX = 115;
				sizeY = 115;
				posX = 105;
				posY = 90;
			}
			case "playerPic-men" -> {
				sizeX = 560;
				sizeY = 560;
				posX = 500;
				posY = 720;
			}
		}
		g.drawImage(image, posX, posY, sizeX, sizeY, null);
	}
	
	public void writeOnPicture(String text, String pos, Font font, Color color, int yStart) {
		g.setColor(color);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int x = 0, y = 0;
		
		int splitCount = text.split("\n").length - 1;
		int textSize = fm.getFont().getSize() + 5;
		int textPos = splitCount * (textSize / -2);
		for (String line : text.split("\n")) {
			switch (pos) {
				//Matchday men
				case "headline2-men" -> {
					x = (backX - fm.stringWidth(line)) / 2;
					y = fm.getHeight();
					yStart = (yStart + fm.getHeight()) - 20;
				}
				case "homeclub-men" -> {
					x = (backX / 2 - fm.stringWidth(line)) / 2;
					yStart += fm.getHeight();
				}
				case "awayclub-men" -> {
					x = (backX / 2 - fm.stringWidth(line)) / 2 + backX / 2;
					yStart += fm.getHeight();
				}
				case "bottom-men" -> {
					x = (backX - fm.stringWidth(line)) / 2;
					y = fm.getHeight();
					yStart += fm.getHeight();
				}
				
				case "head" -> {
					x = (backX - fm.stringWidth(line)) / 2;
					y = 400;
				}
				case "team-name" -> {
					x = 26;
					y = textSize - 5;
				}
				case "match-type-short" -> {
					x = 26;
					y = textSize + 45;
				}
				case "matchType" -> {
					x = 757 - (fm.stringWidth(line) / 2);
					y = textSize + 50;
				}
				case "club-name-home" -> {
					x = 580 - (fm.stringWidth(line) / 2);
					y = 80 + textPos;
					textPos += textSize;
				}
				case "club-name-away" -> {
					x = 923 - (fm.stringWidth(line) / 2);
					y = 80 + textPos;
					textPos += textSize;
				}
				case "bottom-center" -> {
					x = 757 - (fm.stringWidth(line) / 2);
					y = 175 - textSize + textPos;
					textPos += textSize;
				}
				case "center-point" -> {
					x = 757 - (fm.stringWidth(line) / 2);
					y = 80 + textPos;
					textPos += textSize;
				}
			}
			g.drawString(line, x, y + yStart);
		}
		
	}
	
	public String createMatchdaysHead(List<LocalDate> md) {
		LocalDate lastThu = md.get(0);
		if (!lastThu.getDayOfWeek().equals(DayOfWeek.THURSDAY)){
			lastThu = lastThu.with(TemporalAdjusters.previous(DayOfWeek.THURSDAY));
		}
		LocalDate nextWed = md.get(md.size() - 1);
		if (!nextWed.getDayOfWeek().equals(DayOfWeek.WEDNESDAY)){
			nextWed = nextWed.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
		}
		String headDay;
		if (nextWed.getYear() == lastThu.getYear()) {
			headDay = lastThu.format(DateTimeFormatter.ofPattern("dd.MM.")) + " - " + nextWed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		} else {
			headDay = lastThu.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " - " + nextWed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		}
		writeOnPicture(headDay, "head", FontClass.headYouth1, Color.BLACK, 0);
		return lastThu.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	}
	
	public void deleteTempTxt(JSONObject delMatch, String file) throws IOException {
		JSONParser jp = new JSONParser();
		JSONArray ja = new JSONArray();
		JSONObject curObj;
		try {
			ja = (JSONArray) jp.parse(new FileReader("src/main/resources/templates/" + file + ".json"));
			for (int i = 0; i < ja.size(); i++) {
				curObj = (JSONObject) ja.get(i);
				if (curObj.get("game").equals(delMatch)) {
					ja.remove(i);
					break;
				}
			}
			ja.remove(delMatch);
		} catch (ParseException ignored) {
		}
		FileWriter fw = new FileWriter("src/main/resources/templates/" + file + ".json");
		fw.write(ja.toJSONString());
		fw.flush();
		fw.close();
	}
	
	@SuppressWarnings("unchecked")
	public void saveTempTxt(JSONArray ja, JSONObject gameDetails) {
		JSONObject game = new JSONObject();
		game.put("game", gameDetails);
		
		boolean exists = false;
		for (Object oldGame : ja) {
			if (oldGame.equals(game)) {
				exists = true;
				break;
			}
		}
		if (!exists) {
			ja.add(game);
		}
	}
	public JSONObject parser(String match) throws ParseException {
		JSONParser jp = new JSONParser();
		return (JSONObject) jp.parse(match);
	}
	
	public void savePicture(String pathURL, BufferedImage img, String fileName) throws IOException {
		File dir = new File(pathURL);
		if (!dir.exists()) {
			//noinspection ResultOfMethodCallIgnored
			dir.mkdir();
		}
		File fileToSafe = new File(dir + "/" + fileName + ".jpeg");
		ImageIO.write(img, "jpeg", fileToSafe);
	}
	
}
