package it.gov.pagopa.common.config;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

    @Component
    public class CustomMongoHealthIndicator implements HealthIndicator {

        @Value("${spring.data.mongodb.uri}")  // Legge l'URI di MongoDB dal file di configurazione
        private String mongoUri;

        @Override
        public Health health() {
            if (checkMongoConnectionWithPing()) {
                return Health.up().build();  // MongoDB è "up"
            } else {
                return Health.down().withDetail("Error", "MongoDB is down").build();  // MongoDB è "down"
            }
        }

        private boolean checkMongoConnectionWithPing() {
            try (MongoClient mongoClient = MongoClients.create(mongoUri)) {
                // Ottieni il database "admin" per eseguire comandi amministrativi come il ping
                MongoDatabase database = mongoClient.getDatabase("admin");

                // Esegui il comando "ping"
                Document pingCommand = new Document("ping", 1);
                Document result = database.runCommand(pingCommand);

                // Verifica che il ping sia andato a buon fine
                return result.getDouble("ok") == 1.0;
            } catch (Exception e) {
                e.printStackTrace();
                return false;  // Se c'è un'eccezione, significa che il ping ha fallito
            }
        }
    }
