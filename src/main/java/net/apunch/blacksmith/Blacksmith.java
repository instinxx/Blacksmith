package net.apunch.blacksmith;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.apunch.blacksmith.util.Settings.Setting;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.character.Character;
import net.citizensnpcs.api.util.DataKey;

public class Blacksmith extends Character {
    private static final int[] enchantments = new int[Enchantment.values().length];

    private final BlacksmithPlugin plugin;
    private final List<Material> reforgeableItems = new ArrayList<Material>();
    private final Map<String, Calendar> cooldowns = new HashMap<String, Calendar>();
    private ReforgeSession session;

    // Defaults
    private String busyWithPlayerMsg = Setting.BUSY_WITH_PLAYER_MESSAGE.asString();
    private String busyReforgingMsg = Setting.BUSY_WITH_REFORGE_MESSAGE.asString();
    private String costMsg = Setting.COST_MESSAGE.asString();
    private String invalidItemMsg = Setting.INVALID_ITEM_MESSAGE.asString();
    private String startReforgeMsg = Setting.START_REFORGE_MESSAGE.asString();
    private String successMsg = Setting.SUCCESS_MESSAGE.asString();
    private String failMsg = Setting.FAIL_MESSAGE.asString();
    private String insufficientFundsMsg = Setting.INSUFFICIENT_FUNDS_MESSAGE.asString();
    private String cooldownUnexpiredMsg = Setting.COOLDOWN_UNEXPIRED_MESSAGE.asString();
    private String itemChangedMsg = Setting.ITEM_UNEXPECTEDLY_CHANGED_MESSAGE.asString();
    private int minReforgeDelay = Setting.MIN_REFORGE_DELAY.asInt();
    private int maxReforgeDelay = Setting.MAX_REFORGE_DELAY.asInt();
    private int reforgeCooldown = Setting.REFORGE_COOLDOWN.asInt();
    private int failChance = Setting.FAIL_CHANCE.asInt();
    private int maxEnchantments = Setting.MAX_ENCHANTMENTS.asInt();
    private boolean dropItem = Setting.DROP_ITEM.asBoolean();

    public Blacksmith() {
        plugin = (BlacksmithPlugin) Bukkit.getServer().getPluginManager().getPlugin("Blacksmith");
        int i = 0;
        for (Enchantment enchantment : Enchantment.values())
            enchantments[i++] = enchantment.getId();
    }

    @Override
    public void load(DataKey key) {
        for (DataKey sub : key.getRelative("reforgeable-items").getIntegerSubKeys())
            if (Material.getMaterial(sub.getString("").toUpperCase().replace('-', '_')) != null)
                reforgeableItems.add(Material.getMaterial(sub.getString("").toUpperCase().replace('-', '_')));

        // Override defaults if they exist
        if (key.keyExists("messages.busy-with-player"))
            busyWithPlayerMsg = key.getString("messages.busy-with-player");
        if (key.keyExists("messages.busy-with-reforge"))
            busyReforgingMsg = key.getString("messages.busy-with-reforge");
        if (key.keyExists("messages.cost"))
            costMsg = key.getString("messages.cost");
        if (key.keyExists("messages.invalid-item"))
            invalidItemMsg = key.getString("messages.invalid-item");
        if (key.keyExists("messages.start-reforge"))
            startReforgeMsg = key.getString("messages.start-reforge");
        if (key.keyExists("messages.successful-reforge"))
            successMsg = key.getString("messages.successful-reforge");
        if (key.keyExists("messages.fail-reforge"))
            failMsg = key.getString("messages.fail-reforge");
        if (key.keyExists("messages.insufficient-funds"))
            insufficientFundsMsg = key.getString("messages.insufficient-funds");
        if (key.keyExists("messages.cooldown-not-expired"))
            cooldownUnexpiredMsg = key.getString("messages.cooldown-not-expired");
        if (key.keyExists("messages.item-changed-during-reforge"))
            itemChangedMsg = key.getString("messages.item-changed-during-reforge");
        if (key.keyExists("delays-in-seconds.minimum"))
            minReforgeDelay = key.getInt("delays-in-seconds.minimum");
        if (key.keyExists("delays-in-seconds.maximum"))
            maxReforgeDelay = key.getInt("delays-in-seconds.maximum");
        if (key.keyExists("delays-in-seconds.reforge-cooldown"))
            reforgeCooldown = key.getInt("delays-in-seconds.reforge-cooldown");
        if (key.keyExists("percent-chance-to-fail-reforge"))
            failChance = key.getInt("percent-chance-to-fail-reforge");
        if (key.keyExists("maximum-enchantments"))
            maxEnchantments = key.getInt("maximum-enchantments");
        if (key.keyExists("drop-item"))
            dropItem = key.getBoolean("drop-item");
    }

    @Override
    public void onRightClick(NPC npc, Player player) {
        if (!player.hasPermission("blacksmith.reforge"))
            return;

        if (cooldowns.get(player.getName()) != null) {
            if (!Calendar.getInstance().after(cooldowns.get(player.getName()))) {
                npc.chat(player, cooldownUnexpiredMsg);
                return;
            }
            cooldowns.remove(player.getName());
        }

        ItemStack hand = player.getItemInHand();
        if (session != null) {
            if (!session.isInSession(player)) {
                npc.chat(player, busyWithPlayerMsg);
                return;
            }

            if (session.isRunning()) {
                npc.chat(player, busyReforgingMsg);
                return;
            }
            if (session.handleClick())
                session = null;
            else
                reforge(npc, player);
        } else {
            if ((!plugin.isTool(hand) && !plugin.isArmor(hand))
                    || (!reforgeableItems.isEmpty() && !reforgeableItems.contains(hand.getType()))) {
                npc.chat(player, invalidItemMsg);
                return;
            }
            session = new ReforgeSession(player, npc);
            npc.chat(
                    player,
                    costMsg.replace("<price>", plugin.formatCost(player)).replace("<item>",
                            hand.getType().name().toLowerCase().replace('_', ' ')));
        }
    }

