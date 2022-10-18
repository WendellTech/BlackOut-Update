package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/*
Made by OLEPOSSU / Raksamies
*/

public class GhostCrystal extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("Swing")
        .description("Swings.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> setDead = sgGeneral.add(new BoolSetting.Builder()
        .name("SetDead")
        .description("Removes crystals right efter they despawned to maximize the speed.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> slow = sgGeneral.add(new BoolSetting.Builder()
        .name("Slow")
        .description("Slow.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("Delay")
        .description("Delay.")
        .defaultValue(5)
        .range(0, 20)
        .sliderMax(20)
        .build()
    );
    private final Setting<Boolean> obby = sgGeneral.add(new BoolSetting.Builder()
        .name("Obby")
        .description("Places crystal when you place obby.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> obbyDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Obby Delay")
        .description("Delay for switching from obby.")
        .defaultValue(2)
        .range(0, 20)
        .sliderMax(20)
        .build()
    );
    protected BlockPos placePos;
    protected int obbyTimer;
    protected boolean canBreak;
    protected int timer = 0;

    public GhostCrystal() {
        super(Addon.CATEGORY, "GhostCrystal", "Breaks crystals automatically.");
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onInteract(InteractBlockEvent event) {
        if (mc.player != null) {
            BlockHitResult result = event.result;
            boolean holding = mc.player.isHolding(Items.END_CRYSTAL);
            if (holding) {
                if (placePos == null) {
                    timer = 0;
                }
                if (timer == 0 || !slow.get()) {
                    if (slow.get()) {
                        timer = delay.get();
                    }
                    canBreak = true;
                    placePos = new BlockPos(result.getBlockPos().getX(), result.getBlockPos().getY() + 1, result.getBlockPos().getZ());
                } else {
                    event.cancel();
                }
            } else if (mc.player.isHolding(Items.OBSIDIAN) && obby.get()) {
                obbyTimer = obbyDelay.get();
                canBreak = true;
                placePos = event.result.getBlockPos().offset(event.result.getSide()).offset(Direction.UP);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        if (mc.player != null) {
            if (timer > 0) {
                timer -= 1;
            }
            if (obbyTimer > 0) {
                obbyTimer--;
            } else if (obbyTimer == 0) {
                obbyTimer = -1;
                if (placePos != null) {
                    InvUtils.swap(InvUtils.findInHotbar(Items.END_CRYSTAL).slot(), false);
                    mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,
                        new BlockHitResult(new Vec3d(placePos.getX(), placePos.getY() - 1, placePos.getZ()), Direction.UP, placePos.offset(Direction.DOWN), false), 0));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onSpawn(EntityAddedEvent event) {
        if (mc.player != null && event.entity instanceof EndCrystalEntity) {
            BlockPos position = event.entity.getBlockPos();
            if (placePos != null && !mc.player.hasStatusEffect(StatusEffect.byRawId(18))) {
                if (position.equals(placePos)) {
                    if (canBreak) {
                        canBreak = false;
                        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(event.entity, mc.player.isSneaking()));
                        if (setDead.get()){
                            event.entity.setRemoved(Entity.RemovalReason.KILLED);
                        }
                        if (swing.get()) {
                            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        }
                    }
                }
            }
        }
    }
}