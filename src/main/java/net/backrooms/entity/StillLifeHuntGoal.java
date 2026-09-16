package net.backrooms.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The Still Life's whole behaviour in one goal:
 *
 * <p>While any nearby player is actually looking at it, it freezes completely
 * (a mannequin caught mid-pose, slowly turning to face its watcher). The
 * instant nobody has eyes on it, it sprints the victim down and attacks.
 * Blink, and it is closer.</p>
 */
public class StillLifeHuntGoal extends Goal {
	private static final double WATCH_RANGE = 48.0D;
	private static final double WATCH_DOT = 0.40D;
	private static final double ATTACK_RANGE = 1.7D;
	private static final int ATTACK_INTERVAL = 20;

	private final StillLifeEntity mob;
	private Player target;
	private Player watcher;
	private int attackCooldown;
	private int repathCooldown;

	public StillLifeHuntGoal(StillLifeEntity mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK, Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		this.target = this.mob.level().getNearestPlayer(this.mob, WATCH_RANGE);
		if (this.target == null || !this.target.isAlive() || isUntargetable(this.target)) {
			this.target = null;
			return false;
		}
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.target == null || !this.target.isAlive() || isUntargetable(this.target)) {
			return false;
		}
		return this.mob.distanceToSqr(this.target) < WATCH_RANGE * WATCH_RANGE * 1.5D;
	}

	@Override
	public void stop() {
		this.target = null;
		this.watcher = null;
		// Otherwise the mannequin pose would stick after the target leaves.
		this.mob.setFrozenPose(false);
		this.mob.getNavigation().stop();
	}

	/** Like vanilla monsters, it ignores creative and spectator players. */
	private static boolean isUntargetable(Player player) {
		return player.isSpectator() || player.isCreative();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}
		this.watcher = findWatcher();
		this.mob.setFrozenPose(this.watcher != null);
		if (this.watcher != null) {
			// Lock in place, only the head may turn to meet the watcher's eyes.
			this.mob.getNavigation().stop();
			Vec3 v = this.mob.getDeltaMovement();
			this.mob.setDeltaMovement(0.0D, v.y, 0.0D);
			this.mob.getLookControl().setLookAt(this.watcher, 30.0F, 30.0F);
			return;
		}

		this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
		double distance = this.mob.distanceTo(this.target);
		if (distance > ATTACK_RANGE) {
			// Repath at most a few times a second - a full A* every tick is wasteful.
			if (--this.repathCooldown <= 0 || this.mob.getNavigation().isDone()) {
				this.mob.getNavigation().moveTo(this.target, 1.5D);
				this.repathCooldown = 8;
			}
		} else {
			this.mob.getNavigation().stop();
			if (this.attackCooldown <= 0) {
				// Swing first so the two-armed lunge animation actually plays.
				this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
				this.mob.doHurtTarget(this.target);
				this.attackCooldown = ATTACK_INTERVAL;
			}
		}
		if (this.attackCooldown > 0) {
			this.attackCooldown--;
		}
	}

	/** A player within range, roughly facing the mob with line of sight. */
	private Player findWatcher() {
		for (Player player : this.mob.level().players()) {
			if (!player.isAlive() || player.isSpectator() || player.isCreative() || player.isInvisible()) {
				continue;
			}
			double distance = this.mob.distanceToSqr(player);
			if (distance > WATCH_RANGE * WATCH_RANGE) {
				continue;
			}
			Vec3 eyes = player.getEyePosition(1.0F);
			Vec3 toMob = this.mob.position().add(0.0D, this.mob.getBbHeight() * 0.9D, 0.0D).subtract(eyes);
			double length = toMob.length();
			if (length < 1.0E-4D) {
				return player;
			}
			double dot = player.getViewVector(1.0F).dot(toMob.scale(1.0D / length));
			if (dot > WATCH_DOT && player.hasLineOfSight(this.mob)) {
				return player;
			}
		}
		return null;
	}
}
