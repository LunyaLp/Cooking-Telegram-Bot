package io.proj3ct.SpringDemoBot;

import io.proj3ct.SpringDemoBot.config.BotConfig;
import io.proj3ct.SpringDemoBot.service.TelegramBot;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class RecipeSearchTest {

	private final TelegramBot bot = new TelegramBot(mockBotConfig());

	// Метод для создания mock-объекта BotConfig
	private BotConfig mockBotConfig() {
		// Создаем mock-объект BotConfig
		BotConfig botConfig = mock(BotConfig.class);

		// Настроим mock для всех нужных свойств
		Mockito.when(botConfig.getBotName()).thenReturn("MockBotName");
		Mockito.when(botConfig.getToken()).thenReturn("MockToken");
		Mockito.when(botConfig.getOwnerId()).thenReturn(123456L);

		return botConfig;
	}

	@Test
	public void testFindRecipeWithEggsAndCheese() {
		List<String> ingredients = Arrays.asList("Яйца", "Сыр");
		String recipe = bot.findRecipeByIngredients(ingredients);
		assertTrue(recipe.contains("Яичница с сыром"));
	}

	@Test
	public void testNoRecipeWithoutEggs() {
		List<String> ingredients = List.of("Колбаса");
		String result = bot.findRecipeByIngredients(ingredients);
		assertEquals("⚠️ *Внимание!*\nВсе рецепты требуют наличия яиц.\n" +
				"Пожалуйста, начните ввод продуктов заново \n"+
				"Не забудьте добавить яйца в список ингредиентов.", result);
	}

}



