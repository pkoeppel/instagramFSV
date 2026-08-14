package org.fsv.instagramuploader.config;

import org.fsv.instagramuploader.bot.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramConfig {
 private static final Logger logger = LoggerFactory.getLogger(TelegramConfig.class);

 @Bean
 public TelegramBot telegramBot(@Value("${bot.name}") String botName, @Value("${bot.token}") String botToken) {
	TelegramBot bot = new TelegramBot(botName, botToken);
	if (botToken == null || botToken.isBlank()) {
	 logger.warn("No bot.token configured; Telegram bot is disabled");
	 return bot;
	}
	try {
	 TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
	 telegramBotsApi.registerBot(bot);
	 logger.info("Telegram bot '{}' registered successfully", botName);
	} catch (TelegramApiException e) {
	 logger.error("Could not register Telegram bot '{}'; continuing without bot", botName, e);
	}
	return bot;
 }
}