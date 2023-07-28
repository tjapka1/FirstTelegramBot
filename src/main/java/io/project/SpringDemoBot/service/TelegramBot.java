package io.project.SpringDemoBot.service;

import com.vdurmont.emoji.EmojiParser;
import io.project.SpringDemoBot.config.BotConfig;
import io.project.SpringDemoBot.model.User;
import lombok.extern.slf4j.Slf4j;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import io.project.SpringDemoBot.repository.UserRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//@Data
@Component
@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    final BotConfig config;
    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";
    static final String ERROR_TEXT = "Error occurred: ";
    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities. \n\n" +
            "You can execute commands from the main menu on the left or by typing a command: \n\n" +
            "Type /start to see a welcome message\n\n"+
            "Type /phone to see my mobile number\n\n"+
            "Type /help to see this message again"
            ;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List <BotCommand> listOfCommands=new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/phone", "See you Tjapka number"));
        listOfCommands.add(new BotCommand("/mydata", "get your data store"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }catch (TelegramApiException e){
            log.error("Error setting bots command list: " + e.getMessage());
        }
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


                List<Long> botOwners=List.of(
                        config.getOwnerId()
                        //,1375385562l
                );

                if (messageText.contains("/send") && botOwners.contains(chartId)){
                    var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                    var users = userRepository.findAll();
                    for (User user : users){
                        sendMessage(user.getChartId(), textToSend);
                    }
                }else {
                    switch (messageText){
                    case "/start":
                            registerUser(update.getMessage());
                            startCommandReceived(chartId, update.getMessage().getChat().getFirstName());
                            break;
                    case "/phone":
                            phoneCommandReceived(chartId, update.getMessage().getChat().getFirstName());
                            break;
                    case "/help":
                        helpCommandReceived(chartId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/register":
                        register(chartId);
                        break;
                    default:
                            nothingCommand(chartId, update.getMessage().getChat().getFirstName());
                            break;
                }
                }
            } else if (update.hasCallbackQuery()) {
                String callBackData = update.getCallbackQuery().getData();
                long messageID = update.getCallbackQuery().getMessage().getMessageId();
                long chatID = update.getCallbackQuery().getMessage().getChatId();

                if (callBackData.equals(YES_BUTTON)){
                    String text = "You pressed YES Button";
                    executeMessageText(messageID, chatID, text);
                }
                else if (callBackData.equals(NO_BUTTON)){
                    String text = "You pressed NO Button";
                    executeMessageText(messageID, chatID, text);
                }
            }
    }

    private void executeMessageText(Long messageID, long chatID, String text) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatID));
        message.setText(text);
        message.setMessageId(Math.toIntExact(messageID));
        toSendMassage(message);
    }

    private void register(long chartId) {
        SendMessage message = prepareAndSendMessage(chartId, "Do you really want to register?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        toSendMassage(message);
    }
    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()){
            var chatId=msg.getChatId();
            var chat = msg.getChat();

            User user = User.builder()
                    .chartId(chatId)
                    .firstName(chat.getFirstName())
                    .lastName(chat.getLastName())
                    .userName(chat.getUserName())
                    .registeredAt(new Timestamp(System.currentTimeMillis()))
                    .build();

            userRepository.save(user);
            log.info("User saved: " + user);
        }
    }
    private void startCommandReceived(long chatId, String name) {
        String emoji= EmojiParser.parseToUnicode(" :grinning:");
        String answer = "Hi, " + name + " nice to meet you!" + emoji;
        log.info("Replied to user: " + name);
        sendMessage(chatId, answer, "/start");
    }
    private void phoneCommandReceived(long chartId, String name) {
        String answer = "Tjapka number is +4917550383127";
        log.info("Phone replied to user: " + name);
        sendMessage(chartId, answer, "/phone");
    }
    private void helpCommandReceived(long chartId, String name) {
        log.info("Join to help " + name);
        sendMessage(chartId, HELP_TEXT);
    }
    private void nothingCommand(long chatId, String name){
        sendMessage(chatId, "Sorry, command was not recognized", null);
        log.info("Command is not exist " + name);
    }

    private static SendMessage prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        return message;
    }
    private void sendMessage(long chatId, String textToSend){
        SendMessage message = prepareAndSendMessage(chatId, textToSend);
        toSendMassage(message);

    }
    private void sendMessage(long chatId, String textToSend, String btnFunks){
        SendMessage message = prepareAndSendMessage(chatId, textToSend);

        if (btnFunks != null) {
            ReplyKeyboardMarkup keyboardMarkup = changeBtnKeyboardFunction(btnFunks);
            message.setReplyMarkup(keyboardMarkup);
        }
        toSendMassage(message);
    }

    private void toSendMassage(SendMessage message) {
        try {
            execute(message);
        }catch (TelegramApiException e){
            log.error(ERROR_TEXT + e.getMessage());
        }
    }
    private void toSendMassage(EditMessageText message) {
        try {
            execute(message);
        }catch (TelegramApiException e){
            log.error(ERROR_TEXT + e.getMessage());
        }
    }


    private static ReplyKeyboardMarkup changeBtnKeyboardFunction(String btnFunks) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows;
        switch (btnFunks){
            case "/start":
                keyboardRows=startBtnMenu();
                break;
            case "/phone":
                keyboardRows= phoneBtnMenu();
                break;
            case "/help":
                keyboardRows=helpBtnMenu();
                break;

            default:
                keyboardRows=null;
                break;
        }

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }
    private static List<KeyboardRow> startBtnMenu() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add("/Weather");
        row.add("/get Random Joke");
        row.add("/help");

        keyboardRows.add(row);
        row = new KeyboardRow();

        row.add("/register");
        row.add("/Check my data");
        row.add("/delete my data");

        keyboardRows.add(row);
        return keyboardRows;
    }
    private static List<KeyboardRow> phoneBtnMenu() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add("/call");
        row.add("/not call");

        keyboardRows.add(row);
        return keyboardRows;
    }
    private static List<KeyboardRow> helpBtnMenu() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add("/its nice App Help");
        row.add("/not nice App Help");

        keyboardRows.add(row);
        return keyboardRows;
    }
}
