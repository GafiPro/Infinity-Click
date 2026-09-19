package com.gafipro.infinityclick;

import com.gafipro.infinityclick.mixin.MinecraftClientInvoker;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class InfinityClickClient implements ClientModInitializer {
    private static final int MIN_CPS = 1;
    private static final int MAX_CPS = 1000;

    private boolean enabled = true;
    private boolean wasHeld;
    private int clicksPerSecond = 12;
    private long lastClickNanos;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("infinityclick")
                            .then(ClientCommandManager.literal("toggle")
                                    .executes(context -> {
                                        enabled = !enabled;
                                        context.getSource().sendFeedback(
                                                Text.literal("Infinity Click: " + (enabled ? "ON" : "OFF"))
                                        );
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("cps")
                                    .then(ClientCommandManager.argument(
                                                    "number",
                                                    IntegerArgumentType.integer(MIN_CPS, MAX_CPS)
                                            )
                                            .executes(context -> {
                                                clicksPerSecond = IntegerArgumentType.getInteger(context, "number");
                                                context.getSource().sendFeedback(
                                                        Text.literal("Infinity Click CPS set to " + clicksPerSecond)
                                                );
                                                return 1;
                                            })))
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        boolean physicallyHeld = GLFW.glfwGetMouseButton(
                handle,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT
        ) == GLFW.GLFW_PRESS;

        boolean active = enabled
                && physicallyHeld
                && client.currentScreen == null
                && client.player != null
                && client.world != null
                && client.getWindow().isFocused();

        if (!active) {
            wasHeld = false;
            return;
        }

        long now = System.nanoTime();

        // Prevent Minecraft's normal held-use state from duplicating the
        // generated clicks. We read the physical GLFW state above instead.
        KeyBinding useKey = client.options.useKey();
        useKey.setPressed(false);

        long clickIntervalNanos = 1_000_000_000L / clicksPerSecond;

        if (!wasHeld || now - lastClickNanos >= clickIntervalNanos) {
            ((MinecraftClientInvoker) client).infinityClick$doItemUse();
            lastClickNanos = now;
            wasHeld = true;
        }
    }
}
