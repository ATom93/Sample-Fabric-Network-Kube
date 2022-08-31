package org.example;

import java.io.IOException;

public class BlockchainManager {

    private static void executeScript(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);
        processBuilder.inheritIO();

        Process process = processBuilder.start();
        process.waitFor();
    }

    public static void init() throws IOException, InterruptedException {
        executeScript("./init.sh");
    }

    public static void addChannel(String channelName) throws IOException, InterruptedException {
        executeScript("./addNewChannel.sh " + channelName);
    }

    public static void registerAndEnrollUser(String n) throws IOException, InterruptedException {
        executeScript("./script/register-enroll_client.sh" + n);
    }


}
