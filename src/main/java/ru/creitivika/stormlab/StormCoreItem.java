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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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

        // 1. Прицеливание. Луч идёт точно из глаз игрока через центр экрана.
        Vec3 start = user.getEyePosition();
        Vec3 view = user.getViewVector(1.0F);
        Vec3 maxEnd = start.add(view.scale(ModSettings.RANGE));

        // Сначала проверяем блок под прицелом.
        HitResult blockHit = user.pick(ModSettings.RANGE, 1.0F, false);
        Vec3 targetCenter = blockHit.getLocation();
        double blockDistanceSqr = start.distanceToSqr(targetCenter);

        // Затем проверяем живую цель на той же линии прицела.
        AABB searchBox = user.getBoundingBox()
                .expandTowards(view.scale(ModSettings.RANGE))
                .inflate(1.0D);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                user,
                start,
                maxEnd,
                searchBox,
                entity -> entity instanceof LivingEntity && entity != user && entity.isPickable(),
                ModSettings.RANGE * ModSettings.RANGE
        );

        // Если моб оказался ближе блока, ударяем именно в него.
        if (entityHit != null && start.distanceToSqr(entityHit.getLocation()) < blockDistanceSqr) {
            targetCenter = entityHit.getLocation();
        }

        BlockPos target = BlockPos.containing(targetCenter);

        // 2. Электрический луч от глаз игрока точно к выбранной точке.
        Vec3 delta = targetCenter.subtract(start);
        int steps = Math.max(20, (int) Math.ceil(delta.length() * 4.0));

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 point = start.add(delta.scale(t));
            serverWorld.sendParticles(
                    ModSettings.BEAM_PARTICLE,
                    point.x, point.y, point.z,
                    ModSettings.PARTICLES_PER_STEP,
                    0.035, 0.035, 0.035,
                    0.008
            );
        }

        // 3. Молния теперь визуальная: выглядит как гроза, но НЕ поджигает мир.
        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
        lightning.setPos(targetCenter);
        lightning.setVisualOnly(true);
        world.addFreshEntity(lightning);

        // 4. Эпицентр бури: электрические искры и облако ударной волны.
        serverWorld.sendParticles(
                ModSettings.IMPACT_PARTICLE,
                targetCenter.x, targetCenter.y + 0.35, targetCenter.z,
                55,
                1.15, 0.65, 1.15,
                0.10
        );

        serverWorld.sendParticles(
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                targetCenter.x, targetCenter.y + 0.20, targetCenter.z,
                34,
                1.35, 0.35, 1.35,
                0.09
        );

        // 5. Ударная волна отбрасывает живых существ вокруг ТОЧКИ ПРИЦЕЛА.
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

        // 6. Гром и перезарядка.
        world.playSound(
                null,
                target,
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS,
                0.9F,
                1.2F
        );
        user.getCooldowns().addCooldown(this, ModSettings.COOLDOWN_TICKS);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ - удар точно по прицелу").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Молния без пожара + ударная волна").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Настройки силы лежат в ModSettings.java").withStyle(ChatFormatting.DARK_GRAY));
    }
}
