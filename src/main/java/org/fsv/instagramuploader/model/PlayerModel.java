package org.fsv.instagramuploader.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerModel {
	private int number;
	private String name;
	private String role;
	private boolean goalkeeper;
	private boolean captain;

	public PlayerModel() {
	}
 
}
