package com.moises.vitalodyssey.data.local

import androidx.room.TypeConverter
import com.moises.vitalodyssey.domain.model.Frequency
import com.moises.vitalodyssey.domain.model.HabitType
import com.moises.vitalodyssey.domain.model.TargetType

class HabitTypeConverters {

    @TypeConverter
    fun fromHabitType(value: HabitType): String = value.name

    @TypeConverter
    fun toHabitType(value: String): HabitType = HabitType.valueOf(value)

    @TypeConverter
    fun fromTargetType(value: TargetType): String = value.name

    @TypeConverter
    fun toTargetType(value: String): TargetType = TargetType.valueOf(value)

    @TypeConverter
    fun fromFrequency(value: Frequency): String = value.name

    @TypeConverter
    fun toFrequency(value: String): Frequency = Frequency.valueOf(value)
}