package org.fsv.instagramuploader.model;

import org.json.simple.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class GameModel {
 private final String competition;
 private final LocalDate gameDate;
 private final String gameTime;
 private String matchDay;
 private ClubModel homeTeam;
 private ClubModel awayTeam;
 private final String team;
 
 public GameModel(JSONObject game, String team) {
	this.competition = game.get("competition").toString();
	this.gameDate = LocalDate.parse(game.get("gameDate").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	this.gameTime = game.get("gameTime").toString();
	this.matchDay = game.get("matchDay").toString();
	this.homeTeam = (ClubModel) game.get("homeTeam");
	this.awayTeam = (ClubModel) game.get("awayTeam");
	this.team = team;
 }
 
 public GameModel(String competition, LocalDate gameDate, String gameTime, String team) {
	this.competition = competition;
	this.gameDate = gameDate;
	this.gameTime = gameTime;
	this.team = team;
 }
 
 public String getCompetition() {
	return competition;
 }
 
 public String getMatchDay() {
	return matchDay;
 }
 
 public void setMatchDay(String matchDay) {
	this.matchDay = matchDay;
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
 
 public String getGameTime() {
	return gameTime;
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
	return new JSONObject(result);
 }
 
 public String getTeam() {
	return team;
 }
}
