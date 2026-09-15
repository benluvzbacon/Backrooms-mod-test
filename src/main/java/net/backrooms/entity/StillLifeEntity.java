package net.backrooms.entity;

import net.backrooms.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * The Still Life - a gaunt, tricorn-hatted mannequin standing in the halls.
 *
 * <p>It does not move as long as someone is looking at it; the instant vision
 * is broken it sprints the gap and attacks ({@link StillLifeHuntGoal}). When no
 * one is around it idles and occasionally wanders. Navigation is all vanilla,
 * which routes reliably through the procedural doorways and arches.</p>
 */
public class StillLifeEntity extends Monster {
	private boolean frozenPose;
	private long lastFrozenSwitch;

	public StillLifeEntity(EntityType<StillLifeEntity> type, Level level) {
		super(type, level);
		this.setPathfindingMalus(PathType.DANGER_OTHER, 0.0F);
		this.setPathfindingMalus(PathType.DAMAGE_OTHER, 0.0F);
		this.setPathfindingMalus(PathType.WATER, -1.0F);
		this.xpReward = 14;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new StillLifeHuntGoal(this));
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.5D, 60));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 50.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.28D)
				.add(Attributes.ATTACK_DAMAGE, 7.0D)
				.add(Attributes.ATTACK_KNOCKBACK, 0.5D)
				.add(Attributes.FOLLOW_RANGE, 48.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
				.add(Attributes.ARMOR, 2.0D);
	}

	/** Set by the hunt goal each tick; the renderer uses it to lock the pose. */
	public void setFrozenPose(boolean frozen) {
		if (frozen != this.frozenPose) {
			this.lastFrozenSwitch = this.level().getGameTime();
		}
		this.frozenPose = frozen;
	}

	public boolean isFrozenPose() {
		return this.frozenPose;
	}

	public long getLastFrozenSwitch() {
		return this.lastFrozenSwitch;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.STILL_LIFE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.STILL_LIFE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.STILL_LIFE_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 1.0F;
	}

	@Override
	public int getAmbientSoundInterval() {
		// Rare, quiet groan - most of the time it simply stands there.
		return 160;
	}
}
