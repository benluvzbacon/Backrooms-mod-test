package net.backrooms.sanity;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.backrooms.Backrooms;
import net.minecraft.resources.ResourceLocation;

/**
 * Persistent per-player data attachments.
 *
 * <p>Sanity is a 0..1 float, defaults to full, is saved with the player and
 * copied across death/respawn.</p>
 */
public final class ModAttachments {
	@SuppressWarnings("UnstableApiUsage")
	public static final AttachmentType<Float> SANITY = AttachmentRegistry.create(
			ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "sanity"),
			builder -> builder
					.initializer(() -> 1.0F)
					.persistent(Codec.FLOAT)
					.copyOnDeath());

	private ModAttachments() {
	}

	public static void initialize() {
		// Class load registers the attachment.
	}
}
