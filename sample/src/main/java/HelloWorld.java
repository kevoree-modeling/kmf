import cloud.Server;
import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.GraphBuilder;
import cloud.Cloud;

public class HelloWorld {

    public static void main(String[] args) {
        Graph graph = new GraphBuilder().withPlugin(new samplePlugin()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Cloud cloud = (Cloud) graph.newTypedNode(0, 0, Cloud.NODE_NAME);
                Server server = (Server) graph.newTypedNode(0, 0, Server.NODE_NAME);
                server.setName("Hello");
                System.out.println(server.getName());
                System.out.println(server);

                cloud.addToServers(server);
                System.out.println(cloud);

                Server[] servers = cloud.getServers();
                System.out.println(servers);
                System.out.println(servers[0]);


            }
        });

    }

}
