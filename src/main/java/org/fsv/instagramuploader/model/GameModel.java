package org.fsv.instagramuploader.model;

import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class GameModel {
  private final String gameId;
  @Getter
 private final String competition;
 private final LocalDate gameDate;
 @Getter
 private final String gameTime;
 @Getter
 @Setter
 private String matchDay;
 private ClubModel homeTeam;
 private ClubModel awayTeam;
 @Getter
 private final String team;
 @Setter
 @Getter
 private String gameUrl;
 
 public GameModel(String competition, LocalDate gameDate, String gameTime, String team) {
	this.competition = competition;
	this.gameDate = gameDate;
	this.gameTime = gameTime;
	this.team = team;
  this.gameId = (String) game.get("gameId");
 }
  
  public ClubModel getHomeTeam() {
	return new ClubModel(homeTeam);
 }
 
 public void setHomeTeam(ClubModel homeTeam) {
	this.homeTeam = new ClubModel(homeTeam);
 }
 
 public ClubModel getAwayTeam() {
	return new ClubModel(awayTeam);
 }
 
 public void setAwayTeam(ClubModel awayTeam) {
	this.awayTeam = new ClubModel(awayTeam);
 }
  
  public String getSaveGameDate() {
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	return gameDate.format(formatter);
 }
 
 public String getPrintDate() {
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	return gameDate.format(formatter);
 }
 
 public String fullMatchDate() {
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EE, dd.MM.yyyy");
	return gameDate.format(formatter);
 }
 
 public JSONObject toJSON() {
	Map<String, Object> result = new HashMap<>();
	result.put("competition", competition);
	if (homeTeam != null) {
	 result.put("homeTeam", homeTeam.toJSON());
	} else {
	 result.put("homeTeam", null);
	}
	if (awayTeam != null) {
	 result.put("awayTeam", awayTeam.toJSON());
	} else {
	 result.put("awayTeam", null);
	}
	result.put("gameDate", getSaveGameDate());
	result.put("gameTime", gameTime);
	result.put("matchDay", matchDay);
	result.put("gameUrl", gameUrl);
	result.put("id", gameId);
	return new JSONObject(result);
 }
 
}
