# DreamCommand — Quick Start (From a New User’s Perspective)

Welcome! This guide walks you through using **DreamCommand** to build Minecraft plugin commands quickly, cleanly, and safely. No deep Bukkit boilerplate, no fragile parsers — just **annotated Java methods** with strong typing, helpful tab-completion, and clear constraints.

> If you’ve used Bukkit/Spigot/Paper commands before, DreamCommand removes most of the glue code. If you’re brand‑new, this guide will teach you the small set of concepts you need.

---

## What You’ll Build in \~10 Minutes

* A `/hello say <message>` command.
* A `/warp go <name>` command with **function‑driven** tab completions (e.g., `spawn`, `market`).
* A `/fly toggle` command that **requires permission**.
* Optional: Your **first custom argument type** (e.g., parsing a `Location`).

All of this is driven by annotations and a tiny router class that you don’t write — you just **declare methods**, and DreamCommand routes calls to them.

---

## Install & Setup

1. **Add the DreamCommand dependency** (same module as your plugin). If you’re in a multi‑module monorepo, ensure your plugin depends on the `dreamcommand` module.
2. **Call the auto‑registration** once in your plugin entry point:

```java
@Override
public void onEnable() {
    // Registers all discoverable command classes in your plugin automatically
    com.dreamfirestudios.dreamcommand.DreamCommand.Register(this);
}
```

That’s it. From now on, DreamCommand will **find classes** you expose via your project’s auto‑register mechanism (e.g., `DreamfireJavaAPI.getAutoRegisterClasses(plugin)`) and register any handlers it finds.

> **Tip:** Keep command classes small and focused (e.g., `HomeCommands`, `WarpCommands`). This makes maintenance easier and follows best practices.

---

## Core Concepts (Plain English)

* **Handler Class:** Any plain Java class that contains one or more methods annotated with `@PCMethod`. This class represents a command namespace.
* **Command Name:** Either provide a static `COMMAND_NAME` field, or the class name determines it (e.g., `HomeCommands` → `/home`).
* **Method Route:** Each `@PCMethod({"sub", "verb"})` creates a route like `/home sub verb ...`.
* **First Parameter Rule:** Your command methods must take `Player` **or** `CommandSender` as the **first parameter**.
* **Arguments:** All other parameters are parsed from raw text using built‑in or custom resolvers.
* **Tab Completion:** Add `@PCTab` (static/dynamic) or `@PCAutoTab` (resolver‑driven suggestions).
* **Constraints:** Use `@PCPerm`, `@PCOP`, and `@PCWorld` to restrict access.

---

## Your First Command — “Hello”

Create a class named `HelloCommands`:

```java
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
package my.plugin.commands;

import com.dreamfirestudios.dreamcommand.Annotations.PCMethod;
import org.bukkit.entity.Player;

/**
 * /// <summary>Example command namespace for the `/hello` command.</summary>
 * /// <remarks>Registered automatically when DreamCommand.Register(plugin) is called in onEnable().</remarks>
 */
public final class HelloCommands {
    /** /// <summary>Primary command label.</summary> */
    public static final String COMMAND_NAME = "hello"; // results in /hello

    /** /// <summary>Optional short alias.</summary> */
    public static final String[] COMMAND_ALIASES = {"hi"};

    /** /// <summary>Enable debug during development to see route mismatch reasons.</summary> */
    public static final boolean COMMAND_DEBUG = true;

    /**
     * /// <summary>Prints a friendly greeting plus the user-provided message.</summary>
     * /// <param name="player">The executing player.</param>
     * /// <param name="message">Any text that follows the subcommand.</param>
     * /// <example>
     * /// <code>/hello say Welcome to the server!</code>
     * /// </example>
     */
    @PCMethod({"say"})
    public void say(Player player, String message) {
        player.sendMessage("Hello, " + player.getName() + "! You said: " + message);
    }
}
```

➡️ Try it: `/hello say Welcome to the server!`

---

## Add Permissions & OP‑Only Commands

Use `@PCPerm` (one or more permission nodes) and `@PCOP` (requires operator).

