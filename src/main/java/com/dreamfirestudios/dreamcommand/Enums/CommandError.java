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
 * Uniform error codes/messages produced by the command routing and argument resolution layers.
 * /// </summary>
 * /// <remarks>
 * Each enum constant carries a human-readable {@link #message} intended for direct display to command senders.
 * /// </remarks>
 */
public enum CommandError {

    /** /// <summary>Command execution is currently disabled/locked by the system or admin.</summary> */
    CommandsLocked("Commands are locked"),

    /** /// <summary>The first method parameter is not {@code Player} or {@code CommandSender}.</summary> */
    FirstParamInvalid("First parameter must be Player or CommandSender"),

    /** /// <summary>Given input does not match the handler's fixed signature tokens.</summary> */
    SignatureMismatch("Input does not match command signature"),

    /** /// <summary>Argument count does not match the handler's parameters (considering varargs).</summary> */
    WrongParameterCount("Incorrect number of parameters"),

    /** /// <summary>At least one argument could not be parsed into the expected type.</summary> */
    SerializationFailure("Could not parse one or more parameters"),

    /** /// <summary>Command requires a player context but the sender is not a player.</summary> */
    MustBePlayer("Sender must be a player"),

    /** /// <summary>Command requires console/server context but the sender is a player.</summary> */
    MustBeServer("Sender must be the server/console"),

    /** /// <summary>No annotated method matched the provided input and constraints.</summary> */
    NoMatchingMethod("No command method matches the input");

    /** /// <summary>Human-readable message intended for display to the sender.</summary> */
    public final String message;

    /**
     * /// <summary>
     * Constructs a command error with the specified message.
     * /// </summary>
     * /// <param name="message">Displayable message associated with this error code.</param>
     */
    CommandError(String message) {
        this.message = message;
    }
}