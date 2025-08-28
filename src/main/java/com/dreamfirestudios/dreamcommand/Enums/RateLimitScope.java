/*
 * MIT License
 */
package com.dreamfirestudios.dreamcommand.Enums;

/**
 * <summary>Determines how rate-limit buckets are keyed.</summary>
 */
public enum RateLimitScope {
    /** <summary>One bucket per sender (Player UUID when possible, otherwise sender name).</summary> */
    PER_SENDER,

    /** <summary>One bucket per world (Player world UUID; console uses a shared bucket).</summary> */
    PER_WORLD,

    /** <summary>A single shared bucket for everyone calling this handler.</summary> */
    GLOBAL
}