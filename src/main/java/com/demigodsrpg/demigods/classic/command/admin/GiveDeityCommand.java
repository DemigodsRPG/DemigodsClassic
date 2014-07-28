package com.demigodsrpg.demigods.classic.command.admin;

import com.demigodsrpg.demigods.classic.DGClassic;
import com.demigodsrpg.demigods.classic.command.type.AdminPlayerCommand;
import com.demigodsrpg.demigods.classic.command.type.CommandResult;
import com.demigodsrpg.demigods.classic.deity.Deity;
import com.demigodsrpg.demigods.classic.model.PlayerModel;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveDeityCommand extends AdminPlayerCommand {
    @Override
    public CommandResult onCommand(CommandSender sender, PlayerModel model, String[] args) {
        if (args.length == 3) {
            Player p;
            Deity deity;
            boolean major;
            try {
                if(args[2].equalsIgnoreCase("major"))major = true;
                else{major = false;}
                deity = Deity.valueOf(args[1].toUpperCase());
                p = DGClassic.PLAYER_R.fromName(args[0]).getOfflinePlayer().getPlayer();
            } catch (Exception ignored) {
                sender.sendMessage(ChatColor.RED + "Wrong player or deity! Please try a little harder.");
                return CommandResult.QUIET_ERROR;
            }
            if(major)
            {
                DGClassic.PLAYER_R.fromPlayer(p).giveMajorDeity(deity);
                sender.sendMessage(ChatColor.YELLOW + "You added " + deity.getNomen() + " to " + p.getName() + " as major deity.");
                return CommandResult.SUCCESS;
            }
            else
            {
                DGClassic.PLAYER_R.fromPlayer(p).giveDeity(deity);
                sender.sendMessage(ChatColor.YELLOW + "You added " + deity.getNomen() + " to " + p.getName() + " as minor deity.");
                return CommandResult.SUCCESS;
            }

        }
        return CommandResult.NOT_ENOUGH_ARGS;
    }
}
