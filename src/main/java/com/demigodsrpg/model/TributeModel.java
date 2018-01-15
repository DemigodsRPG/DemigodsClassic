package com.demigodsrpg.model;

import com.demigodsrpg.DGData;
import com.demigodsrpg.registry.TributeRegistry;
import com.demigodsrpg.util.datasection.DataSection;
import com.demigodsrpg.util.datasection.Model;
import com.demigodsrpg.util.misc.MapUtil2;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TributeModel implements Model {
    private static final double VALUE_K = 14.286;
    private static double OFFSET = 1.0;

    private final Material material;
    private List<Double> tributeTimes;
    private int fitness;
    private final TributeRegistry.Category category;
    private double lastKnownValue;

    public TributeModel(Material material, DataSection conf) {
        this.material = material;
        tributeTimes = conf.getDoubleList("tribute_times");
        fitness = conf.getInt("fitness");
        category = DGData.TRIBUTE_R.getCategory(material);
        lastKnownValue = conf.getDouble("last_known_value");
    }

    public TributeModel(Material material, int fitness) {
        this.material = material;
        tributeTimes = new ArrayList<>();
        this.fitness = fitness;
        category = DGData.TRIBUTE_R.getCategory(material);
        lastKnownValue = 1.0;
    }

    public Material getMaterial() {
        return material;
    }

    List<Double> getTributeTimes() {
        long twoWeeksAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14);
        List<Double> tributeTimesClone = new ArrayList<>();
        tributeTimesClone.addAll(this.tributeTimes);
        tributeTimesClone.stream().filter(time -> time < twoWeeksAgo).forEach(this.tributeTimes::remove);
        return tributeTimes;
    }

    public void addTributeTime() {
        tributeTimes.add((double) System.currentTimeMillis());
    }

    public int getFitness() {
        return fitness;
    }

    public void setFitness(int amount) {
        this.fitness = amount;
        tributeTimes.add((double) System.currentTimeMillis());
        DGData.TRIBUTE_R.register(this);
    }

    public TributeRegistry.Category getCategory() {
        return category;
    }

    double getFrequency() {
        return getTributeTimes().size() / 336;
    }

    public double getLastKnownValue() {
        return lastKnownValue;
    }

    private void updateValue() {
        if (getCategory().equals(TributeRegistry.Category.WORTHLESS)) {
            lastKnownValue = 0.0;
        } else if (getCategory().equals(TributeRegistry.Category.CHEATING)) {
            lastKnownValue = -3000.0;
        } else {
            lastKnownValue = (getValuePercentage() / OFFSET) * VALUE_K * DGData.TRIBUTE_R.getRegisteredData().size();
        }
        DGData.TRIBUTE_R.register(this);
    }

    private double getValuePercentage() {
        Collection<TributeModel> allInCat = DGData.TRIBUTE_R.find(getCategory());
        int size = allInCat.size();
        if (size < 2) {
            size = 2;
        }
        Map<TributeModel, Double> map = new HashMap<>();
        for (TributeModel model : allInCat) {
            map.put(model, model.getFrequency());
        }
        int count = 1;
        double rankInCategory = 1.0;
        for (TributeModel model : MapUtil2.sortByValue(map, false).keySet()) {
            if (model.equals(this)) {
                rankInCategory = (double) count;
                break;
            }
        }
        double fractionOfCategory = rankInCategory / size;
        double categoryFitness = DGData.TRIBUTE_R.getTributesForCategory(getCategory());
        double fractionOfTotal = 1 - (categoryFitness / DGData.TRIBUTE_R.getTotalTributes());
        return fractionOfTotal * fractionOfCategory;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("tribute_times", tributeTimes);
        map.put("fitness", fitness);
        map.put("last_known_value", lastKnownValue);
        return map;
    }

    @Override
    public String getKey() {
        return getMaterial().name();
    }

    public static class ValueTask extends BukkitRunnable {
        @Override
        public void run() {
            OFFSET = 1.0;
            for (TributeModel model : DGData.TRIBUTE_R.getRegisteredData().values()) {
                OFFSET += model.getValuePercentage();
            }

            for (TributeModel model : DGData.TRIBUTE_R.getRegisteredData().values()) {
                // Trim the tribute times
                if (model.tributeTimes.size() > 300) {
                    model.tributeTimes =
                            model.tributeTimes.subList(model.tributeTimes.size() - 31, model.tributeTimes.size() - 1);
                }

                // Update the value
                model.updateValue();
            }
        }
    }
}
