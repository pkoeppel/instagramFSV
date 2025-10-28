package org.fsv.instagramuploader.bot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.fsv.instagramuploader.Controller;
import org.fsv.instagramuploader.men.MatchdayCreator;
import org.fsv.instagramuploader.model.GameModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
 
 private final String botName;
 private final Controller controller;
 private final List<Long> allowedChatIds = List.of(5047912799L);
 private JSONArray bufferedGames;
 private JSONObject bufferedGame;
 
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
	if (update.hasMessage() && update.getMessage().hasText()) {
	 Message message = update.getMessage();
	 Long chatId = message.getChatId();
	 String text = message.getText();
	 if (allowedChatIds.contains(chatId)) {
		sendStartMsg(chatId.toString(), text);
	 } else {
		log.error("Unallowed user: {}", chatId.toString());
	 }
	} else if (update.hasCallbackQuery()) {
	 String callbackData = update.getCallbackQuery().getData();
	 Long chatId = update.getCallbackQuery().getMessage().getChatId();
	 if (allowedChatIds.contains(chatId)) {
		try {
		 handleCallbackQuery(chatId.toString(), callbackData);
		} catch (IOException e) {
		 throw new RuntimeException(e);
		} catch (ParseException e) {
		 throw new RuntimeException(e);
		}
	 }else {
		log.error("Unallowed user: {}", chatId.toString());
	 }
	}
 }
 
 private void handleCallbackQuery(String chatId, String lastCall) throws IOException, ParseException {
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
		String teamQuery = paths[1];
		try {
		 InputStreamReader reader = new InputStreamReader(new FileInputStream("src/main/resources/templates/allMatches.json"), StandardCharsets.UTF_8);
		 JSONObject result = (JSONObject) new JSONParser().parse(reader);
		 bufferedGames = (JSONArray) result.get(teamQuery);
		 List<ImmutablePair<String, String>> keyboardValues = new ArrayList<>();
		 for (Object teamGame : bufferedGames) {
			JSONObject game = (JSONObject) teamGame;
			JSONObject home = (JSONObject) game.get("homeTeam");
			JSONObject away = (JSONObject) game.get("awayTeam");
			String gameId = game.get("id").toString();
			String gameInfo = gameId + " - " + home.get("clubName") + " VS " + away.get("clubName");
			keyboardValues.add(new ImmutablePair<>(gameInfo, gameId));
		 }
		 sendMsg(chatId, "Wähle ein Spiel aus!", createKeyboard(1, lastCall, keyboardValues));
		} catch (IOException | ParseException e) {
		 throw new RuntimeException(e);
		}
	 } else if  (paths.length == 3) {
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
		 keyboardValues.add(new ImmutablePair<>("Nein", ""));
		 sendMsg(chatId, "Ist Spieltag " + matchday + " richtig?", createKeyboard(2, lastCall, keyboardValues));
		} catch (IOException | ParseException e) {
		 throw new RuntimeException(e);
		}
	 } else if  (paths.length == 4) {
		String teamQuery = paths[1];
		String gameId = paths[2];
		String matchday = paths[3];
		if (!Objects.equals(matchday, "")){
		 GameModel gameModel = new GameModel(bufferedGame, teamQuery);
		 MatchdayCreator mc = new  MatchdayCreator();
		 mc.createMatch(gameModel);
		 sendStartMsg(chatId, "Erstellt");
		}
	 }
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
 
 public synchronized void setButtons(SendMessage message) {
	// Create a keyboard
	
 }
 
}
