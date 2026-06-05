package com.maksimowiczm.foodyou.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.maksimowiczm.foodyou.fooddiary.domain.entity.DiaryEntry
import com.maksimowiczm.foodyou.fooddiary.domain.entity.DiaryMeal
import com.maksimowiczm.foodyou.fooddiary.domain.entity.FoodDiaryEntry
import com.maksimowiczm.foodyou.fooddiary.domain.entity.ManualDiaryEntry
import com.maksimowiczm.foodyou.fooddiary.domain.entity.Meal
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime

class HealthConnectManager(private val context: Context) {

    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getWritePermission(NutritionRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
        )

        private const val MEAL_TYPE_UNKNOWN = 0
        private const val MEAL_TYPE_BREAKFAST = 1
        private const val MEAL_TYPE_LUNCH = 2
        private const val MEAL_TYPE_DINNER = 3
        private const val MEAL_TYPE_SNACK = 4
    }

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    val client: HealthConnectClient?
        get() = if (isAvailable) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasPermissions(): Boolean {
        val client = client ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    }

    suspend fun syncDiaryForDate(date: LocalDate, diaryMeals: List<DiaryMeal>) {
        val client = client ?: return

        val tz = TimeZone.currentSystemDefault()
        val startOfDay = date.atStartOfDayIn(tz)
            .let { java.time.Instant.ofEpochSecond(it.epochSeconds, it.nanosecondsOfSecond.toLong()) }
        val endOfDay = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
            .let { java.time.Instant.ofEpochSecond(it.epochSeconds, it.nanosecondsOfSecond.toLong()) }
        val dataOrigin = DataOrigin(context.packageName)

        val existing = client.readRecords(
            ReadRecordsRequest(
                recordType = NutritionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                dataOriginFilter = setOf(dataOrigin),
            )
        )
        if (existing.records.isNotEmpty()) {
            client.deleteRecords(
                recordType = NutritionRecord::class,
                recordIdsList = existing.records.map { it.metadata.id },
                clientRecordIdsList = emptyList(),
            )
        }

        val records = buildRecords(diaryMeals, date)
        if (records.isNotEmpty()) {
            client.insertRecords(records)
        }
    }

    private fun buildRecords(diaryMeals: List<DiaryMeal>, date: LocalDate): List<NutritionRecord> {
        val zoneOffset = ZoneId.systemDefault().rules.getOffset(java.time.Instant.now())
        val records = mutableListOf<NutritionRecord>()

        for (diaryMeal in diaryMeals) {
            val meal = diaryMeal.meal
            val (startInstant, endInstant) = mealTimeRange(meal, date, zoneOffset)

            for (entry in diaryMeal.entries) {
                records += buildRecord(entry, meal, startInstant, endInstant, zoneOffset)
            }
        }

        return records
    }

    private fun mealTimeRange(
        meal: Meal,
        date: LocalDate,
        zoneOffset: ZoneOffset,
    ): Pair<java.time.Instant, java.time.Instant> {
        val start = localTimeToInstant(meal.from, date, zoneOffset)
        val end = if (meal.from == meal.to) {
            start.plusSeconds(1800)
        } else {
            localTimeToInstant(meal.to, date, zoneOffset).let {
                if (it <= start) it.plusSeconds(86400) else it
            }
        }
        return start to end
    }

    private fun localTimeToInstant(
        time: LocalTime,
        date: LocalDate,
        zoneOffset: ZoneOffset,
    ): java.time.Instant =
        date.toJavaLocalDate()
            .atTime(time.toJavaLocalTime())
            .toInstant(zoneOffset)

    private fun buildRecord(
        entry: DiaryEntry,
        meal: Meal,
        startInstant: java.time.Instant,
        endInstant: java.time.Instant,
        zoneOffset: ZoneOffset,
    ): NutritionRecord {
        val nutrition = entry.nutritionFacts
        val clientId = when (entry) {
            is FoodDiaryEntry -> "foodyou_food_${entry.id.value}"
            is ManualDiaryEntry -> "foodyou_manual_${entry.id.value}"
            else -> "foodyou_${entry.name.hashCode()}_${entry.createdAt.hashCode()}"
        }

        return NutritionRecord(
            startTime = startInstant,
            endTime = endInstant,
            startZoneOffset = zoneOffset,
            endZoneOffset = zoneOffset,
            name = entry.name,
            energy = nutrition.energy.value?.let { Energy.kilocalories(it) },
            protein = nutrition.proteins.value?.let { Mass.grams(it) },
            totalCarbohydrate = nutrition.carbohydrates.value?.let { Mass.grams(it) },
            totalFat = nutrition.fats.value?.let { Mass.grams(it) },
            saturatedFat = nutrition.saturatedFats.value?.let { Mass.grams(it) },
            dietaryFiber = nutrition.dietaryFiber.value?.let { Mass.grams(it) },
            sodium = nutrition.sodium.value?.let { Mass.grams(it) },
            cholesterol = nutrition.cholesterol.value?.let { Mass.grams(it) },
            sugar = nutrition.sugars.value?.let { Mass.grams(it) },
            potassium = nutrition.potassium.value?.let { Mass.grams(it) },
            calcium = nutrition.calcium.value?.let { Mass.grams(it) },
            iron = nutrition.iron.value?.let { Mass.grams(it) },
            vitaminA = nutrition.vitaminA.value?.let { Mass.grams(it) },
            vitaminC = nutrition.vitaminC.value?.let { Mass.grams(it) },
            mealType = mealTypeFor(meal),
            metadata = Metadata.manualEntry(clientRecordId = clientId),
        )
    }

    private fun mealTypeFor(meal: Meal): Int = when (meal.rank) {
        1 -> MEAL_TYPE_BREAKFAST
        2 -> MEAL_TYPE_LUNCH
        3 -> MEAL_TYPE_DINNER
        else -> MEAL_TYPE_SNACK
    }
}