```java
package my.plugin.commands;

import com.dreamfirestudios.dreamcommand.Annotations.PCMethod;
import com.dreamfirestudios.dreamcommand.Annotations.PCPerm;
import com.dreamfirestudios.dreamcommand.Annotations.PCOP;
import org.bukkit.entity.Player;

/** /// <summary>Command set for flight toggling.</summary> */
@PCPerm("myplugin.fly") // All methods here require this permission (can also be applied per-method)
public final class FlyCommands {
    public static final String COMMAND_NAME = "fly"; // /fly

    /** /// <summary>Toggle your own flight.</summary> */
    @PCMethod({"toggle"})
    public void toggle(Player player) {
        boolean enable = !player.getAllowFlight();
        player.setAllowFlight(enable);
        player.sendMessage("Flight: " + (enable ? "enabled" : "disabled"));
    }

    /** /// <summary>OP‑only: toggle someone else’s flight.</summary> */
    @PCMethod({"force"})
    @PCOP
    public void force(Player sender, Player target) {
        boolean enable = !target.getAllowFlight();
        target.setAllowFlight(enable);
        sender.sendMessage("Toggled flight for " + target.getName());
    }
}
```

➡️ Try it: `/fly toggle` (requires `myplugin.fly`) and `/fly force <player>` (also requires OP).

---

## Add Smart Tab‑Completion

You can add completions in 3 ways:

1. **Static** (literal strings with `@PCTab` type `PureData`).
2. **Dynamic via provider** (`TabDataProvider` → server or player lists).
3. **Function‑based** (`@PCMethodData` returns `List<String>`).

### Example: Function‑driven Tabs

```java
package my.plugin.commands;

import com.dreamfirestudios.dreamcommand.Annotations.PCMethod;
import com.dreamfirestudios.dreamcommand.Annotations.PCMethodData;
import com.dreamfirestudios.dreamcommand.Annotations.PCTab;
import com.dreamfirestudios.dreamcommand.Enums.TabType;
import org.bukkit.entity.Player;

import java.util.List;

public final class WarpCommands {
    public static final String COMMAND_NAME = "warp"; // /warp

    /** /// <summary>Source of warp names for tab completion.</summary> */
    @PCMethodData
    public List<String> warpNames() { return List.of("spawn", "market", "arena"); }

    /** /// <summary>Teleport to a named warp.</summary> */
    @PCMethod({"go"})
    @PCTab(pos = 1, type = TabType.InformationFromFunction, data = "warpNames")
    public void go(Player player, String warp) {
        player.sendMessage("Warping to: " + warp);
        // TODO: Integrate your warp logic here
    }
}
```

➡️ Typing `/warp go <TAB>` suggests: `spawn`, `market`, `arena`.

### Example: Online Player Names

```java
@PCMethod({"tp"})
@PCTab(pos = 1, type = TabType.OnlinePlayerNames)
public void teleport(Player player, Player target) {
    player.teleport(target);
}
```

### Example: Static Options

```java
@PCMethod({"set"})
@PCTab(pos = 1, type = TabType.PureData, data = "admin")
@PCTab(pos = 1, type = TabType.PureData, data = "mod")
@PCTab(pos = 1, type = TabType.PureData, data = "vip")
public void setRank(Player player, String rank) { /* ... */ }
```

> **Hiding completions:** Use `@PCHideTab` (hide all) or `@PCFunctionHideTab("functionName")` to show/hide dynamically.

---

## Understanding Method Signatures

* **`@PCMethod({"sub", "verb"})`** defines the **fixed prefix**.
* Everything **after** that prefix maps to your parameters.
* **First parameter** must be `Player` or `CommandSender`.
* Support for **varargs**: if your final parameter is an array (e.g., `String[]`), DreamCommand captures the remaining raw args into that array.
* **Enums**: You can use enum parameters; DreamCommand matches names case‑insensitively.

### Quick Mapping Cheat‑Sheet

