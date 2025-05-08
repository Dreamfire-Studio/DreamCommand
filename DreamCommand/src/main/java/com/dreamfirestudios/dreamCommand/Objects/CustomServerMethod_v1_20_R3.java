package com.dreamfirestudios.dreamCommand.Objects;

import com.dreamfirestudios.dreamCommand.Enums.PlayerCommandError;
import com.dreamfirestudios.dreamCommand.Enums.ServerCommandError;
import com.dreamfirestudios.dreamCommand.Interface.PCOP;
import com.dreamfirestudios.dreamCommand.Interface.PCPerm;
import com.dreamfirestudios.dreamCommand.Interface.PCSignature;
import com.dreamfirestudios.dreamCommand.Interface.PCWorld;
import com.dreamfirestudios.dreamCommand.PlayerCommand;
import com.dreamfirestudios.dreamCommand.ServerCommand;
import com.dreamfirestudios.dreamCore.DreamfireVariable.DreamfireVariable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftOfflinePlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CustomServerMethod_v1_20_R3 implements CustomServerMethod{

    private final ServerCommand serverCommand;
    private final Method method;

    public CustomServerMethod_v1_20_R3(ServerCommand serverCommand, Method method){
        this.serverCommand = serverCommand;
        this.method = method;
    }

    @Override
    public ServerCommandError TryAndInvokeMethod(CommandSender commandSender, String[] server_args) {
        if(!CanServerUseCommand(commandSender)) return ServerCommandError.ServerCommandsLocked;
        if (method.getParameterTypes().length < 1) return ServerCommandError.FirstMethodParamMustBeCommandSender;

        var commandSignature = new ArrayList<String>(Arrays.asList(method.getAnnotation(PCSignature.class).value()));
        if (server_args.length < commandSignature.size()) return ServerCommandError.ServerInputDifferentCommandSing;

        for (var i = 0; i < commandSignature.size(); i++){
            if (!commandSignature.get(i).equals(server_args[i])) return ServerCommandError.ServerInputDifferentCommandSing;
        }

        var serialisedPlayerInput = new ArrayList<String>(Arrays.asList(server_args).subList(commandSignature.size(), server_args.length));
        var methodParameterTypes = method.getParameterTypes();

        var isLastParamArray = methodParameterTypes.length > 1 && methodParameterTypes[methodParameterTypes.length - 1].isArray();
        if(isLastParamArray && serialisedPlayerInput.size() + 1 <= methodParameterTypes.length - 1) return ServerCommandError.CommandInvokedWithWrongParams;
        if(!isLastParamArray && serialisedPlayerInput.size() != methodParameterTypes.length - 1) return ServerCommandError.CommandInvokedWithWrongParams;

        var invokeArgs = new ArrayList<Object>(List.of(commandSender));
        for (var i = 1; i < methodParameterTypes.length; i++) {
            if(!isLastParamArray || i < methodParameterTypes.length - 1){
                invokeArgs.add(SerialiseSingleData(methodParameterTypes[i], serialisedPlayerInput.get(i - 1)));
            }else if(i == methodParameterTypes.length - 1){
                var data = new ArrayList<Object>();
                var startIndex = methodParameterTypes.length - 2;
                var endIndex = serialisedPlayerInput.size();
                for(var playerArgument : new ArrayList<>(serialisedPlayerInput.subList(startIndex, endIndex))) data.add(SerialiseSingleData(methodParameterTypes[i], playerArgument));
                invokeArgs.add(convertArray(data.toArray(), methodParameterTypes[i]));
            }
        }

        for(var i = 0 ; i < method.getParameterTypes().length; i++){
            var parmType = method.getParameterTypes()[i];
            var paramTest = DreamfireVariable.returnTestFromType(parmType);
            if(paramTest != null && !paramTest.IsType(invokeArgs.get(i))) return ServerCommandError.NoMethodOrCommandFound;
        }

        try {
            method.invoke(serverCommand, invokeArgs.toArray(new Object[0]));
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T[] convertArray(Object[] array, Class<?> targetClass) {
        if (array == null || targetClass == null) throw new IllegalArgumentException("Array or target class cannot be null");
        if (array.length == 0) return (T[]) java.lang.reflect.Array.newInstance(targetClass.getComponentType(), 0);

        try {
            T[] convertedArray = (T[]) java.lang.reflect.Array.newInstance(targetClass.getComponentType(), array.length);
            for (int i = 0; i < array.length; i++) convertedArray[i] = (T) targetClass.getComponentType().cast(array[i]);
            return convertedArray;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot cast array element to target class", e);
        }
    }

    @Override
    public LinkedHashMap<String, String> ReturnHelpMenu() {
        var data = new LinkedHashMap<String, String>();
        if(method.isAnnotationPresent(PCSignature.class)) for(var sig : method.getAnnotation(PCSignature.class).value()) data.put(sig, sig);
        for(var i = 1; i < method.getParameterTypes().length; i++){
            var param = method.getParameterTypes()[i];
            data.put(param.getSimpleName(), param.getSimpleName());
        }
        return data;
    }

    @Override
    public String ReturnClipboard(String commandName) {
        var stringBuilder = new StringBuilder();
        stringBuilder.append(commandName);
        if(method.isAnnotationPresent(PCSignature.class)) for(var sig : method.getAnnotation(PCSignature.class).value()) stringBuilder.append(" ").append(sig);
        return stringBuilder.toString();
    }

    @Override
    public boolean CanServerUseCommand(CommandSender commandSender) {
        return true;
    }

    private Object SerialiseSingleData(Class<?> methodParamType, String playerArgument){
        if(methodParamType == Player.class || methodParamType == CraftPlayer.class){
            if(DreamfireVariable.returnTestFromType(UUID.class).IsType(playerArgument)) return Bukkit.getPlayer(UUID.fromString(playerArgument));
            else return Bukkit.getPlayer(playerArgument);
        }else if(methodParamType == OfflinePlayer.class || methodParamType == CraftOfflinePlayer.class){
            if(DreamfireVariable.returnTestFromType(UUID.class).IsType(playerArgument)) return Bukkit.getOfflinePlayer(UUID.fromString(playerArgument));
            else return Bukkit.getOfflinePlayer(playerArgument);
        }
        var paramTest = DreamfireVariable.returnTestFromType(methodParamType);
        return paramTest == null ? playerArgument : paramTest.DeSerializeData(playerArgument);
    }
}
