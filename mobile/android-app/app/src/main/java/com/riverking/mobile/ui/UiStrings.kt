package com.riverking.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

data class RiverStrings(
    val appTitle: String,
    val authSubtitle: String,
    val registerRequirements: String,
    val login: String,
    val password: String,
    val showPassword: String,
    val hidePassword: String,
    val cancel: String,
    val signIn: String,
    val createAccount: String,
    val needAccount: String,
    val alreadyHaveAccount: String,
    val signInGoogle: String,
    val signInTelegram: String,
    val chooseNickname: String,
    val continueLabel: String,
    val logout: String,
    val dailyGift: String,
    val claimDaily: String,
    val dailyClaimed: String,
    val casting: String,
    val waitingBite: String,
    val hook: String,
    val tapFast: String,
    val castRod: String,
    val castCooldown: String,
    val recentCatches: String,
    val quests: String,
    val achievements: String,
    val catalog: String,
    val leaders: String,
    val tournaments: String,
    val ratings: String,
    val club: String,
    val shop: String,
    val fishing: String,
    val gear: String,
    val allLocations: String,
    val allFish: String,
    val personal: String,
    val global: String,
    val largest: String,
    val smallest: String,
    val noData: String,
    val inviteFriends: String,
    val generateLink: String,
    val claimRewards: String,
    val copyLink: String,
    val shareLink: String,
    val viewAll: String,
    val openClub: String,
    val searchClub: String,
    val createClub: String,
    val joinClub: String,
    val leaveClub: String,
    val refresh: String,
    val fishEscaped: String,
    val noBait: String,
    val shopDisabledDirect: String,
    val playPurchaseUnavailable: String,
    val playPurchasePending: String,
    val playPurchaseCancelled: String,
    val playPurchaseInvalid: String,
    val playProductUnavailable: String,
    val playFinalizeFailed: String,
    val chat: String,
    val loading: String,
    val currentTournament: String,
    val upcomingTournaments: String,
    val pastTournaments: String,
    val prizes: String,
    val guideWaters: String,
    val guideFish: String,
    val guideLures: String,
    val guideRods: String,
    val claim: String,
    val shareCatch: String,
    val telegramAccount: String,
    val telegramLinkedTitle: String,
    val telegramNotLinkedTitle: String,
    val linkTelegram: String,
    val telegramLoginPending: String,
    val telegramLinkPending: String,
    val rod: String,
    val bait: String,
    val water: String,
    val autoCast: String,
    val unavailable: String,
    val purchaseForCoins: String,
    val payWithGoogle: String,
    val currentLabel: String,
    val dailyRewardReady: String,
    val dailyStreakLabel: String,
    val dailyReturnTomorrow: String,
    val chooseLocation: String,
    val chooseRod: String,
    val chooseBait: String,
    val dailyQuestsLabel: String,
    val weeklyQuestsLabel: String,
    val changeNickname: String,
    val statistics: String,
    val totalWeight: String,
    val totalCount: String,
    val language: String,
    val support: String,
    val privacyPolicy: String,
    val deleteAccount: String,
    val deleteAccountTitle: String,
    val deleteAccountMessage: String,
    val deleteAccountConfirm: String,
    val deleteAccountWeb: String,
    val deleteAccountDone: String,
)