| Parameter Type                      | How it’s parsed                                             |
| ----------------------------------- | ----------------------------------------------------------- |
| `String`                            | Raw token as typed                                          |
| `int` / `Integer`                   | `Integer.parseInt`                                          |
| `double` / `Double`                 | `Double.parseDouble`                                        |
| `boolean` / `Boolean`               | `Boolean.parseBoolean`                                      |
| `java.util.UUID`                    | `UUID.fromString`                                           |
| `org.bukkit.entity.Player` (online) | First by UUID, then exact name; may be `null` if not online |
| `org.bukkit.OfflinePlayer`          | By UUID or name (even if never joined)                      |
| `YourEnum`                          | Case‑insensitive constant name match                        |

> **Gotcha:** If the first parameter is neither `Player` nor `CommandSender`, DreamCommand will reject the method.

---

## Dynamic Tab Data via `TabDataProvider` (Optional)

If you want server‑ or player‑scoped lists (e.g., regions, teams, homes), implement `TabDataProvider` once and pass it to a custom registration call.

```java
package my.plugin;

import com.dreamfirestudios.dreamcommand.Core.TabDataProvider;
import java.util.List;
import java.util.UUID;

public final class MyProvider implements TabDataProvider {
    @Override public List<String> serverData(String key) {
        return switch (key) {
            case "regions" -> List.of("alpha", "beta", "gamma");
            default -> List.of();
        };
    }
    @Override public List<String> playerData(UUID uuid, String key) {
        return switch (key) {
            case "homes" -> List.of("home", "mine", "village");
            default -> List.of();
        };
    }
}
```

Registration variant (if you control it directly). Many setups will register via the built‑in auto‑discovery instead — this is optional:

```java
// Example helper: register with a provider (if your bootstrap exposes it)
// DreamCommand has a private overload for provider; your platform bootstrap can call it.
```

Use it with tabs:

```java
@PCTab(pos = 1, type = TabType.PullServerData, data = "regions")
@PCTab(pos = 2, type = TabType.PullPlayerData, data = "homes")
```

---

## Creating a Custom Argument Type

You can parse your own types by registering an `ArgumentResolver<T>`.

```java
package my.plugin.resolvers;

import com.dreamfirestudios.dreamcommand.Core.ArgumentResolver;
import com.dreamfirestudios.dreamcommand.Core.ResolutionContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/** /// <summary>Parses a Location from "world,x,y,z".</summary> */
public final class LocationResolver implements ArgumentResolver<Location> {
    @Override public boolean supports(Class<?> type) { return type == Location.class; }
    @Override public Location resolve(String raw, ResolutionContext ctx) {
        String[] p = raw.split(",");
        return new Location(
            Bukkit.getWorld(p[0]),
            Double.parseDouble(p[1]),
            Double.parseDouble(p[2]),
            Double.parseDouble(p[3])
        );
    }
}
```

Register it during enable **before** commands execute:

```java
import com.dreamfirestudios.dreamcommand.Core.ArgumentRegistry;

@Override
public void onEnable() {
    ArgumentRegistry.register(new my.plugin.resolvers.LocationResolver());
    com.dreamfirestudios.dreamcommand.DreamCommand.Register(this);
}
```

Then use it directly in a command:

```java
@PCMethod({"bring"})
public void bring(Player player, Location where) {
    player.teleport(where);
}
// /tp bring world,0,64,0
```

---

## Debugging & Common Errors

DreamCommand emits friendly messages defined by `CommandError` while in debug mode (`COMMAND_DEBUG = true`).

* **FirstParamInvalid** — First parameter isn’t `Player` or `CommandSender`.
* **WrongParameterCount** — Your method expects different args (consider varargs).
* **SerializationFailure** — A parameter couldn’t be parsed (e.g., bad number/UUID/enum).
* **NoMatchingMethod** — No `@PCMethod` signature matched the input prefix.
* **MustBePlayer / MustBeServer** — Sender type doesn’t satisfy the method.

**Pro tips:**

* Start with **one** handler method, enable **debug**, and expand incrementally.
* Prefer **small, focused classes** and **clear subcommand names**.
* Use **enums** for constrained values — you’ll get parsing for free.

---

## Best Practices (Team‑Friendly Code)

* **One responsibility per class**: e.g., `HomeCommands`, `WarpCommands`, `TeamCommands`.
* **Use interfaces/enums** to express intent (`TabType`, permission constants, etc.).
* **Doc comments** using `<summary>`, `<param>`, `<remarks>`, `<example>` so your wiki tooling can extract docs.
* **Avoid heavy logic** inside command methods — delegate to services. Commands should orchestrate, not compute.
* **Keep tab providers pure**: no side effects; fast and cacheable.

