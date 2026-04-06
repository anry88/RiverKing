package app

import io.ktor.server.application.*
import kotlinx.coroutines.*
import service.*
import java.time.*
import kotlin.coroutines.coroutineContext
import db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object Scheduler {
    fun install(app: Application) {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val tournaments = TournamentService()
        val ratingPrizes = RatingPrizeService()
        val clubs = ClubService()
        val prizeService = PrizeService(tournaments, ratingPrizes, clubs)

        val env = Env.fromConfig()
        val bot = TelegramBot(env.botToken)
        val fishing = FishingService()
        val notificationService = NotificationService(bot)

        scope.launch {
            prizeService.distributePrizes()
            while (isActive) {
                delay(Duration.ofMinutes(5).toMillis())
                try {
                    prizeService.distributePrizes()
                } catch (e: Exception) {
                    LoggerFactory.getLogger("Scheduler").error("Error in distributePrizes", e)
                }
            }
        }

        scope.launch {
            startDailyJobs(notificationService, fishing)
        }
    }

    private suspend fun startDailyJobs(notifications: NotificationService, fishing: FishingService) {
        val zone = ZoneId.of("Europe/Belgrade")
        val log = LoggerFactory.getLogger("DailyJobs")

        var lastSubReminderDate: LocalDate? = null
        var lastPrizeReminderDate: LocalDate? = null

        while (coroutineContext.isActive) {
            try {
                val now = ZonedDateTime.now(zone)
                val today = now.toLocalDate()

                // 12:00 - Subscription Reminders
                if (now.hour == 12 && lastSubReminderDate != today) {
                    log.info("Starting daily subscription reminders at $now")
                    runSubscriptionReminders(notifications, fishing)
                    lastSubReminderDate = today
                }

                // 13:00 - Prize Reminders
                if (now.hour == 13 && lastPrizeReminderDate != today) {
                    log.info("Starting daily prize reminders at $now")
                    runPrizeReminders(notifications, fishing)
                    lastPrizeReminderDate = today
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error("Error in daily jobs loop", e)
            }
            delay(Duration.ofMinutes(1).toMillis())
        }
    }

    private suspend fun runSubscriptionReminders(notifications: NotificationService, fishing: FishingService) {
        val now = Instant.now()
        val threeDays = now.plus(Duration.ofDays(3))
        val oneDay = now.plus(Duration.ofDays(1))

        val usersToNotify = transaction {
            Users
                .slice(Users.id, Users.tgId, Users.autoFishUntil, Users.language)
                .select {
                    (Users.autoFishUntil.isNotNull()) and
                    (Users.autoFishUntil greater now) and
                    (Users.autoFishUntil lessEq threeDays) and
                    (Users.canReceiveNotifications eq true)
                }
                .map {
                    Triple(
                        it[Users.id].value,
                        it[Users.tgId],
                        it[Users.autoFishUntil]!! to it[Users.language]
                    )
                }
        }

        for ((uid, tgId, data) in usersToNotify) {
            val (expiresAt, lang) = data
            val remaining = Duration.between(now, expiresAt)

            val type = when {
                remaining <= Duration.ofDays(1) -> "1-day"
                remaining <= Duration.ofDays(3) -> "3-day"
                else -> null
            } ?: continue

            val alreadySent = transaction {
                SubscriptionNotifications.select {
                    (SubscriptionNotifications.userId eq uid) and
                    (SubscriptionNotifications.expiresAt eq expiresAt) and
                    (SubscriptionNotifications.type eq type)
                }.any()
            }

            if (!alreadySent) {
                val textKey = if (type == "1-day") {
                    "🎣 Внимание! Подписка на автоловлю закончится меньше чем через 24 часа. Не забудь заглянуть в магазин!"
                } else {
                    "🎣 Твоя подписка на автоловлю истекает меньше чем через 3 дня! Продли её в магазине, чтобы робот продолжал ловить рыбу за тебя."
                }
                val text = service.I18n.text(textKey, lang)
                val btnText = service.I18n.text("🤖 Купить автоловлю", lang)

                if (notifications.sendNotification(uid, tgId, text, btnText, "/buy $uid:autofish")) {
                    transaction {
                        SubscriptionNotifications.insert {
                            it[userId] = uid
                            it[SubscriptionNotifications.expiresAt] = expiresAt
                            it[SubscriptionNotifications.type] = type
                        }
                    }
                }
                delay(100) // Small delay to avoid hitting rate limits
            }
        }
    }

    private suspend fun runPrizeReminders(notifications: NotificationService, fishing: FishingService) {
        val userIds = transaction {
            val fromPrizes = UserPrizes.slice(UserPrizes.userId).select { UserPrizes.claimed eq false }.map { it[UserPrizes.userId].value }
            val fromRating = RatingPrizes.slice(RatingPrizes.userId).select { RatingPrizes.claimed eq false }.map { it[RatingPrizes.userId].value }
            val fromReferral = ReferralRewards.slice(ReferralRewards.userId).select { ReferralRewards.claimed eq false }.map { it[ReferralRewards.userId].value }
            val fromClubs = ClubWeeklyRewards.slice(ClubWeeklyRewards.userId).select { ClubWeeklyRewards.claimed eq false }.map { it[ClubWeeklyRewards.userId].value }
            
            (fromPrizes + fromRating + fromReferral + fromClubs).distinct()
        }

        for (uid in userIds) {
            val user = transaction {
                Users.slice(Users.tgId, Users.language)
                    .select { (Users.id eq uid) and (Users.canReceiveNotifications eq true) }
                    .singleOrNull()
            } ?: continue

            val tgId = user[Users.tgId]
            val lang = user[Users.language]

            val text = service.I18n.text("🎁 У тебя есть неполученные призы! Давай скорее их заберем.", lang)
            val btnText = service.I18n.text("🎁 Мои призы", lang)

            notifications.sendNotification(uid, tgId, text, btnText, "/prizes")
            delay(100)
        }
    }
}
