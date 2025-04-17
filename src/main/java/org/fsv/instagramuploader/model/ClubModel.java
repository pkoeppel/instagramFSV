package org.fsv.instagramuploader.model;

import java.awt.image.BufferedImage;

public record ClubModel(String clubName, String clubPlace, BufferedImage clubLogo, String saveClubName) {
}
