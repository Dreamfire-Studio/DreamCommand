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

import com.dreamfirestudios.dreamcommand.Enums.TabType;
import java.lang.annotation.*;

/// <summary>
/// Declares a tab completion entry for a command method.
/// </summary>
/// <remarks>
/// Supports position indexing, type of tab completion, and optional extra data.
/// </remarks>
/// <param name="pos">The argument index for tab completion.</param>
/// <param name="type">The type of tab (see <see cref="TabType"/>).</param>
/// <param name="data">Optional extra data for this tab entry.</param>
/// <example>
/// <code>
/// @PCTab(pos = 1, type = TabType.PLAYER_NAME)
/// </code>
/// </example>
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PCTabs.class)
public @interface PCTab {
    int pos();
    TabType type();
    String data() default "";
}