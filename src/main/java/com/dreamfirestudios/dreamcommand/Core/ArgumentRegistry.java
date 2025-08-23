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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.UUID;

public final class ArgumentRegistry {
    private static final List<ArgumentResolver<?>> resolvers = new ArrayList<>();
    static { registerDefaults(); }


    public static void register(ArgumentResolver<?> r){ resolvers.add(r); }


    public static ArgumentResolver<?> find(Class<?> type){
        for (var r : resolvers) if (r.supports(type)) return r;
        return null;
    }

    private static void registerDefaults(){
        register(new ArgumentResolver<String>(){
            public boolean supports(Class<?> t){ return t == String.class; }
            public String resolve(String raw, ResolutionContext ctx){ return raw; }
        });
        register(new ArgumentResolver<Integer>(){
            public boolean supports(Class<?> t){ return t == int.class || t == Integer.class; }
            public Integer resolve(String raw, ResolutionContext ctx){ return Integer.parseInt(raw); }
        });
        register(new ArgumentResolver<Double>(){
            public boolean supports(Class<?> t){ return t == double.class || t == Double.class; }
            public Double resolve(String raw, ResolutionContext ctx){ return Double.parseDouble(raw); }
        });
        register(new ArgumentResolver<Boolean>(){
            public boolean supports(Class<?> t){ return t == boolean.class || t == Boolean.class; }
            public Boolean resolve(String raw, ResolutionContext ctx){ return Boolean.parseBoolean(raw); }
        });
        register(new ArgumentResolver<UUID>(){
            public boolean supports(Class<?> t){ return t == UUID.class; }
            public UUID resolve(String raw, ResolutionContext ctx){ return UUID.fromString(raw); }
        });
        register(new ArgumentResolver<Player>(){
            public boolean supports(Class<?> t){ return t == Player.class; }
            public Player resolve(String raw, ResolutionContext ctx){
                try { return Bukkit.getPlayer(UUID.fromString(raw)); } catch (Exception ignore) { }
                return Bukkit.getPlayerExact(raw);
            }
            public List<String> suggest(String prefix, ResolutionContext ctx){
                var list = new ArrayList<String>();
                for (var p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().contains(prefix.toLowerCase())) list.add(p.getName());
                return list;
            }
        });
        register(new ArgumentResolver<OfflinePlayer>(){
            public boolean supports(Class<?> t){ return t == OfflinePlayer.class; }
            public OfflinePlayer resolve(String raw, ResolutionContext ctx){
                try { return Bukkit.getOfflinePlayer(UUID.fromString(raw)); } catch (Exception ignore) { }
                return Bukkit.getOfflinePlayer(raw);
            }
        });
    }
}