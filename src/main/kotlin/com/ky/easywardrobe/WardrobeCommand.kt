package com.ky.easywardrobe

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object WardrobeCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
                CommandManager.literal("wardrobe").executes { ctx -> openWardrobe(ctx) }
        )
        dispatcher.register(CommandManager.literal("w").executes { ctx -> openWardrobe(ctx) })
    }

    private fun openWardrobe(ctx: CommandContext<ServerCommandSource>): Int {
        val source = ctx.source
        val player = source.player
        if (player == null) {
            source.sendError(Text.of("This command can only be executed by a player."))
            return 0
        }

        WardrobeGui.open(player)
        return 1
    }
}
