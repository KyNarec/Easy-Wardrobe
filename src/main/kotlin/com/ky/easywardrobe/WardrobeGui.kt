package com.ky.easywardrobe

import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object WardrobeGui {
    private const val ROWS = 5
    private const val COLS = 9
    private const val SIZE = COLS * ROWS

    fun open(player: ServerPlayerEntity) {
        val registryLookup = player.registryManager
        val wardrobeData = WardrobeStorage.loadWardrobeData(player, registryLookup)

        val visualInventory = createVisualInventory(wardrobeData)

        val factory =
                SimpleNamedScreenHandlerFactory(
                        { syncId: Int, playerInv: PlayerInventory, _: PlayerEntity ->
                            WardrobeScreenHandler(
                                    syncId,
                                    playerInv,
                                    visualInventory,
                                    wardrobeData,
                                    player
                            )
                        },
                        Text.of("Wardrobe (1/1)")
                )
        player.openHandledScreen(factory)
    }

    private fun createVisualInventory(data: WardrobeData): Inventory {
        val inv = SimpleInventory(SIZE)

        for (col in 0 until COLS) {
            val set = data.sets[col]
            val isActive = (data.activeSet == col)

            // Render Armor
            for (row in 0 until 4) {
                val stack = set.armor[row].copy()
                if (isActive && !stack.isEmpty) {
                    // Add "Equipped" tooltip or similar if possible
                    // We can't easily modify client-side tooltip without mixins or NBT hacks
                    // But we can set custom name color
                    val name = stack.getName()
                    stack.set(
                            DataComponentTypes.CUSTOM_NAME,
                            Text.empty().append(name).formatted(Formatting.RED)
                    )
                }
                inv.setStack(col + (row * 9), stack)
            }

            // Render Button
            val button =
                    if (isActive) {
                        ItemStack(Items.LIME_DYE).apply {
                            set(DataComponentTypes.CUSTOM_NAME, Text.of("§aEquipped"))
                        }
                    } else {
                        val isEmpty = set.armor.all { it.isEmpty }
                        if (isEmpty) {
                            ItemStack(Items.GRAY_DYE).apply {
                                set(DataComponentTypes.CUSTOM_NAME, Text.of("§7Empty Slot"))
                            }
                        } else {
                            ItemStack(Items.PINK_DYE).apply {
                                set(DataComponentTypes.CUSTOM_NAME, Text.of("§eClick to Equip"))
                            }
                        }
                    }
            inv.setStack(col + 36, button)
        }

        return inv
    }

    class WardrobeScreenHandler(
            syncId: Int,
            playerInventory: PlayerInventory,
            private val visualInventory: Inventory,
            private val wardrobeData: WardrobeData,
            private val player: ServerPlayerEntity
    ) :
            GenericContainerScreenHandler(
                    net.minecraft.screen.ScreenHandlerType.GENERIC_9X5,
                    syncId,
                    playerInventory,
                    visualInventory,
                    ROWS
            ) {

        override fun onSlotClick(
                slotIndex: Int,
                button: Int,
                actionType: SlotActionType,
                player: PlayerEntity
        ) {
            if (slotIndex in 0 until SIZE) {
                val col = slotIndex % 9
                val row = slotIndex / 9

                // If this column is the active set, prevent interaction with armor slots
                if (wardrobeData.activeSet == col && row < 4) {
                    return
                }

                if (row < 4) {
                    super.onSlotClick(slotIndex, button, actionType, player)

                    val stack = visualInventory.getStack(slotIndex)
                    wardrobeData.sets[col].armor[row] = stack.copy()

                    updateButtons()
                    save()
                    return
                } else if (row == 4) {
                    // Equip Button
                    if (wardrobeData.activeSet != col) {
                        equipSet(col)
                    }
                    return
                } else {
                    return
                }
            }

            super.onSlotClick(slotIndex, button, actionType, player)
        }

        private fun updateButtons() {
            for (col in 0 until COLS) {
                val set = wardrobeData.sets[col]
                val isActive = (wardrobeData.activeSet == col)

                val button =
                        if (isActive) {
                            ItemStack(Items.LIME_DYE).apply {
                                set(DataComponentTypes.CUSTOM_NAME, Text.of("§aEquipped"))
                            }
                        } else {
                            val isEmpty = set.armor.all { it.isEmpty }
                            if (isEmpty) {
                                ItemStack(Items.GRAY_DYE).apply {
                                    set(DataComponentTypes.CUSTOM_NAME, Text.of("§7Empty Slot"))
                                }
                            } else {
                                ItemStack(Items.PINK_DYE).apply {
                                    set(DataComponentTypes.CUSTOM_NAME, Text.of("§eClick to Equip"))
                                }
                            }
                        }
                visualInventory.setStack(col + 36, button)
            }
        }

        private fun equipSet(newCol: Int) {
            val oldCol = wardrobeData.activeSet

            // 1. Handle Old Armor
            if (oldCol != -1) {
                // Save current player armor to old set
                val oldSet = wardrobeData.sets[oldCol]
                oldSet.armor[0] = player.getEquippedStack(EquipmentSlot.HEAD).copy()
                oldSet.armor[1] = player.getEquippedStack(EquipmentSlot.CHEST).copy()
                oldSet.armor[2] = player.getEquippedStack(EquipmentSlot.LEGS).copy()
                oldSet.armor[3] = player.getEquippedStack(EquipmentSlot.FEET).copy()

                // Update visual inventory for old set (remove red tint if applied, restore items)
                for (row in 0 until 4) {
                    visualInventory.setStack(oldCol + (row * 9), oldSet.armor[row].copy())
                }
            } else {
                // Player has armor but no active set. Dump to inventory.
                val slots =
                        listOf(
                                EquipmentSlot.HEAD,
                                EquipmentSlot.CHEST,
                                EquipmentSlot.LEGS,
                                EquipmentSlot.FEET
                        )
                for (slot in slots) {
                    val stack = player.getEquippedStack(slot)
                    if (!stack.isEmpty) {
                        if (!player.inventory.insertStack(stack)) {
                            player.dropItem(stack, false)
                        }
                        player.equipStack(slot, ItemStack.EMPTY)
                    }
                }
            }

            // 2. Equip New Set
            val newSet = wardrobeData.sets[newCol]

            // Move items to player
            player.equipStack(EquipmentSlot.HEAD, newSet.armor[0].copy())
            player.equipStack(EquipmentSlot.CHEST, newSet.armor[1].copy())
            player.equipStack(EquipmentSlot.LEGS, newSet.armor[2].copy())
            player.equipStack(EquipmentSlot.FEET, newSet.armor[3].copy())

            // 3. Update Active Set
            wardrobeData.activeSet = newCol

            // 4. Update Visuals for New Set (Ghost Items)
            // The items remain in newSet.armor (we copied them).
            // We just need to update the visual inventory to show them (maybe with red tint)
            for (row in 0 until 4) {
                val stack = newSet.armor[row].copy()
                if (!stack.isEmpty) {
                    val name = stack.getName()
                    stack.set(
                            DataComponentTypes.CUSTOM_NAME,
                            Text.empty().append(name).formatted(Formatting.RED)
                    )
                }
                visualInventory.setStack(newCol + (row * 9), stack)
            }

            updateButtons()
            save()
        }

        private fun save() {
            WardrobeStorage.saveWardrobeData(player, wardrobeData, player.registryManager)
        }

        override fun onClosed(player: PlayerEntity) {
            super.onClosed(player)
            save()
        }
    }
}
