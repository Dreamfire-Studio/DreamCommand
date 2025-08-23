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
package com.dreamfirestudios.dreamcommand.Annotations;

import java.lang.annotation.*;

/// <summary>
/// Declares a command method within the DreamCommand framework.
/// </summary>
/// <remarks>
/// This annotation defines one or more command aliases and an optional description.
/// </remarks>
/// <param name="value">Array of command aliases (e.g. {"fly", "f"}).</param>
/// <param name="description">Optional description for documentation/help menus.</param>
/// <example>
/// <code>
/// @PCMethod(value = {"fly", "f"}, description = "Toggles flight mode.")
/// public void flyCommand(Player player) { ... }
/// </code>
/// </example>
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PCMethod {
    String[] value();
    String description() default "";
}