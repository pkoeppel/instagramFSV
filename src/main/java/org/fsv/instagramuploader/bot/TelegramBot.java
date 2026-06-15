package org.fsv.instagramuploader.bot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.fsv.instagramuploader.ClubSelector;
import org.fsv.instagramuploader.Controller;
import org.fsv.instagramuploader.Helper;
import org.fsv.instagramuploader.men.MatchdayCreator;
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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
 
 private final String botName;
 private final Controller controller;
 private final List<Long> allowedChatIds = List.of(5047912799L);
 private JSONArray bufferedGames;
 private JSONObject bufferedGame;
 private String bufferedCall = null;
 private String holdLastCall;
 private List<String> homeScorer = new ArrayList<>();
 private List<String> awayScorer = new ArrayList<>();
 private String headline;
 private String report;
 
 public TelegramBot(String botName, String botToken) {
	super(botToken);
	this.botName = botName;
	controller = new Controller();
 }
 
 @Override
 public String getBotUsername() {
	return this.botName;
 }
 
 @Override
 public void onUpdateReceived(Update update) {
	try{
	 if (update.hasMessage() && update.getMessage().hasText()) {
		Message message = update.getMessage();
		Long chatId = message.getChatId();
		String text = message.getText();
		if (allowedChatIds.contains(chatId)) {
		 if (holdLastCall != null) {
			String nextCall = holdLastCall;
			String[] holding = holdLastCall.split("_");
		 	if (holding.length == 4) {
			 holding[3] = text;
			 nextCall = String.join("_", holding);
		 	}
			 if (holding.length == 5) {
				String[] scorer = text.split("\n");
				for (String s : scorer) {
				 String[] score = s.split(":");
				 homeScorer.add(score[1].trim() + " " + score[0].trim() + "'");
				}
			 }
			if (holding.length == 6) {
			 String[] scorer = text.split("\n");
			 for (String s : scorer) {
				String[] score = s.split(":");
				awayScorer.add(score[0].trim() + "' " + score[1].trim());
			 }
			}
			if (holding.length == 7) {
			 headline = text + "\uD83D\uDD34⚪";
			}
			if (holding.length == 8) {
			 report = text;
			}
		 	holdLastCall = null;
		 	handleCallbackQuery(chatId.toString(), nextCall);
		 } else {
			sendStartMsg(chatId.toString(), text);
		 }
		} else {
		 log.error("Unallowed user: {}", chatId.toString());
		}
	 } else if (update.hasCallbackQuery()) {
		String callbackData = update.getCallbackQuery().getData();
		Long chatId = update.getCallbackQuery().getMessage().getChatId();
		if (allowedChatIds.contains(chatId)) {
			handleCallbackQuery(chatId.toString(), callbackData);
		}else {
		 log.error("Unallowed user: {}", chatId.toString());
		}
	 } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
		Message message = update.getMessage();
		Long chatId = message.getChatId();
		if (allowedChatIds.contains(chatId)) {
		 List<PhotoSize> pics = message.getPhoto();
		}
	 }
	} catch (ParseException | URISyntaxException | IOException e) {
	 throw new RuntimeException(e);
 	}
 }
 
 private void handleCallbackQuery(String chatId, String lastCall) throws IOException, ParseException, URISyntaxException {
	switch (lastCall) {
	 case "createPreview":
	 case "createResult":
		JSONObject teamData = controller.getTeamData().getBody();
		List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
		if (teamData != null) {
		 teamData.forEach((key, value) -> {
			keyboardValues.add(new ImmutablePair<>(key.toString(), key.toString()));
		 });
		}
		sendMsg(chatId, "Wähle eine Mannschaft!", createKeyboard(3, lastCall, keyboardValues));
		break;
	 case "change_settings":
		sendStartMsg(chatId, "Was möchtest du ändern?");
		break;
	 case "edit_teams":
		sendStartMsg(chatId, "Um welches Team geht es?");
		break;
	 case "cancel":
		sendStartMsg(chatId, "Ok!");
		break;
	}
	String[] paths = lastCall.split("_");
	if (paths.length > 0) {
	 if (paths.length == 2) {
		if (paths[0].equals("createPreview")) {
		 loadMatches(chatId, lastCall, paths[1]);
		} else if (paths[0].equals("createResult")) {
		 loadSavedMatches(chatId, lastCall, paths[1]);
		}
	 } else if  (paths.length == 3) {
		if (paths[2].equals("update")) {
		 Helper.updateNextMatchesFromFBDE();
		 String backStepCall = lastCall.replaceAll("_update", "");
		 loadMatches(chatId, backStepCall, paths[1]);
		} else if (paths[2].startsWith("result")) {
		 sendSimpleMsg(chatId, "Wie war das Ergebnis?");
		 holdLastCall = lastCall + "_0:0";
		} else {
		String teamQuery = paths[1];
		String gameId = paths[2];
		JSONObject teamData;
		try {
		 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/teamInfo.json"), StandardCharsets.UTF_8);
		 JSONObject obj = (JSONObject) new JSONParser().parse(reader);
		 teamData = (JSONObject) obj.get(teamQuery);
		 
		 bufferedGames.forEach(game -> {
			JSONObject g = (JSONObject) game;
			if (gameId.equals(g.get("id").toString())) {
			 bufferedGame = g;
			}
		 });
		 String comp = bufferedGame.get("competition").toString();
		 String matchday;
		 if (comp.contains("klasse") || comp.contains("liga")) {
			matchday = teamData.get("lastLeagueMatchday").toString();
		 } else  {
			matchday = teamData.get("lastCupMatchday").toString();
		 }
		 List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
		 keyboardValues.add(new ImmutablePair<>("Ja", matchday));
		 keyboardValues.add(new ImmutablePair<>("Nein", "0"));
		 sendMsg(chatId, "Ist Spieltag " + matchday + " richtig?", createKeyboard(2, lastCall, keyboardValues));
		} catch (IOException | ParseException e) {
		 throw new RuntimeException(e);
		}
		}
	 } else if  (paths.length == 4) {
		String teamQuery = paths[1];
		if (paths[2].startsWith("result")) {
		 String[] ergebnis = paths[3].split(":");
		 if (ergebnis[0].equals("0")) {
			holdLastCall = lastCall + "_" + ergebnis[0] + "_" + ergebnis[1];
			sendSimpleMsg(chatId, "Wer hat für Auswärtsteam getroffen (min:Spieler)?");
		 } else {
			holdLastCall = lastCall + "_" + ergebnis[0];
			sendSimpleMsg(chatId, "Wer hat für Heimteam getroffen (min:Spieler)?");
		 }
		} else {
		String matchday = paths[3];
		if (matchday.equals("0")) {
		 List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
		 holdLastCall = lastCall;
		 sendMsg(chatId, "Welcher Spieltag?", createKeyboard(1, lastCall, keyboardValues));
		} else {
		 GameModel gameModel = new GameModel(bufferedGame, teamQuery);
		 gameModel.setMatchDay(matchday);
		 ClubModel home = ClubSelector.getClubDetails(gameModel.getHomeTeam());
		 ClubModel away = ClubSelector.getClubDetails(gameModel.getAwayTeam());
		 if (home == null || away == null) {
			InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/clubs.json"), StandardCharsets.UTF_8);
			JSONObject obj = (JSONObject) new JSONParser().parse(reader);
			List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
			for (Object key : obj.keySet()) {
			 keyboardValues.add(new ImmutablePair<>(key.toString(), key.toString()));
			}
			sendMsg(chatId, "Unbekanntes Gegnerteam. Bitte wähle das entsprechende Team aus der Datenbank!", createKeyboard(1, lastCall, keyboardValues));
		 } else {
			getMatchdayFile(chatId, gameModel);
		 }
		}
		}
	 } else if  (paths.length == 5) {
		String teamQuery = paths[1];
		if (paths[2].startsWith("result")) {
		 String[] ergebnis = paths[3].split(":");
		 if (ergebnis[1].equals("0")) {
			holdLastCall = lastCall + "_" + ergebnis[1] + "_headline";
			sendSimpleMsg(chatId, "Schreibe deine Headline!");
		 } else {
			holdLastCall = lastCall + "_" + ergebnis[1];
			sendSimpleMsg(chatId, "Wer hat für Auswärtsteam getroffen (min:Spieler)?");
		 }
		}
		String matchday = paths[3];
		if (!Objects.equals(matchday, "")){
		 GameModel gameModel = new GameModel(bufferedGame, teamQuery);
		 gameModel.setMatchDay(matchday);
		 ClubModel home = ClubSelector.getClubDetails(gameModel.getHomeTeam());
		 ClubModel away = ClubSelector.getClubDetails(gameModel.getAwayTeam());
		 if (home == null) {
			home = ClubSelector.searchClubDetails(paths[4]);
			if (home != null) {
			 home.setClubStats(gameModel.getHomeTeam().getClubStats());
			 gameModel.setHomeTeam(home);
			}
		 } else if (away == null) {
			away = ClubSelector.searchClubDetails(paths[4]);
			if (away != null) {
			 away.setClubStats(gameModel.getAwayTeam().getClubStats());
			 gameModel.setAwayTeam(away);
			}
		 }
		 getMatchdayFile(chatId, gameModel);
		}
	 }
	 else if (paths.length == 6) {
		holdLastCall = lastCall + "_headline";
		sendSimpleMsg(chatId, "Schreibe deine Headline!");
	 }
	 else if (paths.length == 7) {
		holdLastCall = lastCall + "_report";
		sendSimpleMsg(chatId, "Schreibe deinen Bericht!");
	 }
	 else if (paths.length == 8) {
		holdLastCall = lastCall + "_pictures";
		sendSimpleMsg(chatId, "Schicke mir die Bilder (Verhältnis 2:3)");
	 }
	}
 }
 
 private void sendSimpleMsg(String chatId, String s) {
	SendMessage sendMessage = new SendMessage();
	sendMessage.setParseMode(ParseMode.MARKDOWN);
	sendMessage.setChatId(chatId);
	sendMessage.setText(s);
	try {
	 execute(sendMessage);
	} catch (TelegramApiException e) {
	 throw new RuntimeException(e);
	}
 }
 
 private void loadSavedMatches(String chatId, String lastCall, String teamQuery) {
	try {
	 String file = "src/main/resources/templates/men-games.json";
	 if (!teamQuery.equals("1") && !teamQuery.equals("2")) {
	 	file = "src/main/resources/templates/youth-games.json";
	 }
	 InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
	 JSONArray arr = (JSONArray) new JSONParser().parse(reader);
	 bufferedGames = arr;
	 List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
	 for (int index = 0; index < bufferedGames.size(); index++) {
		Object teamGame = bufferedGames.get(index);
		JSONObject game = (JSONObject) teamGame;
		JSONObject home = (JSONObject) game.get("homeClub");
		JSONObject away = (JSONObject) game.get("awayClub");
		String gameDate = game.get("matchDate").toString();
		String gameInfo = gameDate + " - " + home.get("clubName") + " VS " + away.get("clubName");
		keyboardValues.add(new ImmutablePair<>(gameInfo, "result" + index));
	 }
	 sendMsg(chatId, "Wähle ein Spiel aus!", createKeyboard(1, lastCall, keyboardValues));
	} catch (IOException | ParseException e) {
	 throw new RuntimeException(e);
	}
 }
 
 private void loadMatches(String chatId, String lastCall, String teamQuery) {
	try {
	 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/allMatches.json"), StandardCharsets.UTF_8);
	 JSONObject result = (JSONObject) new JSONParser().parse(reader);
	 bufferedGames = (JSONArray) result.get(teamQuery);
	 if (bufferedGames == null) {
		Helper.updateNextMatchesFromFBDE();
		reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/allMatches.json"), StandardCharsets.UTF_8);
		result = (JSONObject) new JSONParser().parse(reader);
		bufferedGames = (JSONArray) result.get(teamQuery);
	 }
	 List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
	 for (Object teamGame : bufferedGames) {
		JSONObject game = (JSONObject) teamGame;
		JSONObject home = (JSONObject) game.get("homeTeam");
		JSONObject away = (JSONObject) game.get("awayTeam");
		String gameId = game.get("id").toString();
		String gameInfo = gameId + " - " + home.get("clubName") + " VS " + away.get("clubName");
		keyboardValues.add(new ImmutablePair<>(gameInfo, gameId));
	 }
	 keyboardValues.add(new ImmutablePair<>("Liste aktualisieren", "update"));
	 sendMsg(chatId, "Wähle ein Spiel aus!", createKeyboard(1, lastCall, keyboardValues));
	} catch (IOException | ParseException | URISyntaxException e) {
	 throw new RuntimeException(e);
	}
 }
 
 public synchronized void getMatchdayFile(String chatId, GameModel gameModel) throws IOException, ParseException {
	MatchdayCreator mc = new  MatchdayCreator();
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
 
 private InlineKeyboardMarkup createKeyboard(Integer perRow, String lastCall, List<ImmutablePair<String, String>> allButtons){
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
	SendMessage sendMessage = new SendMessage();
	sendMessage.setParseMode(ParseMode.MARKDOWN);
	sendMessage.setChatId(chatId);
	sendMessage.setText(msg);
	try {
	 InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
	 List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
	 
	 List<InlineKeyboardButton> keyboardRow1 = new ArrayList<>();
	 keyboardRow1.add(createInlineButton("Spieltagsvorschau erstellen", "createPreview"));
	 keyboardRow1.add(createInlineButton("Spieltagsergebnis erstellen", "createResult"));
	 
	 List<InlineKeyboardButton> keyboardRow2 = new ArrayList<>();
	 keyboardRow2.add(createInlineButton("Einstellungen ändern", "change_settings"));
	 keyboardRow2.add(createInlineButton("Teams bearbeiten", "edit_teams"));
	 
	 List<InlineKeyboardButton> keyboardRow3 = new ArrayList<>();
	 keyboardRow3.add(createInlineButton("Abbruch", "cancel"));
	 
	 keyboard.add(keyboardRow1);
	 keyboard.add(keyboardRow2);
	 keyboard.add(keyboardRow3);
	 // and assign this list to our keyboard
	 inlineKeyboardMarkup.setKeyboard(keyboard);
	 sendMessage.setReplyMarkup(inlineKeyboardMarkup);
	 execute(sendMessage);
	} catch (TelegramApiException e) {
	 throw new RuntimeException(e);
	}
 }
 
 private InlineKeyboardButton createInlineButton(String text, String callbackData) {
	InlineKeyboardButton button = new InlineKeyboardButton(text);
	button.setCallbackData(callbackData);
	return button;
 }
}
