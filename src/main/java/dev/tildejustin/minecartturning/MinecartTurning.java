package dev.tildejustin.minecartturning;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class MinecartTurning implements ClientModInitializer {
    private static boolean modEnabled = true;
    private KeyBinding toggleKeybind;

    public static boolean isModEnabled() {
        return modEnabled;
    }

    @Override
    public void onInitializeClient() {
        if (!FabricLoader.getInstance().isModLoaded("fabric-api")) {
            return;
        }

        toggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding("minecart-turning.toggle", GLFW.GLFW_KEY_UNKNOWN, "minecart-turning.category"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKeybind.wasPressed()) {
                modEnabled = !modEnabled;
            }
        });
    }
}
