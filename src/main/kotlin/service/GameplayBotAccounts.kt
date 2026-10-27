package service

import db.AuthIdentities
import db.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class StubAccount(val slot: String, val nickname: String, val language: String)

object GameplayBotAccounts {
    const val PROVIDER = "gameplay-stub"
    private val russian = listOf(
        "Лёха с берега", "Санёк на рыбалке", "Дядя Вова", "Серёга без клёва", "Димон у воды",
        "Михалыч", "Вовчик с удочкой", "Пашка на речке", "Жека с термосом", "Толяныч",
        "Костян на клёве", "Витёк у камышей", "Ромыч", "Игорёк рыбачит", "Андрюха с лодкой",
        "Макс на часок", "Валера с дачи", "Денчик", "Юрок на поплавок", "Василич",
        "Коля после смены", "Антоха у моста", "Стёпа без спешки", "Гоша с прикормом", "Боря на зорьке",
    )
    private val english = listOf(
        "Mikey NoLuck", "Sleepy Dan", "Uncle Joe", "Dave ByTheLake", "Big Pete",
        "Tommy TwoRods", "Lil Kev", "Jimmy GotBait", "Just Jack", "Benny OnBreak",
        "Old Man Rob", "Chill Nate", "Sammy Casts", "Chris AfterWork", "Alfie NoBites",
        "Joey ByTheDock", "Lazy Luke", "Franky GoneFishin", "Eddie OneMore", "Billy WetSocks",
        "Harry HalfAsleep", "Charlie ReelSlow", "Ronnie OnTheBank", "Andy NeedsCoffee", "Freddie NotYet",
    )
    val accounts: List<StubAccount> =
        (russian.map { it to "ru" } + english.map { it to "en" }).mapIndexed { index, (name, lang) ->
            StubAccount("v1-${(index + 1).toString().padStart(2, '0')}", name, lang)
        }

    /** Server-owned provisioning; existing identities keep their profile and progression. */
    @Synchronized
    fun provision(fishing: FishingService): List<Long> = accounts.map { account ->
        transaction {
            val existing = AuthIdentities.select {
                (AuthIdentities.provider eq PROVIDER) and (AuthIdentities.subject eq account.slot)
            }.singleOrNull()
            if (existing != null) return@transaction existing[AuthIdentities.userId].value
            val userId = fishing.createUser(language = account.language, registrationSource = "bot")
            fishing.setNickname(userId, account.nickname)
            Users.update({ Users.id eq userId }) { it[canReceiveNotifications] = false }
            AuthIdentities.insert {
                it[AuthIdentities.userId] = userId
                it[provider] = PROVIDER
                it[subject] = account.slot
            }
            userId
        }
    }
}
