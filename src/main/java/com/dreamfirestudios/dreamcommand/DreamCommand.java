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

/**
 * /// <summary>
 * Bukkit plugin entry point for DreamCommand. Automatically discovers and registers
 * commands at startup using either legacy {@link BukkitCommand} subclasses or
 * annotation-driven handlers wrapped by {@link CommandRouter}.
 * /// </summary>
 * /// <remarks>
 * <ul>
 *   <li><b>Legacy mode:</b> Any class that extends {@link BukkitCommand} is instantiated and registered directly.</li>
 *   <li><b>Router mode:</b> Any plain class that declares at least one {@code @PCMethod} is wrapped by a {@link CommandRouter}.</li>
 *   <li>Static fields on the handler class may configure the router:
 *       <ul>
 *         <li>{@code public static String   COMMAND_NAME}</li>
 *         <li>{@code public static String[] COMMAND_ALIASES}</li>
 *         <li>{@code public static boolean  COMMAND_DEBUG}</li>
 *       </ul>
 *       If {@code COMMAND_NAME} is absent, the simple class name (minus a trailing {@code "Commands"}) is used, lowercased.</li>
 * </ul>
 * Classes to register are obtained from {@link DreamfireJavaAPI#getAutoRegisterClasses(JavaPlugin)}.
 * /// </remarks>
 * /// <example>
 * <code>
 * // Example handler (router mode):
 * public final class HomeCommands {
 *     public static final String COMMAND_NAME = "home";
 *     public static final String[] COMMAND_ALIASES = {"homes"};
 *     public static final boolean COMMAND_DEBUG = true;
 *
 *     &#64;PCMethod({"set"})
 *     public void set(org.bukkit.entity.Player player) { ... }
 *
 *     &#64;PCMethod({"tp"})
 *     public void tp(org.bukkit.entity.Player player, String name) { ... }
 * }
 * </code>
 * /// </example>
 */
public final class DreamCommand extends JavaPlugin {

    /** /// <summary>Singleton reference to the loaded plugin instance.</summary> */
    public static DreamCommand INSTANCE;

    /**
     * /// <summary>
     * Standard Bukkit enable hook. Stores {@link #INSTANCE} and triggers auto-registration.
     * /// </summary>
     */
    @Override
    public void onEnable() {
        INSTANCE = this;
        RegisterRaw(this);
    }

    /**
     * /// <summary>
     * Convenience wrapper that calls {@link #Register(JavaPlugin)} and wraps checked exceptions in {@link RuntimeException}.
     * /// </summary>
     * /// <param name="javaPlugin">The plugin providing classes to auto-register.</param>
     */
    public static void RegisterRaw(JavaPlugin javaPlugin) {
        try {
            Register(javaPlugin);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * /// <summary>
     * Discovers and registers command classes from {@link DreamfireJavaAPI#getAutoRegisterClasses(JavaPlugin)}.
     * </summary>
     * /// <param name="javaPlugin">The plugin providing classes to auto-register.</param>
     * /// <remarks>
     * <b>Registration rules:</b>
     * <ol>
     *   <li>If a class extends {@link BukkitCommand}, it is instantiated via its no-arg constructor and registered directly.</li>
     *   <li>If a class declares at least one method annotated with {@link PCMethod}, it is treated as a handler:
     *       <ul>
     *         <li>{@code COMMAND_NAME}: optional static field for the command name (lowercased).</li>
     *         <li>{@code COMMAND_ALIASES}: optional static field supplying aliases.</li>
     *         <li>{@code COMMAND_DEBUG}: optional static field to enable debug feedback.</li>
     *       </ul>
     *       The object is wrapped in a {@link CommandRouter} and registered.</li>
     * </ol>
     * /// </remarks>
     * /// <example>
     * <code>
     * // Called during onEnable():
     * DreamCommand.Register(this);
     * </code>
     * /// </example>
     * /// <exception cref="Exception">If reflective construction or registration fails.</exception>
     */
    public static void Register(JavaPlugin javaPlugin) throws Exception {
        for (var autoRegisterClass : DreamfireJavaAPI.getAutoRegisterClasses(javaPlugin)) {

            // Case 1: Legacy style — subclasses of BukkitCommand
            if (BukkitCommand.class.isAssignableFrom(autoRegisterClass)) {
                var bukkitCmd = (BukkitCommand) autoRegisterClass.getDeclaredConstructor().newInstance();
                CommandMap map = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
                map.register(javaPlugin.getName(), bukkitCmd);
                continue;
            }

            // Case 2: Router style — plain classes containing @PCMethod handlers
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

    // ------------------------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------------------------

    /**
     * /// <summary>
     * Registers a {@link CommandRouter}-backed command under the given name and aliases.
     * /// </summary>
     * /// <param name="plugin">Owning plugin.</param>
     * /// <param name="name">Primary command name (lowercased).</param>
     * /// <param name="target">Handler instance with {@code @PCMethod} methods.</param>
     * /// <param name="debug">Whether to enable debug messages for this command.</param>
     * /// <param name="aliases">Optional aliases for the command.</param>
     * /// <remarks>
     * Uses Bukkit's internal {@code CommandMap} obtained reflectively.
     * /// </remarks>
     */
    private static void register(JavaPlugin plugin, String name, Object target, boolean debug, String... aliases) {
        try {
            CommandMap map = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
            BukkitCommand cmd = new CommandRouter(plugin, name, target, debug, aliases);
            map.register(plugin.getName(), cmd);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * /// <summary>
     * Registers a {@link CommandRouter}-backed command with a supplied {@link TabDataProvider}.
     * /// </summary>
     * /// <param name="plugin">Owning plugin.</param>
     * /// <param name="name">Primary command name (lowercased).</param>
     * /// <param name="target">Handler instance with {@code @PCMethod} methods.</param>
     * /// <param name="debug">Whether to enable debug messages for this command.</param>
     * /// <param name="provider">Optional tab data provider for dynamic completions.</param>
     * /// <param name="aliases">Optional aliases for the command.</param>
     * /// <remarks>
     * This overload is available for callers that manage their own {@link TabDataProvider}.
     * /// </remarks>
     */
    private static void register(JavaPlugin plugin, String name, Object target, boolean debug, TabDataProvider provider, String... aliases) {
        try {
            CommandMap map = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
            BukkitCommand cmd = new CommandRouter(plugin, name, target, debug, provider, aliases);
            map.register(plugin.getName(), cmd);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * /// <summary>
     * Reflectively obtains a declared field and marks it accessible.
     * /// </summary>
     * /// <param name="c">Class declaring the field.</param>
     * /// <param name="n">Field name.</param>
     * /// <returns>The accessible {@link Field} instance.</returns>
     * /// <exception cref="Exception">If the field is not present or cannot be made accessible.</exception>
     */
    private static Field getField(Class<?> c, String n) throws Exception {
        var f = c.getDeclaredField(n);
        f.setAccessible(true);
        return f;
    }
}