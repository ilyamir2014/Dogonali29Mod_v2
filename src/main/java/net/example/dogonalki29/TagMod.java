package net.example.tagmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.UUID;

public class TagMod implements ModInitializer {
    // Храним UUID текущего водящего
    public static UUID taggerUUID = null;

    @Override
    public void onInitialize() {
        // Регистрация события удара
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && entity instanceof ServerPlayerEntity target) {
                ServerPlayerEntity attacker = (ServerPlayerEntity) player;

                // Проверяем: атакующий — водящий, и у него в руке палка
                if (attacker.getUuid().equals(taggerUUID) && attacker.getStackInHand(hand).isOf(Items.STICK)) {
                    transferTag(attacker, target);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }

    private void transferTag(ServerPlayerEntity oldTagger, ServerPlayerEntity newTagger) {
        // 1. Меняем роли
        taggerUUID = newTagger.getUuid();

        // 2. Убираем подсветку у старого и забираем палку
        oldTagger.setGlowing(false);
        oldTagger.getInventory().remove(stack -> stack.isOf(Items.STICK), 1, oldTagger.getInventory());

        // 3. Даем палку новому водящему и включаем подсветку
        newTagger.getInventory().insertStack(Items.STICK.getDefaultStack());
        newTagger.setGlowing(true);

        // 4. Замораживаем нового водящего на 10 секунд (200 тиков)
        // Используем Слабость и Медлительность максимального уровня, чтобы нельзя было бить и ходить
        newTagger.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 255, false, false));
        newTagger.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 255, false, false));

        // 5. Оповещения
        oldTagger.sendMessage(Text.of("Вы передали роль водящего!"), true);
        newTagger.sendMessage(Text.of("Теперь вы водите! Вы заморожены на 10 секунд."), true);
    }
}