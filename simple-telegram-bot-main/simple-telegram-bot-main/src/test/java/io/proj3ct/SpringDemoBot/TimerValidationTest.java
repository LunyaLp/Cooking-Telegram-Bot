package io.proj3ct.SpringDemoBot;

import io.proj3ct.SpringDemoBot.config.BotConfig;
import io.proj3ct.SpringDemoBot.service.TelegramBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TimerValidationTest {

    private TelegramBot bot;
    private BotConfig mockConfig;

    @BeforeEach
    public void setUp() {
        mockConfig = mock(BotConfig.class);
        when(mockConfig.getBotName()).thenReturn("TestBot");
        when(mockConfig.getToken()).thenReturn("test-token");
        when(mockConfig.getOwnerId()).thenReturn(12345L);

        bot = Mockito.spy(new TelegramBot(mockConfig));

        try {
            Message mockMessage = mock(Message.class);
            doReturn(mockMessage).when(bot).execute(any(SendMessage.class));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testHandleTimerCommandInitialPrompt() throws TelegramApiException {
        bot.handleTimerCommand(12345L, "/timer 5");

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("Введите время в минутах"));
    }



    @Test
    public void testInvalidTimerInput() throws TelegramApiException {
        // Act
        bot.handleTimerCommand(12345L, "/timer 0");

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("от 1 до 60"));
    }
}