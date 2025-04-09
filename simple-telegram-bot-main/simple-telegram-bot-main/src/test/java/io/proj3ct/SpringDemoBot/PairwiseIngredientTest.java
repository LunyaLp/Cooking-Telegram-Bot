package io.proj3ct.SpringDemoBot;

import io.proj3ct.SpringDemoBot.config.BotConfig;
import io.proj3ct.SpringDemoBot.service.TelegramBot;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class PairwiseIngredientTest {
    private final TelegramBot bot = new TelegramBot(mockBotConfig());

    private BotConfig mockBotConfig() {
        BotConfig botConfig = mock(BotConfig.class);
        Mockito.when(botConfig.getBotName()).thenReturn("MockBotName");
        Mockito.when(botConfig.getToken()).thenReturn("MockToken");
        Mockito.when(botConfig.getOwnerId()).thenReturn(123456L);
        return botConfig;
    }

    private static Stream<Arguments> ingredientCombinations() {
        return Stream.of(
                Arguments.of(List.of("Яйца", "Соль"), "Омлет с солью"),
                Arguments.of(List.of("Яйца", "Колбаса"), "Омлет с колбасой"),
                Arguments.of(List.of("Яйца", "Сыр"), "Яичница с сыром"),
                Arguments.of(List.of("Яйца", "Соль", "Колбаса", "Сыр"), "Омлет с колбасой и сыром")
        );
    }

    @ParameterizedTest
    @MethodSource("ingredientCombinations")
    public void testPairwiseCombinations(List<String> ingredients, String expectedRecipePart) {
        String recipe = bot.findRecipeByIngredients(ingredients);
        // Проверяем наличие любой из возможных формулировок
        assertTrue(
                recipe.contains(expectedRecipePart) ||
                        recipe.contains(expectedRecipePart.replace(" и ", ", ")),
                "Рецепт должен содержать '" + expectedRecipePart + "'"
        );
    }
}