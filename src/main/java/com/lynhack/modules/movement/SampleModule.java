package com.lynhack.modules.movement;

import com.lynhack.api.event.FrameUpdateEvent;
import com.lynhack.api.input.InputManager;
import com.lynhack.api.module.Category;
import com.lynhack.api.module.Module;
import com.lynhack.api.setting.BooleanSetting;
import com.lynhack.api.setting.ModeSetting;
import com.lynhack.api.setting.NumberSetting;
import com.lynhack.api.state.ClientState;
import org.lwjgl.glfw.GLFW;

public class SampleModule extends Module {
    private final BooleanSetting onlyOnGround = addSetting(new BooleanSetting("OnlyOnGround", "Only jump when on ground", true));
    private final NumberSetting minForwardSpeed = addSetting(new NumberSetting("MinSpeed", "Minimum forward speed threshold", 0.05, 0.0, 0.5, 0.01));
    private final ModeSetting jumpMode = addSetting(new ModeSetting("Mode", "Timing behavior mode", "Dynamic", "Dynamic", "Strict"));

    public SampleModule() {
        super("AutoJump", "Demonstration module reading ClientState and commanding InputManager", Category.MOVEMENT, GLFW.GLFW_KEY_J);
    }

    @Override
    public void onFrame(FrameUpdateEvent event) {
        // 1. Read from centralized State Provider snapshot
        boolean groundCondition = !onlyOnGround.getValue() || ClientState.isOnGround;
        boolean speedCondition = ClientState.forwardVelocity > minForwardSpeed.getValue();

        // 2. On-demand lazy raycast demonstration (only computed because queried)
        var targetedBlock = ClientState.getTargetedBlock(4.5);

        // 3. Command centralized Input Dispatcher with Gaussian-timed key taps
        if (groundCondition && speedCondition) {
            InputManager.getInstance().simulateKeyTap(this, GLFW.GLFW_KEY_SPACE);
        }
    }
}
