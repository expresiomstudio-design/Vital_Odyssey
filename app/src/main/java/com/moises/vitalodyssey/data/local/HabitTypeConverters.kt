package com.moises.vitalodyssey.data.local

import androidx.room.TypeConverter
import com.moises.vitalodyssey.domain.model.Frequency
import com.moises.vitalodyssey.domain.model.HabitRole
import com.moises.vitalodyssey.domain.model.HabitType
import com.moises.vitalodyssey.domain.model.TargetType
import com.moises.vitalodyssey.domain.model.BodyType
import com.moises.vitalodyssey.domain.model.PlayerClass

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

    @TypeConverter
    fun fromHabitRole(value: HabitRole): String = value.name

    @TypeConverter
    fun toHabitRole(value: String): HabitRole = HabitRole.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")

    @TypeConverter
    fun fromBodyType(value: BodyType?): String? = value?.name

    @TypeConverter
    fun toBodyType(value: String?): BodyType? = value?.let { BodyType.valueOf(it) }

    @TypeConverter
    fun fromPlayerClass(value: PlayerClass?): String? = value?.name

    @TypeConverter
    fun toPlayerClass(value: String?): PlayerClass? = value?.let { PlayerClass.valueOf(it) }
}