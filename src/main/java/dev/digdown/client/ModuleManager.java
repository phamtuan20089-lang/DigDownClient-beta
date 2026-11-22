package dev.digdown.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {
    }

    public static void register(Module module) {
        MODULES.add(module);
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> byCategory(Module.Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : MODULES) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public static void tick(Minecraft mc) {
        for (Module module : MODULES) {
            if (module.isEnabled()) {
                module.onTick(mc);
            }
        }
    }
}
