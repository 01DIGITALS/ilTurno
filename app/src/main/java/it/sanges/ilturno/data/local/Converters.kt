package it.sanges.ilturno.data.local

import androidx.room.TypeConverter
import it.sanges.ilturno.domain.model.Meal
import java.time.LocalDate

class Converters {
    @TypeConverter fun dateToLong(date: LocalDate): Long = date.toEpochDay()
    @TypeConverter fun longToDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)
    @TypeConverter fun mealToString(meal: Meal): String = meal.name
    @TypeConverter fun stringToMeal(value: String): Meal = Meal.valueOf(value)
}
