package com.demigodsrpg.aspect.wind;

import com.demigodsrpg.aspect.Aspect;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.MaterialData;

public class WindAspect implements Aspect.Group {
    @Override
    public String getName() {
        return "Wind Aspect";
    }

    @Override
    public ChatColor getColor() {
        return ChatColor.BLUE;
    }

    @Override
    public Sound getSound() {
        return Sound.UI_BUTTON_CLICK;
    }

    @Override
    public BlockData getClaimMaterial() {
        return Bukkit.getServer().createBlockData(Material.FEATHER);
    }
}
