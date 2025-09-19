package org.fsv.instagramuploader.model;

import org.json.simple.JSONObject;

import java.util.HashMap;

public record ResultModel(JSONObject id, String result, String homeStats, String awayStats, String text) {
 
 public ResultModel {
	id = id != null ? new JSONObject(id) : new JSONObject();
 }
 
 public JSONObject id() {
	return new JSONObject(id);
 }
 
 public String getValue(String val) {
	return id().get(val).toString();
 }
 
 public String getClubName(String val) {
	Object obj = id().get(val);
	if (obj instanceof HashMap<?, ?> map) {
	 Object name = map.get("clubName");
	 return name != null ? name.toString() : null;
	}
	return null;
 }
}
