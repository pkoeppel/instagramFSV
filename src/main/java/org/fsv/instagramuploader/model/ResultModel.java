package org.fsv.instagramuploader.model;

import org.json.simple.JSONObject;

public record ResultModel(JSONObject id, String result, String ownStats, String oppStats, String text) {
	
	public String getValue(String val) {
		return id().get(val).toString();
	}
	
	public String getMatchType() {
		return switch (getValue("matchType")) {
			case "leagueMatch" -> "(Liga)";
			case "cupMatch" -> "(Pokal)";
			case "youthMatch" -> "(Kinderfest)";
			default -> getValue("matchType");
		};
	}
}
