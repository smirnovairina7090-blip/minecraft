package ru.creitivika.stormlab;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StormCoreItem extends Item {
    public StormCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        // Способность создаём только на сервере, чтобы мир не рассинхронизировался.
        if (world.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        ServerLevel serverWorld = (ServerLevel) world;
        BlockPos target = user.blockPosition().relative(user.getDirection(), ModSettings.RANGE);
        Vec3 targetCenter = target.getCenter();

        // 1. Электрический луч от игрока к точке удара.
        Vec3 start = user.getEyePosition();
        Vec3 delta = targetCenter.subtract(start);
        int steps = Math.max(20, ModSettings.RANGE * 4);

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 point = start.add(delta.scale(t));
            serverWorld.sendParticles(
                    ModSettings.BEAM_PARTICLE,
                    point.x, point.y, point.z,
                    ModSettings.PARTICLES_PER_STEP,
                    0.04, 0.04, 0.04,
                    0.01
            );
        }

        // 2. Настоящая молния в конце луча.
        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
        lightning.setPos(targetCenter);
        world.addFreshEntity(lightning);

        // 3. Искры в эпицентре.
        serverWorld.sendParticles(
                ModSettings.IMPACT_PARTICLE,
                targetCenter.x, targetCenter.y + 0.7, targetCenter.z,
                55,
                1.2, 0.8, 1.2,
                0.12
        );

        // 4. Ударная волна отбрасывает живых существ вокруг точки попадания.
        AABB shockwave = new AABB(target).inflate(ModSettings.SHOCKWAVE_RADIUS);
        List<LivingEntity> nearby = world.getEntitiesOfClass(
                LivingEntity.class,
                shockwave,
                entity -> entity != user
        );

        for (LivingEntity entity : nearby) {
            Vec3 away = entity.position().subtract(targetCenter);
            if (away.lengthSqr() > 0.001) {
                Vec3 push = away.normalize().scale(ModSettings.KNOCKBACK_POWER);
                entity.push(push.x, ModSettings.UPWARD_KNOCKBACK, push.z);
            }
        }

        // 5. Звук и перезарядка.
        world.playSound(
                null,
                target,
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS,
                1.0F,
                1.25F
        );
        user.getCooldowns().addCooldown(this, ModSettings.COOLDOWN_TICKS);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ - вызвать удар бури").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Луч + молния + ударная волна").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Настройки силы лежат в ModSettings.java").withStyle(ChatFormatting.DARK_GRAY));
    }
}