@Composable
fun rememberRiverStrings(language: String?): RiverStrings = remember(language) {
    if (language == "ru") {
        RiverStrings(
            appTitle = "RiverKing",
            authSubtitle = "Продолжите игру через Telegram или войдите по логину и паролю.",
            registerRequirements = "Для нового аккаунта: логин 3-32 символа в нижнем регистре, пароль от 8 символов.",
            login = "Логин",
            password = "Пароль",
            showPassword = "Показать пароль",
            hidePassword = "Скрыть пароль",
            cancel = "Отмена",
            signIn = "Войти",
            createAccount = "Создать аккаунт",
            needAccount = "Нужен аккаунт?",
            alreadyHaveAccount = "Уже есть аккаунт?",
            signInGoogle = "Войти через Google",
            signInTelegram = "Войти через Telegram",
            chooseNickname = "Выберите никнейм",
            continueLabel = "Продолжить",
            logout = "Выйти",
            dailyGift = "Ежедневный подарок",
            claimDaily = "Забрать ежедневную наживку",
            dailyClaimed = "Подарок уже забран",
            casting = "Заброс...",
            waitingBite = "Ожидаем поклёвку…",
            hook = "Подсечь!",
            tapFast = "Тащи!!!",
            castRod = "Забросить удочку",
            castCooldown = "Готовим снасти…",
            recentCatches = "Недавние поимки",
            quests = "Квесты",
            achievements = "Достижения",
            catalog = "Каталог",
            leaders = "Лидеры",
            tournaments = "Турниры",
            ratings = "Рейтинги",
            club = "Клуб",
            shop = "Магазин",
            fishing = "Рыбалка",
            gear = "Снасти",
            allLocations = "Все локации",
            allFish = "Все рыбы",
            personal = "Личные",
            global = "Глобальные",
            largest = "Крупнейшие",
            smallest = "Мельчайшие",
            noData = "Пока пусто",
            inviteFriends = "Пригласить друзей",
            generateLink = "Сгенерировать ссылку",
            claimRewards = "Забрать награды",
            copyLink = "Скопировать ссылку",
            shareLink = "Поделиться",
            viewAll = "Смотреть всё",
            openClub = "Открыть клуб",
            searchClub = "Поиск клубов",
            createClub = "Создать клуб",
            joinClub = "Вступить",
            leaveClub = "Покинуть клуб",
            refresh = "Обновить",
            fishEscaped = "Рыба сорвалась",
            noBait = "Нет наживки",
            shopDisabledDirect = "Платные наборы недоступны в direct-сборке",
            playPurchaseUnavailable = "Play-покупка пока недоступна в этой конфигурации",
            playPurchasePending = "Покупка в Google Play ещё ожидает подтверждения",
            playPurchaseCancelled = "Покупка отменена",
            playPurchaseInvalid = "Google Play не подтвердил эту покупку",
            playProductUnavailable = "Этот товар сейчас недоступен в Google Play",
            playFinalizeFailed = "Покупка выдана, но Google Play ещё не завершил обработку",
            chat = "Чат",
            loading = "Загрузка...",
            currentTournament = "Текущий турнир",
            upcomingTournaments = "Скоро",
            pastTournaments = "Прошедшие",
            prizes = "Призы",
            guideWaters = "Локации",
            guideFish = "Рыбы",
            guideLures = "Наживки",
            guideRods = "Удилища",
            claim = "Забрать",
            shareCatch = "Поделиться уловом",
            telegramAccount = "Telegram-аккаунт",
            telegramLinkedTitle = "Telegram уже привязан к этому профилю",
            telegramNotLinkedTitle = "Привяжите Telegram, чтобы играть тем же профилем в Mini App и на Android",
            linkTelegram = "Привязать Telegram",
            telegramLoginPending = "Подтвердите вход в Telegram и нажмите кнопку возврата в приложении бота.",
            telegramLinkPending = "Подтвердите привязку в Telegram и нажмите кнопку возврата в приложении бота.",
            rod = "Удочка",
            bait = "Наживка",
            water = "Локация",
            autoCast = "Автозаброс",
            unavailable = "Недоступно",
            purchaseForCoins = "Купить за монеты",
            payWithGoogle = "Купить через Google Play",
            currentLabel = "Текущая",
            dailyRewardReady = "Награда готова",
            dailyStreakLabel = "Серия",
            dailyReturnTomorrow = "Завтра серия продолжится здесь",
            chooseLocation = "Выбор локации",
            chooseRod = "Выбор удочки",
            chooseBait = "Выбор наживки",
            dailyQuestsLabel = "Ежедневные",
            weeklyQuestsLabel = "Недельные",
            changeNickname = "Изменить никнейм",
            statistics = "Статистика",
            totalWeight = "Общий вес",
            totalCount = "Всего поймано",
            language = "Язык",
            support = "Поддержка",
            privacyPolicy = "Политика конфиденциальности",
            deleteAccount = "Удалить аккаунт",
            deleteAccountTitle = "Удалить аккаунт RiverKing?",
            deleteAccountMessage = "Профиль, прогресс, уловы, квесты, клуб и связанные сессии будут удалены без возможности восстановления.",
            deleteAccountConfirm = "Удалить навсегда",
            deleteAccountWeb = "Открыть веб-инструкции",
            deleteAccountDone = "Аккаунт удалён",
        )
    } else {
        RiverStrings(
            appTitle = "RiverKing",
            authSubtitle = "Continue with Telegram or sign in with your login and password.",
            registerRequirements = "New accounts require a 3-32 character lowercase login and a password with at least 8 characters.",
            login = "Login",
            password = "Password",
            showPassword = "Show password",
            hidePassword = "Hide password",
            cancel = "Cancel",
            signIn = "Sign in",
            createAccount = "Create account",
            needAccount = "Need an account?",
            alreadyHaveAccount = "Already have an account?",
            signInGoogle = "Sign in with Google",
            signInTelegram = "Sign in with Telegram",
            chooseNickname = "Choose your nickname",
            continueLabel = "Continue",
            logout = "Logout",
            dailyGift = "Daily gift",
            claimDaily = "Claim daily bait",
            dailyClaimed = "Daily reward already claimed",
            casting = "Casting...",
            waitingBite = "Waiting for a bite…",
            hook = "Hook!",
            tapFast = "Reel it in!!!",
            castRod = "Cast the rod",
            castCooldown = "Preparing tackle…",
            recentCatches = "Recent catches",
            quests = "Quests",
            achievements = "Achievements",
            catalog = "Catalog",
            leaders = "Leaders",
            tournaments = "Tournaments",
            ratings = "Ratings",
            club = "Club",
            shop = "Shop",
            fishing = "Fishing",
            gear = "Gear",
            allLocations = "All waters",
            allFish = "All fish",
            personal = "Personal",
            global = "Global",
            largest = "Largest",
            smallest = "Smallest",
            noData = "Nothing here yet",
            inviteFriends = "Invite friends",
            generateLink = "Generate link",
            claimRewards = "Claim rewards",
            copyLink = "Copy link",
            shareLink = "Share",
            viewAll = "View all",
            openClub = "Open club",
            searchClub = "Search clubs",
            createClub = "Create club",
            joinClub = "Join club",
            leaveClub = "Leave club",
            refresh = "Refresh",
            fishEscaped = "The fish got away",
            noBait = "No bait available",
            shopDisabledDirect = "Paid packs are unavailable in the direct build",
            playPurchaseUnavailable = "Play purchase is unavailable in this configuration",
            playPurchasePending = "Google Play is still waiting to finalize this purchase",
            playPurchaseCancelled = "Purchase cancelled",
            playPurchaseInvalid = "Google Play did not confirm this purchase",
            playProductUnavailable = "This product is currently unavailable in Google Play",
            playFinalizeFailed = "The purchase was granted, but Google Play still needs to finish processing",
            chat = "Chat",
            loading = "Loading...",
            currentTournament = "Current tournament",
            upcomingTournaments = "Upcoming",
            pastTournaments = "Past",
            prizes = "Prizes",
            guideWaters = "Waters",
            guideFish = "Fish",
            guideLures = "Lures",
            guideRods = "Rods",
            claim = "Claim",
            shareCatch = "Share catch",
            telegramAccount = "Telegram account",
            telegramLinkedTitle = "Telegram is already linked to this profile",
            telegramNotLinkedTitle = "Link Telegram to use the same profile in the Mini App and on Android",
            linkTelegram = "Link Telegram",
            telegramLoginPending = "Confirm sign-in in Telegram, then use the return button in the bot.",
            telegramLinkPending = "Confirm the link in Telegram, then use the return button in the bot.",
            rod = "Rod",
            bait = "Bait",
            water = "Water",
            autoCast = "Auto cast",
            unavailable = "Unavailable",
            purchaseForCoins = "Buy with coins",
            payWithGoogle = "Buy with Google Play",
            currentLabel = "Current",
            dailyRewardReady = "Reward ready",
            dailyStreakLabel = "Streak",
            dailyReturnTomorrow = "Come back tomorrow to continue the streak",
            chooseLocation = "Choose water",
            chooseRod = "Choose rod",
            chooseBait = "Choose bait",
            dailyQuestsLabel = "Daily",
            weeklyQuestsLabel = "Weekly",
            changeNickname = "Change nickname",
            statistics = "Statistics",
            totalWeight = "Total weight",
            totalCount = "Total caught",
            language = "Language",
            support = "Support",
            privacyPolicy = "Privacy policy",
            deleteAccount = "Delete account",
            deleteAccountTitle = "Delete your RiverKing account?",
            deleteAccountMessage = "This permanently removes the profile, progression, catches, quests, club data, and linked sessions.",
            deleteAccountConfirm = "Delete forever",
            deleteAccountWeb = "Open web instructions",
            deleteAccountDone = "Account deleted",
        )
    }
}

