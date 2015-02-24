package cz.muni.fi.frontend;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class Main {
    
    static {
        NativeUtils.loadLibrary("ODE");
        NativeUtils.loadLibrary("Thomas");
    }

    public static void main(@NotNull String[] args) throws InterruptedException, IOException {

    }

}
