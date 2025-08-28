// ... header/comments unchanged ...
package com.dreamfirestudios.dreamcommand.Core;

import com.dreamfirestudios.dreamcommand.Annotations.*;
import com.dreamfirestudios.dreamcommand.Enums.CommandError;
import com.dreamfirestudios.dreamcommand.Enums.RateLimitScope;
import com.dreamfirestudios.dreamcore.DreamConcurrent.DreamKeyedRateLimiter;   // <-- add
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.Duration;                                                    // <-- add
import java.util.*;

// javadoc header unchanged
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

    // --------------------------- NEW: rate limit support ---------------------------
    /** <summary>True when @PCRateLimit is present.</summary> */
    final boolean hasRateLimit;

    /** <summary>Per-handler limiter; key type is derived at runtime.</summary> */
    final DreamKeyedRateLimiter<Object> rateLimiter;

    /** <summary>Scope/keying strategy.</summary> */
    final RateLimitScope rlScope;

    /** <summary>Optional custom denial message with placeholders.</summary> */
    final String rlMessage;

    /** <summary>Cached period millis for message placeholders.</summary> */
    final long rlPeriodMillis;
    // ------------------------------------------------------------------------------

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

        // --------------------------- NEW: read @PCRateLimit ---------------------------
        PCRateLimit rl = method.getAnnotation(PCRateLimit.class);
        if (rl != null) {
            this.hasRateLimit = true;
            Duration period = Duration.ofSeconds(Math.max(0, rl.perSeconds()))
                    .plusMillis(Math.max(0, rl.perMillis()));
            this.rateLimiter   = new DreamKeyedRateLimiter<>(rl.permits(), period);
            this.rlScope       = rl.scope();
            this.rlMessage     = rl.message();
            this.rlPeriodMillis= period.toMillis();
        } else {
            this.hasRateLimit = false;
            this.rateLimiter  = null;
            this.rlScope      = RateLimitScope.PER_SENDER;
            this.rlMessage    = "";
            this.rlPeriodMillis = 0L;
        }
        // ------------------------------------------------------------------------------
    }

    boolean matchesSignature(String[] args) {
        if (args.length < signature.size()) return false;
        for (int i = 0; i < signature.size(); i++) {
            if (!signature.get(i).equalsIgnoreCase(args[i])) return false;
        }
        return true;
    }

    boolean canUse(CommandSender sender) {
        if (requiresOp && !(sender.isOp())) return false;

        if (!requiredPerms.isEmpty()) {
            for (var p : requiredPerms) if (!sender.hasPermission(p)) return false;
        }

        if (!allowedWorlds.isEmpty() && sender instanceof Player pl) {
            return allowedWorlds.contains(pl.getWorld().getName());
        }
        return true;
    }

    // --------------------------- NEW: rate limit gate ---------------------------
    /**
     * <summary>Checks and consumes one permit if a rate limit is configured.</summary>
     * <returns>true if allowed to proceed; false if limited (a message is sent).</returns>
     */
    boolean checkRateLimitAndMaybeWarn(CommandSender sender) {
        if (!hasRateLimit) return true;

        Object key = switch (rlScope) {
            case PER_SENDER -> (sender instanceof Player p) ? p.getUniqueId() : ("CONSOLE:" + sender.getName());
            case PER_WORLD  -> (sender instanceof Player p) ? p.getWorld().getUID() : "CONSOLE_WORLD";
            case GLOBAL     -> Boolean.TRUE; // single bucket per handler
        };

        if (rateLimiter.tryAcquire(key)) return true;

        var wait = rateLimiter.estimateWait(key, 1);
        String msg = rlMessage == null || rlMessage.isBlank()
                ? "You are doing that too fast. Try again in ~" + Math.max(1, (int)Math.ceil(wait.toMillis()/1000.0)) + "s."
                : rlMessage
                .replace("{wait_ms}", String.valueOf(wait.toMillis()))
                .replace("{wait_s}",  String.valueOf(Math.max(0, (int)Math.ceil(wait.toMillis() / 1000.0))))
                .replace("{permits}", String.valueOf(1))
                .replace("{period_ms}", String.valueOf(rlPeriodMillis));
        sender.sendMessage(msg);
        return false;
    }
    // ---------------------------------------------------------------------------

    Object[] mapArguments(String[] args, ResolutionContext ctx) throws Exception {
        var provided = Arrays.asList(args).subList(signature.size(), args.length);
        if (paramTypes.length == 0) return new Object[0];

        var list = new ArrayList<>();
        Class<?> first = paramTypes[0];

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object resolveOne(Class<?> t, String raw, ResolutionContext ctx) throws Exception {
        if (t.isEnum()) {
            for (Object c : t.getEnumConstants()) if (c.toString().equalsIgnoreCase(raw)) return c;
            throw new IllegalArgumentException(CommandError.SerializationFailure.message);
        }
        var r = ArgumentRegistry.find(t);
        if (r == null) return raw;
        return ((ArgumentResolver) r).resolve(raw, ctx);
    }
}