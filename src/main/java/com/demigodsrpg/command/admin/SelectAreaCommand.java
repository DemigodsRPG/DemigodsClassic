package com.demigodsrpg.command.admin;

import com.demigodsrpg.DGData;
import com.demigodsrpg.area.AreaSelection;
import com.demigodsrpg.command.type.BaseCommand;
import com.demigodsrpg.command.type.CommandResult;
import com.demigodsrpg.model.PlayerModel;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SelectAreaCommand extends BaseCommand {

    @Override
    protected CommandResult onCommand(CommandSender sender, Command command, String[] args) {
        if (sender instanceof Player) {
            PlayerModel model = DGData.PLAYER_R.fromPlayer((Player) sender);
            if (model.getAdminMode()) {
                AreaSelection selection = new AreaSelection((Player) sender);
                selection.register();
                AreaSelection.AREA_SELECTION_CACHE.put(model.getMojangId(), selection);

                sender.sendMessage(ChatColor.YELLOW + "You may now make a selection.");

                // TODO Disabling a selection

            } else {
                return CommandResult.NO_PERMISSIONS;
            }
        } else {
            return CommandResult.PLAYER_ONLY;
        }

        return CommandResult.SUCCESS;
    }
}