fun RiverStrings.periodLabel(period: RatingsPeriod): String = when (period) {
    RatingsPeriod.TODAY -> if (login == "Логин") "Сегодня" else "Today"
    RatingsPeriod.YESTERDAY -> if (login == "Логин") "Вчера" else "Yesterday"
    RatingsPeriod.WEEK -> if (login == "Логин") "Неделя" else "Week"
    RatingsPeriod.MONTH -> if (login == "Логин") "Месяц" else "Month"
    RatingsPeriod.YEAR -> if (login == "Логин") "Год" else "Year"
    RatingsPeriod.ALL -> if (login == "Логин") "Всё время" else "All time"
}

fun RiverStrings.statsPeriodLabel(period: String): String = when (period) {
    "day" -> if (login == "Логин") "День" else "Day"
    "week" -> if (login == "Логин") "Неделя" else "Week"
    "month" -> if (login == "Логин") "Месяц" else "Month"
    else -> if (login == "Логин") "Всё время" else "All time"
}

fun RiverStrings.orderLabel(order: RatingsOrder): String = when (order) {
    RatingsOrder.DESC -> largest
    RatingsOrder.ASC -> smallest
}

fun RiverStrings.roleLabel(role: String): String = when (role) {
    "president" -> if (login == "Логин") "Президент" else "President"
    "heir" -> if (login == "Логин") "Наследник" else "Heir"
    "veteran" -> if (login == "Логин") "Ветеран" else "Veteran"
    "novice" -> if (login == "Логин") "Новичок" else "Novice"
    else -> role
}

