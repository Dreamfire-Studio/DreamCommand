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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /// <summary>
 * A central registry for {@code ArgumentResolver} implementations used by the DreamCommand framework
 * to convert raw command argument strings into typed Java objects (e.g., {@code int}, {@code UUID}, {@code Player}).
 * /// </summary>
 * /// <remarks>
 * <ul>
 *   <li>This registry is global and stores resolvers in registration order.</li>
 *   <li>Use {@link #register(ArgumentResolver)} to add custom resolvers for your own types.</li>
 *   <li>Use {@link #find(Class)} to obtain the first resolver that supports the requested type.</li>
 *   <li>Defaults for {@code String}, {@code int}/{@code Integer}, {@code double}/{@code Double},
 *       {@code boolean}/{@code Boolean}, {@code UUID}, {@code Player}, and {@code OfflinePlayer} are registered at class load.</li>
 * </ul>
 * </remarks>
 * /// <example>
 * <code>
 * // Define a custom resolver
 * public final class LocationResolver implements ArgumentResolver&lt;Location&gt; {
 *     &#64;Override public boolean supports(Class&lt;?&gt; t) { return Location.class.equals(t); }
 *     &#64;Override public Location resolve(String raw, ResolutionContext ctx) {
 *         // parse "world,x,y,z" -> Location
 *     }
 * }
 *
 * // Register once on plugin enable:
 * ArgumentRegistry.register(new LocationResolver());
 *
 * // Retrieve and use (framework-internal):
 * ArgumentResolver&lt;?&gt; r = ArgumentRegistry.find(Location.class);
 * </code>
 * /// </example>
 */
public final class ArgumentRegistry {

    /**
     * /// <summary>
     * Ordered list of resolvers. The first resolver that {@link ArgumentResolver#supports(Class)} a type wins.
     * /// </summary>
     */
    private static final List<ArgumentResolver<?>> resolvers = new ArrayList<>();

    /**
     * /// <summary>
     * Static initializer that registers the built-in default resolvers.
     * /// </summary>
     */
    static {
        registerDefaults();
    }

    /**
     * /// <summary>
     * Prevents instantiation. This is a pure static utility holder.
     * /// </summary>
     */
    private ArgumentRegistry() { }

    /**
     * /// <summary>
     * Registers a new {@link ArgumentResolver} instance.
     * /// </summary>
     * /// <remarks>
     * Resolvers are evaluated in the order they are registered. Register more specific resolvers earlier if needed.
     * /// </remarks>
     * /// <param name="r">The resolver to add to the registry.</param>
     */
    public static void register(ArgumentResolver<?> r) {
        resolvers.add(r);
    }

    /**
     * /// <summary>
     * Finds the first resolver capable of handling the requested target type.
     * /// </summary>
     * /// <param name="type">The Java class representing the desired target type.</param>
     * /// <returns>
     * The first matching {@link ArgumentResolver} or {@code null} if none supports the given type.
     * /// </returns>
     * /// <example>
     * <code>
     * ArgumentResolver&lt;?&gt; r = ArgumentRegistry.find(Integer.class);
     * if (r != null) {
     *     Object value = r.resolve("42", someContext);
     * }
     * </code>
     * /// </example>
     */
    public static ArgumentResolver<?> find(Class<?> type) {
        for (var r : resolvers) {
            if (r.supports(type)) return r;
        }
        return null;
    }

    /**
     * /// <summary>
     * Registers the built-in default resolvers for common types.
     * /// </summary>
     * /// <remarks>
     * Includes: {@code String}, {@code int}/{@code Integer}, {@code double}/{@code Double},
     * {@code boolean}/{@code Boolean}, {@code UUID}, {@code Player}, {@code OfflinePlayer}.
     * /// </remarks>
     * /// <example>
     * <code>
     * // Automatically invoked on class load; not intended to be called manually.
     * </code>
     * /// </example>
     */
    private static void registerDefaults() {
        // String
        register(new ArgumentResolver<String>() {
            /// <summary>Returns true if the target type is {@code String}.</summary>
            @Override public boolean supports(Class<?> t) { return t == String.class; }
            /// <summary>Returns the raw argument unchanged.</summary>
            /// <param name="raw">The raw string argument.</param>
            /// <param name="ctx">The resolution context.</param>
            /// <returns>The same string provided.</returns>
            @Override public String resolve(String raw, ResolutionContext ctx) { return raw; }
        });

        // Integer / int
        register(new ArgumentResolver<Integer>() {
            /// <summary>Supports {@code int} and {@code Integer}.</summary>
            @Override public boolean supports(Class<?> t) { return t == int.class || t == Integer.class; }
            /// <summary>Parses the raw value via {@link Integer#parseInt(String)}.</summary>
            /// <param name="raw">Raw numeric text.</param>
            /// <param name="ctx">Resolution context.</param>
            /// <returns>Parsed integer value.</returns>
            /// <remarks>Throws {@link NumberFormatException} if the text is not a valid integer.</remarks>
            @Override public Integer resolve(String raw, ResolutionContext ctx) { return Integer.parseInt(raw); }
        });

        // Double / double
        register(new ArgumentResolver<Double>() {
            /// <summary>Supports {@code double} and {@code Double}.</summary>
            @Override public boolean supports(Class<?> t) { return t == double.class || t == Double.class; }
            /// <summary>Parses the raw value via {@link Double#parseDouble(String)}.</summary>
            /// <param name="raw">Raw numeric text.</param>
            /// <param name="ctx">Resolution context.</param>
            /// <returns>Parsed double value.</returns>
            /// <remarks>Throws {@link NumberFormatException} if the text is not a valid double.</remarks>
            @Override public Double resolve(String raw, ResolutionContext ctx) { return Double.parseDouble(raw); }
        });

        // Boolean / boolean
        register(new ArgumentResolver<Boolean>() {
            /// <summary>Supports {@code boolean} and {@code Boolean}.</summary>
            @Override public boolean supports(Class<?> t) { return t == boolean.class || t == Boolean.class; }
            /// <summary>Parses the raw value via {@link Boolean#parseBoolean(String)}.</summary>
            /// <param name="raw">Raw boolean text (case-insensitive).</param>
            /// <param name="ctx">Resolution context.</param>
            /// <returns>{@code true} if the string equals (ignoring case) "true"; otherwise {@code false}.</returns>
            @Override public Boolean resolve(String raw, ResolutionContext ctx) { return Boolean.parseBoolean(raw); }
        });

        // UUID
        register(new ArgumentResolver<UUID>() {
            /// <summary>Supports {@code UUID}.</summary>
            @Override public boolean supports(Class<?> t) { return t == UUID.class; }
            /// <summary>Parses a UUID via {@link UUID#fromString(String)}.</summary>
            /// <param name="raw">Raw UUID text.</param>
            /// <param name="ctx">Resolution context.</param>
            /// <returns>The parsed {@link UUID}.</returns>
            /// <remarks>Throws {@link IllegalArgumentException} if the text is not a valid UUID.</remarks>
            @Override public UUID resolve(String raw, ResolutionContext ctx) { return UUID.fromString(raw); }
        });

        // Player (online)
        register(new ArgumentResolver<Player>() {
            /// <summary>Supports {@code Player}.</summary>
            @Override public boolean supports(Class<?> t) { return t == Player.class; }

            /// <summary>
            /// Attempts to resolve a {@link Player} by UUID first, then by exact name.
            /// </summary>
            /// <param name="raw">Raw text: either a UUID string or an exact player name.</param>
            /// <param name="ctx">Resolution context.</param>
            /// <returns>The online {@link Player} instance, or {@code null} if not found.</returns>
            @Override public Player resolve(String raw, ResolutionContext ctx) {
                try {
                    return Bukkit.getPlayer(UUID.fromString(raw));
                } catch (Exception ignore) {
                    // not a UUID; fall through to name lookup
                }
                return Bukkit.getPlayerExact(raw);
            }

            /// <summary>
            /// Provides tab-completion suggestions for online player names containing the given prefix (case-insensitive).
            /// </summary>
            /// <param name="prefix">User-typed prefix to match.</param>
            /// <param name="ctx">Resolution context.</param>
            /// <returns>List of matching player names.</returns>
            @Override public List<String> suggest(String prefix, ResolutionContext ctx) {
                var list = new ArrayList<String>();
                for (var p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().contains(prefix.toLowerCase())) {
                        list.add(p.getName());
                    }
                }
                return list;
            }
        });

        // OfflinePlayer
        register(new ArgumentResolver<OfflinePlayer>() {
            /// <summary>Supports {@code OfflinePlayer}.</summary>
            @Override public boolean supports(Class<?> t) { return t == OfflinePlayer.class; }

            /// <summary>
            /// Resolves an {@link OfflinePlayer} by UUID if possible; otherwise falls back to name lookup.
            /// </summary>
            /// <param name="raw">Raw text: UUID string or player name.</param>
            /// <param name="ctx">Resolution context.</param>
            /// <returns>An {@link OfflinePlayer} handle (may not have played before).</returns>
            @Override public OfflinePlayer resolve(String raw, ResolutionContext ctx) {
                try {
                    return Bukkit.getOfflinePlayer(UUID.fromString(raw));
                } catch (Exception ignore) {
                    // not a UUID; fall through to name lookup
                }
                return Bukkit.getOfflinePlayer(raw);
            }
        });
    }
}