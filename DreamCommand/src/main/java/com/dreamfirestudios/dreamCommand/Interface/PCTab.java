package com.dreamfirestudios.dreamCommand.Interface;

import com.dreamfirestudios.dreamCommand.Enums.TabType;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PCTabs.class)
public @interface PCTab {
    public int pos();
    public TabType type();
    public String data();
}