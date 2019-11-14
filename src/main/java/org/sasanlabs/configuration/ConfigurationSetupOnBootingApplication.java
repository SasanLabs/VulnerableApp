package org.sasanlabs.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.internal.utility.JSONSerializationUtils;
import org.sasanlabs.service.vulnerability.nosqlInjection.mongo.entities.HiddenEntity;
import org.sasanlabs.service.vulnerability.nosqlInjection.mongo.entities.UserEntity;
import org.sasanlabs.service.vulnerability.nosqlInjection.mongo.repository.HiddenRepository;
import org.sasanlabs.service.vulnerability.nosqlInjection.mongo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 *         Incase of you want to do some operations on server startup. Insert
 *         all the configuration loading code in this class.
 */
@Configuration
public class ConfigurationSetupOnBootingApplication {

	private UserRepository userRepository;
	private HiddenRepository hiddenRepository;
	private static final Logger LOGGER = LogManager.getLogger(ConfigurationSetupOnBootingApplication.class);

	@Autowired
	public ConfigurationSetupOnBootingApplication(UserRepository userRepository, HiddenRepository hiddenRepository) {
		this.userRepository = userRepository;
		this.hiddenRepository = hiddenRepository;
	}

	/**
	 * On Server Startup it added some dummy data into Mongo DB.
	 * 
	 * @param event
	 */
	@EventListener
	public void populateMongoDB(ApplicationReadyEvent event) {
		InputStream mongoUserEntityStream = this.getClass().getResourceAsStream("/scripts/mongoDB/mongoUserEntity.json");
		InputStream mongoHiddenEntityStream = this.getClass().getResourceAsStream("/scripts/mongoDB/mongoHiddenEntity.json");
		try {
			List<UserEntity> userEntities = JSONSerializationUtils.deserialize(mongoUserEntityStream,
					new TypeReference<List<UserEntity>>() {
					});
			userEntities.forEach(userEntity -> userEntity.setId(UUID.randomUUID().toString()));
			userRepository.insert(userEntities);
			HiddenEntity hiddenEntity = JSONSerializationUtils.deserialize(mongoHiddenEntityStream, HiddenEntity.class);
			hiddenRepository.insert(hiddenEntity);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

}
