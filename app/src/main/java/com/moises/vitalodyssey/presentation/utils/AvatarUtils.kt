package com.moises.vitalodyssey.presentation.utils

import com.moises.vitalodyssey.R
import com.moises.vitalodyssey.domain.model.BodyType
import com.moises.vitalodyssey.domain.model.PlayerClass

object AvatarUtils {
    @androidx.annotation.DrawableRes
    fun getPlayerAvatar(playerClass: PlayerClass?, bodyType: BodyType?): Int {
        return when (playerClass) {
            PlayerClass.WARRIOR -> if (bodyType == BodyType.FEMALE) R.drawable.char_warrior_female else R.drawable.char_warrior_male
            PlayerClass.MAGE -> if (bodyType == BodyType.FEMALE) R.drawable.char_mage_female else R.drawable.char_mage_male
            PlayerClass.DWARF -> if (bodyType == BodyType.FEMALE) R.drawable.char_dwarf_female else R.drawable.char_dwarf_male
            PlayerClass.ROGUE -> if (bodyType == BodyType.FEMALE) R.drawable.char_rogue_female else R.drawable.char_rogue_male
            null -> if (bodyType == BodyType.FEMALE) R.drawable.char_body_female else R.drawable.char_body_male
        }
    }
}
