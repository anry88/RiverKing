package service

import db.AccountDeletionRequests
import db.AccountLinkSessions
import db.AchievementProgress
import db.AuthIdentities
import db.AuthSessions
import db.ClubMembers
import db.ClubQuestMemberProgress
import db.ClubQuestRewardRecipients
import db.ClubWeeklyContributions
import db.ClubWeeklyRewards
import db.ClubWeeklySnapshots
import db.Catches
import db.InventoryLures
import db.InventoryRods
import db.PasswordCredentials
import db.PaySupportRequests
import db.Payments
import db.PendingCatches
import db.QuestProgress
import db.RatingPrizes
import db.ReferralLinks
import db.ReferralRewards
import db.SubscriptionNotifications
import db.UserPrizes
import db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class AccountDeletionService(
    private val clubs: ClubService = ClubService(),
) {
    data class PublicDeletionRequest(
        val requestedLogin: String? = null,
        val authProvider: String? = null,
        val contact: String,
        val note: String? = null,
    )

    fun deleteAccount(userId: Long) {
        runCatching { clubs.leaveClub(userId) }

        transaction {
            val existing = Users.select { Users.id eq userId }.singleOrNull() ?: return@transaction

            Users.update({ Users.referredBy eq userId }) {
                it[referredBy] = null
            }

            AccountLinkSessions.deleteWhere {
                (AccountLinkSessions.requesterUserId eq userId) or
                    (AccountLinkSessions.resolvedUserId eq userId)
            }
            AuthSessions.deleteWhere { AuthSessions.userId eq userId }
            PasswordCredentials.deleteWhere { PasswordCredentials.userId eq userId }
            AuthIdentities.deleteWhere { AuthIdentities.userId eq userId }
            SubscriptionNotifications.deleteWhere { SubscriptionNotifications.userId eq userId }
            PendingCatches.deleteWhere { PendingCatches.userId eq userId }
            InventoryLures.deleteWhere { InventoryLures.userId eq userId }
            InventoryRods.deleteWhere { InventoryRods.userId eq userId }
            Catches.deleteWhere { Catches.userId eq userId }
            PaySupportRequests.deleteWhere { PaySupportRequests.userId eq userId }
            Payments.deleteWhere { Payments.userId eq userId }
            UserPrizes.deleteWhere { UserPrizes.userId eq userId }
            RatingPrizes.deleteWhere { RatingPrizes.userId eq userId }
            ReferralLinks.deleteWhere { ReferralLinks.userId eq userId }
            ReferralRewards.deleteWhere { ReferralRewards.userId eq userId }
            AchievementProgress.deleteWhere { AchievementProgress.userId eq userId }
            QuestProgress.deleteWhere { QuestProgress.userId eq userId }
            ClubQuestMemberProgress.deleteWhere { ClubQuestMemberProgress.userId eq userId }
            ClubQuestRewardRecipients.deleteWhere { ClubQuestRewardRecipients.userId eq userId }
            ClubWeeklyContributions.deleteWhere { ClubWeeklyContributions.userId eq userId }
            ClubWeeklySnapshots.deleteWhere { ClubWeeklySnapshots.userId eq userId }
            ClubWeeklyRewards.deleteWhere { ClubWeeklyRewards.userId eq userId }
            ClubMembers.deleteWhere { ClubMembers.userId eq userId }
            AccountDeletionRequests.deleteWhere { AccountDeletionRequests.userId eq userId }
            Users.deleteWhere { Users.id eq existing[Users.id].value }
        }
    }

    fun createPublicRequest(request: PublicDeletionRequest): Long = transaction {
        AccountDeletionRequests.insertAndGetId {
            it[userId] = null
            it[requestedLogin] = request.requestedLogin?.trim()?.takeIf(String::isNotEmpty)
            it[authProvider] = request.authProvider?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
            it[contact] = request.contact.trim()
            it[note] = request.note?.trim()?.takeIf(String::isNotEmpty)
            it[status] = "pending"
            it[createdAt] = Instant.now()
        }.value
    }
}
