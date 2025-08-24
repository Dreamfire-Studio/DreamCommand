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

import com.dreamfirestudios.dreamcommand.Annotations.PCAutoTab;
import com.dreamfirestudios.dreamcommand.Annotations.PCAutoTabs;
import com.dreamfirestudios.dreamcommand.Annotations.PCFunctionHideTab;
import com.dreamfirestudios.dreamcommand.Annotations.PCHideTab;
import com.dreamfirestudios.dreamcommand.Annotations.PCMethod;
import com.dreamfirestudios.dreamcommand.Annotations.PCOP;
import com.dreamfirestudios.dreamcommand.Annotations.PCPerm;
import com.dreamfirestudios.dreamcommand.Annotations.PCTab;
import com.dreamfirestudios.dreamcommand.Annotations.PCTabs;
import com.dreamfirestudios.dreamcommand.Annotations.PCWorld;
import com.dreamfirestudios.dreamcommand.Enums.CommandError;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

/**
 * /// <summary>
 * Internal binding that encapsulates one annotated command method on a target object,
 * including its fixed signature tokens, parameter types, permission/world constraints,
 * and tab-completion metadata.
 * /// </summary>
 * /// <remarks>
 * <ul>
 *   <li>Built from a {@link Method} annotated with {@link PCMethod} on a command target.</li>
 *   <li>Evaluated by {@code CommandRouter} to check signature match, permission/world access,
 *       and to map raw arguments into typed parameters.</li>
 *   <li>Tab-completion behavior is driven by {@link PCTab}/{@link PCAutoTab} and optional
 *       function-based visibility via {@link PCFunctionHideTab}.</li>
 * </ul>
 * This class is package-private and not intended for direct plugin use.
 * /// </remarks>
 * /// <example>
 * <code>
 * // Constructed reflectively by CommandRouter.scan():
 * MethodBinding mb = new MethodBinding(target, methodAnnotatedWithPCMethod);
 * if (mb.matchesSignature(args) && mb.canUse(sender)) {
 *     Object[] callArgs = mb.mapArguments(args, ctx);
 *     method.invoke(target, callArgs);
 * }
 * </code>
 * /// </example>
 */
final class MethodBinding {

    /** /// <summary>Owning target instance that declares the handler method.</summary> */
    final Object target;

    /** /// <summary>The reflected method annotated with {@link PCMethod}.</summary> */
    final Method method;

    /** /// <summary>Fixed (literal) tokens that must prefix the command (e.g., {@code ["sub","create"]}).</summary> */
    final List<String> signature;

    /** /// <summary>Runtime parameter types of the handler method.</summary> */
    final Class<?>[] paramTypes;

    /** /// <summary>Whether the handler requires an operator (OP) sender.</summary> */
    final boolean requiresOp;

    /** /// <summary>Permission nodes required to execute the handler.</summary> */
    final Set<String> requiredPerms;

    /** /// <summary>World names in which the handler is allowed (empty means unrestricted).</summary> */
    final Set<String> allowedWorlds;

    /** /// <summary>If true, tab completions for this handler are hidden.</summary> */
    final boolean hideTab;

    /**
     * /// <summary>
     * Optional data method name used to dynamically decide whether to hide completions
     * for this handler (see {@link PCFunctionHideTab}).
     * /// </summary>
     */
    final String functionHideName;

    /** /// <summary>Explicit tab entries declared via {@link PCTab} / {@link PCTabs}.</summary> */
    final PCTab[] tabs;

    /** /// <summary>Resolver-driven tab entries declared via {@link PCAutoTab} / {@link PCAutoTabs}.</summary> */
    final PCAutoTab[] autoTabs;

