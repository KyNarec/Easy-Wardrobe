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

    private val GLASS_PANES =
            listOf(
                    Items.RED_STAINED_GLASS_PANE,
                    Items.ORANGE_STAINED_GLASS_PANE,
                    Items.YELLOW_STAINED_GLASS_PANE,
                    Items.LIME_STAINED_GLASS_PANE,
                    Items.GREEN_STAINED_GLASS_PANE,
                    Items.CYAN_STAINED_GLASS_PANE,
                    Items.LIGHT_BLUE_STAINED_GLASS_PANE,
                    Items.BLUE_STAINED_GLASS_PANE,
                    Items.PURPLE_STAINED_GLASS_PANE
            )

    fun open(player: ServerPlayerEntity) {
        val registryLookup = player.registryManager
        val wardrobeData = WardrobeStorage.loadWardrobeData(player, registryLookup)

        val visualInventory = SimpleInventory(SIZE)
        // Populate inventory
        for (col in 0 until COLS) {
            updateColumn(visualInventory, wardrobeData, col)
        }

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

    private fun updateColumn(inv: Inventory, data: WardrobeData, col: Int) {
        val set = data.sets[col]
        val isActive = (data.activeSet == col)
        val glassItem = GLASS_PANES[col % GLASS_PANES.size]

        // Render Armor (Rows 0-3)
        for (row in 0 until 4) {
            val stack = set.armor[row].copy()
            if (stack.isEmpty) {
                // Place Glass Pane
                val glassStack =
                        ItemStack(glassItem).apply {
                            set(DataComponentTypes.CUSTOM_NAME, Text.of(" "))
                        }
                inv.setStack(col + (row * 9), glassStack)
            } else {
                // Place Armor
                if (isActive) {
                    val name = stack.getName()
                    stack.set(
                            DataComponentTypes.CUSTOM_NAME,
                            Text.empty().append(name).formatted(Formatting.RED)
                    )
                }
                inv.setStack(col + (row * 9), stack)
            }
        }

        // Render Button (Row 4)
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
                    // Armor Slot
                    val currentVisual = visualInventory.getStack(slotIndex)
                    val isGlass = GLASS_PANES.contains(currentVisual.item)

                    if (isGlass) {
                        val cursorStack = cursorStack
                        if (!cursorStack.isEmpty) {
                            wardrobeData.sets[col].armor[row] = cursorStack.copy()
                            cursorStack.count = 0
                            updateColumn(visualInventory, wardrobeData, col)
                            save()
                        }
                        return
                    } else {
                        super.onSlotClick(slotIndex, button, actionType, player)
                        val newStack = visualInventory.getStack(slotIndex)
                        if (newStack.isEmpty) {
                            wardrobeData.sets[col].armor[row] = ItemStack.EMPTY
                        } else {
                            wardrobeData.sets[col].armor[row] = newStack.copy()
                        }
                        updateColumn(visualInventory, wardrobeData, col)
                        save()
                        return
                    }
                } else if (row == 4) {
                    // Equip Button
                    if (wardrobeData.activeSet != col) {
                        equipSet(col)
                    } else {
                        unequipSet(col)
                    }
                    return
                } else {
                    return
                }
            }

            super.onSlotClick(slotIndex, button, actionType, player)
        }

        private fun equipSet(newCol: Int) {
            val oldCol = wardrobeData.activeSet

            // 1. Handle Old Armor
            if (oldCol != -1) {
                val oldSet = wardrobeData.sets[oldCol]
                oldSet.armor[0] = player.getEquippedStack(EquipmentSlot.HEAD).copy()
                oldSet.armor[1] = player.getEquippedStack(EquipmentSlot.CHEST).copy()
                oldSet.armor[2] = player.getEquippedStack(EquipmentSlot.LEGS).copy()
                oldSet.armor[3] = player.getEquippedStack(EquipmentSlot.FEET).copy()
            } else {
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
            player.equipStack(EquipmentSlot.HEAD, newSet.armor[0].copy())
            player.equipStack(EquipmentSlot.CHEST, newSet.armor[1].copy())
            player.equipStack(EquipmentSlot.LEGS, newSet.armor[2].copy())
            player.equipStack(EquipmentSlot.FEET, newSet.armor[3].copy())

            // 3. Update Active Set
            wardrobeData.activeSet = newCol

            // 4. Update Visuals
            // Update old column AFTER changing activeSet so it renders as inactive
            if (oldCol != -1) {
                updateColumn(visualInventory, wardrobeData, oldCol)
            }
            updateColumn(visualInventory, wardrobeData, newCol)

            save()
        }

        private fun unequipSet(col: Int) {
            val set = wardrobeData.sets[col]

            // Move Player Armor -> Set
            set.armor[0] = player.getEquippedStack(EquipmentSlot.HEAD).copy()
            set.armor[1] = player.getEquippedStack(EquipmentSlot.CHEST).copy()
            set.armor[2] = player.getEquippedStack(EquipmentSlot.LEGS).copy()
            set.armor[3] = player.getEquippedStack(EquipmentSlot.FEET).copy()

            // Clear Player Armor
            player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY)
            player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY)
            player.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY)
            player.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY)

            wardrobeData.activeSet = -1
            updateColumn(visualInventory, wardrobeData, col)
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
