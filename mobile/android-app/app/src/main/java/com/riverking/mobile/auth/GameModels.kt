package com.riverking.mobile.auth

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    val error: String? = null,
    val message: String? = null,
)

@Serializable
data class DailyRewardItemDto(
    val name: String,
    val qty: Int,
)

@Serializable
data class LocationDto(
    val id: Long,
    val name: String,
    val unlockKg: Double,
    val unlocked: Boolean,
)

@Serializable
data class RodDto(
    val id: Long,
    val code: String,
    val name: String,
    val unlockKg: Double,
    val unlocked: Boolean,
    val bonusWater: String? = null,
    val bonusPredator: Boolean? = null,
    val priceStars: Int? = null,
    val packId: String? = null,
)

@Serializable
data class RecentDto(
    val id: Long,
    val fish: String,
    val weight: Double,
    val location: String,
    val rarity: String,
    val at: String,
)

@Serializable
data class LureDto(
    val id: Long,
    val name: String,
    val qty: Int,
    val predator: Boolean,
    val water: String,
    val rarityBonus: Double,
    val displayName: String = name,
    val description: String = "",
)

@Serializable
data class StartCastResultDto(
    val currentLureId: Long? = null,
    val lureChanged: Boolean = false,
    val newLureName: String? = null,
    val recommendedRodId: Long? = null,
    val recommendedRodCode: String? = null,
    val recommendedRodName: String? = null,
    val recommendedRodUnlocked: Boolean? = null,
    val recommendedRodPriceStars: Int? = null,
    val recommendedRodPackId: String? = null,
)

@Serializable
data class HookRequestDto(
    val wait: Int,
    val reaction: Double,
)

@Serializable
data class HookResultDto(
    val success: Boolean,
    val autoFish: Boolean,
)

@Serializable
data class CastRequestDto(
    val wait: Int,
    val reaction: Double,
    val success: Boolean,
)

@Serializable
data class CatchDto(
    val id: Long,
    val fish: String,
    val weight: Double,
    val location: String,
    val rarity: String,
    val userId: Long? = null,
    val fishId: Long? = null,
    val user: String? = null,
    val at: String? = null,
    val rank: Int? = null,
    val prizeCoins: Int? = null,
)

@Serializable
data class QuestUpdateDto(
    val code: String,
    val period: String,
    val rewardCoins: Int,
    val name: String = "",
)

@Serializable
data class AchievementUnlockDto(
    val code: String,
    val newLevelIndex: Int,
)

@Serializable
data class CastResultDto(
    val caught: Boolean,
    val catch: CatchDto? = null,
    val autoFish: Boolean = false,
    val unlockedLocations: List<String> = emptyList(),
    val unlockedRods: List<String> = emptyList(),
    val coins: Int = 0,
    val totalCoins: Long? = null,
    val todayCoins: Long? = null,
    val achievements: List<AchievementUnlockDto> = emptyList(),
    val questUpdates: List<QuestUpdateDto> = emptyList(),
)

@Serializable
data class DailyClaimResponseDto(
    val lures: List<LureDto> = emptyList(),
    val currentLureId: Long? = null,
    val dailyStreak: Int = 0,
)

@Serializable
data class PrizeSpecDto(
    val packageId: String,
    val qty: Int,
    val coins: Int? = null,
)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int,
    val userId: Long? = null,
    val user: String? = null,
    val value: Double,
    val catchId: Long? = null,
    val fish: String? = null,
    val fishId: Long? = null,
    val location: String? = null,
    val at: Long? = null,
    val prize: PrizeSpecDto? = null,
)

@Serializable
data class TournamentDto(
    val id: Long,
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val fish: String? = null,
    val fishRarity: String? = null,
    val location: String? = null,
    val metric: String,
    val prizePlaces: Int,
)

@Serializable
data class CurrentTournamentDto(
    val tournament: TournamentDto,
    val leaderboard: List<LeaderboardEntryDto>,
    val mine: LeaderboardEntryDto? = null,
)

@Serializable
data class PrizeDto(
    val id: Long,
    val packageId: String,
    val qty: Int,
    val rank: Int,
    val coins: Int? = null,
    val source: String,
)

@Serializable
data class FishBriefDto(
    val name: String,
    val rarity: String,
)

@Serializable
data class GuideLocationDto(
    val id: Long,
    val name: String,
    val fish: List<FishBriefDto>,
    val lures: List<String>,
)

