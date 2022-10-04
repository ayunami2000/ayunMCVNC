package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;

import java.util.Set;
import java.util.stream.Collectors;

public class CommandGenAltDisplay extends AyunCommand {
	CommandGenAltDisplay() {
		super("genaltdisplay", "ayunmcvnc.manage", new int[] {1, -1});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 1) {
			sendUsage(sender, "<name>");
			return;
		}
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		Set<ArmorStand> fard = displayInfo.location.getWorld().getEntitiesByClass(ArmorStand.class).stream().filter(armorStand -> armorStand.hasMetadata("mcvnc-alt_display")).collect(Collectors.toSet());
		for (ArmorStand armorStand : fard) {
			armorStand.teleport(armorStand.getLocation().subtract(0, 1000, 0));
			armorStand.remove();
		}
		int width = displayInfo.width;
		int height = displayInfo.mapIds.size() / width;
		width *= 128;
		height *= 128;
		Location entityLoc = displayInfo.location.clone();
		double origY = entityLoc.getY();
		double origZ = entityLoc.getZ();
		for (int y = 0; y < height; y++) {
			if (y % 16 != 0) continue;
			for (int x = 0; x < width; x++) {
				// if (x % 16 != (y % 32 == 16 ? 8 : 0)) continue;
				if (x % 16 != 0) continue;
				entityLoc.setYaw(0);
				entityLoc.setPitch(0);
				ArmorStand armorStand = (ArmorStand) displayInfo.location.getWorld().spawnEntity(entityLoc, EntityType.ARMOR_STAND);
				armorStand.setHelmet(new ItemStack(Material.LEATHER_HELMET));
				armorStand.setSmall(true);
				armorStand.setVisible(false);
				armorStand.setInvulnerable(true);
				armorStand.setGravity(false);
				armorStand.setHeadPose(new EulerAngle(45, 0, 45));
				armorStand.setMetadata("mcvnc-alt_display", new FixedMetadataValue(Main.plugin, x + (y * width)));
				armorStand.teleport(entityLoc);
				entityLoc.setZ(origZ - 0.0078125 * x);
			}
			entityLoc.setY(origY - 0.0078125 * y);
		}
		sendMessage(sender, "Generated alt display!");
	}
}
