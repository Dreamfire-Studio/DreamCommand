/*
 * MIT License
 *
 * Copyright (c) 2025 Dreamfire Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dreamfirestudios.dreamcommand;

import com.dreamfirestudios.dreamcommand.Annotations.PCMethod;
import com.dreamfirestudios.dreamcommand.Core.CommandRouter;
import com.dreamfirestudios.dreamcommand.Core.TabDataProvider;
import com.dreamfirestudios.dreamcore.DreamJava.DreamfireJavaAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class DreamCommand extends JavaPlugin {
    public static DreamCommand INSTANCE;

    @Override public void onEnable(){ INSTANCE = this; RegisterRaw(this); }

    public static void RegisterRaw(JavaPlugin javaPlugin) {
        try { Register(javaPlugin); } catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Pulse auto-register:
     *  - If class extends BukkitCommand (incl. your PlayerCommand/ServerCommand): instantiate and register directly.
     *  - Else if class contains at least one @PCMethod: wrap with CommandRouter.
     *    Name/aliases/debug are resolved from optional static fields:
     *      public static String   COMMAND_NAME
     *      public static String[] COMMAND_ALIASES
     *      public static boolean  COMMAND_DEBUG
     *    If COMMAND_NAME is absent, fallback = simple class name (strip trailing "Commands"), lowercase.
     */
    public static void Register(JavaPlugin javaPlugin) throws Exception {
        for (var autoRegisterClass : DreamfireJavaAPI.getAutoRegisterClasses(javaPlugin)) {

            // Case 1: Legacy style — subclasses of BukkitCommand (your abstract PlayerCommand/ServerCommand)
            if (BukkitCommand.class.isAssignableFrom(autoRegisterClass)) {
                var bukkitCmd = (BukkitCommand) autoRegisterClass.getDeclaredConstructor().newInstance();
                CommandMap map = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
                map.register(javaPlugin.getName(), bukkitCmd);
                continue;
            }

            // Case 2: New router style — plain classes containing @PCMethod handlers
            boolean hasPCMethod = false;
            for (Method m : autoRegisterClass.getMethods()) {
                if (m.isAnnotationPresent(PCMethod.class)) { hasPCMethod = true; break; }
            }
            if (!hasPCMethod) continue;

            // Build router from target + static config (if provided)
            Object target = autoRegisterClass.getDeclaredConstructor().newInstance();

            // COMMAND_NAME (required for router; fallback to class name if not present)
            String name = null;
            try {
                Field f = autoRegisterClass.getField("COMMAND_NAME");
                Object v = f.get(null);
                if (v instanceof String s && !s.isBlank()) name = s.toLowerCase();
            } catch (NoSuchFieldException ignored) { /* fallback below */ }
            if (name == null) {
                String simple = autoRegisterClass.getSimpleName();
                if (simple.endsWith("Commands") && simple.length() > "Commands".length()) {
                    simple = simple.substring(0, simple.length() - "Commands".length());
                }
                name = simple.toLowerCase();
            }

            // COMMAND_ALIASES (optional)
            String[] aliases = new String[0];
            try {
                Field f = autoRegisterClass.getField("COMMAND_ALIASES");
                Object v = f.get(null);
                if (v instanceof String[] arr) aliases = arr;
            } catch (NoSuchFieldException ignored) { /* none */ }

            // COMMAND_DEBUG (optional)
            boolean debug = false;
            try {
                Field f = autoRegisterClass.getField("COMMAND_DEBUG");
                Object v = f.get(null);
                if (v instanceof Boolean b) debug = b;
            } catch (NoSuchFieldException ignored) { /* default false */ }

            // Register router (no TabDataProvider variant here; DreamCore handles the rest)
            register(javaPlugin, name, target, debug, aliases);
        }
    }

    // --- your original private helpers kept verbatim ---

    private static void register(JavaPlugin plugin, String name, Object target, boolean debug, String... aliases) {
        try {
            CommandMap map = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
            BukkitCommand cmd = new CommandRouter(plugin, name, target, debug, aliases);
            map.register(plugin.getName(), cmd);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void register(JavaPlugin plugin, String name, Object target, boolean debug, TabDataProvider provider, String... aliases) {
        try {
            CommandMap map = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
            BukkitCommand cmd = new CommandRouter(plugin, name, target, debug, provider, aliases);
            map.register(plugin.getName(), cmd);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static Field getField(Class<?> c, String n) throws Exception {
        var f = c.getDeclaredField(n); f.setAccessible(true); return f;
    }
}