@Serializable
data class GuideFishDto(
    val id: Long,
    val name: String,
    val rarity: String,
    val locationIds: List<Long>,
    val locations: List<String>,
    val lures: List<String>,
)

@Serializable
data class GuideLureDto(
    val name: String,
    val fish: List<FishBriefDto>,
    val locations: List<String>,
)

@Serializable
data class GuideRodDto(
    val code: String,
    val name: String,
    val unlockKg: Double,
    val bonusWater: String? = null,
    val bonusPredator: Boolean? = null,
)

@Serializable
data class GuideDto(
    val locations: List<GuideLocationDto>,
    val fish: List<GuideFishDto>,
    val lures: List<GuideLureDto>,
    val rods: List<GuideRodDto>,
)

@Serializable
data class AchievementDto(
    val code: String,
    val name: String,
    val description: String,
    val level: String,
    val levelIndex: Int,
    val progress: Double,
    val target: Int,
    val progressLabel: String,
    val targetLabel: String,
    val claimable: Boolean,
)

@Serializable
data class QuestDto(
    val code: String,
    val period: String,
    val name: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val rewardCoins: Int,
    val completed: Boolean,
)

@Serializable
data class QuestListDto(
    val daily: List<QuestDto>,
    val weekly: List<QuestDto>,
)

@Serializable
data class AchievementRewardDto(
    val packageId: String,
    val qty: Int,
    val coins: Int? = null,
)

@Serializable
data class AchievementClaimDto(
    val code: String,
    val rewards: List<AchievementRewardDto> = emptyList(),
)

@Serializable
data class ShopPackageDto(
    val id: String,
    val name: String,
    val desc: String,
    val price: Int,
    val until: String? = null,
    val originalPrice: Int? = null,
    val discountStart: String? = null,
    val discountEnd: String? = null,
    val coinPrice: Int? = null,
    val rodCode: String? = null,
)

@Serializable
data class ShopCategoryDto(
    val id: String,
    val name: String,
    val packs: List<ShopPackageDto> = emptyList(),
)

@Serializable
data class ShopPurchaseResultDto(
    val lures: List<LureDto> = emptyList(),
    val currentLureId: Long? = null,
)

@Serializable
data class ReferralInfoDto(
    val token: String,
    val invited: List<String> = emptyList(),
    val link: String,
    val telegramLink: String = link,
    val androidShareText: String = "",
    val webFallbackLink: String = link,
)

@Serializable
data class ReferralLinkDto(
    val token: String,
    val link: String,
    val telegramLink: String = link,
    val androidShareText: String = "",
    val webFallbackLink: String = link,
)

@Serializable
data class ReferralRewardDto(
    val packageId: String,
    val qty: Int,
    val name: String,
)

@Serializable
data class ClubMemberDto(
    val userId: Long,
    val name: String? = null,
    val role: String,
    val coins: Int,
)

@Serializable
data class ClubWeekDto(
    val weekStart: String,
    val totalCoins: Int,
    val members: List<ClubMemberDto> = emptyList(),
)

@Serializable
data class ClubDetailsDto(
    val id: Long,
    val name: String,
    val role: String,
    val memberCount: Int,
    val capacity: Int,
    val info: String,
    val minJoinWeightKg: Double,
    val recruitingOpen: Boolean,
    val currentWeek: ClubWeekDto,
    val previousWeek: ClubWeekDto,
)

@Serializable
data class ClubChatMessageDto(
    val id: Long,
    val message: String,
    val createdAt: String,
)

@Serializable
data class ClubSummaryDto(
    val id: Long,
    val name: String,
    val memberCount: Int,
    val capacity: Int,
    val info: String,
    val minJoinWeightKg: Double,
    val recruitingOpen: Boolean,
)

@Serializable
data class ClubCreateRequestDto(
    val name: String,
)

@Serializable
data class ClubInfoRequestDto(
    val info: String,
)

@Serializable
data class ClubSettingsRequestDto(
    val minJoinWeightKg: Double,
    val recruitingOpen: Boolean,
)

@Serializable
data class PlayPurchaseCompleteRequestDto(
    val purchaseToken: String,
    val orderId: String? = null,
    val purchaseTimeMillis: Long? = null,
)

@Serializable
data class RarityStatDto(
    val rarity: String,
    val count: Long,
    val weight: Double,
)

@Serializable
data class CatchStatsDto(
    val totalWeight: Double,
    val totalCount: Long,
    val byRarity: List<RarityStatDto> = emptyList(),
)
