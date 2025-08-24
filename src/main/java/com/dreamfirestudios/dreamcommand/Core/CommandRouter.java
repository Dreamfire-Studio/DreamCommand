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
package com.dreamfirestudios.dreamcommand.Core;

import com.dreamfirestudios.dreamcommand.Annotations.PCMethod;
import com.dreamfirestudios.dreamcommand.Annotations.PCMethodData;
import com.dreamfirestudios.dreamcommand.Enums.CommandError;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

/**
 * /// <summary>
 * Routes Bukkit command executions and tab-completion to annotated handler methods
 * on a target object, converting arguments via {@link ArgumentRegistry} and enforcing
 * any constraints defined by your annotation model.
 * /// </summary>
 * /// <remarks>
 * <ul>
 *   <li>Scans {@code target} for methods annotated with {@link PCMethod} and {@link PCMethodData}.</li>
 *   <li>Matches the best handler by fixed signature prefix and parameter count.</li>
 *   <li>Maps raw arguments using registered {@link ArgumentResolver}s.</li>
 *   <li>Supports optional external {@link TabDataProvider} for dynamic completion sources.</li>
 * </ul>
 * </remarks>
 * /// <example>
 * <code>
 * // Registration (simplified):
 * CommandRouter router = new CommandRouter(plugin, "mycmd", new MyCommands(), true, "mc", "my");
 * Bukkit.getCommandMap().register(plugin.getName(), router);
 * </code>
 * /// </example>
 */
public final class CommandRouter extends BukkitCommand {

    /** /// <summary>Owning plugin instance.</summary> */
    private final Plugin plugin;

    /** /// <summary>Target instance that contains annotated command methods.</summary> */
    private final Object target;

    /** /// <summary>When true, routes show debug messages (e.g., why a method didn't match).</summary> */
    private final boolean debug;

    /**
     * /// <summary>
     * Map of auxiliary data methods by name (methods annotated with {@link PCMethodData}).
     * Used to provide dynamic data for tab completion and visibility checks.
     * /// </summary>
     */
    private final Map<String, Method> dataMethods = new HashMap<>();

    /**
     * /// <summary>
     * Resolved handler bindings discovered on the target (methods annotated with {@link PCMethod}).
     * Sorted by descending fixed signature size so the most specific handlers are tried first.
     * /// </summary>
     */
    private final List<MethodBinding> handlers = new ArrayList<>();

    /** /// <summary>Optional data provider for server- or player-scoped completion sources.</summary> */
    private final TabDataProvider provider; // optional

    /**
     * /// <summary>
     * Creates a router for the given command name and aliases.
     * /// </summary>
     * /// <param name="plugin">Owning plugin.</param>
     * /// <param name="name">Primary command name (registered in Bukkit).</param>
     * /// <param name="target">Object hosting annotated command methods.</param>
     * /// <param name="debug">Enable debug feedback to the sender.</param>
     * /// <param name="aliases">Optional additional aliases for the command.</param>
     */
    public CommandRouter(Plugin plugin, String name, Object target, boolean debug, String... aliases) {
        this(plugin, name, target, debug, null, aliases);
    }

    /**
     * /// <summary>
     * Creates a router for the given command with an optional tab data provider.
     * /// </summary>
     * /// <param name="plugin">Owning plugin.</param>
     * /// <param name="name">Primary command name (registered in Bukkit).</param>
     * /// <param name="target">Object hosting annotated command methods.</param>
     * /// <param name="debug">Enable debug feedback to the sender.</param>
     * /// <param name="provider">Optional external tab data provider.</param>
     * /// <param name="aliases">Optional additional aliases for the command.</param>
     */
    public CommandRouter(Plugin plugin, String name, Object target, boolean debug, TabDataProvider provider, String... aliases) {
        super(name.toLowerCase());
        this.plugin = plugin;
        this.target = target;
        this.debug = debug;
        this.provider = provider;
        setAliases(Arrays.asList(aliases));
        scan();
    }

