package mongodb;

import com.mongodb.*;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDB {
public static void main(String[] args){
    String CONNECTION_STRING = "mongodb+srv://amaliamirisan:amaliamirisan@minidbms.gk207.mongodb.net/?retryWrites=true&w=majority&appName=miniDBMS";

    ServerApi serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build();

    MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString((CONNECTION_STRING)))
            .serverApi(serverApi)
            .build();

    try (MongoClient mongoClient = MongoClients.create(settings)) {
        try {
            // Send a ping to confirm a successful connection
            MongoDatabase database = mongoClient.getDatabase("admin");
            database.runCommand(new Document("ping", 1));
            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
        } catch (MongoException e) {
            e.printStackTrace();
        }
}
    /*private static final String CONNECTION_STRING = "mongodb+srv://amaliamirisan:amaliamirisan@minidbms.gk207.mongodb.net/?retryWrites=true&w=majority&appName=miniDBMS";

   public static MongoClient connectToCluster() {
       return MongoClients.create(CONNECTION_STRING);
   }

   public static MongoDatabase getDatabase(String dbName) {
       MongoClient mongoClient = connectToCluster();
       return mongoClient.getDatabase(dbName);
   }*/
}
}
