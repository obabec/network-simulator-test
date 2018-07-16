package com.redhat.patriot.network_simulator_test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.redhat.patriot.network_simulator.example.DockerController;
import com.redhat.patriot.network_simulator.example.args.CommandLineParser;
import com.redhat.patriot.network_simulator.example.cleanup.Cleaner;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * The type Main.
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.
            getLogger(com.redhat.patriot.network_simulator.example.Main.class);

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws InterruptedException the interrupted exception
     */
    public static void main(String[] args) throws InterruptedException {
        com.redhat.patriot.network_simulator.example.DockerController dockerController = new DockerController();
        CommandLineParser cmdArgs = new CommandLineParser();
        CmdLineParser parser = new CmdLineParser(cmdArgs);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
        }

        if (cmdArgs.isClean()) {
            LOGGER.info("Cleaning docker.");
            DockerClient dockerClient = DockerClientBuilder.
                    getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder().build()).build();
            Cleaner cleaner = new Cleaner(dockerClient);
            cleaner.cleanUp(Arrays.asList("client_network", "server_network"),
                    Arrays.asList("comm_client", "comm_server", "router"));
        } else {
            LOGGER.info("Generating enviropment");
            dockerController.genererateEnviroment();
        }


    }



}