    /**
     * /// <summary>
     * Reflectively scans the target for annotated methods and prepares handler metadata.
     * /// </summary>
     * /// <remarks>
     * Populates {@link #dataMethods} (for {@link PCMethodData}) and {@link #handlers} (for {@link PCMethod}).
     * Handlers are then sorted so that more specific signatures are evaluated first.
     * /// </remarks>
     */
    private void scan() {
        for (var m : target.getClass().getMethods()) {
            if (m.isAnnotationPresent(PCMethodData.class)) dataMethods.put(m.getName(), m);
            if (m.isAnnotationPresent(PCMethod.class))     handlers.add(new MethodBinding(target, m));
        }
        // Most specific first (longest fixed signature)
        handlers.sort(Comparator.comparingInt(h -> -h.signature.size()));
    }

    /**
     * /// <summary>
     * Executes the command by selecting a matching handler and invoking it with typed arguments.
     * /// </summary>
     * /// <param name="sender">The command sender.</param>
     * /// <param name="label">The label used (primary name or alias).</param>
     * /// <param name="args">Raw string arguments.</param>
     * /// <returns><c>true</c> if a handler executed; otherwise <c>false</c>.</returns>
     * /// <remarks>
     * If {@link #debug} is enabled, additional error information is sent to the sender.
     * Exceptions from the invoked method are wrapped in {@link RuntimeException}.
     * /// </remarks>
     */
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        var ctx = new ResolutionContext(plugin, sender);

        for (var h : handlers) {
            if (!h.matchesSignature(args)) continue;
            if (!h.canUse(sender))         continue;

            try {
                Object[] invoke = h.mapArguments(args, ctx);
                h.method.invoke(h.target, invoke);
                return true;
            } catch (IllegalArgumentException ex) {
                if (debug) sender.sendMessage(ex.getMessage());
            } catch (Exception ex) {
                // Preserve current behavior: escalate unexpected errors.
                throw new RuntimeException(ex);
            }
        }

