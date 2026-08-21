package dev.totem.automata.containersafety;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LocksmithOptionalBridgeTest {
    @Test
    void routeAdapterFailsClosedWhenMissingOrThrowing() throws ReflectiveOperationException {
        assertFalse(LocksmithAutomationBridge.invokeTransfer(
                null, null, null, null, null));
        Method throwing = ThrowingApi.class.getMethod(
                "transfer", ServerLevel.class, BlockPos.class, BlockPos.class, UUID.class);
        assertFalse(LocksmithAutomationBridge.invokeTransfer(
                throwing, null, BlockPos.ZERO, BlockPos.ZERO, null));
    }

    @Test
    void playerAdapterFailsClosedWhenMissingOrThrowing() throws ReflectiveOperationException {
        assertFalse(LocksmithPlayerAccessBridge.invokePlayer(null, null, null, null));
        Method throwing = ThrowingApi.class.getMethod(
                "player", ServerPlayer.class, ServerLevel.class, BlockPos.class);
        assertFalse(LocksmithPlayerAccessBridge.invokePlayer(
                throwing, null, null, BlockPos.ZERO));
    }

    public static final class ThrowingApi {
        public static boolean transfer(
                ServerLevel level,
                BlockPos source,
                BlockPos destination,
                UUID operatorId
        ) {
            throw new IllegalStateException("simulated adapter failure");
        }

        public static boolean player(
                ServerPlayer player,
                ServerLevel level,
                BlockPos position
        ) {
            throw new IllegalStateException("simulated adapter failure");
        }
    }
}
