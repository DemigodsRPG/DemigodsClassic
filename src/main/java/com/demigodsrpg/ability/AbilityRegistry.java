package com.demigodsrpg.ability;

import com.demigodsrpg.aspect.Aspect;
import com.demigodsrpg.util.ZoneUtil;
import com.demigodsrpg.util.misc.StringUtil2;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AbilityRegistry implements Listener {
    // FIXME Do we really need two collections for the same data? This is expensive...
    private static AbilityCasterProvider CASTER_PROVIDER;
    private static CooldownHandler COOLDOWNS;
    private final boolean NO_COST_ASPECT_MODE;

    private static final ConcurrentMap<String, AbilityMetaData> REGISTERED_COMMANDS = new ConcurrentHashMap<>();
    private static final Multimap<String, AbilityMetaData> REGISTERED_ABILITIES =
            Multimaps.newListMultimap(new ConcurrentHashMap<>(), () -> new ArrayList<>(0));

    public AbilityRegistry(AbilityCasterProvider casterProvider, CooldownHandler cooldowns, boolean noCostAspectMode) {
        CASTER_PROVIDER = casterProvider;
        COOLDOWNS = cooldowns;
        NO_COST_ASPECT_MODE = noCostAspectMode;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onEvent(PlayerInteractEvent event) {
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK:
            case LEFT_CLICK_AIR:
                return;
        }
        for (AbilityMetaData ability : REGISTERED_ABILITIES.get(event.getClass().getName())) {
            try {
                AbilityCaster model = CASTER_PROVIDER.fromPlayer(event.getPlayer());
                if (processAbility1(model, ability)) {
                    Object rawResult = ability.getMethod().invoke(ability.getAspect(), event);
                    processAbility2(event.getPlayer(), model, ability, rawResult);
                    event.setCancelled(true);
                    return;
                }
            } catch (Exception oops) {
                oops.printStackTrace();
            }

        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onEvent(EntityDamageByEntityEvent event) {
        for (AbilityMetaData ability : REGISTERED_ABILITIES.get(event.getClass().getName())) {
            try {
                if (event.getDamager() instanceof Player) {
                    Player player = (Player) event.getDamager();
                    AbilityCaster model = CASTER_PROVIDER.fromPlayer(player);
                    if (processAbility1(model, ability)) {
                        Object rawResult = ability.getMethod().invoke(ability.getAspect(), event);
                        processAbility2(player, model, ability, rawResult);
                    }
                }
            } catch (Exception oops) {
                oops.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void bindCommands(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        message = message.substring(1);
        String[] args = message.split("\\s+");
        Player player = event.getPlayer();

        if (!ZoneUtil.inNoDGZone(event.getPlayer().getLocation())) {
            // Process the command
            try {
                if (args.length == 2 && "info".equals(args[1])) {
                    if (abilityInfo(player, args[0].toLowerCase())) {
                        //CONSOLE.info(event.getPlayer().getName() + " used the command: /" + message);
                        event.setCancelled(true);
                        return;
                    }
                }
                if (bindAbility(player, args[0].toLowerCase())) {
                    //CONSOLE.info(event.getPlayer().getName() + " used the command: /" + message);
                    event.setCancelled(true);
                }
            } catch (Exception errored) {
                // Not a command
                errored.printStackTrace();
            }
        }
    }

    boolean abilityInfo(Player player, String command) {
        for (AbilityMetaData ability : REGISTERED_ABILITIES.values()) {
            if (ability.getCommand().equals(command)) {
                player.sendMessage(StringUtil2.chatTitle(ability.getName()));
                player.sendMessage(
                        " - Aspect: " + ability.getAspect().getGroup().getColor() + ability.getAspect().name());
                player.sendMessage(" - Type: " + StringUtil2.beautify(ability.getType().name()));
                if (!ability.getType().equals(Ability.Type.PASSIVE)) {
                    player.sendMessage(" - Cost: " + ability.getCost());
                }
                if (ability.getCooldown() > 0) {
                    player.sendMessage(" - Cooldown (ms): " + ability.getCooldown());
                }
                player.sendMessage(ability.getInfo());
                return true;
            }
        }
        return false;
    }

    boolean bindAbility(Player player, String command) {
        // Is this a correct command?
        if (!REGISTERED_COMMANDS.containsKey(command)) {
            return false;
        }

        // Can't bind to air.
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (Material.AIR.equals(handItem.getType())) {
            abilityInfo(player, command);
            return true;
        }

        AbilityCaster model = CASTER_PROVIDER.fromPlayer(player);
        Material material = handItem.getType();
        Optional<AbilityMetaData> boundOptional = model.getBound(material);
        if (boundOptional.isPresent()) {
            AbilityMetaData bound = boundOptional.get();
            if (!bound.getCommand().equals(command)) {
                player.sendMessage(ChatColor.RED + "This item already has /" + bound.getCommand() + " bound to it.");
            } else {
                model.unbind(bound);
                player.sendMessage(ChatColor.YELLOW + bound.getName() + " has been unbound.");
            }
            return true;
        } else {
            Optional<AbilityMetaData> abilityOptional = fromCommand(command);
            if (abilityOptional.isPresent()) {
                AbilityMetaData ability = abilityOptional.get();
                if (ability.getCommand().equals(command) && model.getAspects().contains(ability.getAspect().name()) &&
                        ability.getCommand().equals(command)) {
                    model.bind(ability, material);
                    player.sendMessage(ChatColor.YELLOW + ability.getName() + " has been bound to " +
                            StringUtil2.beautify(material.name()) + ".");
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("ConstantConditions") // This should never be run on an offline player
    boolean processAbility1(AbilityCaster model, AbilityMetaData ability) {
        if (!model.getLocation().isPresent()) return false;
        if (ZoneUtil.inNoDGZone(model.getLocation().get())) return false;
        if (!ability.getType().equals(Ability.Type.PASSIVE)) {
            if ((ability.getType().equals(Ability.Type.OFFENSIVE) || ability.getType().equals(Ability.Type.ULTIMATE)) &&
                    ZoneUtil.inNoPvpZone(model.getOfflinePlayer().getPlayer(), model.getLocation().get())) {
                return false;
            }
            if (!model.getBound(ability).isPresent()) {
                return false;
            }
            model.getOfflinePlayer().getPlayer().getInventory().getItemInMainHand().getType();
            model.getBound(ability);
            return false;

        }
        return true;
    }

    void processAbility2(Player player, AbilityCaster model, AbilityMetaData ability, @Nullable Object rawResult) {
        // Check for result
        if (rawResult == null) {
            throw new NullPointerException("An ability (" + ability.getName() + ") returned null while casting.");
        }

        try {
            // Process result
            AbilityResult result = (AbilityResult) rawResult;
            switch (result) {
                case SUCCESS: {
                    break;
                }
                case NO_TARGET_FOUND: {
                    player.sendMessage(ChatColor.YELLOW + "No target found.");
                    return;
                }
                case OTHER_FAILURE: {
                    return;
                }
            }

            // Process ability
            double cost = ability.getCost();
            long delay = ability.getDelay();
            long cooldown = ability.getCooldown();

            if (delay > 0) {
                COOLDOWNS.delay(model, ability);
            }
            if (cooldown > 0) {
                COOLDOWNS.cooldown(model, ability);
            }
            if (!NO_COST_ASPECT_MODE && cost > 0) {
                model.setFavor(model.getFavor() - cost);
            }
        } catch (Exception ignored) {
        }
    }

    public List<AbilityMetaData> getAbilities(Aspect aspect) {
        Class<? extends Aspect> deityClass = aspect.getClass();
        List<AbilityMetaData> abilityMetaDatas = new ArrayList<>();
        for (Method method : deityClass.getMethods()) {
            if (method.isAnnotationPresent(Ability.class)) {
                Ability ability = method.getAnnotation(Ability.class);
                abilityMetaDatas.add(new AbilityMetaData(aspect, method, ability));
            }
        }
        return abilityMetaDatas;
    }

    public void registerAbilities(Aspect[] aspects) {
        for (Aspect aspect : aspects) {
            Class<? extends Aspect> deityClass = aspect.getClass();
            for (Method method : deityClass.getMethods()) {
                if (method.isAnnotationPresent(Ability.class)) {
                    Ability ability = method.getAnnotation(Ability.class);
                    register(aspect, method, ability);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    void register(Aspect aspect, Method method, Ability ability) {
        if (ability.placeholder()) return;
        Class<?>[] paramaters = method.getParameterTypes();
        try {
            if (paramaters.length < 1) {
                //CONSOLE.severe("An ability (" + ability.name() + ") tried to register without any parameters.");
                return;
            }
            Class<? extends Event> eventClass = (Class<? extends Event>) paramaters[0];
            AbilityMetaData data = new AbilityMetaData(aspect, method, ability);
            REGISTERED_ABILITIES.put(eventClass.getName(), data);
            if (!"".equals(data.getCommand())) {
                REGISTERED_COMMANDS.put(data.getCommand(), data);
            }
        } catch (Exception oops) {
            oops.printStackTrace();
        }
    }

    public static Optional<AbilityMetaData> fromCommand(String commandName) {
        if (REGISTERED_COMMANDS.containsKey(commandName)) {
            return Optional.of(REGISTERED_COMMANDS.get(commandName));
        }
        return Optional.empty();
    }
}
