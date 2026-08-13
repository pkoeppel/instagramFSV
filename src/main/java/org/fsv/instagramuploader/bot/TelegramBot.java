package org.fsv.instagramuploader.bot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.Controller;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.men.MatchdayCreator;
import org.fsv.instagramuploader.men.ResultCreator;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.GameModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final String botName;
    private final Controller controller;
    private final List<Long> allowedChatIds = List.of(5047912799L);
    private final String botToken;
    private JSONArray bufferedGames;
    private JSONObject bufferedGame;
    private ResultCreator resultCreator;
    private GameModel resultGameModel;
    private String resultHeadline;
    private String resultReport;
    private String pendingAction;

    public TelegramBot(String botName, String botToken) {
        super(botToken);
        this.botName = botName;
        this.botToken = botToken;
        controller = new Controller();
    }

    @Override
    public String getBotUsername() {
        return this.botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                if (allowedChatIds.contains(chatId)) {
                    handleCallbackQuery(chatId.toString(), update.getCallbackQuery().getData());
                } else {
                    log.error("Unallowed user: {}", chatId);
                }
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();
                if (allowedChatIds.contains(chatId)) {
                    handleTextMessage(chatId.toString(), update.getMessage().getText());
                } else {
                    log.error("Unallowed user: {}", chatId);
                }
            } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
                Long chatId = update.getMessage().getChatId();
                if (allowedChatIds.contains(chatId)) {
                    handlePhotoMessage(chatId.toString(), update.getMessage().getPhoto());
                } else {
                    log.error("Unallowed user: {}", chatId);
                }
            }
        } catch (ParseException | URISyntaxException | IOException | TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleCallbackQuery(String chatId, String lastCall) throws IOException, ParseException, URISyntaxException {
        if ("createPreview".equals(lastCall)) {
            resetResultFlow();
            startTeamSelection(chatId, "createPreview");
            return;
        }
        if ("createResult".equals(lastCall)) {
            resetResultFlow();
            startTeamSelection(chatId, "createResult");
            return;
        }
        if ("cancel".equals(lastCall)) {
            resetResultFlow();
            sendStartMsg(chatId, "Ok!");
            return;
        }
        String[] paths = lastCall.split("_");
        if (paths.length < 2 || (!"createPreview".equals(paths[0]) && !"createResult".equals(paths[0]))) {
            return;
        }
        String mode = paths[0];
        String teamQuery = paths[1];
        if (paths.length == 2) {
            if ("createResult".equals(mode)) {
                loadResultMatches(chatId, lastCall, teamQuery);
            } else {
                loadMatches(chatId, lastCall, teamQuery);
            }
        } else if (paths.length == 3) {
            if ("update".equals(paths[2])) {
                Helper.updateNextMatchesFromFBDE();
                loadMatches(chatId, mode + "_" + teamQuery, teamQuery);
            } else {
                handleGameSelection(chatId, teamQuery, paths[2], mode);
            }
        } else if (paths.length == 4) {
            handleClubResolution(chatId, teamQuery, paths[2], paths[3], mode);
        }
    }

    private void startTeamSelection(String chatId, String mode) {
        JSONObject teamData = controller.getTeamData().getBody();
        List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
        if (teamData != null) {
            teamData.forEach((key, value) -> keyboardValues.add(new ImmutablePair<>(key.toString(), key.toString())));
        }
        sendMsg(chatId, "Wähle eine Mannschaft!", createKeyboard(3, mode, keyboardValues));
    }

    private void handleGameSelection(String chatId, String teamQuery, String matchKey, String mode) throws IOException, ParseException {
        if (bufferedGames == null) {
            sendStartMsg(chatId, "Keine Spiele geladen. Bitte starte den Vorgang erneut.");
            return;
        }
        String[] keyParts = matchKey.split("~", 2);
        String matchDay = keyParts[0];
        String competition = keyParts.length > 1 ? keyParts[1] : null;
        bufferedGame = null;
        for (Object teamGame : bufferedGames) {
            JSONObject g = (JSONObject) teamGame;
            if (matchDay.equals(g.get("matchDay").toString()) &&
                    (competition == null || competition.equals(g.get("competition").toString()))) {
                bufferedGame = g;
                break;
            }
        }
        if (bufferedGame == null) {
            sendStartMsg(chatId, "Spiel nicht gefunden. Bitte starte den Vorgang erneut.");
            return;
        }
        GameModel gameModel = new GameModel(bufferedGame, teamQuery);
        gameModel.setMatchDay(bufferedGame.get("matchDay").toString());
        generateOrResolveClub(chatId, gameModel, lastCallFor(mode, teamQuery, matchKey), mode);
    }

    private void generateOrResolveClub(String chatId, GameModel gameModel, String lastCall, String mode) throws IOException, ParseException {
        ClubModel home = ClubSelector.getClubDetails(gameModel.getHomeTeam());
        ClubModel away = ClubSelector.getClubDetails(gameModel.getAwayTeam());
        if (home != null && away != null) {
            gameModel.setHomeTeam(home);
            gameModel.setAwayTeam(away);
            if ("createResult".equals(mode)) {
                startResultInput(chatId, gameModel, lastCall);
            } else {
                getMatchdayFile(chatId, gameModel);
            }
            return;
        }
        sendClubSelection(chatId, lastCall);
    }

    private void sendClubSelection(String chatId, String lastCall) throws IOException, ParseException {
        JSONObject clubs = (JSONObject) new JSONParser().parse(new InputStreamReader(
                new FileInputStream("src/main/resources/templates/clubs.json"), StandardCharsets.UTF_8));
        List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
        for (Object key : clubs.keySet()) {
            keyboardValues.add(new ImmutablePair<>(key.toString(), key.toString()));
        }
        sendMsg(chatId, "Unbekanntes Gegnerteam. Bitte wähle das entsprechende Team aus der Datenbank!",
                createKeyboard(1, lastCall, keyboardValues));
    }

    private void handleClubResolution(String chatId, String teamQuery, String matchKey, String clubKey, String mode) throws IOException, ParseException {
        if (bufferedGames == null) {
            sendStartMsg(chatId, "Kein Spiel im Zwischenspeicher. Bitte starte den Vorgang erneut.");
            return;
        }
        String[] keyParts = matchKey.split("~", 2);
        String matchDay = keyParts[0];
        String competition = keyParts.length > 1 ? keyParts[1] : null;
        bufferedGame = null;
        for (Object teamGame : bufferedGames) {
            JSONObject g = (JSONObject) teamGame;
            if (matchDay.equals(g.get("matchDay").toString()) &&
                    (competition == null || competition.equals(g.get("competition").toString()))) {
                bufferedGame = g;
                break;
            }
        }
        if (bufferedGame == null) {
            sendStartMsg(chatId, "Spiel nicht gefunden. Bitte starte den Vorgang erneut.");
            return;
        }
        GameModel gameModel = new GameModel(bufferedGame, teamQuery);
        gameModel.setMatchDay(bufferedGame.get("matchDay").toString());
        ClubModel selected = ClubSelector.searchClubDetails(clubKey);
        if (selected == null) {
            sendStartMsg(chatId, "Verein nicht gefunden. Bitte starte den Vorgang erneut.");
            return;
        }
        ClubModel home = ClubSelector.getClubDetails(gameModel.getHomeTeam());
        ClubModel away = ClubSelector.getClubDetails(gameModel.getAwayTeam());
        if (home == null) {
            selected.setClubStats(gameModel.getHomeTeam().getClubStats());
            gameModel.setHomeTeam(selected);
        } else if (away == null) {
            selected.setClubStats(gameModel.getAwayTeam().getClubStats());
            gameModel.setAwayTeam(selected);
        }
        generateOrResolveClub(chatId, gameModel, lastCallFor(mode, teamQuery, matchKey), mode);
    }

    private String lastCallFor(String mode, String teamQuery, String matchKey) {
        return mode + "_" + teamQuery + "_" + matchKey;
    }

    private void loadResultMatches(String chatId, String lastCall, String teamQuery) throws IOException, ParseException {
        InputStreamReader reader = new InputStreamReader(
                new FileInputStream("src/main/resources/templates/men-games.json"), StandardCharsets.UTF_8);
        JSONParser parser = new JSONParser();
        JSONArray result = (JSONArray) parser.parse(reader);
        bufferedGames = new JSONArray();
        for (Object entry : result) {
            JSONObject game = (JSONObject) entry;
            String teamVal = game.containsKey("team") && game.get("team") != null ? game.get("team").toString() : null;
            if (!teamQuery.equals(teamVal)) {
                continue;
            }
            JSONObject transformed = new JSONObject();
            transformed.put("competition", game.get("competition"));
            String matchDate = game.get("matchDate").toString();
            transformed.put("gameDate", matchDate);
            transformed.put("matchDay", matchDate);
            transformed.put("gameTime", null);
            transformed.put("team", teamQuery);
            transformed.put("gameUrl", game.get("gameUrl"));
            transformed.put("savePath", game.get("savePath"));
            transformed.put("homeTeam", game.get("homeClub"));
            transformed.put("awayTeam", game.get("awayClub"));
            bufferedGames.add(transformed);
        }
        if (bufferedGames.isEmpty()) {
            sendStartMsg(chatId, "Keine Spiele für diese Mannschaft in men-games.json gefunden.");
            return;
        }
        List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
        for (Object teamGame : bufferedGames) {
            JSONObject game = (JSONObject) teamGame;
            JSONObject home = (JSONObject) game.get("homeTeam");
            JSONObject away = (JSONObject) game.get("awayTeam");
            String matchDay = game.get("matchDay").toString();
            String competition = game.get("competition").toString();
            String matchKey = matchDay + "~" + competition;
            String gameInfo = matchDay + " | " + competition + ": " + home.get("clubName") + " - " + away.get("clubName");
            keyboardValues.add(new ImmutablePair<>(gameInfo, matchKey));
        }
        sendMsg(chatId, "Wähle ein Spiel aus!", createKeyboard(1, lastCall, keyboardValues));
    }

    private void loadMatches(String chatId, String lastCall, String teamQuery) throws IOException, ParseException, URISyntaxException {
        InputStreamReader reader = new InputStreamReader(
                new FileInputStream("src/main/resources/templates/allMatches.json"), StandardCharsets.UTF_8);
        JSONObject result = (JSONObject) new JSONParser().parse(reader);
        bufferedGames = (JSONArray) result.get(teamQuery);
        if (bufferedGames == null) {
            Helper.updateNextMatchesFromFBDE();
            reader = new InputStreamReader(
                    new FileInputStream("src/main/resources/templates/allMatches.json"), StandardCharsets.UTF_8);
            result = (JSONObject) new JSONParser().parse(reader);
            bufferedGames = (JSONArray) result.get(teamQuery);
        }
        if (bufferedGames == null) {
            sendStartMsg(chatId, "Keine Spiele für diese Mannschaft gefunden.");
            return;
        }
        List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
        for (Object teamGame : bufferedGames) {
            JSONObject game = (JSONObject) teamGame;
            JSONObject home = (JSONObject) game.get("homeTeam");
            JSONObject away = (JSONObject) game.get("awayTeam");
            String matchDay = game.get("matchDay").toString();
            String competition = game.get("competition").toString();
            String matchKey = matchDay + "~" + competition;
            String gameInfo = matchDay + " | " + competition + ": " + home.get("clubName") + " - " + away.get("clubName");
            keyboardValues.add(new ImmutablePair<>(gameInfo, matchKey));
        }
        keyboardValues.add(new ImmutablePair<>("Liste aktualisieren", "update"));
        sendMsg(chatId, "Wähle ein Spiel aus!", createKeyboard(1, lastCall, keyboardValues));
    }

    public synchronized void getMatchdayFile(String chatId, GameModel gameModel) throws IOException, ParseException {
        MatchdayCreator mc = new MatchdayCreator();
        String savePath = mc.createMatch(gameModel);
        Path filePath = Paths.get("src/main/resources/save", savePath, "/Matchday.jpeg");
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(filePath.toFile()));
        sendPhoto.setChatId(chatId);
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        sendStartMsg(chatId, "Prozess abgeschlossen! Nächster Prozess kann gestartet werden!");
    }

    public synchronized void sendMsg(String chatId, String msg, InlineKeyboardMarkup keyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setParseMode(ParseMode.MARKDOWN);
        sendMessage.setChatId(chatId);
        sendMessage.setText(msg);
        try {
            sendMessage.setReplyMarkup(keyboard);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private InlineKeyboardMarkup createKeyboard(Integer perRow, String lastCall, List<ImmutablePair<String, String>> allButtons) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
        Integer counter = 0;
        for (ImmutablePair<String, String> button : allButtons) {
            keyboardRow.add(createInlineButton(button.getKey(), lastCall + "_" + button.getValue()));
            counter++;
            if (counter >= perRow) {
                keyboard.add(keyboardRow);
                counter = 0;
                keyboardRow = new ArrayList<>();
            }
        }
        if (counter != 0) {
            keyboard.add(keyboardRow);
        }
        List<InlineKeyboardButton> lastRow = new ArrayList<>();
        lastRow.add(createInlineButton("Abbruch", "cancel"));
        keyboard.add(lastRow);
        inlineKeyboard.setKeyboard(keyboard);
        return inlineKeyboard;
    }

    public synchronized void sendStartMsg(String chatId, String msg) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("Spieltagsvorschau erstellen", "createPreview"));
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("Spieltagsergebnis erstellen", "createResult"));
        keyboard.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton("Abbruch", "cancel"));
        keyboard.add(row3);

        inlineKeyboardMarkup.setKeyboard(keyboard);
        sendMsg(chatId, msg, inlineKeyboardMarkup);
    }

    private void startResultInput(String chatId, GameModel gameModel, String lastCall) {
        this.resultCreator = new ResultCreator();
        this.resultGameModel = gameModel;
        this.resultHeadline = null;
        this.resultReport = null;
        this.pendingAction = "headline";
        sendSimpleMsg(chatId, "Schreibe deine Headline!");
    }

    private void handleTextMessage(String chatId, String text) throws IOException, ParseException {
        if ("abbruch".equalsIgnoreCase(text) || "abbrechen".equalsIgnoreCase(text)) {
            resetResultFlow();
            sendStartMsg(chatId, "Abbruch.");
            return;
        }
        if ("headline".equals(pendingAction)) {
            resultHeadline = text;
            pendingAction = "report";
            sendSimpleMsg(chatId, "Schreibe deinen Bericht!");
        } else if ("report".equals(pendingAction)) {
            resultReport = text;
            pendingAction = "photos";
            sendSimpleMsg(chatId, "Schicke mir die Bilder (Verhältnis 4:5). Wenn du fertig bist, sende 'Fertig'.");
        } else if ("photos".equals(pendingAction) && "fertig".equalsIgnoreCase(text)) {
            finalizeResult(chatId);
        } else if ("photos".equals(pendingAction)) {
            sendSimpleMsg(chatId, "Sende weitere Bilder oder 'Fertig', um fortzufahren.");
        } else {
            resetResultFlow();
            sendStartMsg(chatId, text);
        }
    }

    private void handlePhotoMessage(String chatId, List<PhotoSize> photos) throws IOException, TelegramApiException {
        if (!"photos".equals(pendingAction) || resultCreator == null) {
            sendSimpleMsg(chatId, "Bitte starte zuerst den Ergebnis-Flow.");
            return;
        }
        PhotoSize largest = photos.stream()
                .max(Comparator.comparingInt(p -> p.getFileSize() != null ? p.getFileSize() : 0))
                .orElse(null);
        if (largest == null) {
            return;
        }
        GetFile getFile = new GetFile(largest.getFileId());
        org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFile);
        String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath();
        try (InputStream is = new URL(fileUrl).openStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                sendSimpleMsg(chatId, "Bild konnte nicht geladen werden.");
                return;
            }
            resultCreator.addImage(image);
            sendSimpleMsg(chatId, "Bild erhalten. Nächstes Bild oder 'Fertig'.");
        }
    }

    private void resetResultFlow() {
        pendingAction = null;
        resultCreator = null;
        resultGameModel = null;
        resultHeadline = null;
        resultReport = null;
    }

    private void finalizeResult(String chatId) throws IOException, ParseException {
        if (resultGameModel == null || resultCreator == null) {
            sendSimpleMsg(chatId, "Kein Ergebnis-Prozess aktiv.");
            resetResultFlow();
            return;
        }
        ClubModel home = resultGameModel.getHomeTeam();
        ClubModel away = resultGameModel.getAwayTeam();
        String savePath = resultGameModel.getSavePath();
        if (savePath == null || savePath.isBlank()) {
            savePath = resultGameModel.getSaveGameDate() + "_" + resultGameModel.getCompetition() + "_" + home.getSaveName() + "_" + away.getSaveName();
        }
        Map<String, Object> match = new HashMap<>();
        match.put("homeClub", home.toJSON());
        match.put("awayClub", away.toJSON());
        match.put("team", resultGameModel.getTeam());
        match.put("matchDate", resultGameModel.getSaveGameDate());
        match.put("savePath", savePath);
        match.put("competition", resultGameModel.getCompetition());
        match.put("gameUrl", resultGameModel.getGameUrl());
        JSONObject matchJson = new JSONObject(match);

        Map<String, Object> outer = new HashMap<>();
        outer.put("match", matchJson.toJSONString());
        outer.put("headline", resultHeadline != null ? resultHeadline : "");
        outer.put("report", resultReport != null ? resultReport : "");
        JSONObject result = resultCreator.createResult(new JSONObject(outer));
        sendResultImage(chatId, result.get("fileDir").toString(), result.get("caption").toString());
        removeMatchFromMenGames(resultGameModel);
        resetResultFlow();
        sendStartMsg(chatId, "Ergebnis erstellt!");
    }

    private void sendResultImage(String chatId, String fileDir, String caption) {
        String formatedDate = resultGameModel.getSaveGameDate().replace("-", "");
        Path filePath = Paths.get("src/main/resources/save", fileDir, "Bilder", formatedDate + "_0.jpeg");
        java.io.File file = filePath.toFile();
        if (!file.exists()) {
            sendSimpleMsg(chatId, "Ergebnis erstellt:\n\n" + caption);
            return;
        }
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(file));
        sendPhoto.setCaption(caption);
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Could not send result image", e);
            sendSimpleMsg(chatId, "Ergebnis erstellt, aber Bild konnte nicht gesendet werden.");
        }
    }

    private void removeMatchFromMenGames(GameModel gameModel) {
        if (gameModel == null || gameModel.getSaveGameDate() == null || gameModel.getCompetition() == null || gameModel.getHomeTeam() == null || gameModel.getAwayTeam() == null) {
            return;
        }
        String matchDate = gameModel.getSaveGameDate();
        String competition = gameModel.getCompetition();
        String team = gameModel.getTeam();
        String homeName = gameModel.getHomeTeam().getClubName();
        String awayName = gameModel.getAwayTeam().getClubName();
        if (team == null || homeName == null || awayName == null) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/men-games.json"), StandardCharsets.UTF_8)) {
            JSONParser parser = new JSONParser();
            JSONArray games = (JSONArray) parser.parse(reader);
            boolean removed = false;
            Iterator<?> it = games.iterator();
            while (it.hasNext()) {
                JSONObject game = (JSONObject) it.next();
                String gDate = game.containsKey("matchDate") && game.get("matchDate") != null ? game.get("matchDate").toString() : null;
                String gComp = game.containsKey("competition") && game.get("competition") != null ? game.get("competition").toString() : null;
                String gTeam = game.containsKey("team") && game.get("team") != null ? game.get("team").toString() : null;
                JSONObject homeClub = game.get("homeClub") instanceof JSONObject ? (JSONObject) game.get("homeClub") : null;
                JSONObject awayClub = game.get("awayClub") instanceof JSONObject ? (JSONObject) game.get("awayClub") : null;
                String gHome = homeClub != null && homeClub.get("clubName") != null ? homeClub.get("clubName").toString() : null;
                String gAway = awayClub != null && awayClub.get("clubName") != null ? awayClub.get("clubName").toString() : null;
                if (matchDate.equals(gDate) && competition.equals(gComp) && team.equals(gTeam) && homeName.equals(gHome) && awayName.equals(gAway)) {
                    it.remove();
                    removed = true;
                    break;
                }
            }
            if (removed) {
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("src/main/resources/templates/men-games.json"), StandardCharsets.UTF_8)) {
                    writer.write(games.toJSONString());
                }
                log.info("Removed finished match from men-games.json: {} vs {}", homeName, awayName);
            } else {
                log.warn("Could not find match in men-games.json to remove: {} vs {}", homeName, awayName);
            }
        } catch (IOException | ParseException e) {
            log.error("Could not remove finished match from men-games.json", e);
        }
    }

    private void sendSimpleMsg(String chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void sendAutomaticMatchHint(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        for (Long chatId : allowedChatIds) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId.toString());
            sendMessage.setText(message);
            try {
                execute(sendMessage);
                log.info("Sent automatic match hint to chat {}: {}", chatId, message);
            } catch (TelegramApiException e) {
                log.error("Could not send automatic match hint to chat {}", chatId, e);
            }
        }
    }

    private InlineKeyboardButton createInlineButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton(text);
        button.setCallbackData(callbackData);
        return button;
    }
}