fun RiverStrings.rarityLabel(rarity: String?): String = when (rarity) {
    "common" -> if (login == "Логин") "Обычная" else "Common"
    "uncommon" -> if (login == "Логин") "Необычная" else "Uncommon"
    "rare" -> if (login == "Логин") "Редкая" else "Rare"
    "epic" -> if (login == "Логин") "Эпическая" else "Epic"
    "mythic" -> if (login == "Логин") "Мифическая" else "Mythic"
    "legendary" -> if (login == "Логин") "Легендарная" else "Legendary"
    else -> rarity.orEmpty()
}

fun RiverStrings.dayLabel(day: Int): String =
    if (login == "Логин") "День $day" else "Day $day"

fun RiverStrings.questRewardLabel(coins: Int): String =
    if (login == "Логин") "Награда: $coins монет" else "Reward: $coins coins"

fun RiverStrings.catchResultTitle(): String =
    if (login == "Логин") "Улов" else "Catch"

fun RiverStrings.newFishLabel(): String =
    if (login == "Логин") "Новая рыба" else "New fish"

fun RiverStrings.locationUnlockedLine(location: String): String =
    if (login == "Логин") "Открыта локация: $location" else "Location unlocked: $location"

fun RiverStrings.rodUnlockedLine(rod: String): String =
    if (login == "Логин") "Открыта удочка: $rod" else "Rod unlocked: $rod"

fun RiverStrings.achievementUnlockedLine(name: String, level: String): String =
    if (login == "Логин") "Достижение: $name • $level" else "Achievement unlocked: $name • $level"

fun RiverStrings.questCompletedLine(name: String, coins: Int): String =
    if (login == "Логин") "Квест: $name • +$coins монет" else "Quest: $name • +$coins coins"

fun RiverStrings.coinsEarnedLine(coins: Int): String =
    if (login == "Логин") "+$coins монет" else "+$coins coins"

fun RiverStrings.prizePlacesLabel(count: Int): String =
    if (login == "Логин") "Призовые места: $count" else "Prize places: $count"

fun RiverStrings.achievementLevelLabel(index: Int): String {
    val labels = if (login == "Логин") {
        listOf("Нет уровня", "Бронза", "Серебро", "Золото", "Платина")
    } else {
        listOf("No tier", "Bronze", "Silver", "Gold", "Platinum")
    }
    val safeIndex = index.coerceIn(0, labels.lastIndex)
    return labels[safeIndex]
}

fun RiverStrings.rodBonusLabel(bonusWater: String?, bonusPredator: Boolean?): String {
    if (bonusWater == null) {
        return if (login == "Логин") "Бонусов нет." else "No bonus."
    }
    return when {
        bonusWater == "fresh" && bonusPredator == true ->
            if (login == "Логин") "−50% шанс побега пресноводных хищных рыб." else "50% less escape chance for freshwater predator fish."
        bonusWater == "fresh" && bonusPredator == false ->
            if (login == "Логин") "−50% шанс побега пресноводных мирных рыб." else "50% less escape chance for freshwater peaceful fish."
        bonusWater == "salt" && bonusPredator == true ->
            if (login == "Логин") "−50% шанс побега морских хищных рыб." else "50% less escape chance for saltwater predator fish."
        bonusWater == "salt" && bonusPredator == false ->
            if (login == "Логин") "−50% шанс побега морских мирных рыб." else "50% less escape chance for saltwater peaceful fish."
        else -> if (login == "Логин") "Бонусов нет." else "No bonus."
    }
}

fun RiverStrings.playBillingMessage(code: String): String = when (code) {
    "purchase_pending" -> playPurchasePending
    "purchase_cancelled" -> playPurchaseCancelled
    "play_product_unavailable" -> playProductUnavailable
    "play_finalize_failed" -> playFinalizeFailed
    "invalid_purchase",
    "purchase_product_mismatch",
    "purchase_user_mismatch",
    "purchase_quantity_unsupported",
    "purchase_already_consumed",
    -> playPurchaseInvalid
    "play_verification_unavailable",
    "play_verification_failed",
    "play_purchase_unavailable",
    -> playPurchaseUnavailable
    else -> code
}