---

## Full Example — `DreamCommandExamples.java`

> You can copy‑paste this file as a starting point. It wires up multiple handlers and demonstrates annotations, tabs, and permissions in one place.

```java
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
package my.plugin.examples;

import com.dreamfirestudios.dreamcommand.Annotations.*;
import com.dreamfirestudios.dreamcommand.Enums.TabType;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /// <summary>Turn‑key example bundle showing common DreamCommand patterns.</summary>
 * /// <remarks>Register all of these by calling DreamCommand.Register(this) in onEnable().</remarks>
 */
public final class DreamCommandExamples {

    private DreamCommandExamples() { }

    // ----------------------- /hello -----------------------
    public static final class HelloCommands {
        public static final String COMMAND_NAME = "hello";
        public static final String[] COMMAND_ALIASES = {"hi"};
        public static final boolean COMMAND_DEBUG = true;

        @PCMethod({"say"})
        public void say(Player player, String message) {
            player.sendMessage("Hello, " + player.getName() + "! You said: " + message);
        }
    }

    // ------------------------ /fly ------------------------
    @PCPerm("myplugin.fly")
    public static final class FlyCommands {
        public static final String COMMAND_NAME = "fly";

        @PCMethod({"toggle"})
        public void toggle(Player player) {
            boolean enable = !player.getAllowFlight();
            player.setAllowFlight(enable);
            player.sendMessage("Flight: " + (enable ? "enabled" : "disabled"));
        }

        @PCMethod({"force"}) @PCOP
        public void force(Player sender, Player target) {
            boolean enable = !target.getAllowFlight();
            target.setAllowFlight(enable);
            sender.sendMessage("Toggled flight for " + target.getName());
        }
    }

    // ------------------------ /warp -----------------------
    public static final class WarpCommands {
        public static final String COMMAND_NAME = "warp";

        @PCMethodData
        public List<String> warpNames() { return List.of("spawn", "market", "arena"); }

        @PCMethod({"go"})
        @PCTab(pos = 1, type = TabType.InformationFromFunction, data = "warpNames")
        public void go(Player player, String warp) {
            player.sendMessage("Warping to: " + warp);
        }
    }
}
```

---

## Frequently Asked Questions

**Q: Do I need to register commands in `plugin.yml`?**
**A:** DreamCommand registers at runtime via Bukkit’s `CommandMap`. You don’t need static `plugin.yml` command entries for router‑based commands.

**Q: Can I still use old `BukkitCommand` classes?**
**A:** Yes. DreamCommand will register them directly if it finds them. You can mix both styles.

**Q: How do I support enums in parameters?**
**A:** Just use your enum type. DreamCommand matches constants by name (case‑insensitive).

**Q: What if I need complex parsing?**
**A:** Implement and register an `ArgumentResolver<T>` (see the custom argument example above).

**Q: Are commands thread‑safe?**
**A:** Bukkit command callbacks are on the **main thread**. Keep heavy work off‑thread and shuttle results back safely.

---

## Troubleshooting Checklist

* **Nothing happens:** Ensure `DreamCommand.Register(this)` is called in `onEnable()`.
* **Route not matching:** Turn on `COMMAND_DEBUG = true` in your class and re‑try; check your `@PCMethod` prefix and argument types.
* **WrongParameterCount:** Compare your method parameters to the arguments you typed. For varargs, ensure your final parameter is an array type.
* **SerializationFailure:** A resolver failed to parse. Re‑check numbers/UUIDs/enums or create a custom resolver.
* **Permissions blocked:** Make sure you granted the correct `@PCPerm` node or OP status for `@PCOP` methods.

---

## Next Steps

* Create small, focused command classes for each feature area.
* Leverage `@PCTab` and `@PCMethodData` to give players strong autocomplete.
* Extract heavy logic into services; keep commands thin.
* Add dox comments everywhere so your docs site can auto‑build from code.

**You’re ready to ship your first DreamCommand‑powered commands.** Have fun! 🎉
