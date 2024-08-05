package org.fsv.instagramuploader.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record MatchModel(String team, String matchType, String matchDay, String homeGame, String ownStats,
                         String opponent,
                         String oppStats,
                         String matchDate, String matchTime, String homePlace, String oppName) {
	
	@Override
	public String ownStats() {
		if (ownStats != null) {
			return ownStats;
		}
		return "";
	}
	
	@Override
	public String oppStats() {
		if (oppStats != null) {
			return oppStats;
		}
		return "";
	}
	
	@Override
	public String matchDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate date = LocalDate.parse(matchDate);
		return date.format(formatter);
	}
	
	public String fullMatchDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EE, dd.MM.yyyy");
		LocalDate date = LocalDate.parse(matchDate);
		return date.format(formatter);
	}
	
	
	public String getSaveMatchDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDate date = LocalDate.parse(matchDate);
		return date.format(formatter);
	}
	
	public String getMatchType() {
		return switch (matchType) {
			case "leagueMatch" -> "Liga";
			case "cupMatch" -> "Pokal";
			case "friendMatch" -> "Testspiel";
			case "youthMatch" -> "Kinderfest";
			default -> matchType;
		};
	}
}
