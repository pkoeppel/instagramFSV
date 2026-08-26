package org.fsv.instagramuploader.men;

import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.FontClass;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.GameModel;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScoreCreator {
    private static final String TEMPLATE_PATH = "src/main/resources/pictures/template/men/";
    private static final String SAVE_PATH = "src/main/resources/save/";

    public String createScore(GameModel game, String score, boolean halftime) throws IOException, ParseException {
        ClubModel home = ClubSelector.getClubDetails(game.getHomeTeam());
        ClubModel away = ClubSelector.getClubDetails(game.getAwayTeam());
        if (home == null || away == null) {
            throw new IOException("Could not resolve clubs for men score");
        }
        validateScore(score);

        String savePath = game.getSavePath();
        if (savePath == null || savePath.isBlank()) {
            savePath = game.getSaveGameDate() + "_" + game.getCompetition() + "_" + home.getSaveName() + "_" + away.getSaveName();
            game.setSavePath(savePath);
        }
        Path directory = Path.of(SAVE_PATH, savePath, "Bilder");
        Files.createDirectories(directory);
        String template = halftime ? "halftimeTemp.png" : "finishTemp.png";
        String fileName = halftime ? "Halbzeit.png" : "Endstand.png";
        return render(template, score, home, away, directory.resolve(fileName));
    }

    private String render(String templateName, String score, ClubModel home, ClubModel away, Path target) throws IOException {
        BufferedImage background = ImageIO.read(new File(TEMPLATE_PATH + templateName));
        drawLogo(background, home, "score-logo-home");
        drawLogo(background, away, "score-logo-away");
        String[] scoreParts = score.split(":", 2);
        Helper.writeOnPicture(background, scoreParts[0], "score-home", FontClass.resultMen, Color.WHITE, 0);
        Helper.writeOnPicture(background, ":", "score-colon", FontClass.resultMen, Color.WHITE, 0);
        Helper.writeOnPicture(background, scoreParts[1], "score-away", FontClass.resultMen, Color.WHITE, 0);
        ImageIO.write(background, "png", target.toFile());
        return target.toString();
    }

    private void drawLogo(BufferedImage background, ClubModel club, String coordinate) throws IOException {
        BufferedImage logo = ImageIO.read(new File(club.getClubLogoDir()));
        if (logo == null) {
            throw new IOException("Could not read logo for " + club.getClubName());
        }
        Helper.pictureOnPicture(background, logo, coordinate, Helper.isOwnClub(club));
    }

    private void validateScore(String score) throws IOException {
        if (score == null || !score.matches("\\d{1,2}:\\d{1,2}")) {
            throw new IOException("Score must have the format 0:0");
        }
    }
}