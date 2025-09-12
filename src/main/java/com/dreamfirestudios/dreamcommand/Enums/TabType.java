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
package com.dreamfirestudios.dreamcommand.Enums;

/**
 * /// <summary>
 * Types of tab-completion sources supported by the command router.
 * /// </summary>
 * /// <remarks>
 * These values are used by the {@code @PCTab} annotation to declare where and how to
 * source suggestions for a parameter position.
 * /// </remarks>
 */
public enum TabType {

    /**
     * /// <summary>
     * Invoke a named function (registered via {@code @PCMethodData}) on the command target
     * to obtain a {@code List&lt;String&gt;} of suggestions.
     * /// </summary>
     */
    InformationFromFunction,

    /**
     * /// <summary>
     * Request player-scoped suggestions from the configured {@code TabDataProvider},
     * using the annotation's {@code data()} field as the logical key.
     * /// </summary>
     */
    PullPlayerData,

    /**
     * /// <summary>
     * Request server-wide suggestions from the configured {@code TabDataProvider},
     * using the annotation's {@code data()} field as the logical key.
     * /// </summary>
     */
    PullServerData,

    /**
     * /// <summary>
     * Provide a literal suggestion equal to the annotation's {@code data()} value.
     * Useful for fixed keywords.
     * /// </summary>
     */
    PureData,

    /**
     * /// <summary>
     * Suggest online player names (case-insensitive contains match against the current prefix).
     * /// </summary>
     */
    OnlinePlayerNames,

    /**
     * /// <summary>
     * Suggest known offline player names (case-insensitive contains match against the current prefix).
     * /// </summary>
     */
    OfflinePlayerNames,
    Enum,
    WorldNames,
    WorldIDS,
    PluginNames,
    PermissionNodes
}