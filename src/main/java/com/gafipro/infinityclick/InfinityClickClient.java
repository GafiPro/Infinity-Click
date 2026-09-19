package com.gafipro.infinityclick;

import com.gafipro.infinityclick.mixin.MinecraftClientInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class InfinityClickClient implements ClientModInitializer {
    private static final int CLICKS_PER_SECOND = 12;
    private static final long CLICK_INTERVAL_NANOS = 1_000_000_000L / CLICKS_PER_SECOND;

    private boolean wasHeld;
    private long lastClickNanos;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        boolean physicallyHeld = GLFW.glfwGetMouseButton(
                handle,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT
        ) == GLFW.GLFW_PRESS;

        boolean active = physicallyHeld
                && client.currentScreen == null
                && client.player != null
                && client.world != null
                && client.getWindow().isFocused();

        if (!active) {
            wasHeld = false;
            return;
        }

        long now = System.nanoTime();

        // Prevent Minecraft's normal "held" use state from duplicating the
        // generated clicks. We read the physical GLFW state above instead.
        KeyBinding useKey = client.options.useKey();
        useKey.setPressed(false);

        if (!wasHeld || now - lastClickNanos >= CLICK_INTERVAL_NANOS) {
            ((MinecraftClientInvoker) client).infinityClick$doItemUse();
            lastClickNanos = now;
            wasHeld = true;
        }
    }
}
