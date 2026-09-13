package net.backrooms.entity;

import net.backrooms.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * The Bacteria - the sole hostile entity of Level 0.
 *
 * <p>It wanders the fluorescent halls, senses nearby players (even through the
 * wallpaper - you can hear it coming), then sprints them down and attacks in
 * melee. The AI intentionally builds only on vanilla goals so navigation
 * through the procedurally generated doorways stays reliable.</p>
 */
public class BacteriaEntity extends Monster {
	public BacteriaEntity(EntityType<BacteriaEntity> type, Level level) {
		super(type, level);
		this.setPathfindingMalus(PathType.DANGER_OTHER, 0.0F);
		this.setPathfindingMalus(PathType.DAMAGE_OTHER, 0.0F);
		this.setPathfindingMalus(PathType.WATER, -1.0F);
		this.xpReward = 12;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.08D, true));
		this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.35F));
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.55D, 40));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		// mustSee=false: it can sense you through walls, which is exactly as
		// unsettling as it should be, and the navigator routes around them.
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 46.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.27D)
				.add(Attributes.ATTACK_DAMAGE, 7.0D)
				.add(Attributes.ATTACK_KNOCKBACK, 0.6D)
				.add(Attributes.FOLLOW_RANGE, 48.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.35D)
				.add(Attributes.ARMOR, 2.0D);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.BACTERIA_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.BACTERIA_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.BACTERIA_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 1.1F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 60;
	}
}
