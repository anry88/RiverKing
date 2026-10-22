package service

import db.DB
import db.Fish
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import support.testEnv
import java.io.File
import java.time.Instant
import kotlin.math.abs
import kotlin.test.*

class November2026EventTest {
    private val directory = File("docs/events/2026-11-south-america")
    private val manifest = Json.parseToJsonElement(File(directory, "event.json").readText()).jsonObject

    @Test
    fun approvedPoolCreatesCompleteFutureEventWithoutChangingRegularLocations() {
        DB.init(testEnv("november-2026"))
        val catalog = transaction { Fish.selectAll().associateBy { it[Fish.name] } }
        val pool = manifest.getValue("fish").jsonArray
        assertEquals(37, pool.size)
        assertEquals(37, pool.map { it.jsonObject.getValue("name").jsonPrimitive.content }.toSet().size)
        pool.forEach { entry ->
            val fish = entry.jsonObject
            val name = fish.getValue("name").jsonPrimitive.content
            assertEquals(fish.getValue("rarity").jsonPrimitive.content, catalog.getValue(name)[Fish.rarity])
            assertTrue(fish.getValue("weight").jsonPrimitive.double > 0)
        }
        val events = SpecialEventService()
        fun prizes(key: String): SpecialEventPrizeConfig {
            val config = manifest.getValue(key).jsonObject
            return SpecialEventPrizeConfig(config.getValue("prizePlaces").jsonPrimitive.int, config.getValue("prizes").toString())
        }
        val start = Instant.parse(manifest.getValue("start").jsonPrimitive.content)
        val end = Instant.parse(manifest.getValue("end").jsonPrimitive.content)
        val id = events.createEvent(
            nameRu = manifest.getValue("nameRu").jsonPrimitive.content,
            nameEn = manifest.getValue("nameEn").jsonPrimitive.content,
            start = start,
            end = end,
            imagePath = manifest.getValue("imagePath").jsonPrimitive.content,
            castZone = Json.decodeFromString<CastZoneDTO>(manifest.getValue("castZone").toString()),
            fish = pool.map {
                val entry = it.jsonObject
                SpecialEventFishSpec(catalog.getValue(entry.getValue("name").jsonPrimitive.content)[Fish.id].value, entry.getValue("weight").jsonPrimitive.double)
            },
            weightPrizes = prizes("weightPrizes"),
            countPrizes = prizes("countPrizes"),
            fishPrizes = prizes("fishPrizes"),
        )
        assertEquals(37, events.fishSpecs(id).size)
        val locationId = assertNotNull(events.eventLocationId(id))
        transaction {
            assertNull(events.isActiveEventLocationTx(locationId, start.minusSeconds(1)))
            assertEquals(id, events.isActiveEventLocationTx(locationId, start)?.id)
            assertNull(events.isActiveEventLocationTx(locationId, end.plusSeconds(1)))
        }
        assertEquals(3, events.listEvents().single().weightPrizePlaces)
        assertEquals(3, events.listEvents().single().countPrizePlaces)
        assertEquals(10, events.listEvents().single().fishPrizePlaces)
    }

    @Test
    fun baseRarityOddsMatchColumbiaForFreshwaterPredatorLure() {
        DB.init(testEnv("november-2026-balance"))
        val catalog = transaction { Fish.selectAll().associateBy { it[Fish.name] } }
        val previous = Json.parseToJsonElement(File(directory, "previous-event-pools.json").readText())
            .jsonArray.single { it.jsonObject.getValue("name_en").jsonPrimitive.content == "Columbia River Gorge" }
            .jsonObject.getValue("fish").jsonArray
        val oldWeights = previous.groupBy { it.jsonObject.getValue("rarity").jsonPrimitive.content }.mapValues { (_, rows) ->
            rows.sumOf {
                val fish = it.jsonObject
                fish.getValue("weight").jsonPrimitive.double *
                    (if (fish.getValue("predator").jsonPrimitive.boolean) 1.0 else 0.18) *
                    (if (fish.getValue("water").jsonPrimitive.content == "fresh") 1.0 else 0.65)
            }
        }
        val newWeights = manifest.getValue("fish").jsonArray.groupBy { it.jsonObject.getValue("rarity").jsonPrimitive.content }.mapValues { (_, rows) ->
            rows.sumOf {
                val fish = it.jsonObject
                val profile = catalog.getValue(fish.getValue("name").jsonPrimitive.content)
                fish.getValue("weight").jsonPrimitive.double * (if (profile[Fish.predator]) 1.0 else 0.18)
            }
        }
        assertEquals(oldWeights.keys, newWeights.keys)
        oldWeights.forEach { (rarity, weight) ->
            assertTrue(abs(weight - newWeights.getValue(rarity)) < 1e-8, "Additional base rarity boost: $rarity")
        }
    }
}
