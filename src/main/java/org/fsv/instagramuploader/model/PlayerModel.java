package org.fsv.instagramuploader.model;

public class PlayerModel {
	private int number;
	private String name;
	private String role;
	private boolean goalkeeper;
	private boolean captain;

	public PlayerModel() {
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public boolean isGoalkeeper() {
		return goalkeeper;
	}

	public void setGoalkeeper(boolean goalkeeper) {
		this.goalkeeper = goalkeeper;
	}

	public boolean isCaptain() {
		return captain;
	}

	public void setCaptain(boolean captain) {
		this.captain = captain;
	}
}
