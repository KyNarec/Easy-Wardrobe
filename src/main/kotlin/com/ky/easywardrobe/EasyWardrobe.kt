package com.ky.easywardrobe

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.slf4j.LoggerFactory

object EasyWardrobe : ModInitializer {
    private val logger = LoggerFactory.getLogger("easy-wardrobe")

    override fun onInitialize() {
        logger.info("Initializing Easy Wardrobe...")

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            WardrobeCommand.register(dispatcher)
        }
    }
}
