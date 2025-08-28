/*
 * MIT License
 * Copyright (c) 2025 ...
 */
package com.dreamfirestudios.dreamcommand.Annotations;

import com.dreamfirestudios.dreamcommand.Enums.RateLimitScope;

import java.lang.annotation.*;

/**
 * <summary>
 * Declares a token-bucket rate limit for a command method.
 * </summary>
 *
 * <remarks>
 * Applied to a {@code @PCMethod}-annotated handler. Limits are enforced
 * by the command router <em>before</em> parameters are mapped and the
 * handler is invoked.
 *
 * <ul>
 *   <li>{@code permits} — bucket capacity and refill units per period</li>
 *   <li>{@code perSeconds}/{@code perMillis} — period length</li>
 *   <li>{@code scope} — key space for buckets (per-sender, per-world, global)</li>
 *   <li>{@code message} — optional denial message with placeholders:
 *       <code>{wait_ms}</code>, <code>{wait_s}</code>, <code>{permits}</code>, <code>{period_ms}</code></li>
 * </ul>
 *
 * Example: allow <b>3 uses per 10 seconds per player</b>
 * <pre>{@code
 * @PCMethod({"foo"})
 * @PCRateLimit(permits = 3, perSeconds = 10, scope = RateLimitScope.PER_SENDER,
 *              message = "Too fast! Try again in ~{wait_s}s.")
 * public void foo(Player player) { ... }
 * }</pre>
 * </remarks>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PCRateLimit {

    /** <summary>Bucket capacity and refill count per period.</summary> */
    int permits();

    /** <summary>Whole seconds component of the period.</summary> */
    long perSeconds();

    /** <summary>Extra milliseconds component of the period (added to seconds).</summary> */
    long perMillis() default 0L;

    /** <summary>Keying scope for buckets (default: per sender).</summary> */
    RateLimitScope scope() default RateLimitScope.PER_SENDER;

    /**
     * <summary>
     * Optional denial message. Supports placeholders:
     * {wait_ms}, {wait_s}, {permits}, {period_ms}.
     * Leave empty to use a sensible default.
     * </summary>
     */
    String message() default "";
}