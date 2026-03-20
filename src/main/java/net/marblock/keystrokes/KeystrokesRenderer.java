package net.marblock.keystrokes;

import net.marblock.keystrokes.config.KeystrokesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Color;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class KeystrokesRenderer {

    private final AtomicInteger frameCount = new AtomicInteger(0);
    private final Deque<Long> leftClickTimestamps = new LinkedList<>();
    private final Deque<Long> rightClickTimestamps = new LinkedList<>();

    private int leftCPS = 0, rightCPS = 0, fps = 0;

    private static final int KEY_SIZE = 18;
    private static final int KEY_SPACING = 5;
    private static final int LMB_RMB_Y_OFFSET = (KEY_SPACING + KEY_SIZE) - 3;
    private static final int SPACEBAR_Y_OFFSET = 2 * KEY_SPACING;

    private static final int INDICATOR_WIDTH = 45;
    private static final int INDICATOR_HEIGHT = 11;

    private static final float TEXT_SCALE = 0.75f;
    private static final float CPS_TEXT_SCALE = 0.65f;

    private double huePhase = 0;
    private static final double HUE_INCREMENT = Math.PI / 75;

    private boolean leftClickTransition = false;
    private boolean rightClickTransition = false;

    public KeystrokesRenderer() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::updateState, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void updateState() {
        long now = System.currentTimeMillis();

        while (!leftClickTimestamps.isEmpty() && now - leftClickTimestamps.peekFirst() > 1000) {
            leftClickTimestamps.pollFirst();
        }
        while (!rightClickTimestamps.isEmpty() && now - rightClickTimestamps.peekFirst() > 1000) {
            rightClickTimestamps.pollFirst();
        }

        leftCPS = leftClickTimestamps.size();
        rightCPS = rightClickTimestamps.size();
        fps = frameCount.getAndSet(0) * 20;

        huePhase = (huePhase + HUE_INCREMENT) % (2 * Math.PI);

        leftClickTransition = false;
        rightClickTransition = false;
    }

    public void render(GuiGraphics guiGraphics, float partialTick) {
        if (!KeystrokesConfig.isEnabled) return;

        frameCount.incrementAndGet();

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int rgbColor = Color.getHSBColor(
                (float) (0.5 * (1 + Math.sin(huePhase))), 1, 1
        ).getRGB();

        int screenHeight = mc.getWindow().getScreenHeight();
        int fpsBoxTop = Math.min(5, screenHeight - INDICATOR_HEIGHT - 5);

        if (KeystrokesConfig.showFPS) {
            renderFPS(guiGraphics, font, "FPS: " + fps,
                    KeystrokesConfig.x + 4 - KEY_SIZE - KEY_SPACING,
                    fpsBoxTop, rgbColor);
        }

        if (KeystrokesConfig.showKeys) {
            renderKeys(guiGraphics, font, mc, rgbColor);
        }

        if (KeystrokesConfig.showSpace) {
            renderSpaceBar(guiGraphics, mc, rgbColor);
        }

        if (KeystrokesConfig.showCPS) {
            int totalWidth = 2 * INDICATOR_WIDTH + KEY_SPACING;
            int startX = KeystrokesConfig.x - totalWidth / 2;

            int lmbX = startX;
            int rmbX = startX + INDICATOR_WIDTH + KEY_SPACING;
            int y = KeystrokesConfig.y + 2 * KEY_SIZE + LMB_RMB_Y_OFFSET;

            renderCPS(guiGraphics, font, "LMB", leftCPS + " CPS", lmbX, y, rgbColor);
            renderCPS(guiGraphics, font, "RMB", rightCPS + " CPS", rmbX, y, rgbColor);
        }
    }

    private void renderKeys(GuiGraphics guiGraphics, Font font, Minecraft mc, int rgbColor) {
        int textColor = 0xFFFFFFFF;

        renderKeyBar(guiGraphics, font, "W",
                KeystrokesConfig.x, KeystrokesConfig.y,
                KEY_SIZE, KEY_SIZE,
                mc.options.keyUp.isDown(), rgbColor, textColor);

        renderKeyBar(guiGraphics, font, "A",
                KeystrokesConfig.x - KEY_SIZE - KEY_SPACING,
                KeystrokesConfig.y + KEY_SIZE + KEY_SPACING,
                KEY_SIZE, KEY_SIZE,
                mc.options.keyLeft.isDown(), rgbColor, textColor);

        renderKeyBar(guiGraphics, font, "S",
                KeystrokesConfig.x,
                KeystrokesConfig.y + KEY_SIZE + KEY_SPACING,
                KEY_SIZE, KEY_SIZE,
                mc.options.keyDown.isDown(), rgbColor, textColor);

        renderKeyBar(guiGraphics, font, "D",
                KeystrokesConfig.x + KEY_SIZE + KEY_SPACING,
                KeystrokesConfig.y + KEY_SIZE + KEY_SPACING,
                KEY_SIZE, KEY_SIZE,
                mc.options.keyRight.isDown(), rgbColor, textColor);
    }

    private void renderSpaceBar(GuiGraphics guiGraphics, Minecraft mc, int rgbColor) {
        int width = 3 * KEY_SIZE + 2 * KEY_SPACING;
        int height = KEY_SIZE / 4;

        int x = KeystrokesConfig.x - KEY_SIZE - KEY_SPACING + 1;
        int y = KeystrokesConfig.y + 2 * KEY_SIZE + SPACEBAR_Y_OFFSET;

        int bgColor = mc.options.keyJump.isDown() ? rgbColor : 0x80000000;

        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        drawBorder(guiGraphics, x, y, x + width, y + height, rgbColor);
    }

    private void renderFPS(GuiGraphics guiGraphics, Font font, String text, int x, int y, int borderColor) {
        int width = INDICATOR_WIDTH;
        int height = INDICATOR_HEIGHT;

        guiGraphics.fill(x, y, x + width, y + height, 0x80000000);
        drawBorder(guiGraphics, x, y, x + width, y + height, borderColor);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE);

        int centerX = (int) ((x + width / 2) / TEXT_SCALE);
        int centerY = (int) ((y + height / 2 - (font.lineHeight * TEXT_SCALE / 2)) / TEXT_SCALE);

        guiGraphics.drawString(font, text,
                centerX - font.width(text) / 2,
                centerY,
                0xFFFFFFFF, false);

        guiGraphics.pose().popMatrix();
    }

    private void renderCPS(GuiGraphics guiGraphics, Font font, String label, String cps, int x, int y, int borderColor) {
        int height = INDICATOR_HEIGHT * 2;

        int bgColor = 0x80000000;
        int textColor = 0xFFFFFFFF;

        if ((label.equals("LMB") && leftClickTransition) ||
            (label.equals("RMB") && rightClickTransition)) {
            bgColor = 0xFFFFFFFF;
            textColor = 0xFF000000;
        }

        guiGraphics.fill(x, y, x + INDICATOR_WIDTH, y + height, bgColor);
        drawBorder(guiGraphics, x, y, x + INDICATOR_WIDTH, y + height, borderColor);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(CPS_TEXT_SCALE, CPS_TEXT_SCALE);

        int centerX = (int) ((x + INDICATOR_WIDTH / 2) / CPS_TEXT_SCALE);

        guiGraphics.drawCenteredString(font, label,
                centerX,
                (int) ((y + 3) / CPS_TEXT_SCALE),
                textColor);

        guiGraphics.drawCenteredString(font, cps,
                centerX,
                (int) ((y + INDICATOR_HEIGHT + 2) / CPS_TEXT_SCALE),
                textColor);

        guiGraphics.pose().popMatrix();
    }

    private void renderKeyBar(GuiGraphics guiGraphics, Font font, String key,
                              int x, int y, int width, int height,
                              boolean pressed, int borderColor, int textColor) {

        int bgColor = pressed ? 0xFFFFFFFF : 0x80000000;
        if (pressed) textColor = 0xFF000000;

        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        drawBorder(guiGraphics, x, y, x + width, y + height, borderColor);

        guiGraphics.drawCenteredString(font, key,
                x + width / 2,
                y + height / 2 - 4,
                textColor);
    }

    private void drawBorder(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1 - 1, y1 - 1, x2 + 1, y1, color);
        guiGraphics.fill(x1 - 1, y2, x2 + 1, y2 + 1, color);
        guiGraphics.fill(x1 - 1, y1, x1, y2, color);
        guiGraphics.fill(x2, y1, x2 + 1, y2, color);
    }

    public void incrementLeftClicks() {
        leftClickTimestamps.addLast(System.currentTimeMillis());
        leftClickTransition = true;
    }

    public void incrementRightClicks() {
        rightClickTimestamps.addLast(System.currentTimeMillis());
        rightClickTransition = true;
    }
}
