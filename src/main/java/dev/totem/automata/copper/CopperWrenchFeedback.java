package dev.totem.automata.copper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Stable overlay feedback for migrated Wrench interaction results. */
public final class CopperWrenchFeedback {
    private CopperWrenchFeedback() { }
    public static void send(Player player, CopperWrenchInteractionPlanner.Intent intent, boolean changed, String blockId) {
        String key = switch (intent) {
            case SELECT_GOLEM_FIRST -> "message.deadrecall.copper_wrench.left_click_select";
            case REMOVE_SOURCE -> changed ? "message.deadrecall.copper_wrench.source_unbind_success" : "message.deadrecall.copper_wrench.source_unbind_missing";
            case REMOVE_BINDING -> changed ? "message.deadrecall.copper_wrench.unbind_success" : "message.deadrecall.copper_wrench.unbind_missing";
            case TOGGLE_GATHERING_TARGET -> changed ? "message.deadrecall.copper_wrench.gathering_target_added" : "message.deadrecall.copper_wrench.gathering_target_removed";
            case REJECT_GATHERING_CONTAINER -> "message.deadrecall.copper_wrench.gathering_container_binding_disabled";
            case SET_SOURCE -> changed ? "message.deadrecall.copper_wrench.source_bind_success" : "message.deadrecall.copper_wrench.source_bind_duplicate";
            case ADD_BINDING -> changed ? "message.deadrecall.copper_wrench.bind_success" : "message.deadrecall.copper_wrench.bind_duplicate";
            case NEED_CONTAINER -> "message.deadrecall.copper_wrench.need_container";
            case SET_GATHERING_CORNER_A -> changed ? "message.deadrecall.copper_wrench.gathering_corner_a_set" : "message.deadrecall.copper_wrench.gathering_area_too_large";
            case SET_GATHERING_CORNER_B -> changed ? "message.deadrecall.copper_wrench.gathering_corner_b_set" : "message.deadrecall.copper_wrench.gathering_area_too_large";
            default -> null;
        };
        if (key == null) return;
        Component message = intent == CopperWrenchInteractionPlanner.Intent.TOGGLE_GATHERING_TARGET
                ? Component.translatable(key, blockId) : Component.translatable(key);
        player.sendOverlayMessage(message);
    }
}
