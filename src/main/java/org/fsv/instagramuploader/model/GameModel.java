package org.fsv.instagramuploader.model;

import org.json.simple.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GameModel {
 private final String competition;
 private final LocalDate gameDate;
 private final String gameTime;
 private boolean homeGame;
 private String matchDay;
 private String homeTeam;
 private String homeStats;
 private String awayTeam;
 private String awayStats;
 private String changedName;
 private String youthTeam;
 
 public GameModel(String competition, String homeTeam, String homeStats, String awayTeam, String awayStats, LocalDate gameDate, String gameTime, String changedName) {
	this.competition = competition;
	this.homeTeam = homeTeam;
	this.homeStats = homeStats;
	this.awayTeam = awayTeam;
	this.awayStats = awayStats;
	this.gameDate = gameDate;
	this.gameTime = gameTime;
	
	this.changedName = changedName;
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
 
 public String getHomeTeam() {
	return homeTeam;
 }
 
 public void setHomeTeam(String homeTeam) {
	this.homeTeam = homeTeam;
 }
 
 public String getHomeStats() {
	return homeStats;
 }
 
 public void setHomeStats(String homeStats) {
	this.homeStats = homeStats;
 }
 
 public String getAwayTeam() {
	return awayTeam;
 }
 
 public void setAwayTeam(String awayTeam) {
	this.awayTeam = awayTeam;
 }
 
 public String getAwayStats() {
	return awayStats;
 }
 
 public void setAwayStats(String awayStats) {
	this.awayStats = awayStats;
 }
 
 public String getGameTime() {
	return gameTime;
 }
 
 public String getChangeName() {
	return changedName;
 }
 
 public void setChangeName(String changeName) {
	this.changedName = changeName;
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
 
 public boolean getHomeGame() {
	return homeGame;
 }
 
 public void setHomeGame(boolean homeGame) {
	this.homeGame = homeGame;
 }
 
 public JSONObject toJSON() {
	JSONObject result = new JSONObject();
	result.put("competition", competition);
	result.put("homeTeam", homeTeam);
	result.put("homeStats", homeStats);
	result.put("awayTeam", awayTeam);
	result.put("awayStats", awayStats);
	result.put("gameDate", getSaveGameDate());
	result.put("gameTime", gameTime);
	result.put("changedName", changedName);
	result.put("matchDay", matchDay);
	return result;
 }
 
 public String getYouthTeam() {
	return youthTeam;
 }
}
