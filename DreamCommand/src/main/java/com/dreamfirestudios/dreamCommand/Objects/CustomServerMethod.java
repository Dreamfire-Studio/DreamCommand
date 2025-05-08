package com.dreamfirestudios.dreamCommand.Objects;

import com.dreamfirestudios.dreamCommand.Enums.PlayerCommandError;
import com.dreamfirestudios.dreamCommand.Enums.ServerCommandError;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;

public interface CustomServerMethod {
    public ServerCommandError TryAndInvokeMethod(CommandSender commandSender, String[] args);
    public <T> T[] convertArray(Object[] array, Class<?> targetClass);
    public LinkedHashMap<String, String> ReturnHelpMenu();
    public String ReturnClipboard(String commandName);
    public boolean CanServerUseCommand(CommandSender commandSender);
}
