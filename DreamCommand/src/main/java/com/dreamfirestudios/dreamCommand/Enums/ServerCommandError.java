package com.dreamfirestudios.dreamCommand.Enums;

public enum ServerCommandError {
    ServerCommandsLocked("This server Commands Have Been Locked!"),
    FirstMethodParamMustBeCommandSender("The first param in a PCMethod must be CommandSender.class"),
    ServerInputDifferentCommandSing("The input the server has used is different from command signature"),
    CommandInvokedWithWrongParams("The provided arguments don't work with command methods!"),
    MustBeServerTOUseCommand("The command sender must be the server to use this command"),
    NoMethodOrCommandFound("No command method that matches the signature");

    public final String error;
    private ServerCommandError(String error) {
        this.error = error;
    }
}
