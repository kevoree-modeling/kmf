import cloud.Server;
import org.mwg.Callback;
import org.mwg.Graph;
import org.mwg.GraphBuilder;
import cloud.Cloud;

public class HelloWorld {

    public static void main(String[] args) {

        sampleModel model = new sampleModel(new GraphBuilder());
        model.graph().connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
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
            }
        });


    }

}
