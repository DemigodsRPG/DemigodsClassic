package com.demigodsrpg.aspect.fire;

import com.demigodsrpg.aspect.Aspect;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.MaterialData;

public class FireAspect implements Aspect.Group {
    @Override
    public String getName() {
        return "Fire Aspect";
    }

    @Override
    public ChatColor getColor() {
        return ChatColor.GOLD;
    }

    @Override
    public Sound getSound() {
        return Sound.BLOCK_FIRE_AMBIENT;
    }

    @Override
    public BlockData getClaimMaterial() {
        return Bukkit.getServer().createBlockData(Material.FIRE_CHARGE);
    }
}
