package io.proj3ct.SpringDemoBot.service;

import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.SpringDemoBot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;
    private final Map<Long, List<String>> userFavorites = new HashMap<>(); // Избранное по userId
    private final Map<String, String> favoriteRecipes = new HashMap<>();// Рецепты в избранном

    // Текстовые константы
    static final String HELP_TEXT = "🆘 *Помощь по командам* 🆘\n\n" +
            "📌 Доступные команды:\n" +
            "▶ /start - приветственное сообщение и список команд.\n" +
            "▶ /help - информация о командах и их назначении.\n" +
            "▶ /ingredients - выбор ингредиентов (например, яйца, соль, сыр и т. д.). Можно выбрать несколько ингредиентов. После выбора появится кнопка 'Подтвердить'.\n" +
            "▶ /timer <минуты> - установка таймера. Введите количество минут, например '/timer 5'. Когда время истечет, вы получите уведомление.\n" +
            "▶ /favorites - просмотр избранных рецептов в виде кнопок. Нажмите на кнопку, чтобы увидеть полный рецепт.\n\n" +
            "Начните с выбора ингредиентов с помощью /ingredients и узнайте, какие рецепты можно приготовить! 🍽";

    // База рецептов [ингредиенты, описание]
    private static final  String[][] recipes = {
            {"Яйца", "1. Яйца\nРецепт: Простое омлетное блюдо 🍳🥚\nВремя приготовления: 5 минут\nИнгредиенты: Яйца\nПриготовление: Взбейте яйца, посолите, вылейте на разогретую сковороду и готовьте на среднем огне 2-3 минуты."},
            {"Соль, Яйца", "2. Яйца, Соль\nРецепт: Омлет с солью 🍳🧂\nВремя приготовления: 5 минут\nИнгредиенты: Яйца, Соль\nПриготовление: Взбейте яйца, добавьте щепотку соли, вылейте на сковороду и готовьте до золотистой корочки."},
            {"Колбаса, Яйца", "3. Яйца, Колбаса\nРецепт: Омлет с колбасой🍳🌭\nВремя приготовления: 7 минут\nИнгредиенты: Яйца, Колбаса\nПриготовление: Нарежьте колбасу, обжарьте на сковороде, затем добавьте взбитые яйца и готовьте до золотистой корочки."},
            {"Сыр, Яйца", "4. Яйца, Сыр\nРецепт: Яичница с сыром 🧀🍳\nВремя приготовления: 6 минут\nИнгредиенты: Яйца, Сыр\nПриготовление: Взбейте яйца, налейте на сковороду, посыпьте тертым сыром и готовьте до расплавления сыра."},
            {"Колбаса, Соль, Яйца", "6. Яйца, Соль, Колбаса\nРецепт: Омлет с колбасой и солью 🍳🧂🌭\nВремя приготовления: 8 минут\nИнгредиенты: Яйца, Соль, Колбаса\nПриготовление: Обжарьте колбасу на сковороде, добавьте взбитые яйца с солью и готовьте на среднем огне."},
            {"Соль, Сыр, Яйца", "7. Яйца, Соль, Сыр\nРецепт: Омлет с солью и сыром 🍳🧂🧀\nВремя приготовления: 7 минут\nИнгредиенты: Яйца, Соль, Сыр\nПриготовление: Взбейте яйца с солью, вылейте на сковороду и посыпьте сыром, готовьте до расплавления сыра."},
            {"Колбаса, Сыр, Яйца", "9. Яйца, Колбаса, Сыр\nРецепт: Омлет с колбасой и сыром 🍳🌭🧀\nВремя приготовления: 8 минут\nИнгредиенты: Яйца, Колбаса, Сыр\nПриготовление: Обжарьте колбасу, добавьте взбитые яйца и тертый сыр, готовьте до плавления сыра."},
            {"Колбаса, Соль, Сыр, Яйца", "12. Яйца, Соль, Колбаса, Сыр\nРецепт: Омлет с колбасой, сыром и солью 🍳🧂🌭🧀\nВремя приготовления: 10 минут\nИнгредиенты: Яйца, Соль, Колбаса, Сыр\nПриготовление: Обжарьте колбасу, добавьте яйца с солью, посыпьте сыром и готовьте до золотистой корочки."},
    };

    // UI-константы
    static final String CONFIRM_BUTTON = "Подтвердить";
    static final String ERROR_TEXT = "Error occurred: ";
    private List<String> selectedIngredients = new ArrayList<>(); // Текущий выбор пользователя

    /**
     * Конструктор бота
     * @param config конфигурация с именем и токеном бота
     */
    public TelegramBot(BotConfig config) {
        this.config = config;
        initCommands();
    }

    /** Инициализирует список команд бота */
    private void initCommands() {
        List<BotCommand> commands = Arrays.asList(
                new BotCommand("/start", "Начать работу"),
                new BotCommand("/help", "Помощь"),
                new BotCommand("/ingredients", "Выбрать ингредиенты"),
                new BotCommand("/timer", "Установить таймер"),
                new BotCommand("/favorites", "Избранные рецепты")
        );

        try {
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка установки команд: " + e.getMessage());
        }
    }

    /**
     * Возвращает имя бота, зарегистрированное в Telegram.
     * Имя используется для идентификации бота в системе Telegram (@BotName).
     * Данные берутся из конфигурации (BotConfig).
     *
     * @return строку с username бота (без символа @)
     */
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    /**
     * Возвращает уникальный токен авторизации бота.
     * Токен выдается BotFather при создании бота и используется
     * для аутентификации в Telegram API.
     *
     * @return строку с API-токеном бота
     */
    @Override
    public String getBotToken() {
        return config.getToken();
    }

    /**
     * Основной метод обработки входящих обновлений от Telegram API.
     * Обрабатывает текстовые команды и callback-запросы от inline-кнопок.
     *
     * @param update входящее обновление, содержащее либо сообщение, либо callback-запрос
     * @implNote Логика обработки разделена на две части:
     *           1. Обработка текстовых команд (/start, /help и др.)
     *           2. Обработка действий с inline-кнопками (выбор ингредиентов, подтверждение)
     */
    @Override
    public void onUpdateReceived(Update update) {
        // Получаем chatId из сообщения или callback-запроса
        long chatId = update.getMessage() != null ? update.getMessage().getChatId() : update.getCallbackQuery().getMessage().getChatId();

        // Обработка текстовых сообщений
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            switch (messageText) {
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    handleHelpCommand(chatId);
                    break;
                case "/ingredients":
                    handleIngredientsCommand(chatId);
                    break;
                case "/timer":
                    handleTimerCommand(chatId, "/timer 5");
                    break;
                case "/favorites":
                    handleFavoritesCommand(chatId);
                    break;
                default:
                    if (messageText.startsWith("/timer")) {
                        try {
                            int minutes = Integer.parseInt(messageText.split(" ")[1]);
                            long millis = System.currentTimeMillis() + (minutes * 60000);

                            // Отправляем сообщение о том, что таймер установлен
                            String timerMessage = "Таймер на " + minutes + " " + getMinutesText(minutes) + " поставлен.";
                            sendMessage(chatId, timerMessage);

                            // Создаём таймер
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    sendMessage(chatId, "Время истекло!");
                                }
                            }, millis - System.currentTimeMillis());
                        } catch (Exception e) {
                            sendMessage(chatId, "Неверный формат. Используйте: /timer <количество минут>");
                        }
                    } else {
                        sendMessage(chatId, "Команда не распознана. Введите /help для списка команд.");
                    }
                    break;
            }
        }
        // Обработка Callback-запросов (нажатия на inline-кнопки)
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();

            // Логируем callbackData для отладки
            log.info("Callback data received: " + callbackData);

            // Обработка выбора ингредиента
            if (callbackData.startsWith("ingredient_")) {
                String ingredient = callbackData.split("_")[1];
                if (!selectedIngredients.contains(ingredient)) {
                    selectedIngredients.add(ingredient);
                    // Отправляем сообщение с кнопкой "Подтвердить"
                    sendMessageWithConfirmButton(chatId, "Продукт добавлен: " + ingredient + ". Если это все, нажмите 'Подтвердить'.");
                }
            }

            // Обработка нажатия кнопки "Подтвердить"
            else if (callbackData.equals("confirm")) {
                log.info("Confirm button clicked");

                // Проверяем, что список ингредиентов не пуст
                if (selectedIngredients.isEmpty()) {
                    sendMessage(chatId, "⚠️ Вы не выбрали ни одного ингредиента!\n" +
                            "Пожалуйста, нажмите /ingredients и выберите продукты.");
                    return;
                }

                // Преобразуем список ингредиентов в строку
                String ingredientsList = String.join(", ", selectedIngredients);

                // Находим рецепт по выбранным ингредиентам
                String recipe = findRecipeByIngredients(selectedIngredients);

                // Проверяем, что рецепт найден и не пуст
                if (recipe == null || recipe.isEmpty()) {
                    sendMessage(chatId, "Не удалось найти рецепт для выбранных ингредиентов.");
                } else {

                    // Проверяем, есть ли яйца в рецепте перед добавлением кнопки "Сохранить рецепт"
                    if (recipe.contains("Яйца")) {

                        // Отправляем рецепт с кнопкой "Сохранить рецепт в избранное"
                        sendMessageWithSaveButton(chatId, "Ваши ингредиенты: " + ingredientsList + "\n\n" + recipe, recipe);
                    } else {

                        // Отправляем только рецепт без кнопки "Сохранить рецепт"
                        sendMessage(chatId, "Ваши ингредиенты: " + ingredientsList + "\n\n" + recipe);
                    }
                }
                selectedIngredients.clear(); // Очищаем список выбранных ингредиентов
            }
            // Обработка нажатия кнопки сохранения рецепта
            else if (callbackData.startsWith("save_recipe_")) {

                // Получаем ингредиенты из callbackData (например, "Яйца, Соль")
                String ingredientsString = callbackData.split("_")[2];

                // Получаем список ингредиентов для данного рецепта
                List<String> ingredients = Arrays.asList(ingredientsString.split(", "));

                if (ingredients != null && !ingredients.isEmpty()) {
                    // Вызов метода для сохранения рецепта в избранное
                    saveRecipeToFavorites(ingredients, chatId);
                } else {
                    sendMessage(chatId, "Ошибка: Рецепт не найден.");
                }
            }

            // Обработка выбора рецепта из избранного
            else if (callbackData.startsWith("recipe_")) {

                // Получаем ID рецепта из callbackData
                String recipeId = callbackData.split("_")[1];

                // Получаем рецепт из избранного
                String recipe = favoriteRecipes.get(recipeId);
                if (recipe != null) {
                    sendMessage(chatId, "Вы выбрали рецепт с ингредиентами:\n\n" + recipe);
                } else {
                    sendMessage(chatId, "Рецепт не найден.");
                }
            }
        }
    }

    /**
     * Ищет подходящий рецепт по списку выбранных ингредиентов.
     *
     * Основная логика работы метода:
     *   Проверяет обязательное наличие яиц в списке ингредиентов
     *   Сравнивает выбранные ингредиенты с рецептами из базы
     *   Возвращает найденный рецепт или сообщение об ошибке
     *
     *
     * @param ingredients список выбранных пользователем ингредиентов
     * @return строку с найденным рецептом или сообщением об ошибке
     */
    public String findRecipeByIngredients(List<String> ingredients) {
        // Проверка обязательного ингредиента - яйца
        if (!ingredients.contains("Яйца")) {
            // Форматированное сообщение с предупреждением
            return "⚠️ *Внимание!*\nВсе рецепты требуют наличия яиц.\n" +
                    "Пожалуйста, начните ввод продуктов заново \n" +
                    "Не забудьте добавить яйца в список ингредиентов.";
        }

        // Конвертируем список в Set для независимости от порядка элементов
        Set<String> selectedSet = new HashSet<>(ingredients);

        // Поиск по базе рецептов
        for (String[] recipe : recipes) {
            // Разбиваем строку ингредиентов из рецепта и конвертируем в Set
            Set<String> recipeIngredients = new HashSet<>(
                    Arrays.asList(recipe[0].split("\\s*,\\s*")) // Регулярка для обработки пробелов
            );

            // Точное совпадение наборов ингредиентов
            if (selectedSet.equals(recipeIngredients)) {
                // Форматируем результат с предложением сохранить рецепт
                return "🔹 *Найден рецепт* 🔹\n\n" + recipe[1] +
                        "\n\nХотите сохранить его в избранное?";
            }
        }

        // Возвращаем сообщение, если рецепт не найден
        return "Рецепт не найден. Попробуйте выбрать другие ингредиенты.";
    }

    /**
     * Обрабатывает команду /ingredients - инициализирует процесс выбора ингредиентов.
     *
     * Выполняет следующие действия:
     *   Очищает список ранее выбранных ингредиентов
     *   Отображает интерфейс выбора ингредиентов
     *
     * @param chatId идентификатор чата для отправки сообщения
     */
    private void handleIngredientsCommand(long chatId) {
        // Очищаем предыдущий выбор пользователя
        selectedIngredients.clear();

        // Показываем меню выбора ингредиентов
        showIngredientSelection(chatId);
    }

    /**
     * Отображает интерфейс выбора ингредиентов с inline-кнопками.
     *
     * Создает сообщение с:
     *   Текстовой инструкцией
     *   Набором кнопок для выбора ингредиентов
     *   Поддержкой множественного выбора
     *
     * @param chatId идентификатор чата для отправки сообщения
     * @implNote Для каждого ингредиента создается кнопка с callback-данными в формате "ingredient_НАЗВАНИЕ"
     */
    private void showIngredientSelection(long chatId) {
        // Формируем текст сообщения с инструкцией
        String text = "Выберите ингредиенты🔍:\n" +
                "Можно выбрать несколько. После выбора нажмите 'Подтвердить'";

        // Создаем базовое сообщение
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        // Инициализируем клавиатуру для кнопок
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        // --- Создаем кнопки для каждого ингредиента ---

        // Кнопка для яиц
        InlineKeyboardButton eggsButton = new InlineKeyboardButton();
        eggsButton.setText("Яйца"); // Отображаемый текст
        eggsButton.setCallbackData("ingredient_Яйца"); // Данные для обработки callback
        row.add(eggsButton);

        // Кнопка для соли
        InlineKeyboardButton saltButton = new InlineKeyboardButton();
        saltButton.setText("Соль");
        saltButton.setCallbackData("ingredient_Соль");
        row.add(saltButton);

        // Кнопка для колбасы
        InlineKeyboardButton sausageButton = new InlineKeyboardButton();
        sausageButton.setText("Колбаса");
        sausageButton.setCallbackData("ingredient_Колбаса");
        row.add(sausageButton);

        // Кнопка для сыра
        InlineKeyboardButton cheeseButton = new InlineKeyboardButton();
        cheeseButton.setText("Сыр");
        cheeseButton.setCallbackData("ingredient_Сыр");
        row.add(cheeseButton);

        // Добавляем строку с кнопками в клавиатуру
        rows.add(row);
        markup.setKeyboard(rows);

        // Устанавливаем клавиатуру в сообщение
        message.setReplyMarkup(markup);

        // Отправляем сообщение с кнопками
        executeMessage(message);
    }

    /**
     * Обрабатывает команду /timer - показывает инструкцию по установке таймера.
     *
     * @param chatId ID чата для отправки сообщения
     * @param s неиспользуемый параметр (оставлен для совместимости)
     */
    public void handleTimerCommand(long chatId, String s) {
        // Формируем текст с инструкцией
        String text = "⏳ Введите время в минутах:\n" +
                "Формат: /timer <число от 1 до 60>\n" +
                "Пример: /timer 5";

        // Создаем и отправляем сообщение
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        executeMessage(message);
    }

    /**
     * Обрабатывает команду /start - приветственное сообщение и инструкция.
     *
     * @param chatId ID чата пользователя
     * @param name имя пользователя
     */
    private void startCommandReceived(long chatId, String name) {
        // Форматируем приветственное сообщение с эмодзи
        String answer = EmojiParser.parseToUnicode(
                "Здравствуйте, " + name + "!\n" +
                        "Я бот, который поможет тебе найти рецепты на основе тех ингредиентов, что у тебя есть.\n\n" +
                        "Вот как это работает:\n" +
                        "1. Выбери ингредиенты из списка.\n" +
                        "2. Я предложу тебе рецепты, которые можно приготовить из этих ингредиентов.\n" +
                        "3. Получай пошаговые инструкции по каждому рецепту!\n\n" +
                        "Команды:\n" +
                        "🍳 /start - начать работу с ботом\n" +
                        "🍽 /ingredients - выбрать ингредиенты\n" +
                        "⏲ /timer <минуты> - установить таймер на определенное время\n" +
                        "❤️ /favorites - показать избранные рецепты\n" +
                        "❓ /help - помощь по командам\n\n" +
                        "⏲ /timer <минуты> - установить таймер на определенное время\n" +
                        "Готов начать? Просто выбери команду или введи ингредиенты, и я предложу тебе подходящие рецепты!"
        );
        // Логируем факт ответа пользователю
        log.info("Replied to user " + name);

        // Отправляем сообщение и очищаем избранное
        sendMessage(chatId, answer);
        userFavorites.remove(chatId); // Сброс избранного при старте
    }

    /**
     * Отправляет текстовое сообщение пользователю.
     *
     * @param chatId ID чата получателя
     * @param textToSend текст сообщения
     */
    public void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    /**
     * Выполняет отправку подготовленного сообщения с обработкой ошибок.
     *
     * @param message подготовленное сообщение для отправки
     */
    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
            sendMessage(Long.parseLong(message.getChatId()),
                    "⚠️ Произошла ошибка при обработке запроса.\n" +
                            "Попробуйте повторить действие или обратитесь к разработчику.");
        }
    }

    /**
     * Подготавливает и отправляет текстовое сообщение.
     *
     * @param chatId ID чата получателя
     * @param textToSend текст для отправки
     */
    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    /**
     * Обрабатывает команду /help - отправляет справочную информацию.
     *
     * @param chatId ID чата пользователя
     */
    private void handleHelpCommand(long chatId) {
        prepareAndSendMessage(chatId, HELP_TEXT);
    }


    /**
     * Сохраняет рецепт в избранное пользователя.
     *
     * @param ingredients список ингредиентов рецепта
     * @param chatId ID чата пользователя
     * @throws NullPointerException если ingredients == null
     */
    private void saveRecipeToFavorites(List<String> ingredients, long chatId) {
        // Генерируем уникальный ID рецепта
        String recipeId = String.join(", ", ingredients);

        // Инициализируем список избранного, если его нет
        userFavorites.putIfAbsent(chatId, new ArrayList<>());

        // Проверяем на дубликаты
        if (userFavorites.get(chatId).contains(recipeId)) {
            sendMessage(chatId, "Этот рецепт уже добавлен в избранное! ❌");
            return;
        }

        // Сохраняем рецепт
        userFavorites.get(chatId).add(recipeId);
        favoriteRecipes.put(recipeId, findRecipeByIngredients(ingredients));

        // Отправляем подтверждение
        sendMessage(chatId, "✅ Рецепт успешно сохранен в избранное!\n" +
                "Ингредиенты: " + recipeId);
    }

    /**
     * Обрабатывает команду /favorites - отображает список избранных рецептов пользователя.
     *
     * Логика работы:
     *   Проверяет наличие избранных рецептов
     *   При отсутствии - показывает подсказку
     *   При наличии - формирует список с inline-кнопками
     *
     * @param chatId идентификатор чата пользователя
     */
    private void handleFavoritesCommand(long chatId) {
        // Получаем список избранного для текущего пользователя
        List<String> favorites = userFavorites.get(chatId);

        if (favorites == null || favorites.isEmpty()) {
            // Пользователь еще не сохранял рецепты
            sendMessage(chatId, "📌 У вас пока нет сохраненных рецептов.\n" +
                    "Чтобы добавить рецепт в избранное, найдите его через /ingredients " +
                    "и нажмите кнопку 'Сохранить рецепт в избранное'");
        } else {
            // Формируем сообщение со списком рецептов
            String text = "⭐ *Ваши избранные рецепты* ⭐\n" +
                    "Выберите рецепт для просмотра:";
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);

            // Создаем inline-клавиатуру
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // Добавляем кнопку для каждого рецепта
            for (String recipeName : favorites) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton recipeButton = new InlineKeyboardButton();
                recipeButton.setText("Рецепт с ингредиентами: " + recipeName);
                recipeButton.setCallbackData("recipe_" + recipeName);
                row.add(recipeButton);
                rows.add(row);
            }

            markup.setKeyboard(rows);
            message.setReplyMarkup(markup);
            executeMessage(message);
        }
    }

    /**
     * Отправляет сообщение с кнопкой подтверждения.
     *
     * @param chatId идентификатор чата
     * @param text текст сообщения
     */
    private void sendMessageWithConfirmButton(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        // Создаем кнопку подтверждения
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText(CONFIRM_BUTTON);
        confirmButton.setCallbackData("confirm");
        row.add(confirmButton);

        rows.add(row);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    /**
     * Отправляет рецепт с кнопкой сохранения в избранное.
     *
     * @param chatId идентификатор чата
     * @param text текст рецепта
     * @param recipe полное описание рецепта
     * @implNote Формат callbackData: "save_recipe_ингредиент1, ингредиент2"
     */
    private void sendMessageWithSaveButton(long chatId, String text, String recipe) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        // Формируем строку ингредиентов для callback
        String ingredientsList = String.join(", ", selectedIngredients);

        // Создаем кнопку сохранения
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton saveButton = new InlineKeyboardButton();
        saveButton.setText("Сохранить рецепт в избранное");
        saveButton.setCallbackData("save_recipe_" + ingredientsList);
        row.add(saveButton);

        rows.add(row);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        log.info("Sending message with recipe and callbackData: save_recipe_" + ingredientsList);
        executeMessage(message);
    }

    /**
     * Возвращает правильную форму слова "минута" для числительного.
     *
     * @param minutes количество минут
     * @return строку с правильной формой слова
     * @implNote Учитывает правила русского языка для склонения числительных
     */
    private String getMinutesText(int minutes) {
        if (minutes % 10 == 1 && minutes % 100 != 11) {
            return "минуту";
        } else if (minutes % 10 >= 2 && minutes % 10 <= 4 && (minutes % 100 < 10 || minutes % 100 >= 20)) {
            return "минуты";
        } else {
            return "минут";
        }
    }
}