    /**
     * /// <summary>
     * Creates a binding from a target object and a {@link PCMethod}-annotated method.
     * /// </summary>
     * /// <param name="target">The instance hosting the command method.</param>
     * /// <param name="method">The reflected method annotated with {@link PCMethod}.</param>
     */
    MethodBinding(Object target, Method method) {
        this.target = target;
        this.method = method;
        this.paramTypes = method.getParameterTypes();

        var pcm = method.getAnnotation(PCMethod.class);
        this.signature = List.of(pcm.value());

        this.requiresOp =
                method.isAnnotationPresent(PCOP.class) ||
                        target.getClass().isAnnotationPresent(PCOP.class);

        var perms = new HashSet<String>();
        if (target.getClass().isAnnotationPresent(PCPerm.class))
            perms.addAll(List.of(target.getClass().getAnnotation(PCPerm.class).value()));
        if (method.isAnnotationPresent(PCPerm.class))
            perms.addAll(List.of(method.getAnnotation(PCPerm.class).value()));
        this.requiredPerms = perms;

        var worlds = new HashSet<String>();
        if (target.getClass().isAnnotationPresent(PCWorld.class))
            worlds.addAll(List.of(target.getClass().getAnnotation(PCWorld.class).value()));
        if (method.isAnnotationPresent(PCWorld.class))
            worlds.addAll(List.of(method.getAnnotation(PCWorld.class).value()));
        this.allowedWorlds = worlds;

        this.hideTab = method.isAnnotationPresent(PCHideTab.class);
        this.functionHideName = method.isAnnotationPresent(PCFunctionHideTab.class)
                ? method.getAnnotation(PCFunctionHideTab.class).value()
                : null;

        this.tabs = method.isAnnotationPresent(PCTabs.class)
                ? method.getAnnotation(PCTabs.class).value()
                : (method.isAnnotationPresent(PCTab.class)
                ? new PCTab[]{method.getAnnotation(PCTab.class)}
                : new PCTab[0]);

        this.autoTabs = method.isAnnotationPresent(PCAutoTabs.class)
                ? method.getAnnotation(PCAutoTabs.class).value()
                : (method.isAnnotationPresent(PCAutoTab.class)
                ? new PCAutoTab[]{method.getAnnotation(PCAutoTab.class)}
                : new PCAutoTab[0]);
    }

    /**
     * /// <summary>
     * Checks whether the provided raw arguments start with this handler's fixed signature tokens.
     * /// </summary>
     * /// <param name="args">Raw arguments passed to the command (excluding the base label).</param>
     * /// <returns><c>true</c> when {@code args} begins with all literal tokens in {@link #signature}; otherwise <c>false</c>.</returns>
     */
    boolean matchesSignature(String[] args) {
        if (args.length < signature.size()) return false;
        for (int i = 0; i < signature.size(); i++) {
            if (!signature.get(i).equalsIgnoreCase(args[i])) return false;
        }
        return true;
    }

    /**
     * /// <summary>
     * Validates whether the specified sender is permitted to execute this handler
     * based on OP requirement, permissions, and world restrictions.
     * /// </summary>
     * /// <param name="sender">The command sender.</param>
     * /// <returns><c>true</c> if the sender can execute; otherwise <c>false</c>.</returns>
     * /// <remarks>
     * World restriction applies only when the sender is a {@link Player}.
     * </remarks>
     */
    boolean canUse(CommandSender sender) {
        if (requiresOp && !(sender.isOp())) return false;

        if (!requiredPerms.isEmpty()) {
            for (var p : requiredPerms) {
                if (!sender.hasPermission(p)) return false;
            }
        }

        if (!allowedWorlds.isEmpty() && sender instanceof Player pl) {
            return allowedWorlds.contains(pl.getWorld().getName());
        }
        return true;
    }

