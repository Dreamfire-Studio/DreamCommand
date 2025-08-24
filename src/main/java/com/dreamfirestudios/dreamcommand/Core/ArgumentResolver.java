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

import java.util.Collections;
import java.util.List;

/**
 * /// <summary>
 * Strategy interface that converts a raw command argument (string) into a typed value
 * and optionally provides tab-completion suggestions for that type.
 * /// </summary>
 * /// <remarks>
 * Implementations are discovered via {@link ArgumentRegistry#register(ArgumentResolver)} and
 * selected at runtime by {@link ArgumentRegistry#find(Class)} based on {@link #supports(Class)}.
 * </remarks>
 * /// <typeparam name="T">The target Java type produced by this resolver.</typeparam>
 * /// <example>
 * <code>
 * public final class IntegerResolver implements ArgumentResolver&lt;Integer&gt; {
 *     &#64;Override public boolean supports(Class&lt;?&gt; type) { return type == int.class || type == Integer.class; }
 *     &#64;Override public Integer resolve(String raw, ResolutionContext ctx) { return Integer.parseInt(raw); }
 * }
 *
 * // Registration:
 * ArgumentRegistry.register(new IntegerResolver());
 * </code>
 * /// </example>
 */
public interface ArgumentResolver<T> {

    /**
     * /// <summary>
     * Indicates whether this resolver can handle the provided target type.
     * /// </summary>
     * /// <param name="type">The requested Java class.</param>
     * /// <returns><c>true</c> if the type is supported; otherwise <c>false</c>.</returns>
     */
    boolean supports(Class<?> type);

    /**
     * /// <summary>
     * Converts a raw string argument into the target type.
     * /// </summary>
     * /// <param name="raw">The raw, user-supplied argument text.</param>
     * /// <param name="ctx">Ambient context for resolution (sender, plugin, etc.).</param>
     * /// <returns>The parsed/converted value.</returns>
     * /// <remarks>
     * Implementations may throw an exception (e.g., {@link NumberFormatException}) to indicate invalid input;
     * callers should handle and surface a friendly error message.
     * </remarks>
     * /// <exception cref="Exception">Thrown if the value cannot be parsed or resolved.</exception>
     */
    T resolve(String raw, ResolutionContext ctx) throws Exception;

    /**
     * /// <summary>
     * Produces tab-completion suggestions for this type given the user's current prefix.
     * /// </summary>
     * /// <param name="prefix">The partial text entered by the user.</param>
     * /// <param name="ctx">Ambient context for suggestion evaluation.</param>
     * /// <returns>A list of suggestion strings; empty by default.</returns>
     * /// <remarks>
     * Override to provide richer completions (e.g., online player names). The default implementation returns an empty list.
     * </remarks>
     */
    default List<String> suggest(String prefix, ResolutionContext ctx) {
        return Collections.emptyList();
    }
}