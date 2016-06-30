import cloud.Server;

import cloud.Software;
import org.mwg.Callback;
import org.mwg.GraphBuilder;
import cloud.Cloud;
import org.mwg.Node;
import org.mwg.task.Task;

public class HelloWorld {

    public static void main(String[] args) {

        sampleModel model = new sampleModel(new GraphBuilder().withOffHeapMemory());
        model.graph().connect(result -> {

            //Test typed node creation
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
                    ((Software) soft0_t10).setLoad(50.0);
                    System.out.println(soft0_t10);
                }
            });

            System.out.println(soft0.getLoad());

            //Test find usage
            model.graph().findAll(0, 0, "clouds", cloudsResult -> {
                System.out.println(cloudsResult[0]);
            });
            model.graph().find(0, 0, "clouds", "name=Hello", cloudsResult -> {
                System.out.println(cloudsResult[0]);
            });

            Software[] softwares = model.findAllClouds(0, 0);
            System.out.println(softwares[0]);

            System.out.println(model.findClouds(0, 0, "name=Hello"));

            System.out.println(model.findClouds(0, 0, "name=NOOP"));

            //Test task usage
            Task t = model.graph().newTask();
            t.fromIndexAll("clouds")
                    .get("name")
                    .foreach(model.graph().newTask()
                            .then(context -> System.out.println(context.result()))
                    ).execute();

        });


    }

}
