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

import com.dreamfirestudios.dreamcommand.Annotations.*;
import com.dreamfirestudios.dreamcommand.Enums.CommandError;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

public final class CommandRouter extends BukkitCommand {
    private final Plugin plugin;
    private final Object target;
    private final boolean debug;
    private final Map<String, Method> dataMethods = new HashMap<>();
    private final List<MethodBinding> handlers = new ArrayList<>();
    private final TabDataProvider provider; // optional


    public CommandRouter(Plugin plugin, String name, Object target, boolean debug, String... aliases){
        this(plugin, name, target, debug, null, aliases);
    }


    public CommandRouter(Plugin plugin, String name, Object target, boolean debug, TabDataProvider provider, String... aliases){
        super(name.toLowerCase());
        this.plugin = plugin; this.target = target; this.debug = debug; this.provider = provider;
        setAliases(Arrays.asList(aliases));
        scan();
    }


    private void scan(){
        for (var m : target.getClass().getMethods()){
            if (m.isAnnotationPresent(PCMethodData.class)) dataMethods.put(m.getName(), m);
            if (m.isAnnotationPresent(PCMethod.class)) handlers.add(new MethodBinding(target, m));
        }
        handlers.sort(Comparator.comparingInt(h -> -h.signature.size()));
    }


    @Override
    public boolean execute(CommandSender sender, String label, String[] args){
        var ctx = new ResolutionContext(plugin, sender);
        for (var h : handlers){
            if (!h.matchesSignature(args)) continue;
            if (!h.canUse(sender)) continue;
            try {
                Object[] invoke = h.mapArguments(args, ctx);
                h.method.invoke(h.target, invoke);
                return true;
            } catch (IllegalArgumentException ex){
                if (debug) sender.sendMessage(ex.getMessage());
            } catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }
        if (debug) sender.sendMessage(CommandError.NoMatchingMethod.message);
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        var out = new LinkedList<String>();
        int idx = Math.max(0, args.length-1);
        for (var h : handlers){
            if (!h.canUse(sender)) continue;
            if (h.hideTab) continue;
            if (!prefixMatches(h.signature, args)) continue;
            if (h.functionHideName != null){
                var fn = dataMethods.get(h.functionHideName);
                if (fn != null){
                    try {
                        Object res;
                        if (fn.getParameterCount()==1){
                            var pt = fn.getParameterTypes()[0];
                            Object arg = null;
                            if (pt == String.class) arg = args.length==0? "" : args[args.length-1];
                            else if (pt == java.util.UUID.class && sender instanceof Player p) arg = p.getUniqueId();
                            res = fn.invoke(target, arg);
                        } else {
                            res = fn.invoke(target);
                        }
                        if (res instanceof Boolean b && !b) continue;
                    } catch (Exception ignored) { }
                }
            }
            if (idx < h.signature.size()){
                out.add(h.signature.get(idx));
                continue;
            }
            var paramTypes = h.paramTypes;
            boolean vararg = paramTypes[paramTypes.length-1].isArray();
            int testLoc = args.length - h.signature.size();
            if (!vararg && testLoc >= paramTypes.length) continue;
            Class<?> type = testLoc >= paramTypes.length ? paramTypes[paramTypes.length-1] : paramTypes[testLoc];
            if (vararg && type.isArray()) type = type.getComponentType();


            String current = args.length==0? "" : args[args.length-1];
            for (var at : h.autoTabs){
                int pos = Math.min(testLoc, h.paramTypes.length-1);
                if (pos == at.pos()){
                    var r = ArgumentRegistry.find(type);
                    if (r != null) out.addAll(r.suggest(current, new ResolutionContext(plugin, sender)));
                }
            }
            for (var tab : h.tabs){
                int pos = Math.min(testLoc, h.paramTypes.length-1);
                if (pos != tab.pos()) continue;
                switch (tab.type()){
                    case PureData -> out.add(tab.data());
                    case OnlinePlayerNames -> Bukkit.getOnlinePlayers().forEach(p -> { if (p.getName().toLowerCase().contains(current.toLowerCase())) out.add(p.getName()); });
                    case OfflinePlayerNames -> Arrays.stream(Bukkit.getOfflinePlayers()).forEach(p -> { if (p.getName()!=null && p.getName().toLowerCase().contains(current.toLowerCase())) out.add(p.getName()); });
                    case PullServerData -> {
                        if (provider != null){
                            var list = provider.serverData(tab.data());
                            if (list != null) list.forEach(s -> { if (s.toLowerCase().contains(current.toLowerCase())) out.add(s); });
                        }
                    }
                    case PullPlayerData -> {
                        if (provider != null && sender instanceof Player pl){
                            var list = provider.playerData(pl.getUniqueId(), tab.data());
                            if (list != null) list.forEach(s -> { if (s.toLowerCase().contains(current.toLowerCase())) out.add(s); });
                        }
                    }
                    case InformationFromFunction -> {
                        var fn = dataMethods.get(tab.data());
                        if (fn != null){
                            try {
                                Object res;
                                if (fn.getParameterCount()==1){
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


    private boolean prefixMatches(List<String> sig, String[] args){
        int upto = Math.min(sig.size(), Math.max(0, args.length-1));
        for (int i=0;i<upto;i++) if (!sig.get(i).equalsIgnoreCase(args[i])) return false;
        return true;
    }
}
