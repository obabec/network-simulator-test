package com.redhat.patriot.network_simulator.example;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.redhat.patriot.network_simulator.example.cleanup.Cleaner;
import com.redhat.patriot.network_simulator.example.container.Container;
import com.redhat.patriot.network_simulator.example.image.DockerImage;
import com.redhat.patriot.network_simulator.example.manager.DockerManager;
import com.redhat.patriot.network_simulator.example.network.DockerNetwork;
import com.redhat.patriot.network_simulator.example.network.Network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * The type Docker controller.
 */
public class DockerController {
    private DockerClient dockerClient = DockerClientBuilder.
            getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder().build()).build();
    private DockerManager dockerManager = new DockerManager();

    /**
     * Genererate enviroment.
     */
    public void genererateEnviroment() {
        List<String> networks = new ArrayList<>();
        List<String> conts = new ArrayList<>();
        try {
            DockerManager dockerManager = new DockerManager();
            String tagApp = "app_test:01";
            String tagRouter  = "router_test:01";

            buildImages(tagApp, tagRouter);

            DockerNetwork serverNetwork =
                    (DockerNetwork) dockerManager.createNetwork("server_network", "172.22.0.0/16");
            networks.add(serverNetwork.getName());
            DockerNetwork clientNetwork =
                    (DockerNetwork) dockerManager.createNetwork("client_network", "172.23.0.0/16");
            networks.add(clientNetwork.getName());

            Container router = dockerManager.createContainer("router",tagRouter);
            conts.add(connectAndStart(dockerManager, router, Arrays.asList(clientNetwork, serverNetwork)));

            System.out.println(dockerManager.findIpAddress(router, serverNetwork));

            Container commClient = dockerManager.createContainer("comm_client", tagApp);
            conts.add(connectAndStart(dockerManager, commClient, Arrays.asList(clientNetwork)));

            Container commServer = dockerManager.createContainer("comm_server", tagApp);
            conts.add(connectAndStart(dockerManager, commServer, Arrays.asList(serverNetwork)));

            setGW(commClient, commServer, networks, router);

        } catch (Exception e ) {
            e.printStackTrace();
            Cleaner cleaner = new Cleaner(dockerClient);
            cleaner.cleanUp(networks, conts);
        }

    }

    /**
     * Connect containers to networks and start them.
     *
     * @param manager   the manager
     * @param container the container
     * @param networks  the networks
     * @return the string
     */
    String connectAndStart(DockerManager manager,Container container, List<Network> networks) {
        container.connectToNetwork(networks);
        manager.startContainer(container);
        return container.getName();
    }

    /**
     * Build images.
     *
     * @param tagApp       the tag app
     * @param tagRouter    the tag router
     */
    void buildImages(String tagApp, String tagRouter){
        DockerImage dockerImage = new DockerImage(dockerManager);
        dockerImage.buildImage(new HashSet<>(Arrays.asList(tagApp)), "app/Dockerfile");
        dockerImage.buildImage(new HashSet<>(Arrays.asList(tagRouter)), "router/Dockerfile");
    }


    /**
     * Setting default gw targeted to router`s ip, to provide communication only in simulated network without route to
     * outside world.
     *
     * @param client   the client
     * @param server   the server
     * @param networks the networks
     * @param router   the router
     */
    void setGW(Container client, Container server, List<String> networks, Container router){

        dockerManager.runCommand(server, "./setGW " +
                dockerManager.findIpAddress(router, new DockerNetwork(networks.get(0))));

        dockerManager.runCommand(client, "./setGW " +
                dockerManager.findIpAddress(router, new DockerNetwork(networks.get(1))));

    }
}
