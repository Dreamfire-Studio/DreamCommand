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

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * /// <summary>
 * Lightweight ambient data passed to {@code ArgumentResolver}s and mapping logic
 * during command execution and tab completion.
 * /// </summary>
 * /// <remarks>
 * Carries the owning {@link Plugin} and the current {@link CommandSender}. Extend as needed
 * in the future if additional context is required (e.g., locale, command label).
 * /// </remarks>
 * /// <example>
 * <code>
 * ResolutionContext ctx = new ResolutionContext(plugin, sender);
 * Integer value = integerResolver.resolve("42", ctx);
 * </code>
 * /// </example>
 */
public final class ResolutionContext {

    /** /// <summary>The plugin owning the command execution.</summary> */
    public final Plugin plugin;

    /** /// <summary>The sender who executed the command (console or player).</summary> */
    public final CommandSender sender;

    /**
     * /// <summary>
     * Creates a new resolution context.
     * /// </summary>
     * /// <param name="plugin">Owning plugin instance.</param>
     * /// <param name="sender">Command sender for the current invocation.</param>
     */
    public ResolutionContext(Plugin plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }
}