    /**
     * /// <summary>
     * Maps raw string arguments into an object array suitable for invoking the bound method,
     * enforcing the first-parameter rule (must be {@link Player} or {@link CommandSender}),
     * handling fixed signature tokens, and supporting varargs.
     * /// </summary>
     * /// <param name="args">Raw arguments passed to the command (including fixed tokens).</param>
     * /// <param name="ctx">Resolution context containing plugin and sender.</param>
     * /// <returns>An array of typed arguments aligned with {@link #paramTypes}.</returns>
     * /// <remarks>
     * <ul>
     *   <li>First parameter must be {@link Player} or {@link CommandSender} (enforced).</li>
     *   <li>When the last parameter is an array, remaining trailing args are resolved into that array (varargs).</li>
     *   <li>Non-enum parameters are resolved via {@link ArgumentRegistry}; enums are matched case-insensitively by name.</li>
     *   <li>Throws {@link IllegalArgumentException} with {@link CommandError} messages on shape mismatches.</li>
     * </ul>
     * </remarks>
     * /// <exception cref="Exception">Propagated from underlying resolvers or reflection if resolution fails.</exception>
     */
    Object[] mapArguments(String[] args, ResolutionContext ctx) throws Exception {
        var provided = Arrays.asList(args).subList(signature.size(), args.length);
        if (paramTypes.length == 0) return new Object[0];

        var list = new ArrayList<>();
        Class<?> first = paramTypes[0];

        // First parameter must be Player or CommandSender; enforce as-is.
        if (Player.class.isAssignableFrom(first)) {
            if (!(ctx.sender instanceof Player))
                throw new IllegalArgumentException(CommandError.MustBePlayer.message);
            list.add(ctx.sender);
        } else if (CommandSender.class.isAssignableFrom(first)) {
            list.add(ctx.sender);
        } else {
            throw new IllegalArgumentException(CommandError.FirstParamInvalid.message);
        }

        boolean vararg = paramTypes[paramTypes.length - 1].isArray();
        int fixed = paramTypes.length - (vararg ? 2 : 1);

        if (!vararg && provided.size() != paramTypes.length - 1)
            throw new IllegalArgumentException(CommandError.WrongParameterCount.message);

        if (vararg && provided.size() < (1 + fixed))
            throw new IllegalArgumentException(CommandError.WrongParameterCount.message);

        for (int i = 1; i < paramTypes.length; i++) {
            Class<?> t = paramTypes[i];

            if (vararg && i == paramTypes.length - 1) {
                // Build array for varargs tail.
                Class<?> comp = t.getComponentType();
                int from = signature.size() + fixed;
                var varItems = Arrays.asList(args).subList(from, args.length);
                Object arr = Array.newInstance(comp, varItems.size());
                for (int k = 0; k < varItems.size(); k++) {
                    Array.set(arr, k, resolveOne(comp, varItems.get(k), ctx));
                }
                list.add(arr);
            } else {
                String raw = provided.get(i - 1);
                list.add(resolveOne(t, raw, ctx));
            }
        }
        return list.toArray();
    }

    /**
     * /// <summary>
     * Resolves a single argument from raw text into the requested target type.
     * /// </summary>
     * /// <param name="t">The target parameter type.</param>
     * /// <param name="raw">Raw input text for the argument.</param>
     * /// <param name="ctx">Resolution context.</param>
     * /// <returns>The converted value, or the raw string if no resolver exists and type is not an enum.</returns>
     * /// <remarks>
     * <ul>
     *   <li>Enums are matched case-insensitively by constant name; failure results in {@link IllegalArgumentException} with {@link CommandError#SerializationFailure}.</li>
     *   <li>Non-enum types use {@link ArgumentRegistry#find(Class)} and delegate to the discovered {@link ArgumentResolver}.</li>
     *   <li>If no resolver is found, the raw string is returned as-is (existing behavior).</li>
     * </ul>
     * </remarks>
     * /// <exception cref="Exception">Propagated from resolver implementations on parse failure.</exception>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object resolveOne(Class<?> t, String raw, ResolutionContext ctx) throws Exception {
        if (t.isEnum()) {
            for (Object c : t.getEnumConstants()) {
                if (c.toString().equalsIgnoreCase(raw)) return c;
            }
            throw new IllegalArgumentException(CommandError.SerializationFailure.message);
        }
        var r = ArgumentRegistry.find(t);
        if (r == null) return raw;
        return ((ArgumentResolver) r).resolve(raw, ctx);
    }
}