package com.dreamfirestudios.dreamCommand;

import com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireChat;
import com.dreamfirestudios.dreamCore.DreamfireJava.DreamfireJavaAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public final class DreamCommand extends JavaPlugin {
    public static DreamCommand DreamfireCommands;

    @Override
    public void onEnable() {
        DreamfireCommands = this;
        RegisterRaw(DreamfireCommands);
    }

    public static void RegisterRaw(JavaPlugin javaPlugin) {
        try {
            Register(javaPlugin);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void Register(JavaPlugin javaPlugin) throws Exception {
        for(var autoRegisterClass : DreamfireJavaAPI.getAutoRegisterClasses(javaPlugin)){
            if(PlayerCommand.class.isAssignableFrom(autoRegisterClass)) RegisterPlayerCommand(javaPlugin, (PlayerCommand) autoRegisterClass.getConstructor().newInstance());
            else if(ServerCommand.class.isAssignableFrom(autoRegisterClass)) RegisterServerCommand(javaPlugin, (ServerCommand) autoRegisterClass.getConstructor().newInstance());
            else if(BukkitCommand.class.isAssignableFrom(autoRegisterClass)) RegisterBukkitCommand(javaPlugin, (BukkitCommand) autoRegisterClass.getConstructor().newInstance());
        }
    }

    private static void RegisterPlayerCommand(JavaPlugin plugin, PlayerCommand playerCommand) throws Exception{
        var commandMap = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
        commandMap.register(plugin.getName(), playerCommand);
        DreamfireChat.SendMessageToConsole(String.format("&8Registered PlayerCommand: %s", playerCommand.getName()));
    }

    private static void RegisterServerCommand(JavaPlugin plugin, ServerCommand serverCommand) throws Exception{
        var commandMap = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
        commandMap.register(plugin.getName(), serverCommand);
        DreamfireChat.SendMessageToConsole(String.format("&8Registered ServerCommand: %s", serverCommand.getName()));
    }

    private static void RegisterBukkitCommand(JavaPlugin plugin, BukkitCommand bukkitCommand) throws Exception{
        var commandMap = (CommandMap) getField(Bukkit.getServer().getClass(), "commandMap").get(Bukkit.getServer());
        commandMap.register(plugin.getName(), bukkitCommand);
        DreamfireChat.SendMessageToConsole(String.format("&8Registered BukkitCommand: %s", bukkitCommand.getName()));
    }

    private static Field getField(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }
}