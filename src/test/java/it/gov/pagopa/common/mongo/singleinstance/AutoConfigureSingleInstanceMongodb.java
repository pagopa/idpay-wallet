package it.gov.pagopa.common.mongo.singleinstance;

import de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.mongodb.test.autoconfigure.AutoConfigureDataMongo;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** It will enable the usage of a single instance of {@link EmbeddedMongoAutoConfiguration}, dropping the database at each new Spring Context */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AutoConfigureDataMongo
@ImportAutoConfiguration(EmbeddedMongoAutoConfiguration.class)
public @interface AutoConfigureSingleInstanceMongodb {
}
