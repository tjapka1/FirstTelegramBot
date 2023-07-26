package io.project.SpringDemoBot.service;

import io.project.SpringDemoBot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()){
                String messageText = update.getMessage().getText();
                long chartId = update.getMessage().getChatId();
                switch (messageText){
                    case "/start":
                            startCommandReceived(chartId, update.getMessage().getChat().getFirstName());
                            break;
                    case "/phone":
                            phoneCommandReceived(chartId, update.getMessage().getChat().getFirstName());
                            break;
                    default:
                            nothingCommand(chartId, update.getMessage().getChat().getFirstName());
                            break;
                }
            }
    }

    private void phoneCommandReceived(long chartId, String name) {
        String answer = "Tjapka number is +4917550383127";
        log.info("Phone replied to user: " + name);
        sendMessage(chartId, answer);
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Hi, " + name + " nice to meet you!";
        log.info("Replied to user: " + name);
        sendMessage(chatId, answer);
    }
    private void nothingCommand(long chatId, String name){
        sendMessage(chatId, "Sorry, command was not recognized");
        log.info("Command is not exist " + name);
    }
    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        }catch (TelegramApiException e){
            log.error("Error occurred: " + e.getMessage());
        }
    }
}
