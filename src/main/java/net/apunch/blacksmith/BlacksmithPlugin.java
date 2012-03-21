package net.apunch.blacksmith;

import java.util.logging.Level;

import net.apunch.blacksmith.util.Settings;
import net.apunch.blacksmith.util.Settings.Setting;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.character.CharacterFactory;
import net.citizensnpcs.api.util.DataKey;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BlacksmithPlugin extends JavaPlugin {
    private Settings config;
    private Economy economy;

    @Override
    public void onDisable() {
        config.save();

        getLogger().log(Level.INFO, " v" + getDescription().getVersion() + " disabled.");
    }

    @Override
    public void onEnable() {
        config = new Settings(this);
        config.load();

        // Setup Vault
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(
                Economy.class);
        if (economyProvider != null)
            economy = economyProvider.getProvider();
        else {
            // Disable if no economy plugin was found
            final Plugin plugin = this;
            getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

                public void run() {
                    getServer().getLogger().log(Level.SEVERE, "Failed to load an economy plugin. Disabling...");
                    getServer().getPluginManager().disablePlugin(plugin);
                }
            });
        }

        CitizensAPI.getCharacterManager().registerCharacter(
                new CharacterFactory(Blacksmith.class).withName("blacksmith"));

        getLogger().log(Level.INFO, " v" + getDescription().getVersion() + " enabled.");
    }

    public boolean isTool(ItemStack item) {
        switch (item.getType()) {
        case WOOD_PICKAXE:
        case WOOD_SPADE:
        case WOOD_HOE:
        case WOOD_SWORD:
        case WOOD_AXE:
        case STONE_PICKAXE:
        case STONE_SPADE:
        case STONE_HOE:
        case STONE_SWORD:
        case STONE_AXE:
        case GOLD_PICKAXE:
        case GOLD_SPADE:
        case GOLD_HOE:
        case GOLD_SWORD:
        case GOLD_AXE:
        case IRON_PICKAXE:
        case IRON_SPADE:
        case IRON_HOE:
        case IRON_SWORD:
        case IRON_AXE:
        case DIAMOND_PICKAXE:
        case DIAMOND_SPADE:
        case DIAMOND_HOE:
        case DIAMOND_SWORD:
        case DIAMOND_AXE:
        case BOW:
        case FLINT_AND_STEEL:
        case FISHING_ROD:
        case SHEARS:
            return true;
        default:
            return false;
        }
    }

    public boolean isArmor(ItemStack item) {
        switch (item.getType()) {
        case LEATHER_HELMET:
        case LEATHER_CHESTPLATE:
        case LEATHER_LEGGINGS:
        case LEATHER_BOOTS:
        case CHAINMAIL_HELMET:
        case CHAINMAIL_CHESTPLATE:
        case CHAINMAIL_LEGGINGS:
        case CHAINMAIL_BOOTS:
        case GOLD_HELMET:
        case GOLD_CHESTPLATE:
        case GOLD_LEGGINGS:
        case GOLD_BOOTS:
        case IRON_HELMET:
        case IRON_CHESTPLATE:
        case IRON_LEGGINGS:
        case IRON_BOOTS:
        case DIAMOND_HELMET:
        case DIAMOND_CHESTPLATE:
        case DIAMOND_LEGGINGS:
        case DIAMOND_BOOTS:
            return true;
        default:
            return false;
        }
    }

    public boolean doesPlayerHaveEnough(Player player) {
        return economy.getBalance(player.getName()) - getCost(player.getItemInHand()) >= 0;
    }

    public String formatCost(Player player) {
        return economy.format(getCost(player.getItemInHand()));
    }

    public void withdraw(Player player) {
        economy.withdrawPlayer(player.getName(), getCost(player.getItemInHand()));
    }

    private double getCost(ItemStack item) {
        DataKey root = config.getConfig().getKey("");
        double price = Setting.BASE_PRICE.asDouble();
        if (root.keyExists("base-prices." + item.getType().name().toLowerCase().replace('_', '-')))
            price = root.getDouble("base-prices." + item.getType().name().toLowerCase().replace('_', '-'));

        // Adjust price based on durability and enchantments
        price += (item.getType().getMaxDurability() - item.getDurability());

        double enchantmentModifier = Setting.ENCHANTMENT_MODIFIER.asDouble();
        for (Enchantment enchantment : item.getEnchantments().keySet()) {
            if (root.keyExists("enchantment-modifiers." + enchantment.getName().toLowerCase().replace('_', '-')))
                enchantmentModifier = root.getDouble("enchantment-modifiers."
                        + enchantment.getName().toLowerCase().replace('_', '-'));
            price += enchantmentModifier * item.getEnchantmentLevel(enchantment);
        }
        return price;
    }
}