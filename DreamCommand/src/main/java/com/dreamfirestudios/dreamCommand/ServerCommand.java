package com.dreamfirestudios.dreamCommand;

import com.dreamfirestudios.dreamCommand.Enums.PlayerCommandError;
import com.dreamfirestudios.dreamCommand.Enums.ServerCommandError;
import com.dreamfirestudios.dreamCommand.Interface.PCMethod;
import com.dreamfirestudios.dreamCommand.Interface.PCMethodData;
import com.dreamfirestudios.dreamCommand.Objects.CustomServerMethod_v1_20_R3;
import com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireChat;
import com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;

public abstract class ServerCommand extends BukkitCommand {

    private final List<CustomServerMethod_v1_20_R3> customPlayerMethods = new ArrayList<>();
    private final HashMap<String, Method> liveData = new HashMap<>();
    private final String commandName;
    private final boolean debugErrors;

    protected ServerCommand(String commandName, boolean debugErrors, String... alias) {
        super(commandName.toLowerCase());
        for(var method : this.getClass().getMethods()){
            if(method.isAnnotationPresent(PCMethod.class)) customPlayerMethods.add(new CustomServerMethod_v1_20_R3(this, method));
            if(method.isAnnotationPresent(PCMethodData.class)) liveData.put(method.getName(), method);
        }
        setAliases(Arrays.stream(alias).toList());
        this.commandName = commandName;
        this.debugErrors = debugErrors;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return new ArrayList<>();
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] args) {
        if(commandSender instanceof Player){
            if(debugErrors) DreamfireChat.SendMessageToConsole(ServerCommandError.MustBeServerTOUseCommand.error);
            return false;
        }

        for(var cm : customPlayerMethods){
            var invoke = cm.TryAndInvokeMethod(commandSender, args);
            if(invoke == null) return true;
            else if(debugErrors) DreamfireChat.SendMessageToConsole(invoke.error);
        }

        if(debugErrors) DreamfireChat.SendMessageToConsole(ServerCommandError.NoMethodOrCommandFound.error);
        NoMethodFound(commandSender, s, args);
        DisplayHelpMenu(commandSender);
        return false;
    }

    public void DisplayHelpMenu(CommandSender commandSender){
        if(helpMenuPrefix(commandSender).isEmpty() || helpMenuSuffix(commandSender).isEmpty()) return;
        DreamfireChat.SendMessageToConsole(helpMenuPrefix(commandSender));
        for(var customPlayerMethod : customPlayerMethods){
            if(!customPlayerMethod.CanServerUseCommand(commandSender)) return;
            var stringBuilder = new StringBuilder();
            stringBuilder.append("/").append("#7a7a7a").append(commandName).append(" ");
            for(var value : helpMenuFormat(commandSender,  customPlayerMethod.ReturnHelpMenu()).values()) stringBuilder.append("#adacac(").append(value).append("#adacac)").append(" ");
            var textComp = new TextComponent(PlainTextComponentSerializer.plainText().serialize(DreamfireMessage.formatMessage(stringBuilder.toString())));
            textComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click To Copy!")));
            textComp.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, customPlayerMethod.ReturnClipboard("/" + commandName)));
            commandSender.spigot().sendMessage(textComp);
        }
        DreamfireChat.SendMessageToConsole(helpMenuSuffix(commandSender));
    }

    public Method ReturnMethodByName(String methodName){
        return liveData.getOrDefault(methodName, null);
    }
    public abstract void NoMethodFound(CommandSender commandSender, String s, String[] args);
    public abstract String helpMenuPrefix(CommandSender commandSender);
    public abstract LinkedHashMap<String, String> helpMenuFormat(CommandSender commandSender, LinkedHashMap<String, String> params);
    public abstract String helpMenuSuffix(CommandSender commandSender);
}
