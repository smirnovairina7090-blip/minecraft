package ru.creitivika.stormlab;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * УЧЕБНАЯ ПАНЕЛЬ НАСТРОЕК.
 * Ребёнок может менять значения ниже и сразу получать другую способность.
 */
public final class ModSettings {
    // Дальность способности в блоках.
    public static final int RANGE = 12;

    // Перезарядка: 20 тиков = 1 секунда.
    public static final int COOLDOWN_TICKS = 60;

    // Радиус ударной волны вокруг точки молнии.
    public static final double SHOCKWAVE_RADIUS = 5.0;

    // Сила, с которой мобов отбрасывает от эпицентра.
    public static final double KNOCKBACK_POWER = 1.7;
    public static final double UPWARD_KNOCKBACK = 0.55;

    // Сколько частиц рисовать на каждом шаге луча.
    public static final int PARTICLES_PER_STEP = 2;

    // Попробуй заменить ELECTRIC_SPARK на FLAME, SNOWFLAKE или PORTAL.
    public static final SimpleParticleType BEAM_PARTICLE = ParticleTypes.ELECTRIC_SPARK;

    // Частицы в месте удара.
    public static final SimpleParticleType IMPACT_PARTICLE = ParticleTypes.END_ROD;

    private ModSettings() {
    }
}
