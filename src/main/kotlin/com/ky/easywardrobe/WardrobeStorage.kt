package com.ky.easywardrobe

import java.io.File
import java.nio.file.Files
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.registry.RegistryWrapper

object WardrobeStorage {
    private val configDir =
            FabricLoader.getInstance().configDir.resolve("easy-wardrobe").resolve("players")

    init {
        Files.createDirectories(configDir)
    }

    fun loadWardrobeData(
            player: PlayerEntity,
            registryLookup: RegistryWrapper.WrapperLookup
    ): WardrobeData {
        val file = getPlayerFile(player)
        val data = WardrobeData()

        if (file.exists()) {
            try {
                val nbt = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.of(Long.MAX_VALUE))

                val setsOptional = nbt.getList("Sets")
                if (setsOptional.isPresent) {
                    val setsList = setsOptional.get()
                    for (i in 0 until setsList.size) {
                        if (i >= data.sets.size) break

                        val setElement = setsList.get(i)
                        if (setElement is NbtCompound) {
                            val armorListOptional = setElement.getList("Armor")
                            if (armorListOptional.isPresent) {
                                val armorList = armorListOptional.get()
                                val set = data.sets[i]
                                for (j in 0 until armorList.size) {
                                    if (j >= set.armor.size) break

                                    val itemElement = armorList.get(j)
                                    if (itemElement is NbtCompound) {
                                        val itemTag = itemElement
                                        if (itemTag.contains("ItemData")) {
                                            val dataTagOptional = itemTag.getCompound("ItemData")
                                            if (dataTagOptional.isPresent) {
                                                val stack =
                                                        ItemStack.CODEC
                                                                .parse(
                                                                        registryLookup.getOps(
                                                                                NbtOps.INSTANCE
                                                                        ),
                                                                        dataTagOptional.get()
                                                                )
                                                                .result()
                                                                .orElse(ItemStack.EMPTY)
                                                set.armor[j] = stack
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Load activeSet
                if (nbt.contains("ActiveSet")) {
                    val activeSetOptional = nbt.getInt("ActiveSet")
                    if (activeSetOptional.isPresent) {
                        data.activeSet = activeSetOptional.get()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return data
    }

    fun saveWardrobeData(
            player: PlayerEntity,
            data: WardrobeData,
            registryLookup: RegistryWrapper.WrapperLookup
    ) {
        val file = getPlayerFile(player)
        val nbt = NbtCompound()
        val setsList = NbtList()

        for (set in data.sets) {
            val setTag = NbtCompound()
            val armorList = NbtList()

            for (stack in set.armor) {
                val itemTag = NbtCompound()
                if (!stack.isEmpty) {
                    val encoded =
                            ItemStack.CODEC.encodeStart(
                                    registryLookup.getOps(NbtOps.INSTANCE),
                                    stack
                            )
                    encoded.result().ifPresent { tag -> itemTag.put("ItemData", tag) }
                }
                armorList.add(itemTag)
            }
            setTag.put("Armor", armorList)
            setsList.add(setTag)
        }
        nbt.put("Sets", setsList)
        nbt.putInt("ActiveSet", data.activeSet)

        try {
            Files.createDirectories(file.parentFile.toPath())
            NbtIo.writeCompressed(nbt, file.toPath())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPlayerFile(player: PlayerEntity): File {
        return configDir.resolve("${player.uuid}.dat").toFile()
    }
}
