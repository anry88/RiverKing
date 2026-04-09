package com.riverking.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

data class RiverStrings(
    val appTitle: String,
    val authSubtitle: String,
    val login: String,
    val password: String,
    val signIn: String,
    val createAccount: String,
    val needAccount: String,
    val alreadyHaveAccount: String,
    val signInGoogle: String,
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
    val guide: String,
    val tournaments: String,
    val ratings: String,
    val club: String,
    val shop: String,
    val fishing: String,
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
)

@Composable
fun rememberRiverStrings(language: String?): RiverStrings = remember(language) {
    if (language == "ru") {
        RiverStrings(
            appTitle = "RiverKing",
            authSubtitle = "Android-клиент использует общий backend. Mini App остаётся только Telegram.",
            login = "Логин",
            password = "Пароль",
            signIn = "Войти",
            createAccount = "Создать аккаунт",
            needAccount = "Нужен аккаунт?",
            alreadyHaveAccount = "Уже есть аккаунт?",
            signInGoogle = "Войти через Google",
            chooseNickname = "Выберите никнейм",
            continueLabel = "Продолжить",
            logout = "Выйти",
            dailyGift = "Ежедневный подарок",
            claimDaily = "Забрать ежедневную наживку",
            dailyClaimed = "Подарок уже забран",
            casting = "Заброс...",
            waitingBite = "Ожидаем поклёвку",
            hook = "Подсечь",
            tapFast = "Быстро тапайте",
            castRod = "Забросить",
            castCooldown = "Готовим следующий заброс",
            recentCatches = "Недавние поимки",
            quests = "Квесты",
            achievements = "Достижения",
            guide = "Справочник",
            tournaments = "Турниры",
            ratings = "Рейтинги",
            club = "Клуб",
            shop = "Магазин",
            fishing = "Рыбалка",
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
        )
    } else {
        RiverStrings(
            appTitle = "RiverKing",
            authSubtitle = "Android uses the shared backend. The Mini App remains Telegram-only.",
            login = "Login",
            password = "Password",
            signIn = "Sign in",
            createAccount = "Create account",
            needAccount = "Need an account?",
            alreadyHaveAccount = "Already have an account?",
            signInGoogle = "Sign in with Google",
            chooseNickname = "Choose your nickname",
            continueLabel = "Continue",
            logout = "Logout",
            dailyGift = "Daily gift",
            claimDaily = "Claim daily bait",
            dailyClaimed = "Daily reward already claimed",
            casting = "Casting...",
            waitingBite = "Waiting for a bite",
            hook = "Hook",
            tapFast = "Tap fast",
            castRod = "Cast rod",
            castCooldown = "Preparing next cast",
            recentCatches = "Recent catches",
            quests = "Quests",
            achievements = "Achievements",
            guide = "Guide",
            tournaments = "Tournaments",
            ratings = "Ratings",
            club = "Club",
            shop = "Shop",
            fishing = "Fishing",
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
            fishEscaped = "The fish escaped",
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

fun RiverStrings.rodBonusLabel(bonusWater: String?, bonusPredator: Boolean?): String {
    if (bonusWater == null) {
        return if (login == "Логин") "Без бонуса к типу воды" else "No water bonus"
    }
    return when {
        bonusWater == "fresh" && bonusPredator == true ->
            if (login == "Логин") "Бонус к пресным хищникам" else "Bonus for freshwater predators"
        bonusWater == "fresh" && bonusPredator == false ->
            if (login == "Логин") "Бонус к пресной мирной рыбе" else "Bonus for freshwater peaceful fish"
        bonusWater == "salt" && bonusPredator == true ->
            if (login == "Логин") "Бонус к морским хищникам" else "Bonus for saltwater predators"
        bonusWater == "salt" && bonusPredator == false ->
            if (login == "Логин") "Бонус к морской мирной рыбе" else "Bonus for saltwater peaceful fish"
        else -> if (login == "Логин") "Особый бонус удилища" else "Special rod bonus"
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
