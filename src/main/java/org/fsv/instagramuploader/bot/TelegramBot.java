package org.fsv.instagramuploader.bot;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.Controller;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.men.LineupCreator;
import org.fsv.instagramuploader.men.MatchdayCreator;
import org.fsv.instagramuploader.men.ResultCreator;
import org.fsv.instagramuploader.model.ClubModel;
import org.fsv.instagramuploader.model.GameModel;
import org.fsv.instagramuploader.model.PlayerModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
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
import java.util.stream.Collectors;

import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final String botName;
    private final Controller controller;
    private final List<Long> allowedChatIds = List.of(5047912799L, 1911405789L);
    private final String botToken;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private List<Map<String, Object>> bufferedGames;
    private Map<String, Object> bufferedGame;
    private ResultCreator resultCreator;
    private GameModel resultGameModel;
    private String resultHeadline;
    private String resultReport;
    private String pendingAction;
    private GameModel pendingMatchdayGame;
    private BufferedImage pendingMatchdayPhoto;
    private String pendingMatchdayAction;
    private final LineupCreator lineupCreator;
    private GameModel pendingLineupGame;
    private String pendingLineupAction;
    private List<PlayerModel> pendingLineupPlayers;
    private int pendingLineupIndex;
    private String pendingLineupTrainer;
    private Integer pendingLineupMessageId;

    public TelegramBot(String botName, String botToken) {
        super(botToken);
        this.botName = botName;
        this.botToken = botToken;
        controller = new Controller();
        lineupCreator = new LineupCreator();
    }

    @Override
    public String getBotUsername() {
        return this.botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = extractChatId(update);
        if (chatId == null) {
            return;
        }
        if (!allowedChatIds.contains(chatId)) {
            log.error("Unallowed user: {}", chatId);
            return;
        }
        String chatIdString = chatId.toString();
        try {
            if (update.hasCallbackQuery()) {
                handleCallbackQuery(chatIdString, update.getCallbackQuery());
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(chatIdString, update.getMessage().getText());
            } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
                handlePhotoMessage(chatIdString, update.getMessage().getPhoto());
            }
        } catch (RuntimeException | IOException | ParseException | URISyntaxException | TelegramApiException e) {
            log.error("Error processing update from chat {}", chatId, e);
            sendSimpleMsg(chatIdString, "Ein Fehler ist aufgetreten: " + e.getMessage());
        }
    }

    private Long extractChatId(Update update) {
        if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }
        return null;
    }

    private void handleCallbackQuery(String chatId, CallbackQuery callbackQuery) throws IOException, ParseException, URISyntaxException {
        if (callbackQuery == null) {
            return;
        }
        try {
            execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQuery.getId()).build());
        } catch (TelegramApiException e) {
            log.error("Could not answer callback query", e);
        }
        String lastCall = callbackQuery.getData();
        if ("createPreview".equals(lastCall)) {
            resetResultFlow();
            resetMatchdayPreview();
            startTeamSelection(chatId, "createPreview");
            return;
        }
        if ("createResult".equals(lastCall)) {
            resetResultFlow();
            resetMatchdayPreview();
            startTeamSelection(chatId, "createResult");
            return;
        }
        if ("createLineup".equals(lastCall)) {
            resetResultFlow();
            resetMatchdayPreview();
            startTeamSelection(chatId, "createLineup");
            return;
        }
        if ("cancel".equals(lastCall)) {
            resetResultFlow();
            resetMatchdayPreview();
            resetLineupFlow();
            sendStartMsg(chatId, "Ok!");
            return;
        }
        if (lastCall != null && lastCall.startsWith("lineup_role_") && "lineup_role".equals(pendingLineupAction)) {
            handleLineupRoleCallback(chatId, lastCall.substring("lineup_role_".length()));
            return;
        }
        if (lastCall == null) {
            return;
        }
        String[] paths = lastCall.split("_");
        if (paths.length < 2 || (!"createPreview".equals(paths[0]) && !"createResult".equals(paths[0]) && !"createLineup".equals(paths[0]))) {
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
            for (Object key : teamData.keySet()) {
                keyboardValues.add(new ImmutablePair<>(key.toString(), key.toString()));
            }
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
        for (Map<String, Object> g : bufferedGames) {
            Object dayObj = g.get("matchDay");
            Object compObj = g.get("competition");
            if (dayObj != null && matchDay.equals(dayObj.toString()) &&
                    (competition == null || (compObj != null && competition.equals(compObj.toString())))) {
                bufferedGame = g;
                break;
            }
        }
        if (bufferedGame == null) {
            sendStartMsg(chatId, "Spiel nicht gefunden. Bitte starte den Vorgang erneut.");
            return;
        }
        GameModel gameModel = new GameModel(new JSONObject(bufferedGame), teamQuery);
        gameModel.setMatchDay(getString(bufferedGame, "matchDay"));
        generateOrResolveClub(chatId, gameModel, lastCallFor(mode, teamQuery, matchKey), mode);
    }

    private void generateOrResolveClub(String chatId, GameModel gameModel, String lastCall, String mode) throws IOException, ParseException {
        ClubModel home = ClubSelector.getClubDetails(gameModel.getHomeTeam());
        ClubModel away = ClubSelector.getClubDetails(gameModel.getAwayTeam());
        if (home != null && away != null) {
            gameModel.setHomeTeam(home);
            gameModel.setAwayTeam(away);
            if ("createResult".equals(mode)) {
                startResultInput(chatId, gameModel);
            } else if ("createLineup".equals(mode)) {
                startLineupInput(chatId, gameModel);
            } else {
                startMatchdayPhotoRequest(chatId, gameModel);
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
        for (Map<String, Object> g : bufferedGames) {
            Object dayObj = g.get("matchDay");
            Object compObj = g.get("competition");
            if (dayObj != null && matchDay.equals(dayObj.toString()) &&
                    (competition == null || (compObj != null && competition.equals(compObj.toString())))) {
                bufferedGame = g;
                break;
            }
        }
        if (bufferedGame == null) {
            sendStartMsg(chatId, "Spiel nicht gefunden. Bitte starte den Vorgang erneut.");
            return;
        }
        GameModel gameModel = new GameModel(new JSONObject(bufferedGame), teamQuery);
        gameModel.setMatchDay(getString(bufferedGame, "matchDay"));
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

    private String getString(Map<String, Object> map, String key) {
        Object value = map != null ? map.get(key) : null;
        return value != null ? value.toString() : "";
    }

    private String getClubName(Object club) {
        if (club instanceof Map<?, ?> map) {
            Object name = map.get("clubName");
            return name != null ? name.toString() : "?";
        }
        return "?";
    }

    private void loadResultMatches(String chatId, String lastCall, String teamQuery) throws IOException {
        JavaType stringType = OBJECT_MAPPER.constructType(String.class);
        JavaType objectType = OBJECT_MAPPER.constructType(Object.class);
        JavaType gameType = OBJECT_MAPPER.getTypeFactory().constructMapType(HashMap.class, stringType, objectType);
        JavaType gameListType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, gameType);
        List<Map<String, Object>> result = OBJECT_MAPPER.readValue(new File("src/main/resources/templates/men-games.json"), gameListType);
        bufferedGames = new ArrayList<>();
        for (Map<String, Object> game : result) {
            String teamVal = game.containsKey("team") && game.get("team") != null ? game.get("team").toString() : null;
            if (!teamQuery.equals(teamVal)) {
                continue;
            }
            Map<String, Object> transformed = new HashMap<>();
            transformed.put("competition", game.get("competition"));
            Object matchDateObj = game.get("matchDate");
            String matchDate = matchDateObj != null ? matchDateObj.toString() : null;
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
        for (Map<String, Object> game : bufferedGames) {
            String matchDay = getString(game, "matchDay");
            String competition = getString(game, "competition");
            String matchKey = matchDay + "~" + competition;
            String homeName = getClubName(game.get("homeTeam"));
            String awayName = getClubName(game.get("awayTeam"));
            String gameInfo = matchDay + " | " + competition + ": " + homeName + " - " + awayName;
            keyboardValues.add(new ImmutablePair<>(gameInfo, matchKey));
        }
        sendMsg(chatId, "Wähle ein Spiel aus!", createKeyboard(1, lastCall, keyboardValues));
    }

    private void loadMatches(String chatId, String lastCall, String teamQuery) throws IOException, URISyntaxException {
        JavaType stringType = OBJECT_MAPPER.constructType(String.class);
        JavaType objectType = OBJECT_MAPPER.constructType(Object.class);
        JavaType gameType = OBJECT_MAPPER.getTypeFactory().constructMapType(HashMap.class, stringType, objectType);
        JavaType gameListType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, gameType);
        JavaType rootType = OBJECT_MAPPER.getTypeFactory().constructMapType(HashMap.class, stringType, gameListType);
        Map<String, List<Map<String, Object>>> result = OBJECT_MAPPER.readValue(new File("src/main/resources/templates/allMatches.json"), rootType);
        bufferedGames = result.get(teamQuery);
        if (bufferedGames == null) {
            Helper.updateNextMatchesFromFBDE();
            result = OBJECT_MAPPER.readValue(new File("src/main/resources/templates/allMatches.json"), rootType);
            bufferedGames = result.get(teamQuery);
        }
        if (bufferedGames == null) {
            sendStartMsg(chatId, "Keine Spiele für diese Mannschaft gefunden.");
            return;
        }
        List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
        for (Map<String, Object> game : bufferedGames) {
            String matchDay = getString(game, "matchDay");
            String competition = getString(game, "competition");
            String matchKey = matchDay + "~" + competition;
            String homeName = getClubName(game.get("homeTeam"));
            String awayName = getClubName(game.get("awayTeam"));
            String gameInfo = matchDay + " | " + competition + ": " + homeName + " - " + awayName;
            keyboardValues.add(new ImmutablePair<>(gameInfo, matchKey));
        }
        keyboardValues.add(new ImmutablePair<>("Liste aktualisieren", "update"));
        sendMsg(chatId, "Wähle ein Spiel aus!", createKeyboard(1, lastCall, keyboardValues));
    }

    public synchronized boolean getMatchdayFile(String chatId, GameModel gameModel, BufferedImage photo) throws IOException, ParseException {
        MatchdayCreator mc = new MatchdayCreator();
        String savePath = mc.createMatch(gameModel, photo);
        Path filePath = Paths.get("src/main/resources/save", savePath, "/Matchday.jpeg");
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(filePath.toFile()));
        sendPhoto.setChatId(chatId);
        try {
            execute(sendPhoto);
            return true;
        } catch (TelegramApiException e) {
            log.error("Could not send matchday preview", e);
            sendSimpleMsg(chatId, "Vorschau konnte nicht gesendet werden: " + e.getMessage());
            return false;
        }
    }

    private void startMatchdayPhotoRequest(String chatId, GameModel gameModel) {
        resetResultFlow();
        pendingMatchdayGame = gameModel;
        pendingMatchdayPhoto = null;
        pendingMatchdayAction = "photo";
        sendSimpleMsg(chatId, "Sende ein quadratisches Bild (1:1) für die obere Hälfte. Wenn du fertig bist, sende 'Fertig'.");
    }

    private void resetMatchdayPreview() {
        pendingMatchdayGame = null;
        pendingMatchdayPhoto = null;
        pendingMatchdayAction = null;
    }

    private void finalizeMatchdayPreview(String chatId) throws IOException, ParseException {
        if (pendingMatchdayGame == null || pendingMatchdayPhoto == null || !"photo".equals(pendingMatchdayAction)) {
            sendSimpleMsg(chatId, "Kein Bild vorhanden. Bitte starte den Vorgang erneut.");
            resetMatchdayPreview();
            return;
        }
        boolean sent = getMatchdayFile(chatId, pendingMatchdayGame, pendingMatchdayPhoto);
        resetMatchdayPreview();
        if (sent) {
            sendStartMsg(chatId, "Vorschau erstellt!");
        }
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
            log.error("Could not send message to chat {}", chatId, e);
        }
    }

    private InlineKeyboardMarkup createKeyboard(Integer perRow, String lastCall, List<ImmutablePair<String, String>> allButtons) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
        int counter = 0;
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
        row2.add(createInlineButton("Startaufstellung erstellen", "createLineup"));
        keyboard.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton("Spieltagsergebnis erstellen", "createResult"));
        keyboard.add(row3);
        
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createInlineButton("Abbruch", "cancel"));
        keyboard.add(row4);

        inlineKeyboardMarkup.setKeyboard(keyboard);
        sendMsg(chatId, msg, inlineKeyboardMarkup);
    }

    private void startResultInput(String chatId, GameModel gameModel) {
        this.resultCreator = new ResultCreator();
        this.resultGameModel = gameModel;
        this.resultHeadline = null;
        this.resultReport = null;
        this.pendingAction = "headline";
        sendSimpleMsg(chatId, "Schreibe deine Headline!");
    }

    private void startLineupInput(String chatId, GameModel gameModel) {
        resetResultFlow();
        resetMatchdayPreview();
        resetLineupFlow();
        this.pendingLineupGame = gameModel;
        try {
            this.pendingLineupPlayers = loadPlayersFromJson();
        } catch (IOException | ParseException e) {
            log.error("Could not load players.json", e);
            sendSimpleMsg(chatId, "Spielerliste konnte nicht geladen werden.");
            return;
        }
        this.pendingLineupIndex = 0;
        this.pendingLineupTrainer = null;
        this.pendingLineupAction = "lineup_role";
        askRoleForCurrentPlayer(chatId);
    }

    private List<PlayerModel> loadPlayersFromJson() throws IOException, ParseException {
        JSONObject data = (JSONObject) new JSONParser().parse(new InputStreamReader(new FileInputStream("src/main/resources/templates/players.json"), StandardCharsets.UTF_8));
        JSONArray arr = (JSONArray) data.get("players");
        List<PlayerModel> players = new ArrayList<>();
        if (arr == null) {
            return players;
        }
        for (Object o : arr) {
            JSONObject p = (JSONObject) o;
            PlayerModel player = new PlayerModel();
            player.setNumber(((Number) p.get("number")).intValue());
            player.setName(p.get("name") != null ? p.get("name").toString() : "");
            player.setRole("absent");
            player.setGoalkeeper(false);
            player.setCaptain(false);
            players.add(player);
        }
        return players;
    }

    private void askRoleForCurrentPlayer(String chatId) {
        if (pendingLineupPlayers == null || pendingLineupIndex >= pendingLineupPlayers.size()) {
            pendingLineupAction = "lineup_add";
            String doneText = "Alle Spieler abgefragt.\n\nWeiteren Spieler hinzufügen? Format: <Nummer>, <Name>, <Start/Bank>\nOder 'Fertig' zum Fortfahren.";
            if (pendingLineupMessageId != null) {
                editMessageText(chatId, pendingLineupMessageId, doneText, null);
            } else {
                sendSimpleMsg(chatId, doneText);
            }
            return;
        }
        PlayerModel p = pendingLineupPlayers.get(pendingLineupIndex);
        String text = String.format("Spieler %d/%d: %d %s", pendingLineupIndex + 1, pendingLineupPlayers.size(), p.getNumber(), p.getName());
        InlineKeyboardMarkup keyboard = createRoleKeyboard();
        if (pendingLineupMessageId != null) {
            editMessageText(chatId, pendingLineupMessageId, text, keyboard);
        } else {
            SendMessage message = SendMessage.builder().chatId(chatId).text(text).replyMarkup(keyboard).build();
            try {
                Message sent = execute(message);
                pendingLineupMessageId = sent.getMessageId();
            } catch (TelegramApiException e) {
                log.error("Could not send role selection message", e);
            }
        }
    }

    private InlineKeyboardMarkup createRoleKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createInlineButton("Start", "lineup_role_start"));
        row.add(createInlineButton("Bank", "lineup_role_bench"));
        row.add(createInlineButton("Abwesend", "lineup_role_absent"));
        keyboard.add(row);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    private void handleLineupRoleCallback(String chatId, String roleInput) {
        String role = normalizeRole(roleInput);
        if (role == null) {
            editMessageText(chatId, pendingLineupMessageId, "Ungültige Auswahl. Bitte erneut versuchen.", createRoleKeyboard());
            return;
        }
        PlayerModel current = pendingLineupPlayers.get(pendingLineupIndex);
        current.setRole(role);
        pendingLineupIndex++;
        askRoleForCurrentPlayer(chatId);
    }

    private void editMessageText(String chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        if (messageId == null) {
            sendSimpleMsg(chatId, text);
            return;
        }
        try {
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .replyMarkup(keyboard)
                    .build();
            execute(edit);
        } catch (TelegramApiException e) {
            log.error("Could not edit message", e);
            sendSimpleMsg(chatId, text);
        }
    }

    private void handleLineupInput(String chatId, String text) {
        if (text == null) {
            return;
        }
        String input = text.trim();
        switch (pendingLineupAction) {
            case "lineup_role" -> handleLineupRoleInput(chatId, input);
            case "lineup_add" -> handleLineupAddInput(chatId, input);
            case "lineup_summary" -> handleLineupSummaryInput(chatId, input);
            case "lineup_keepers" -> handleLineupKeepersInput(chatId, input);
            case "lineup_captain" -> handleLineupCaptainInput(chatId, input);
            case "lineup_trainer" -> handleLineupTrainerInput(chatId, input);
            default -> sendSimpleMsg(chatId, "Unbekannter Schritt im Aufstellungs-Flow.");
        }
    }

    private void handleLineupRoleInput(String chatId, String input) {
        String role = normalizeRole(input);
        if (role == null) {
            sendSimpleMsg(chatId, "Ungültige Eingabe. Antworte 'Start', 'Bank' oder 'Abwesend'.");
            return;
        }
        PlayerModel current = pendingLineupPlayers.get(pendingLineupIndex);
        current.setRole(role);
        pendingLineupIndex++;
        askRoleForCurrentPlayer(chatId);
    }

    private String normalizeRole(String input) {
        if ("start".equalsIgnoreCase(input) || "startelf".equalsIgnoreCase(input) || "start11".equalsIgnoreCase(input) || "elf".equalsIgnoreCase(input)) {
            return "start";
        }
        if ("bench".equalsIgnoreCase(input) || "bank".equalsIgnoreCase(input)) {
            return "bench";
        }
        if ("absent".equalsIgnoreCase(input) || "abwesend".equalsIgnoreCase(input) || "nicht".equalsIgnoreCase(input) || "none".equalsIgnoreCase(input)) {
            return "absent";
        }
        return null;
    }

    private void handleLineupAddInput(String chatId, String input) {
        if (isDone(input)) {
            if (hasSelectedPlayers()) {
                sendSimpleMsg(chatId, "Bitte wähle mindestens einen Spieler für die Aufstellung aus.");
                return;
            }
            sendSummary(chatId);
            return;
        }
        PlayerModel player = parseAddPlayerInput(input);
        if (player == null) {
            sendSimpleMsg(chatId, "Ungültiges Format. Beispiel: 25, Max Mustermann, Start\nOder 'Fertig' zum Fortfahren.");
            return;
        }
        pendingLineupPlayers.add(player);
        sendSimpleMsg(chatId, String.format("%s (%d) hinzugefügt. Nächsten Spieler senden oder 'Fertig'.", player.getName(), player.getNumber()));
    }

    private boolean isDone(String input) {
        return "fertig".equalsIgnoreCase(input) || "ok".equalsIgnoreCase(input) || "weiter".equalsIgnoreCase(input) || "done".equalsIgnoreCase(input);
    }

    private boolean hasSelectedPlayers() {
        return pendingLineupPlayers == null || pendingLineupPlayers.stream().noneMatch(p -> "start".equalsIgnoreCase(p.getRole()) || "bench".equalsIgnoreCase(p.getRole()));
    }

    private PlayerModel parseAddPlayerInput(String input) {
        String[] parts = input.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            int number = Integer.parseInt(parts[0].trim());
            if (number < 1) {
                return null;
            }
            String name = parts[1].trim();
            if (name.isBlank()) {
                return null;
            }
            String role = normalizeRole(parts[2].trim());
            if (role == null || "absent".equalsIgnoreCase(role)) {
                return null;
            }
            PlayerModel player = new PlayerModel();
            player.setNumber(number);
            player.setName(name);
            player.setRole(role);
            return player;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isKeeperFlag(String flag) {
        return "tw".equalsIgnoreCase(flag) || "torwart".equalsIgnoreCase(flag) || "keeper".equalsIgnoreCase(flag) || "goalkeeper".equalsIgnoreCase(flag);
    }

    private boolean isCaptainFlag(String flag) {
        return "c".equalsIgnoreCase(flag) || "captain".equalsIgnoreCase(flag) || "kapitän".equalsIgnoreCase(flag) || "k".equalsIgnoreCase(flag);
    }

    private void sendSummary(String chatId) {
        pendingLineupAction = "lineup_summary";
        String summary = buildSummaryMessage();
        sendSimpleMsg(chatId, summary);
        sendSimpleMsg(chatId, "Sende 'OK' zum Fortfahren oder kopiere die vorherige Nachricht, ändere Werte und sende sie zurück.\nFormat pro Zeile: Nummer;Name;Rolle (Start, Bank, Abwesend)\nTrainer am Ende: trainer;Name");
    }

    private String buildSummaryMessage() {
        StringBuilder sb = new StringBuilder();
        for (PlayerModel p : pendingLineupPlayers) {
            sb.append(p.getNumber()).append(";").append(p.getName()).append(";").append(formatRole(p.getRole())).append("\n");
        }
        String trainer = pendingLineupTrainer != null && !pendingLineupTrainer.isBlank() ? pendingLineupTrainer : "Toni Seidel";
        sb.append("trainer;").append(trainer);
        return sb.toString();
    }

    private String formatRole(String role) {
        if ("start".equalsIgnoreCase(role)) {
            return "Start";
        }
        if ("bench".equalsIgnoreCase(role)) {
            return "Bank";
        }
        if ("absent".equalsIgnoreCase(role)) {
            return "Abwesend";
        }
        return role;
    }

    private void handleLineupSummaryInput(String chatId, String input) {
        if (isConfirmation(input)) {
            if (hasSelectedPlayers()) {
                sendSimpleMsg(chatId, "Bitte wähle mindestens einen Spieler aus.");
                return;
            }
            pendingLineupAction = "lineup_keepers";
            sendKeepersPrompt(chatId);
            return;
        }
        List<PlayerModel> parsed = parseSummaryInput(input);
        if (parsed == null) {
            sendSimpleMsg(chatId, "Ungültiges Format. Bitte verwende das gezeigte Format.");
            return;
        }
        pendingLineupPlayers = parsed;
        sendSummary(chatId);
    }

    private boolean isConfirmation(String input) {
        return "ok".equalsIgnoreCase(input) || "ja".equalsIgnoreCase(input) || "fertig".equalsIgnoreCase(input) || "weiter".equalsIgnoreCase(input);
    }

    private List<PlayerModel> parseSummaryInput(String input) {
        List<PlayerModel> parsed = new ArrayList<>();
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("trainer")) {
                String[] parts = trimmed.split(";", 2);
                if (parts.length > 1) {
                    pendingLineupTrainer = parts[1].trim();
                } else {
                    pendingLineupTrainer = null;
                }
                continue;
            }
            String[] parts = trimmed.split(";");
            if (parts.length < 3) {
                return null;
            }
            try {
                int number = Integer.parseInt(parts[0].trim());
                if (number < 1) {
                    return null;
                }
                String name = parts[1].trim();
                String role = normalizeRole(parts[2].trim());
                if (role == null) {
                    return null;
                }
                PlayerModel player = new PlayerModel();
                player.setNumber(number);
                player.setName(name);
                player.setRole(role);
                if (parts.length >= 4 && isKeeperFlag(parts[3].trim())) {
                    player.setGoalkeeper(true);
                }
                if (parts.length >= 5 && isCaptainFlag(parts[4].trim())) {
                    player.setCaptain(true);
                }
                parsed.add(player);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return parsed;
    }

    private void sendKeepersPrompt(String chatId) {
        List<Integer> keepers = pendingLineupPlayers.stream()
                .filter(this::isSelected)
                .filter(PlayerModel::isGoalkeeper)
                .map(PlayerModel::getNumber)
                .toList();
        String current = keepers.isEmpty() ? "keine" : keepers.stream().map(String::valueOf).collect(Collectors.joining(", "));
        sendSimpleMsg(chatId, "Aktuelle Torwarte: " + current + ".\nGib alle Keeper-Nummern ein (Komma-getrennt) oder 'OK'.");
    }

    private boolean isSelected(PlayerModel p) {
        return "start".equalsIgnoreCase(p.getRole()) || "bench".equalsIgnoreCase(p.getRole());
    }

    private void handleLineupKeepersInput(String chatId, String input) {
        if (!isConfirmation(input)) {
            Set<Integer> numbers = parseNumberList(input);
            if (numbers == null) {
                sendSimpleMsg(chatId, "Ungültige Eingabe. Bitte gib Keeper-Nummern ein (Komma-getrennt) oder 'OK'.");
                return;
            }
            for (Integer number : numbers) {
                boolean found = pendingLineupPlayers.stream()
                        .filter(this::isSelected)
                        .anyMatch(p -> p.getNumber() == number);
                if (!found) {
                    sendSimpleMsg(chatId, "Nummer " + number + " ist nicht in der Aufstellung.");
                    return;
                }
            }
            pendingLineupPlayers.forEach(p -> {
                if (isSelected(p)) {
                    p.setGoalkeeper(numbers.contains(p.getNumber()));
                } else {
                    p.setGoalkeeper(false);
                }
            });
        }
        pendingLineupAction = "lineup_captain";
        sendCaptainPrompt(chatId);
    }

    private Set<Integer> parseNumberList(String input) {
        Set<Integer> numbers = new LinkedHashSet<>();
        if (input == null || input.isBlank()) {
            return numbers;
        }
        String[] parts = input.split(",");
        for (String part : parts) {
            try {
                int number = Integer.parseInt(part.trim());
                if (number < 1) {
                    return null;
                }
                numbers.add(number);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return numbers;
    }

    private void sendCaptainPrompt(String chatId) {
        Optional<PlayerModel> captain = pendingLineupPlayers.stream()
                .filter(this::isSelected)
                .filter(PlayerModel::isCaptain)
                .findFirst();
        String current = captain.map(p -> p.getNumber() + " " + p.getName()).orElse("keiner");
        sendSimpleMsg(chatId, "Aktueller Kapitän: " + current + ".\nGib die Kapitänsnummer ein oder 'OK'.");
    }

    private void handleLineupCaptainInput(String chatId, String input) {
        if (!isConfirmation(input)) {
            try {
                int number = Integer.parseInt(input.trim());
                boolean found = pendingLineupPlayers.stream()
                        .filter(this::isSelected)
                        .anyMatch(p -> p.getNumber() == number);
                if (!found) {
                    sendSimpleMsg(chatId, "Nummer " + number + " ist nicht in der Aufstellung.");
                    return;
                }
                pendingLineupPlayers.forEach(p -> p.setCaptain(false));
                pendingLineupPlayers.stream()
                        .filter(p -> p.getNumber() == number)
                        .findFirst()
                        .ifPresent(p -> p.setCaptain(true));
            } catch (NumberFormatException e) {
                sendSimpleMsg(chatId, "Ungültige Eingabe. Bitte gib die Kapitänsnummer ein oder 'OK'.");
                return;
            }
        }
        pendingLineupAction = "lineup_trainer";
        sendTrainerPrompt(chatId);
    }

    private void sendTrainerPrompt(String chatId) {
        String trainer = pendingLineupTrainer != null && !pendingLineupTrainer.isBlank() ? pendingLineupTrainer : "Toni Seidel";
        sendSimpleMsg(chatId, "Aktueller Trainer: " + trainer + ".\nRichtig? Sende 'ja' oder einen neuen Trainer.");
    }

    private void handleLineupTrainerInput(String chatId, String input) {
        String trainer = pendingLineupTrainer != null && !pendingLineupTrainer.isBlank() ? pendingLineupTrainer : "Toni Seidel";
        if (isConfirmation(input)) {
            generateLineupImage(chatId, trainer);
            return;
        }
        if (input.isBlank()) {
            sendSimpleMsg(chatId, "Bitte gib einen Trainer ein oder 'ja'.");
            return;
        }
        pendingLineupTrainer = input;
        sendTrainerPrompt(chatId);
    }

    private void generateLineupImage(String chatId, String trainer) {
        if (pendingLineupGame == null) {
            sendSimpleMsg(chatId, "Kein Spiel ausgewählt. Bitte starte erneut.");
            resetLineupFlow();
            return;
        }
        try {
            String playersJson = buildPlayersJson(pendingLineupPlayers);
            Map<String, Object> matchMap = new HashMap<>();
            matchMap.put("matchDate", pendingLineupGame.getSaveGameDate());
            matchMap.put("competition", pendingLineupGame.getCompetition());
            matchMap.put("savePath", pendingLineupGame.getSavePath());
            matchMap.put("team", pendingLineupGame.getTeam());
            ClubModel home = pendingLineupGame.getHomeTeam();
            ClubModel away = pendingLineupGame.getAwayTeam();
            if (home != null) {
                matchMap.put("homeClub", home.toJSON());
            }
            if (away != null) {
                matchMap.put("awayClub", away.toJSON());
            }
            JSONObject matchJson = new JSONObject(matchMap);

            String savePath = lineupCreator.createLineup(matchJson.toJSONString(), playersJson, trainer);
            if (savePath == null) {
                sendSimpleMsg(chatId, "Aufstellung konnte nicht erstellt werden.");
            } else {
                sendLineupImage(chatId, savePath);
                sendStartMsg(chatId, "Aufstellung erstellt!");
            }
            resetLineupFlow();
        } catch (ParseException | IOException e) {
            log.error("Could not create lineup in Telegram flow", e);
            sendSimpleMsg(chatId, "Fehler beim Erstellen der Aufstellung: " + e.getMessage());
        }
    }

    private String buildPlayersJson(List<PlayerModel> players) throws IOException {
        List<Map<String, Object>> arr = new ArrayList<>();
        for (PlayerModel player : players) {
            Map<String, Object> obj = new HashMap<>();
            obj.put("number", player.getNumber());
            obj.put("name", player.getName());
            obj.put("role", player.getRole());
            obj.put("goalkeeper", player.isGoalkeeper());
            obj.put("captain", player.isCaptain());
            arr.add(obj);
        }
        return OBJECT_MAPPER.writeValueAsString(arr);
    }

    private void sendLineupImage(String chatId, String fileDir) {
        Path filePath = Paths.get("src/main/resources/save", fileDir, "Lineup.jpeg");
        java.io.File file = filePath.toFile();
        if (!file.exists()) {
            sendSimpleMsg(chatId, "Aufstellung erstellt, aber Bild konnte nicht gefunden werden.");
            return;
        }
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(file));
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Could not send lineup image", e);
            sendSimpleMsg(chatId, "Aufstellung erstellt, aber Bild konnte nicht gesendet werden.");
        }
    }

    private void resetLineupFlow() {
        pendingLineupGame = null;
        pendingLineupAction = null;
        pendingLineupPlayers = null;
        pendingLineupIndex = 0;
        pendingLineupTrainer = null;
        pendingLineupMessageId = null;
    }

    private void handleTextMessage(String chatId, String text) throws IOException, ParseException {
        if ("abbruch".equalsIgnoreCase(text) || "abbrechen".equalsIgnoreCase(text)) {
            resetResultFlow();
            resetMatchdayPreview();
            resetLineupFlow();
            sendStartMsg(chatId, "Abbruch.");
            return;
        }
        if ("photo".equals(pendingMatchdayAction) && "fertig".equalsIgnoreCase(text)) {
            finalizeMatchdayPreview(chatId);
            return;
        }
        if (pendingLineupAction != null && pendingLineupAction.startsWith("lineup")) {
            handleLineupInput(chatId, text);
            return;
        }
        if (pendingMatchdayAction != null) {
            sendSimpleMsg(chatId, "Bitte sende ein Bild oder 'Fertig', um fortzufahren.");
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
        if ("photo".equals(pendingMatchdayAction)) {
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
                pendingMatchdayPhoto = image;
                sendSimpleMsg(chatId, "Bild erhalten. Sende 'Fertig', um die Vorschau zu erstellen, oder ein neues Bild zum Ersetzen.");
            }
            return;
        }

        if (!"photos".equals(pendingAction) || resultCreator == null) {
            sendSimpleMsg(chatId, "Bitte starte zuerst einen Prozess.");
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
        sendSimpleMsg(chatId, caption);
        Path bilderDir = Paths.get("src/main/resources/save", fileDir, "Bilder");
        java.io.File dir = bilderDir.toFile();
        
        if (!dir.exists() || !dir.isDirectory()) {
            log.error("Bilder-Ordner nicht gefunden unter: {}", bilderDir);
            sendSimpleMsg(chatId, "Bericht gesendet, aber der Ordner mit den Bildern konnte nicht gefunden werden.");
            return;
        }
        
        java.io.File[] imageFiles = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
        });
        
        if (imageFiles == null || imageFiles.length == 0) {
            sendSimpleMsg(chatId, "Bericht gesendet, aber es wurden keine Bilder im Ordner gefunden.");
            return;
        }
        
        Arrays.sort(imageFiles);
        
        for (java.io.File imageFile : imageFiles) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(imageFile));
            try {
                execute(sendPhoto);
                log.info("Bild erfolgreich gesendet: {}", imageFile.getName());
            } catch (TelegramApiException e) {
              log.error("Fehler beim Senden des Bildes: {}", imageFile.getName(), e);
                sendSimpleMsg(chatId, "Fehler beim Senden des Bildes: " + imageFile.getName());
            }
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
            log.error("Could not send simple message to chat {}", chatId, e);
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
