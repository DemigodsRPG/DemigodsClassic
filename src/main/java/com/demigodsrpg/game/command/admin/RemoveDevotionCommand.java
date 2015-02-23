package com.demigodsrpg.game.command.admin;

import com.demigodsrpg.game.DGGame;
import com.demigodsrpg.game.aspect.Aspects;
import com.demigodsrpg.game.command.type.AdminPlayerCommand;
import com.demigodsrpg.game.command.type.CommandResult;
import com.demigodsrpg.game.model.PlayerModel;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveDevotionCommand extends AdminPlayerCommand {
    @Override
    public CommandResult onCommand(CommandSender sender, PlayerModel model, String[] args) {
        if (args.length == 3) {

            PlayerModel m;
            try {
                Player p = DGGame.PLAYER_R.fromName(args[0]).getOfflinePlayer().getPlayer();
                double amount = Double.parseDouble(args[2]);
                m = DGGame.PLAYER_R.fromPlayer(p);
                Aspects aspect = Aspects.valueOf(args[1].toUpperCase());
                if (!m.getAllDeities().contains(aspect)) {
                    sender.sendMessage(ChatColor.RED + "The player you are accessing does not have that deity.");
                    return CommandResult.QUIET_ERROR;
                }
                double newAmount = m.getExperience(aspect) - amount;
                if (newAmount < 0) newAmount = 0;

                m.setExperience(aspect, newAmount);

                sender.sendMessage(ChatColor.YELLOW + "You removed " + amount + " devotion from " + p.getName() + " in the deity " + aspect.getNomen() + ".");
            } catch (Exception ignored) {
                sender.sendMessage(ChatColor.RED + "Invalid syntax! /RemoveDevotion [Name, Deity, Amount]");
                return CommandResult.QUIET_ERROR;
            }
            return CommandResult.SUCCESS;
        }
        return CommandResult.INVALID_SYNTAX;
    }
}