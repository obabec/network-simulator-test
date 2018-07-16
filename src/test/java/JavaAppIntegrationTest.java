import com.redhat.patriot.network_simulator.example.cleanup.Cleaner;
import com.redhat.patriot.network_simulator.example.container.Container;
import com.redhat.patriot.network_simulator.example.container.DockerContainer;
import com.redhat.patriot.network_simulator.example.image.DockerImage;
import com.redhat.patriot.network_simulator.example.manager.DockerManager;
import com.redhat.patriot.network_simulator.example.network.DockerNetwork;
import com.redhat.patriot.network_simulator.example.network.Network;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaAppIntegrationTest {

    @Test
    public void appTest() {
        DockerManager dockerManager = new DockerManager();
        DockerImage dockerImage = new DockerImage(dockerManager);
        String tag = "app_test:01";
        dockerImage.buildImage(new HashSet<>(Arrays.asList(tag)), "app/Dockerfile");
        String volumeClient = "/opt/client";
        String volumeServer = "/opt/server";
        String bindClient = System.getenv("HOME") + "/Documents/network_app/client";
        String bindServer = System.getenv("HOME") + "/Documents/network_app/server";

        DockerNetwork client_network =
                (DockerNetwork) dockerManager.createNetwork("client_network", "172.32.0.0/16");

        DockerContainer client =
                (DockerContainer) dockerManager.createContainer("client",tag, volumeClient, bindClient);
        connectAndStart(client, Arrays.asList(client_network), dockerManager);
        DockerNetwork server_network =
                (DockerNetwork) dockerManager.createNetwork("server_network", "172.64.0.0/16");
        DockerContainer server =
                (DockerContainer) dockerManager.createContainer("server", tag, volumeServer, bindServer);
        connectAndStart(server, Arrays.asList(server_network), dockerManager);

        DockerContainer router = (DockerContainer) dockerManager.createContainer("router", tag);
        connectAndStart(router, Arrays.asList(client_network, server_network), dockerManager);

        List<String> networks = Arrays.asList(client_network.getName(), server_network.getName());
        List<String> containers = Arrays.asList(client.getName(), server.getName(), router.getName());

        setGW(client, server, networks, router, dockerManager);

        dockerManager.runCommand(server, "java -jar /opt/server/app.jar");
        clientCallAfterTime(500, server.getIpAddress(server_network), dockerManager, client);

        assertTrue(waitForResponse());
        cleanUp(networks, containers);

    }

    void connectAndStart(DockerContainer container, List<Network> dockerNetworks, DockerManager dockerManager) {
            container.connectToNetwork(dockerNetworks);
            dockerManager.startContainer(container);
    }

    public void setGW(Container client, Container server, List<String> networks, Container router, DockerManager dockerManager){

        dockerManager.runCommand(server, "./setGW " +
                dockerManager.findIpAddress(router, new DockerNetwork(networks.get(1))));

        dockerManager.runCommand(client, "./setGW " +
                dockerManager.findIpAddress(router, new DockerNetwork(networks.get(0))));

    }

    void cleanUp(List<String> networks, List<String> containers) {
        Cleaner cleaner = new Cleaner();
        cleaner.cleanUp(networks, containers);
    }

    boolean waitForResponse() {
        try {
            ServerSocket listener = new ServerSocket(9090);
            String response;
            try {
                while (true) {
                    Socket socket = listener.accept();
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        response = in.readLine();
                        if(response.isEmpty()) {
                            return false;
                        } else {
                            return true;
                        }

                    } finally {
                        socket.close();
                    }
                }
            } finally {
                listener.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    void clientCallAfterTime(int time, String ipAddress, DockerManager dockerManager, DockerContainer client) {
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        dockerManager.runCommand(client, "java -jar /opt/client/app.jar "
                                + ipAddress);                    }
                },
                time
        );
    }



}
