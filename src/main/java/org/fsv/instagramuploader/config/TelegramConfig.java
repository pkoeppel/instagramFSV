package org.fsv.instagramuploader.config;

import org.fsv.instagramuploader.bot.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramConfig {
 @Bean
 public TelegramBot telegramBot(@Value("${bot.name}") String botName, @Value("${bot.token}") String botToken) {
	TelegramBot bot = new TelegramBot(botName, botToken);
	try {
	 TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
	 telegramBotsApi.registerBot(bot);
	} catch (TelegramApiException e) {
	 throw new RuntimeException(e);
	}
	return bot;
 }
 
}