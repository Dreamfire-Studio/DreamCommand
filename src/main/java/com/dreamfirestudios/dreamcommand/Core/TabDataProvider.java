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

import java.util.List;
import java.util.UUID;

/**
 * /// <summary>
 * Abstraction used by the command router to pull dynamic tab-completion data
 * from server-wide or player-scoped sources.
 * /// </summary>
 * /// <remarks>
 * Implement and supply an instance to {@code CommandRouter} when you need custom,
 * runtime-populated completions (e.g., list of regions, teams, warp names).
 * /// </remarks>
 * /// <example>
 * <code>
 * public final class MyProvider implements TabDataProvider {
 *     &#64;Override
 *     public List&lt;String&gt; serverData(String key) {
 *         return switch (key) {
 *             case "regions" -&gt; regionService.getAllNames();
 *             default -&gt; List.of();
 *         };
 *     }
 *     &#64;Override
 *     public List&lt;String&gt; playerData(UUID uuid, String key) {
 *         if ("homes".equals(key)) return homesService.listHomes(uuid);
 *         return List.of();
 *     }
 * }
 * </code>
 * /// </example>
 */
public interface TabDataProvider {

    /**
     * /// <summary>
     * Returns server-scoped suggestion data for a given logical key.
     * /// </summary>
     * /// <param name="key">A logical identifier indicating which dataset to return (e.g., "regions").</param>
     * /// <returns>A list of suggestion strings (possibly empty, never {@code null}).</returns>
     */
    List<String> serverData(String key);

    /**
     * /// <summary>
     * Returns player-scoped suggestion data for a given logical key.
     * /// </summary>
     * /// <param name="uuid">The player whose data should be queried.</param>
     * /// <param name="key">A logical identifier indicating which dataset to return (e.g., "homes").</param>
     * /// <returns>A list of suggestion strings (possibly empty, never {@code null}).</returns>
     */
    List<String> playerData(UUID uuid, String key);
}