        if (debug) sender.sendMessage(CommandError.NoMatchingMethod.message);
        return false;
    }

    /**
     * /// <summary>
     * Produces tab-completion options based on handler signatures, visibility rules, and
     * registered {@link ArgumentResolver} suggestions.
     * /// </summary>
     * /// <param name="sender">The command sender.</param>
     * /// <param name="alias">The label used (primary name or alias).</param>
     * /// <param name="args">Raw arguments including the one being completed.</param>
     * /// <returns>A list of completion candidates (may be empty).</returns>
     * /// <remarks>
     * <ul>
     *   <li>Skips handlers that the sender cannot use.</li>
     *   <li>Supports fixed-prefix signature matching for sub-commands.</li>
     *   <li>Handles dynamic hiding via function hooks registered with {@link PCMethodData}.</li>
     *   <li>Pulls data from {@link TabDataProvider} when requested by tab annotations.</li>
     * </ul>
     * /// </remarks>
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        var out = new LinkedList<String>();
        int idx = Math.max(0, args.length - 1);

        for (var h : handlers) {
            if (!h.canUse(sender)) continue;
            if (h.hideTab)        continue;
            if (!prefixMatches(h.signature, args)) continue;

            // Conditional hiding via function name (PCFunctionHideTab / PCHideTab logic baked into MethodBinding)
            if (h.functionHideName != null) {
                var fn = dataMethods.get(h.functionHideName);
                if (fn != null) {
                    try {
                        Object res;
                        if (fn.getParameterCount() == 1) {
                            var pt = fn.getParameterTypes()[0];
                            Object arg = null;
                            if (pt == String.class) arg = args.length == 0 ? "" : args[args.length - 1];
                            else if (pt == java.util.UUID.class && sender instanceof Player p) arg = p.getUniqueId();
                            res = fn.invoke(target, arg);
                        } else {
                            res = fn.invoke(target);
                        }
                        if (res instanceof Boolean b && !b) continue;
                    } catch (Exception ignored) { }
                }
            }

            // If still within fixed signature tokens, just propose the next literal token.
            if (idx < h.signature.size()) {
                out.add(h.signature.get(idx));
                continue;
            }

            // Determine target parameter type for this position (supports varargs).
            var paramTypes = h.paramTypes;
            boolean vararg = paramTypes[paramTypes.length - 1].isArray();
            int testLoc = args.length - h.signature.size();
            if (!vararg && testLoc >= paramTypes.length) continue;

            Class<?> type = (testLoc >= paramTypes.length) ? paramTypes[paramTypes.length - 1] : paramTypes[testLoc];
            if (vararg && type.isArray()) type = type.getComponentType();

            String current = args.length == 0 ? "" : args[args.length - 1];

            // Auto tabs (resolver-provided)
            for (var at : h.autoTabs) {
                int pos = Math.min(testLoc, h.paramTypes.length - 1);
                if (pos == at.pos()) {
                    var r = ArgumentRegistry.find(type);
                    if (r != null) out.addAll(r.suggest(current, new ResolutionContext(plugin, sender)));
                }
            }

            // Explicit tabs (annotation-driven)
            for (var tab : h.tabs) {
                int pos = Math.min(testLoc, h.paramTypes.length - 1);
                if (pos != tab.pos()) continue;

                switch (tab.type()) {
                    case PureData -> out.add(tab.data());

                    case OnlinePlayerNames -> Bukkit.getOnlinePlayers()
                            .forEach(p -> { if (p.getName().toLowerCase().contains(current.toLowerCase())) out.add(p.getName()); });

                    case OfflinePlayerNames -> Arrays.stream(Bukkit.getOfflinePlayers())
                            .forEach(p -> { if (p.getName() != null && p.getName().toLowerCase().contains(current.toLowerCase())) out.add(p.getName()); });

                    case PullServerData -> {
                        if (provider != null) {
                            var list = provider.serverData(tab.data());
                            if (list != null) list.forEach(s -> { if (s.toLowerCase().contains(current.toLowerCase())) out.add(s); });
                        }
                    }

                    case PullPlayerData -> {
                        if (provider != null && sender instanceof Player pl) {
                            var list = provider.playerData(pl.getUniqueId(), tab.data());
                            if (list != null) list.forEach(s -> { if (s.toLowerCase().contains(current.toLowerCase())) out.add(s); });
                        }
                    }

                    case InformationFromFunction -> {
                        var fn = dataMethods.get(tab.data());
                        if (fn != null) {
                            try {
                                Object res;
                                if (fn.getParameterCount() == 1) {
                                    var pt = fn.getParameterTypes()[0];
                                    Object arg = null;
                                    if (pt == String.class) arg = current;
                                    else if (pt == java.util.UUID.class && sender instanceof Player p) arg = p.getUniqueId();
                                    res = fn.invoke(target, arg);
                                } else {
                                    res = fn.invoke(target);
                                }
                                if (res instanceof List<?> l) l.forEach(o -> out.add(String.valueOf(o)));
                            } catch (Exception ignored) { }
                        }
                    }
                }
            }
        }
        return out;
    }

    /**
     * /// <summary>
     * Checks whether the provided raw arguments match the fixed (literal) part of a handler's signature.
     * /// </summary>
     * /// <param name="sig">The handler's fixed signature tokens (e.g., {"sub", "create"}).</param>
     * /// <param name="args">The raw user-supplied arguments.</param>
     * /// <returns><c>true</c> if all fixed tokens up to the current position match (case-insensitive); otherwise <c>false</c>.</returns>
     */
    private boolean prefixMatches(List<String> sig, String[] args) {
        int upto = Math.min(sig.size(), Math.max(0, args.length - 1));
        for (int i = 0; i < upto; i++) if (!sig.get(i).equalsIgnoreCase(args[i])) return false;
        return true;
    }
}