package cheesepaste.blocktasy;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class WaterBallParticleEffect {

    private static final Random RANDOM = new Random();
    private static final Map<UUID, WaterBallEffect> ACTIVE_EFFECTS = new HashMap<>();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("waterball")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        Vec3d pos = source.getPosition();
                        ServerWorld world = source.getWorld();

                        spawnWaterBall(world, pos.add(0, 1, 0));

                        source.sendFeedback(() -> Text.literal("§9生成巨型蓝色水球！§b输入 /waterball big 获取更大效果"), true);
                        return 1;
                    })
                    .then(CommandManager.literal("big")
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                Vec3d pos = source.getPosition();
                                ServerWorld world = source.getWorld();

                                spawnBigWaterBall(world, pos.add(0, 2, 0));

                                source.sendFeedback(() -> Text.literal("§9§l生成§b超大型§9蓝色水球！§c注意：粒子数量极多！"), true);
                                return 1;
                            })
                    )
                    .then(CommandManager.literal("huge")
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                Vec3d pos = source.getPosition();
                                ServerWorld world = source.getWorld();

                                spawnHugeWaterBall(world, pos.add(0, 3, 0));

                                source.sendFeedback(() -> Text.literal("§9§l生成§b巨型§6史诗级§9水球！§c§l警告：可能造成卡顿！"), true);
                                return 1;
                            })
                    )
            );
        });

        // 注册tick事件来处理粒子效果
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, WaterBallEffect>> iterator = ACTIVE_EFFECTS.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, WaterBallEffect> entry = iterator.next();
                WaterBallEffect effect = entry.getValue();

                // 更新效果
                effect.update();

                // 如果效果结束，移除
                if (effect.isFinished()) {
                    iterator.remove();
                }
            }
        });
    }

    private static void spawnWaterBall(ServerWorld world, Vec3d centerPos) {
        UUID effectId = UUID.randomUUID();
        WaterBallEffect effect = new WaterBallEffect(world, centerPos, 3.0, 0.08, 800, 8000);
        ACTIVE_EFFECTS.put(effectId, effect);
    }

    private static void spawnBigWaterBall(ServerWorld world, Vec3d centerPos) {
        UUID effectId = UUID.randomUUID();
        WaterBallEffect effect = new WaterBallEffect(world, centerPos, 4.5, 0.06, 1200, 15000);
        effect.setGlowIntensity(0.1f); // 轻微发光
        ACTIVE_EFFECTS.put(effectId, effect);
    }

    private static void spawnHugeWaterBall(ServerWorld world, Vec3d centerPos) {
        UUID effectId = UUID.randomUUID();
        WaterBallEffect effect = new WaterBallEffect(world, centerPos, 6.0, 0.05, 1600, 25000);
        effect.setGlowIntensity(0.15f); // 更明显的发光
        effect.setTrailEffect(true); // 添加拖尾效果
        ACTIVE_EFFECTS.put(effectId, effect);
    }

    private static class WaterBallEffect {
        private final ServerWorld world;
        private final Vec3d startPos;
        private final double radius;
        private final double riseSpeed;
        private final int particlesPerLayer;
        private final int explosionParticles;

        private int tick = 0;
        private boolean risingPhase = true;
        private boolean explodingPhase = false;
        private boolean finished = false;

        // 效果增强参数
        private float glowIntensity = 0.0f; // 发光强度
        private boolean trailEffect = false; // 拖尾效果

        public WaterBallEffect(ServerWorld world, Vec3d startPos, double radius, double riseSpeed,
                               int particlesPerLayer, int explosionParticles) {
            this.world = world;
            this.startPos = startPos;
            this.radius = radius;
            this.riseSpeed = riseSpeed;
            this.particlesPerLayer = particlesPerLayer;
            this.explosionParticles = explosionParticles;
        }

        public void update() {
            if (finished) return;

            tick++;

            if (risingPhase) {
                updateRisingPhase();
            } else if (explodingPhase) {
                updateExplosionPhase();
            }
        }

        private void updateRisingPhase() {
            double currentY = startPos.y + (tick * riseSpeed);
            Vec3d currentPos = new Vec3d(startPos.x, currentY, startPos.z);

            // 水球上升阶段持续100个tick（5秒）
            if (tick <= 100) {
                // 生成水球主体
                spawnWaterBallSphere(currentPos, radius);

                // 如果启用了拖尾效果，在底部添加拖尾
                if (trailEffect && tick % 2 == 0) {
                    spawnTrailParticles(currentPos);
                }
            } else {
                // 进入爆炸准备阶段（短暂停顿）
                if (tick == 105) {
                    // 在爆炸前先收缩一下水球
                    for (int i = 0; i < 50; i++) {
                        spawnImplosionEffect(currentPos);
                    }
                } else if (tick >= 110) {
                    // 开始爆炸
                    risingPhase = false;
                    explodingPhase = true;
                    tick = 0; // 重置tick用于爆炸阶段
                }
            }
        }

        private void updateExplosionPhase() {
            Vec3d explosionPos = new Vec3d(startPos.x, startPos.y + (100 * riseSpeed), startPos.z);

            // 爆炸阶段持续60个tick（3秒）
            if (tick <= 60) {
                // 主要爆炸效果
                if (tick == 0) {
                    // 第一帧：初始爆炸冲击波
                    spawnInitialExplosion(explosionPos);
                } else if (tick < 20) {
                    // 前20tick：强烈爆炸
                    spawnIntenseExplosion(explosionPos, 1.0 - (tick / 20.0));
                } else if (tick < 40) {
                    // 中间20tick：扩散爆炸
                    spawnDiffusionExplosion(explosionPos);
                } else {
                    // 最后20tick：余波
                    spawnAftermath(explosionPos);
                }

                // 添加水雾效果
                if (tick % 3 == 0) {
                    spawnMistEffect(explosionPos);
                }
            } else {
                finished = true;
            }
        }

        private void spawnWaterBallSphere(Vec3d center, double radius) {
            // 生成球体表面
            for (int layer = 0; layer < 20; layer++) {
                double yOffset = (layer / 20.0) * radius * 2 - radius;
                double currentRadius = Math.sqrt(radius * radius - yOffset * yOffset);

                // 根据水球大小调整粒子密度
                int density = Math.max(20, (int)(particlesPerLayer * (currentRadius / radius)));

                for (int i = 0; i < density; i++) {
                    double angle = 2 * Math.PI * i / density;
                    double x = currentRadius * Math.cos(angle);
                    double z = currentRadius * Math.sin(angle);

                    Vec3d particlePos = center.add(x, yOffset, z);

                    // 添加一些随机偏移使水球看起来更自然
                    double jitter = 0.1;
                    double jitterX = (RANDOM.nextDouble() - 0.5) * jitter;
                    double jitterY = (RANDOM.nextDouble() - 0.5) * jitter;
                    double jitterZ = (RANDOM.nextDouble() - 0.5) * jitter;

                    // 生成蓝色水下粒子
                    world.spawnParticles(
                            ParticleTypes.UNDERWATER,
                            particlePos.x + jitterX,
                            particlePos.y + jitterY,
                            particlePos.z + jitterZ,
                            1,
                            0, 0, 0,
                            0.005
                    );

                    // 如果启用了发光效果，添加额外的亮色粒子
                    if (glowIntensity > 0 && i % 3 == 0) {
                        // 使用水下粒子但更密集来模拟发光
                        world.spawnParticles(
                                ParticleTypes.UNDERWATER,
                                particlePos.x, particlePos.y, particlePos.z,
                                2, // 更多粒子
                                0.03, 0.03, 0.03, // 小范围扩散
                                0.002 // 非常慢的速度
                        );
                    }
                }
            }

            // 在水球内部添加随机运动的粒子
            int innerParticles = (int)(radius * 15);
            for (int i = 0; i < innerParticles; i++) {
                // 在球体内生成随机位置
                double u = RANDOM.nextDouble();
                double v = RANDOM.nextDouble();
                double theta = 2 * Math.PI * u;
                double phi = Math.acos(2 * v - 1);
                double r = radius * Math.cbrt(RANDOM.nextDouble());

                double x = r * Math.sin(phi) * Math.cos(theta);
                double y = r * Math.sin(phi) * Math.sin(theta);
                double z = r * Math.cos(phi);

                Vec3d innerPos = center.add(x, y, z);

                // 内部粒子有轻微的运动
                double speed = 0.01 + RANDOM.nextDouble() * 0.02;
                double vx = (RANDOM.nextDouble() - 0.5) * speed;
                double vy = (RANDOM.nextDouble() - 0.5) * speed;
                double vz = (RANDOM.nextDouble() - 0.5) * speed;

                world.spawnParticles(
                        ParticleTypes.UNDERWATER,
                        innerPos.x, innerPos.y, innerPos.z,
                        1,
                        vx, vy, vz,
                        0.003
                );
            }
        }

        private void spawnTrailParticles(Vec3d center) {
            // 在水球底部生成拖尾粒子
            int trailCount = (int)(radius * 10);
            for (int i = 0; i < trailCount; i++) {
                double angle = 2 * Math.PI * RANDOM.nextDouble();
                double distance = RANDOM.nextDouble() * radius * 0.8;

                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                double y = -radius * 0.7; // 在水球底部

                Vec3d trailPos = center.add(x, y, z);

                // 拖尾粒子向下飘落
                world.spawnParticles(
                        ParticleTypes.UNDERWATER,
                        trailPos.x, trailPos.y, trailPos.z,
                        1,
                        0, -0.02, 0,
                        0.01
                );
            }
        }

        private void spawnImplosionEffect(Vec3d center) {
            // 爆炸前的收缩效果
            for (int i = 0; i < 30; i++) {
                double angle = 2 * Math.PI * RANDOM.nextDouble();
                double distance = radius * (0.7 + RANDOM.nextDouble() * 0.3);

                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                double y = (RANDOM.nextDouble() - 0.5) * radius * 1.5;

                Vec3d particlePos = center.add(x, y, z);

                // 粒子向中心收缩
                double dx = -x * 0.05;
                double dy = -y * 0.05;
                double dz = -z * 0.05;

                world.spawnParticles(
                        ParticleTypes.UNDERWATER,
                        particlePos.x, particlePos.y, particlePos.z,
                        1,
                        dx, dy, dz,
                        0.03
                );
            }
        }

        private void spawnInitialExplosion(Vec3d center) {
            // 初始爆炸冲击波 - 大量粒子瞬间向四周爆发
            int initialBurst = explosionParticles / 3;

            for (int i = 0; i < initialBurst; i++) {
                // 球面均匀分布
                double theta = 2 * Math.PI * RANDOM.nextDouble();
                double phi = Math.acos(2 * RANDOM.nextDouble() - 1);

                // 爆炸速度 - 初始爆炸非常强烈
                double speed = 8 + RANDOM.nextDouble() * 12;

                double vx = speed * Math.sin(phi) * Math.cos(theta);
                double vy = speed * Math.sin(phi) * Math.sin(theta);
                double vz = speed * Math.cos(phi);

                // 添加一些随机偏移
                double offset = 0.2;
                double ox = (RANDOM.nextDouble() - 0.5) * offset;
                double oy = (RANDOM.nextDouble() - 0.5) * offset;
                double oz = (RANDOM.nextDouble() - 0.5) * offset;

                world.spawnParticles(
                        ParticleTypes.UNDERWATER,
                        center.x + ox, center.y + oy, center.z + oz,
                        2, // 每个位置生成2个粒子
                        vx, vy, vz,
                        1.5
                );
            }

            // 额外的爆炸核心效果
            for (int i = 0; i < 200; i++) {
                double angle = 2 * Math.PI * RANDOM.nextDouble();
                double speed = 5 + RANDOM.nextDouble()*10;

                double vx = speed * Math.cos(angle);
                double vz = speed * Math.sin(angle);
                double vy = (RANDOM.nextDouble() - 0.2) * 0.8; // 稍微向上偏

                world.spawnParticles(
                        ParticleTypes.UNDERWATER,
                        center.x, center.y, center.z,
                        1,
                        vx, vy, vz,
                        1
                );
            }
        }

        private void spawnIntenseExplosion(Vec3d center, double intensity) {
            // 强烈爆炸阶段 - 持续发射大量粒子
            int particlesThisFrame = (int)(explosionParticles * 0.02 * intensity);

            for (int i = 0; i < particlesThisFrame; i++) {
                // 随机方向，但主要向外
                double theta = 2 * Math.PI * RANDOM.nextDouble();
                double phi = Math.acos(2 * RANDOM.nextDouble() - 1);

                // 速度随强度减弱
                double speed = 4 + RANDOM.nextDouble() * 8 * intensity;

                double vx = speed * Math.sin(phi) * Math.cos(theta);
                double vy = speed * Math.sin(phi) * Math.sin(theta);
                double vz = speed * Math.cos(phi);

                // 从稍微偏离中心的位置发射
                double offset = 0.5;
                double ox = (RANDOM.nextDouble() - 0.5) * offset;
                double oy = (RANDOM.nextDouble() - 0.5) * offset;
                double oz = (RANDOM.nextDouble() - 0.5) * offset;

                int particleCount = RANDOM.nextDouble() < 0.3 ? 2 : 1;

                world.spawnParticles(
                        ParticleTypes.UNDERWATER,
                        center.x + ox, center.y + oy, center.z + oz,
                        particleCount,
                        vx, vy, vz,
                        0.8 * intensity
                );
            }
        }

        private void spawnDiffusionExplosion(Vec3d center) {
            // 扩散爆炸阶段 - 粒子向四周扩散
            int particlesThisFrame = explosionParticles / 40;

            for (int i = 0; i < particlesThisFrame; i++) {
                double angle = 2 * Math.PI * RANDOM.nextDouble();
                double speed = 2 + RANDOM.nextDouble() * 4;

                double vx = speed * Math.cos(angle);
                double vz = speed * Math.sin(angle);
                double vy = (RANDOM.nextDouble() - 0.5) * 0.3;

                // 从爆炸边缘开始
                double distance = 1.0 + RANDOM.nextDouble() * 2.0;
                double x = center.x + distance * Math.cos(angle);
                double z = center.z + distance * Math.sin(angle);
                double y = center.y + (RANDOM.nextDouble() - 0.5) * 1.5;

                world.spawnParticles(
                        ParticleTypes.UNDERWATER,
                        x, y, z,
                        1,
                        vx, vy, vz,
                        0.5
                );
            }
        }

        private void spawnAftermath(Vec3d center) {
            // 爆炸余波 - 缓慢下落的粒子
            int particlesThisFrame = explosionParticles / 60;

            for (int i = 0; i < particlesThisFrame; i++) {
                double angle = 2 * Math.PI * RANDOM.nextDouble();
                double distance = 2.0 + RANDOM.nextDouble() * 4.0;

                double x = center.x + distance * Math.cos(angle);
                double z = center.z + distance * Math.sin(angle);
                double y = center.y + (RANDOM.nextDouble() - 0.5) * 3.0;

                // 余波粒子缓慢下落
                world.spawnParticles(
                        ParticleTypes.UNDERWATER,
                        x, y, z,
                        1,
                        0, -0.05, 0,
                        0.3
                );
            }
        }

        private void spawnMistEffect(Vec3d center) {
            // 水雾效果 - 爆炸产生的水汽
            int mistParticles = 50;

            for (int i = 0; i < mistParticles; i++) {
                double angle = 2 * Math.PI * RANDOM.nextDouble();
                double distance = RANDOM.nextDouble() * 5.0;

                double x = center.x + distance * Math.cos(angle);
                double z = center.z + distance * Math.sin(angle);
                double y = center.y + (RANDOM.nextDouble() - 0.5) * 2.0;

                // 水雾粒子缓慢飘散
                world.spawnParticles(
                        ParticleTypes.UNDERWATER,
                        x, y, z,
                        1,
                        0, 0.01, 0, // 轻微向上飘
                        0.1
                );
            }
        }

        public boolean isFinished() {
            return finished;
        }

        public void setGlowIntensity(float intensity) {
            this.glowIntensity = intensity;
        }

        public void setTrailEffect(boolean enabled) {
            this.trailEffect = enabled;
        }
    }
}