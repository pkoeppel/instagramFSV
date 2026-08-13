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
 
 public GameModel(JSONObject game, String team) {
	this.gameId = game.containsKey("id") && game.get("id") != null ? game.get("id").toString()
			: game.containsKey("gameId") && game.get("gameId") != null ? game.get("gameId").toString() : null;
	this.competition = game.containsKey("competition") && game.get("competition") != null ? game.get("competition").toString() : null;
	String dateStr = game.containsKey("gameDate") && game.get("gameDate") != null ? game.get("gameDate").toString() : null;
	this.gameDate = dateStr != null ? LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;
	this.gameTime = game.containsKey("gameTime") && game.get("gameTime") != null ? game.get("gameTime").toString() : null;
	String teamFromGame = game.containsKey("team") && game.get("team") != null ? game.get("team").toString() : null;
	this.team = team != null ? team : teamFromGame;
	if (game.containsKey("matchDay") && game.get("matchDay") != null) {
		this.matchDay = game.get("matchDay").toString();
	}
	if (game.containsKey("homeTeam") && game.get("homeTeam") instanceof Map<?, ?> ht) {
		this.homeTeam = new ClubModel(ht);
	}
	if (game.containsKey("awayTeam") && game.get("awayTeam") instanceof Map<?, ?> at) {
		this.awayTeam = new ClubModel(at);
	}
	if (game.containsKey("gameUrl") && game.get("gameUrl") != null) {
		this.gameUrl = game.get("gameUrl").toString();
	}
 }

 public GameModel(String gameId, String competition, LocalDate gameDate, String gameTime, String team) {
	this.gameId = gameId;
	this.competition = competition;
	this.gameDate = gameDate;
	this.gameTime = gameTime;
	this.team = team;
 }
  
  public ClubModel getHomeTeam() {
	return homeTeam != null ? new ClubModel(homeTeam) : null;
 }
 
 public void setHomeTeam(ClubModel homeTeam) {
	this.homeTeam = new ClubModel(homeTeam);
 }
 
 public ClubModel getAwayTeam() {
	return awayTeam != null ? new ClubModel(awayTeam) : null;
 }
 
 public void setAwayTeam(ClubModel awayTeam) {
	this.awayTeam = new ClubModel(awayTeam);
 }
  
  public String getSaveGameDate() {
	if (gameDate == null) return null;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	return gameDate.format(formatter);
 }
 
 public String getPrintDate() {
	if (gameDate == null) return null;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	return gameDate.format(formatter);
 }
 
 public String fullMatchDate() {
	if (gameDate == null) return null;
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