    @Override
    public void save(DataKey key) {
        for (int i = 0; i < reforgeableItems.size(); i++)
            key.getRelative("reforgeable-items").setString(String.valueOf(i),
                    reforgeableItems.get(i).name().toLowerCase().replace('_', '-'));

        key.setString("messages.busy-with-player", busyWithPlayerMsg);
        key.setString("messages.busy-with-reforge", busyReforgingMsg);
        key.setString("messages.cost", costMsg);
        key.setString("messages.invalid-item", invalidItemMsg);
        key.setString("messages.start-reforge", startReforgeMsg);
        key.setString("messages.successful-reforge", successMsg);
        key.setString("messages.fail-reforge", failMsg);
        key.setString("messages.insufficient-funds", insufficientFundsMsg);
        key.setString("messages.cooldown-not-expired", cooldownUnexpiredMsg);
        key.setString("messages.item-changed-during-reforge", itemChangedMsg);
        key.setInt("delays-in-seconds.minimum", minReforgeDelay);
        key.setInt("delays-in-seconds.maximum", maxReforgeDelay);
        key.setInt("delays-in-seconds.reforge-cooldown", reforgeCooldown);
        key.setInt("percent-chance-to-fail-reforge", failChance);
        key.setInt("maximum-enchantments", maxEnchantments);
        key.setBoolean("drop-item", dropItem);
    }

    private void reforge(NPC npc, Player player) {
        npc.chat(player, startReforgeMsg);
        plugin.withdraw(player);
        session.beginReforge();
        if (npc.getBukkitEntity() instanceof Player)
            ((Player) npc.getBukkitEntity()).setItemInHand(player.getItemInHand());
        player.setItemInHand(null);
    }

    private class ReforgeSession implements Runnable {
        private final Player player;
        private final NPC npc;
        private final ItemStack reforge;
        private int taskId;

        private ReforgeSession(Player player, NPC npc) {
            this.player = player;
            this.npc = npc;
            reforge = player.getItemInHand();
        }

        @Override
        public void run() {
            npc.chat(player, reforgeItemInHand() ? successMsg : failMsg);
            if (npc.getBukkitEntity() instanceof Player)
                ((Player) npc.getBukkitEntity()).setItemInHand(null);
            if (dropItem)
                player.getWorld().dropItemNaturally(npc.getBukkitEntity().getLocation(), reforge);
            else {
                for (ItemStack stack : player.getInventory().addItem(reforge).values())
                    player.getWorld().dropItemNaturally(npc.getBukkitEntity().getLocation(), stack);
            }
            session = null;
            // Start cooldown
            Calendar wait = Calendar.getInstance();
            wait.add(Calendar.SECOND, reforgeCooldown);
            cooldowns.put(player.getName(), wait);
        }

        private boolean reforgeItemInHand() {
            Random random = new Random();
            if (random.nextInt(100) < failChance) {
                for (Enchantment enchantment : reforge.getEnchantments().keySet()) {
                    // Remove or downgrade enchantments
                    if (random.nextBoolean())
                        reforge.removeEnchantment(enchantment);
                    else {
                        if (reforge.getEnchantmentLevel(enchantment) > 1) {
                            reforge.removeEnchantment(enchantment);
                            reforge.addEnchantment(enchantment, 1);
                        }
                    }
                }
                // Damage the item
                short durability = (short) (reforge.getDurability() + reforge.getDurability() * random.nextInt(8));
                short maxDurability = reforge.getType().getMaxDurability();
                if (durability <= 0)
                    durability = (short) (maxDurability / 3);
                else if (reforge.getDurability() + durability > maxDurability)
                    durability = (short) (maxDurability - random.nextInt(maxDurability - 25));
                reforge.setDurability(durability);
                return false;
            }
            // Add random enchantments
            int chance = 10;
            if (reforge.getDurability() == 0)
                chance *= 4;
            else
                reforge.setDurability((short) 0);
            for (int i = 0; i < chance; i++) {
                if (reforge.getEnchantments().keySet().size() == maxEnchantments)
                    break;
                Enchantment enchantment = Enchantment.getById(enchantments[random.nextInt(enchantments.length)]);
                if (enchantment.canEnchantItem(reforge))
                    reforge.addEnchantment(enchantment, random.nextInt(enchantment.getMaxLevel()) + 1);
            }
            return true;
        }

        // Return if the session should end
        private boolean handleClick() {
            // Prevent player from switching items during session
            if (!reforge.equals(player.getItemInHand())) {
                npc.chat(player, itemChangedMsg);
                return true;
            }
            if (!plugin.doesPlayerHaveEnough(player)) {
                npc.chat(player, insufficientFundsMsg);
                return true;
            }
            return false;
        }

        private boolean isRunning() {
            return plugin.getServer().getScheduler().isQueued(taskId);
        }

        private boolean isInSession(Player other) {
            return player.getName().equals(other.getName());
        }

        private void beginReforge() {
            taskId = plugin
                    .getServer()
                    .getScheduler()
                    .scheduleAsyncDelayedTask(plugin, this,
                            (new Random().nextInt(maxReforgeDelay) + minReforgeDelay) * 20);
        }
    }
}