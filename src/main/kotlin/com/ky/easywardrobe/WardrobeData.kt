package com.ky.easywardrobe

import net.minecraft.item.ItemStack

data class WardrobeSet(val armor: MutableList<ItemStack> = MutableList(4) { ItemStack.EMPTY })

data class WardrobeData(
        val sets: MutableList<WardrobeSet> = MutableList(9) { WardrobeSet() },
        var activeSet: Int = -1
)
