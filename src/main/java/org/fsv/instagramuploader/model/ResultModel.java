package org.fsv.instagramuploader.model;

import org.json.simple.JSONObject;

public record ResultModel(JSONObject id, String result, String homeStats, String awayStats, String text) {
 
 public String getValue(String val) {
	return id().get(val).toString();
 }
}
