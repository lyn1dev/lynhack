package com.lynhack.gui;

import com.lynhack.api.input.InputManager;
import com.lynhack.api.module.Category;
import com.lynhack.api.module.Module;
import com.lynhack.api.module.ModuleManager;
import com.lynhack.api.setting.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static ClickGuiScreen INSTANCE;

    public static ClickGuiScreen getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClickGuiScreen();
        }
        return INSTANCE;
    }

    private final List<Panel> panels = new ArrayList<>();
    private Setting<?> listeningKeybindSetting = null;

    public ClickGuiScreen() {
        super(Text.literal("LynHack ClickGUI"));
        int startX = 20;
        int startY = 30;
        int panelWidth = 110;
        int spacing = 120;

        int index = 0;
        for (Category category : Category.values()) {
            panels.add(new Panel(category, startX + (index * spacing), startY, panelWidth));
            index++;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        listeningKeybindSetting = null;
        InputManager.getInstance().flushAll();
    }

    @Override
    public void close() {
        super.close();
        InputManager.getInstance().flushAll();
    }

    public static void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Darkened background vignette
        context.fill(0, 0, width, height, 0x77000000);

        // Render header title
        context.drawTextWithShadow(textRenderer, Text.literal("LynHack §7v1.0.0 (1.21.11)"), 10, 10, 0xFFCBA6F7);

        for (Panel panel : panels) {
            panel.render(context, mouseX, mouseY);
        }

        if (listeningKeybindSetting != null) {
            String msg = "Press any key to bind (ESC to clear)...";
            int textWidth = textRenderer.getWidth(msg);
            int cx = width / 2;
            int cy = height - 30;
            context.fill(cx - textWidth / 2 - 8, cy - 6, cx + textWidth / 2 + 8, cy + 14, 0xEE11111B);
            drawOutline(context, cx - textWidth / 2 - 8, cy - 6, textWidth + 16, 20, 0xFFF38BA8);
            context.drawTextWithShadow(textRenderer, Text.literal(msg), cx - textWidth / 2, cy, 0xFFF38BA8);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (listeningKeybindSetting != null) {
            listeningKeybindSetting = null;
            return true;
        }

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        for (Panel panel : panels) {
            panel.mouseReleased(click.x(), click.y(), click.button());
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        for (Panel panel : panels) {
            if (panel.mouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();

        if (listeningKeybindSetting != null) {
            if (listeningKeybindSetting instanceof KeybindSetting kb) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    kb.setValue(GLFW.GLFW_KEY_UNKNOWN);
                } else {
                    kb.setValue(keyCode);
                }
            }
            listeningKeybindSetting = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    // Inner class representing a draggable Category Panel
    private class Panel {
        private final Category category;
        private int x, y, width;
        private boolean open = true;
        private boolean dragging = false;
        private int dragOffsetX, dragOffsetY;
        private final List<ModuleComponent> moduleComponents = new ArrayList<>();

        public Panel(Category category, int x, int y, int width) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.width = width;

            List<Module> mods = ModuleManager.getInstance().getModulesByCategory(category);
            for (Module m : mods) {
                moduleComponents.add(new ModuleComponent(m));
            }
        }

        public void render(DrawContext context, int mouseX, int mouseY) {
            if (dragging) {
                x = mouseX - dragOffsetX;
                y = mouseY - dragOffsetY;
            }

            int headerHeight = 18;
            context.fill(x, y, x + width, y + headerHeight, 0xFF181825);
            drawOutline(context, x, y, width, headerHeight, 0xFF89B4FA);

            String title = category.getDisplayName() + (open ? " [-]" : " [+]");
            context.drawTextWithShadow(textRenderer, Text.literal(title), x + 6, y + 5, 0xFF89B4FA);

            if (open) {
                int currentY = y + headerHeight;
                for (ModuleComponent comp : moduleComponents) {
                    currentY += comp.render(context, x, currentY, width, mouseX, mouseY);
                }
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int headerHeight = 18;
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (button == 0) { // Left click: drag
                    dragging = true;
                    dragOffsetX = (int) mouseX - x;
                    dragOffsetY = (int) mouseY - y;
                    return true;
                } else if (button == 1) { // Right click: toggle fold
                    open = !open;
                    return true;
                }
            }

            if (open) {
                int currentY = y + headerHeight;
                for (ModuleComponent comp : moduleComponents) {
                    int compHeight = comp.getHeight();
                    if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + compHeight) {
                        if (comp.mouseClicked(mouseX, mouseY, button, x, currentY, width)) {
                            return true;
                        }
                    }
                    currentY += compHeight;
                }
            }
            return false;
        }

        public void mouseReleased(double mouseX, double mouseY, int button) {
            dragging = false;
            for (ModuleComponent comp : moduleComponents) {
                comp.mouseReleased(mouseX, mouseY, button);
            }
        }

        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (dragging) return true;
            if (open) {
                for (ModuleComponent comp : moduleComponents) {
                    if (comp.mouseDragged(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // Inner class representing a Module button and expandable settings
    private class ModuleComponent {
        private final Module module;
        private boolean expanded = false;
        private NumberSetting draggingSlider = null;

        public ModuleComponent(Module module) {
            this.module = module;
        }

        public int getHeight() {
            int h = 16;
            if (expanded) {
                for (Setting<?> s : module.getSettings()) {
                    if (s instanceof NumberSetting) h += 18;
                    else h += 14;
                }
            }
            return h;
        }

        public int render(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
            int btnHeight = 16;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + btnHeight;

            int bgColor = module.isEnabled() ? 0xFF89B4FA : (hovered ? 0xFF313244 : 0xFF1E1E2E);
            int textColor = module.isEnabled() ? 0xFF11111B : (hovered ? 0xFFCDD6F4 : 0xFFA6ADC8);

            context.fill(x, y, x + width, y + btnHeight, bgColor);
            drawOutline(context, x, y, width, btnHeight, 0xFF313244);

            String label = module.getName() + (module.getSettings().size() > 1 ? (expanded ? " v" : " >") : "");
            context.drawTextWithShadow(textRenderer, Text.literal(label), x + 6, y + 4, textColor);

            int totalRenderedHeight = btnHeight;

            if (expanded) {
                int settingY = y + btnHeight;
                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting bool) {
                        int h = 14;
                        boolean sHovered = mouseX >= x && mouseX <= x + width && mouseY >= settingY && mouseY <= settingY + h;
                        context.fill(x, settingY, x + width, settingY + h, sHovered ? 0xFF28283D : 0xFF181825);
                        int sColor = bool.getValue() ? 0xFFA6E3A1 : 0xFFF38BA8;
                        context.drawTextWithShadow(textRenderer, Text.literal("  " + bool.getName() + ": " + (bool.getValue() ? "[ON]" : "[OFF]")), x + 4, settingY + 3, sColor);
                        settingY += h;
                        totalRenderedHeight += h;
                    } else if (setting instanceof NumberSetting num) {
                        int h = 18;
                        context.fill(x, settingY, x + width, settingY + h, 0xFF181825);
                        double pct = (num.getValue() - num.getMin()) / (num.getMax() - num.getMin());
                        int fillW = (int) (pct * (width - 8));
                        context.fill(x + 4, settingY + 12, x + 4 + fillW, settingY + 15, 0xFF89B4FA);
                        drawOutline(context, x + 4, settingY + 12, width - 8, 3, 0xFF45475A);

                        String valStr = String.format("%.2f", num.getValue());
                        context.drawTextWithShadow(textRenderer, Text.literal("  " + num.getName() + ": §7" + valStr), x + 4, settingY + 2, 0xFFCDD6F4);
                        settingY += h;
                        totalRenderedHeight += h;
                    } else if (setting instanceof ModeSetting mode) {
                        int h = 14;
                        boolean sHovered = mouseX >= x && mouseX <= x + width && mouseY >= settingY && mouseY <= settingY + h;
                        context.fill(x, settingY, x + width, settingY + h, sHovered ? 0xFF28283D : 0xFF181825);
                        context.drawTextWithShadow(textRenderer, Text.literal("  " + mode.getName() + ": §b" + mode.getValue()), x + 4, settingY + 3, 0xFFBAC2DE);
                        settingY += h;
                        totalRenderedHeight += h;
                    } else if (setting instanceof KeybindSetting kb) {
                        int h = 14;
                        boolean sHovered = mouseX >= x && mouseX <= x + width && mouseY >= settingY && mouseY <= settingY + h;
                        context.fill(x, settingY, x + width, settingY + h, sHovered ? 0xFF28283D : 0xFF181825);
                        boolean isBinding = (listeningKeybindSetting == kb);
                        String bindText = isBinding ? "§e[...]" : "§7[" + kb.getKeyName() + "]";
                        context.drawTextWithShadow(textRenderer, Text.literal("  Bind: " + bindText), x + 4, settingY + 3, 0xFFCDD6F4);
                        settingY += h;
                        totalRenderedHeight += h;
                    }
                }
            }
            return totalRenderedHeight;
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
            int btnHeight = 16;
            if (mouseY >= y && mouseY <= y + btnHeight) {
                if (button == 0) { // Left click: Toggle
                    module.toggle();
                    return true;
                } else if (button == 1) { // Right click: Expand settings
                    expanded = !expanded;
                    return true;
                }
            }

            if (expanded) {
                int settingY = y + btnHeight;
                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting bool) {
                        int h = 14;
                        if (mouseY >= settingY && mouseY <= settingY + h) {
                            bool.toggle();
                            return true;
                        }
                        settingY += h;
                    } else if (setting instanceof NumberSetting num) {
                        int h = 18;
                        if (mouseY >= settingY && mouseY <= settingY + h) {
                            draggingSlider = num;
                            updateSlider(num, mouseX, x, width);
                            return true;
                        }
                        settingY += h;
                    } else if (setting instanceof ModeSetting mode) {
                        int h = 14;
                        if (mouseY >= settingY && mouseY <= settingY + h) {
                            mode.cycle();
                            return true;
                        }
                        settingY += h;
                    } else if (setting instanceof KeybindSetting kb) {
                        int h = 14;
                        if (mouseY >= settingY && mouseY <= settingY + h) {
                            listeningKeybindSetting = kb;
                            return true;
                        }
                        settingY += h;
                    }
                }
            }
            return false;
        }

        public void mouseReleased(double mouseX, double mouseY, int button) {
            draggingSlider = null;
        }

        public boolean mouseDragged(double mouseX, double mouseY, int button) {
            if (draggingSlider != null) {
                return true;
            }
            return false;
        }

        private void updateSlider(NumberSetting num, double mouseX, int x, int width) {
            double pct = Math.clamp((mouseX - (x + 4)) / (double) (width - 8), 0.0, 1.0);
            double val = num.getMin() + pct * (num.getMax() - num.getMin());
            num.setValue(val);
        }
    }
}
