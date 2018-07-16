import com.redhat.patriot.network_simulator.example.container.Container;
import com.redhat.patriot.network_simulator.example.container.DockerContainer;
import com.redhat.patriot.network_simulator.example.image.DockerImage;
import com.redhat.patriot.network_simulator.example.manager.DockerManager;
import com.redhat.patriot.network_simulator.example.network.DockerNetwork;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConnTest {
    @Test
    public void TestIp() {
        DockerManager dockerManager = new DockerManager();
        DockerImage dockerImage = new DockerImage(dockerManager);
        String tag = "conn_test:01";
        dockerImage.buildImage(new HashSet<>(Arrays.asList(tag)), "app/Dockerfile");
        DockerContainer dockerContainer =
                (DockerContainer) dockerManager.createContainer("conn_test", tag);
        DockerNetwork dockerNetwork = (
                DockerNetwork) dockerManager.createNetwork("conn_test_network", "172.62.0.0/16");

        dockerContainer.connectToNetwork(Arrays.asList(dockerNetwork));
        dockerManager.startContainer(dockerContainer);
        System.out.println(dockerContainer.getIpAddress(dockerNetwork));
        assertTrue(dockerContainer.getIpAddress(dockerNetwork).contains("172.62.0"));

        dockerManager.destroyContainer(dockerContainer);
        dockerManager.destroyNetwork(dockerNetwork);
    }


}
