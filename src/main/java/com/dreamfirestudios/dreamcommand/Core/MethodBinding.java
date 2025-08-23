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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

final class MethodBinding {
    final Object target;
    final Method method;
    final List<String> signature;
    final Class<?>[] paramTypes;
    final boolean requiresOp;
    final Set<String> requiredPerms;
    final Set<String> allowedWorlds;
    final boolean hideTab;
    final String functionHideName;
    final PCTab[] tabs;
    final PCAutoTab[] autoTabs;


    MethodBinding(Object target, Method method){
        this.target = target;
        this.method = method;
        this.paramTypes = method.getParameterTypes();
        var pcm = method.getAnnotation(PCMethod.class);
        this.signature = List.of(pcm.value());
        this.requiresOp = method.isAnnotationPresent(PCOP.class) || target.getClass().isAnnotationPresent(PCOP.class);
        var perms = new HashSet<String>();
        if (target.getClass().isAnnotationPresent(PCPerm.class)) perms.addAll(List.of(target.getClass().getAnnotation(PCPerm.class).value()));
        if (method.isAnnotationPresent(PCPerm.class)) perms.addAll(List.of(method.getAnnotation(PCPerm.class).value()));
        this.requiredPerms = perms;
        var worlds = new HashSet<String>();
        if (target.getClass().isAnnotationPresent(PCWorld.class)) worlds.addAll(List.of(target.getClass().getAnnotation(PCWorld.class).value()));
        if (method.isAnnotationPresent(PCWorld.class)) worlds.addAll(List.of(method.getAnnotation(PCWorld.class).value()));
        this.allowedWorlds = worlds;
        this.hideTab = method.isAnnotationPresent(PCHideTab.class);
        this.functionHideName = method.isAnnotationPresent(PCFunctionHideTab.class) ? method.getAnnotation(PCFunctionHideTab.class).value() : null;
        this.tabs = method.isAnnotationPresent(PCTabs.class) ? method.getAnnotation(PCTabs.class).value() : (method.isAnnotationPresent(PCTab.class) ? new PCTab[]{method.getAnnotation(PCTab.class)} : new PCTab[0]);
        this.autoTabs = method.isAnnotationPresent(PCAutoTabs.class) ? method.getAnnotation(PCAutoTabs.class).value() : (method.isAnnotationPresent(PCAutoTab.class) ? new PCAutoTab[]{method.getAnnotation(PCAutoTab.class)} : new PCAutoTab[0]);
    }


    boolean matchesSignature(String[] args){
        if (args.length < signature.size()) return false;
        for (int i=0;i<signature.size();i++) if (!signature.get(i).equalsIgnoreCase(args[i])) return false;
        return true;
    }


    boolean canUse(CommandSender sender){
        if (requiresOp && !(sender.isOp())) return false;
        if (!requiredPerms.isEmpty()) for (var p : requiredPerms) if (!sender.hasPermission(p)) return false;
        if (!allowedWorlds.isEmpty() && sender instanceof Player pl) return allowedWorlds.contains(pl.getWorld().getName());
        return true;
    }


    Object[] mapArguments(String[] args, ResolutionContext ctx) throws Exception {
        var provided = Arrays.asList(args).subList(signature.size(), args.length);
        if (paramTypes.length == 0) return new Object[0];
        var list = new ArrayList<>();
        Class<?> first = paramTypes[0];
        if (Player.class.isAssignableFrom(first)){
            if (!(ctx.sender instanceof Player)) throw new IllegalArgumentException(CommandError.MustBePlayer.message);
            list.add(ctx.sender);
        } else if (CommandSender.class.isAssignableFrom(first)){
            list.add(ctx.sender);
        } else throw new IllegalArgumentException(CommandError.FirstParamInvalid.message);


        boolean vararg = paramTypes[paramTypes.length-1].isArray();
        int fixed = paramTypes.length - (vararg ? 2 : 1);
        if (!vararg && provided.size() != paramTypes.length-1) throw new IllegalArgumentException(CommandError.WrongParameterCount.message);
        if (vararg && provided.size() < (1+fixed)) throw new IllegalArgumentException(CommandError.WrongParameterCount.message);


        for (int i=1;i<paramTypes.length;i++){
            Class<?> t = paramTypes[i];
            if (vararg && i==paramTypes.length-1){
                Class<?> comp = t.getComponentType();
                int from = signature.size()+fixed;
                var varItems = Arrays.asList(args).subList(from, args.length);
                Object arr = Array.newInstance(comp, varItems.size());
                for (int k=0;k<varItems.size();k++) Array.set(arr, k, resolveOne(comp, varItems.get(k), ctx));
                list.add(arr);
            } else {
                String raw = provided.get(i-1);
                list.add(resolveOne(t, raw, ctx));
            }
        }
        return list.toArray();
    }


    @SuppressWarnings({"unchecked","rawtypes"})
    private Object resolveOne(Class<?> t, String raw, ResolutionContext ctx) throws Exception {
        if (t.isEnum()){
            for (Object c : t.getEnumConstants()) if (c.toString().equalsIgnoreCase(raw)) return c;
            throw new IllegalArgumentException(CommandError.SerializationFailure.message);
        }
        var r = ArgumentRegistry.find(t);
        if (r==null) return raw;
        return ((ArgumentResolver) r).resolve(raw, ctx);
    }
}