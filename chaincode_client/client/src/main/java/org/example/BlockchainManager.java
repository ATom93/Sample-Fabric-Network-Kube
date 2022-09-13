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

    /**
     * Executes the script to add a new channel on the blockchain infrastructure
     *
     * @param channelName Name of the new Fabric Channel to create
     * @throws IOException
     * @throws InterruptedException
     */
    public static void addChannel(String channelName) throws IOException, InterruptedException {
        executeScript("./addNewChannel.sh " + channelName);
    }

}
