import cloud.Server;

import cloud.Software;
import org.mwg.Callback;
import org.mwg.GraphBuilder;
import cloud.Cloud;
import org.mwg.Node;

public class HelloWorld {

    public static void main(String[] args) {

        sampleModel model = new sampleModel(new GraphBuilder().withOffHeapMemory());
        model.graph().connect(result -> {
            Cloud cloud = model.newCloud(0, 0);
            Server server = model.newServer(0, 0);
            server.setName("Hello");
            System.out.println(server.getName());
            System.out.println(server);

            cloud.addToServers(server);
            System.out.println(cloud);

            Server[] servers = cloud.getServers();
            System.out.println(servers);
            System.out.println(servers[0]);

            Software soft0 = model.newSoftware(0, 0);
            soft0.setName("Hello");
            soft0.setLoad(42.0);

            soft0.jump(10, new Callback<Node>() {
                @Override
                public void on(Node soft0_t10) {
                    ((Software)soft0_t10).setLoad(50.0);
                    System.out.println(soft0_t10);
                }
            });

            System.out.println(soft0.getLoad());



        });


    }

}
