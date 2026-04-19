package net.example.dogonalki29;

import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import java.util.UUID;

@Mod(TagMod.MOD_ID)
public class TagMod {
    public static final String MOD_ID = "tagmod";
    // Храним UUID текущего водящего
    public static UUID taggerUUID = null;

    public TagMod() {
        // Регистрируем наш класс в шине событий Forge
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        // Работаем только на сервере и только если цель — игрок
        if (!event.getEntity().level().isClientSide && event.getTarget() instanceof ServerPlayer target) {
            ServerPlayer attacker = (ServerPlayer) event.getEntity();

            // Проверка: атакующий — водящий и бьет палкой
            if (attacker.getUUID().equals(taggerUUID) && attacker.getMainHandItem().is(Items.STICK)) {
                transferTag(attacker, target);
                // Отменяем стандартный урон, чтобы не убить игрока палкой
                event.setCanceled(true);
            }
        }
    }

    private void transferTag(ServerPlayer oldTagger, ServerPlayer newTagger) {
        taggerUUID = newTagger.getUUID();

        // 1. Настройки старого водящего
        oldTagger.setGlowingTag(false);
        // Забираем одну палку
        oldTagger.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.STICK), 1, oldTagger.inventoryMenu.getCraftSlots());

        // 2. Настройки нового водящего
        newTagger.setGlowingTag(true);
        newTagger.addItem(Items.STICK.getDefaultInstance());

        // 3. Заморозка на 10 секунд (200 тиков)
        // В Forge/NeoForge уровни эффектов начинаются с 0 (255 — это очень сильно)
        newTagger.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 255, false, false));
        newTagger.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 255, false, false));

        // 4. Уведомления
        oldTagger.sendSystemMessage(Component.literal("§aВы передали роль водящего!"));
        newTagger.sendSystemMessage(Component.literal("§cТеперь вы водите! Заморозка на 10 секунд."));
    }
}