package org.fsv.instagramuploader.model;

import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ClubModel {
 String clubName;
 String clubStats;
 String clubPlace;
 String clubLogoDir;
 String saveName;
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
	this.clubName = clubModel.clubName;
	this.clubStats = clubModel.clubStats;
	this.clubPlace = clubModel.clubPlace;
	this.clubLogoDir = clubModel.clubLogoDir;
	this.saveName = clubModel.saveName;
	this.changedName = clubModel.changedName;
 }
 
 public ClubModel(JSONObject club) {
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
 }
 
 public String getClubName() {
	return clubName;
 }
 
 public String getClubStats() {
	if (clubStats == null) {
	 return "";
	}
	return clubStats;
 }
 
 public void setClubStats(String clubStats) {
	this.clubStats = clubStats;
 }
 
 public void setClubPlace(String clubPlace) {
	this.clubPlace = clubPlace;
 }
 
 public String getClubPlace() {
	return clubPlace;
 }
 
 public void setClubLogoDir(String clubLogoDir) {
	this.clubLogoDir = clubLogoDir;
 }
 
 public String getClubLogoDir() {
	return clubLogoDir;
 }
 
 public String getSaveName() {
	return saveName;
 }
 
 public void setSaveName(String saveName) {
	this.saveName = saveName;
 }
 
 public String getChangedName() {
	return changedName;
 }
 
 public void setChangedName(String changedName) {
	this.changedName = changedName;
 }
 
 public JSONObject toJSON() {
	Map<String, String> resultMap = new HashMap<>();
	resultMap.put("clubName", clubName);
	resultMap.put("clubStats", clubStats);
	resultMap.put("clubPlace", clubPlace);
	resultMap.put("clubLogoDir", clubLogoDir);
	resultMap.put("changedName", changedName);
	return new JSONObject(resultMap);
 }
}
