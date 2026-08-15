package org.fsv.instagramuploader.model;

import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ClubModel {
 @Getter
 String clubName;
 @Setter
 String clubStats;
 @Getter
 @Setter
 String clubPlace;
 @Getter
 @Setter
 String clubLogoDir;
 @Setter
 @Getter
 String saveName;
 @Setter
 @Getter
 String changedName;
 
 public ClubModel(String clubName, String clubStats, String clubPlace, String clubLogoDir, String saveName, String changedName) {
	this.clubName = clubName;
	this.clubStats = clubStats;
	this.clubPlace = clubPlace;
	this.clubLogoDir = clubLogoDir;
	this.saveName = saveName;
	this.changedName = changedName;
 }
 
 public ClubModel(ClubModel clubModel) {
	if (clubModel != null) {
	 this.clubName = clubModel.clubName;
	 this.clubStats = clubModel.clubStats;
	 this.clubPlace = clubModel.clubPlace;
	 this.clubLogoDir = clubModel.clubLogoDir;
	 this.saveName = clubModel.saveName;
	 this.changedName = clubModel.changedName;
	}
 }
 
 public ClubModel(Map<?, ?> club) {
	if (club == null) {
	 return;
	}
	if (club.containsKey("clubName") && club.get("clubName") != null) {
	 this.clubName = club.get("clubName").toString();
	}
	if (club.containsKey("clubStats") && club.get("clubStats") != null) {
	 this.clubStats = club.get("clubStats").toString();
	}
	if (club.containsKey("clubPlace") && club.get("clubPlace") != null) {
	 this.clubPlace = club.get("clubPlace").toString();
	}
	if (club.containsKey("clubLogoDir") && club.get("clubLogoDir") != null) {
	 this.clubLogoDir = club.get("clubLogoDir").toString();
	}
	if (club.containsKey("changedName") && club.get("changedName") != null) {
	 this.changedName = club.get("changedName").toString();
	}
	if (club.containsKey("saveName") && club.get("saveName") != null) {
	 this.saveName = club.get("saveName").toString();
	}
 }
  
  public String getClubStats() {
	if (clubStats == null) {
	 return "";
	}
	return clubStats;
 }
  
  public JSONObject toJSON() {
	Map<String, String> resultMap = new HashMap<>();
	resultMap.put("clubName", clubName);
	resultMap.put("clubStats", clubStats);
	resultMap.put("clubPlace", clubPlace);
	resultMap.put("clubLogoDir", clubLogoDir);
	resultMap.put("saveName", saveName);
	resultMap.put("changedName", changedName);
	return new JSONObject(resultMap);
 }
}
