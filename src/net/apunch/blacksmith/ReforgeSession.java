package net.apunch.blacksmith;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ReforgeSession {
    private final Blacksmith plugin;
    private final Player player;
    private final ItemStack reforge;
    private final NPC npc;
    private int taskId;

    public ReforgeSession(Player player, NPC npc) {
        this.player = player;
        reforge = player.getItemInHand();
        this.npc = npc;

        plugin = (Blacksmith) player.getServer().getPluginManager().getPlugin("Blacksmith");
    }

    // Return is the session should end
    public boolean handleClick() {
        // Prevent player from switching items during session
        if (!reforge.equals(player.getItemInHand())) {
            npc.chat(player, "<c>That's not the item you wanted to reforge before!");
            return true;
        }
        if (!plugin.doesPlayerHaveEnough(player)) {
            npc.chat(player, ((BlacksmithCharacter) npc.getCharacter()).getInsufficientFundsMessage());
            return true;
        }
        return false;
    }

    public boolean isRunning() {
        return plugin.getServer().getScheduler().isQueued(taskId);
    }

    public boolean isInSession(Player other) {
        return player.getName().equals(other.getName());
    }

    public void setTask(int taskId) {
        this.taskId = taskId;
    }
}