/*
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.hotrodclient;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.redhat.hotrodclient.model.User;
import com.redhat.hotrodclient.serviceImpl.UserServiceImpl;

@SpringBootApplication
public class Application {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private Bus bus;

	@Value("${infinispan.server}")
	private String cacheServer;

	@Value("${infinispan.port}")
	private int cachePort;

	public static void main(String[] args) {

		ApplicationContext applicationContext = SpringApplication.run(Application.class, args);

		for (String name : applicationContext.getBeanDefinitionNames()) {
			System.out.println(name);
		}
	}

	@Bean
	public JacksonJaxbJsonProvider jacksonJaxbJsonProvider() {
		return new JacksonJaxbJsonProvider();
	}

	@Bean
	public Server rsServer() throws DescriptorParserException, IOException {
		// setup CXF-RS
		JAXRSServerFactoryBean endpoint = new JAXRSServerFactoryBean();
		endpoint.setBus(bus);
		endpoint.setServiceBeans(Arrays.<Object>asList(new UserServiceImpl(remoteCacheContainer())));
		endpoint.setProvider(jacksonJaxbJsonProvider());
		endpoint.setAddress("/");
		endpoint.setFeatures(Arrays.asList(new Swagger2Feature()));
		return endpoint.create();
	}

	@Bean
	public RemoteCacheManager remoteCacheContainer() throws DescriptorParserException, IOException {

		Objects.requireNonNull(cacheServer, "Infinispan service host not found in the environment");
		Objects.requireNonNull(cachePort, "Infinispan service port not found in the environment");

		String hostPort = cacheServer + ":" + cachePort;
		logger.info("Connecting to the Infinispan service at {}", hostPort);

		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.addServer().host(cacheServer).port(cachePort).marshaller(new ProtoStreamMarshaller());

		RemoteCacheManager remoteCacheManager = new RemoteCacheManager(builder.create(), true);

		SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(remoteCacheManager);

		ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
		String userSchemaFile = protoSchemaBuilder.fileName("user.proto").packageName("quickstart").addClass(User.class)
				.build(ctx);

		// register the schemas with the server too
		RemoteCache<String, String> metadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
		metadataCache.clear();
		metadataCache.put("user.proto", userSchemaFile);
		String errors = metadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
		if (errors != null) {
			throw new IllegalStateException("Some Protobuf schema files contain errors:\n" + errors);
		}

		return remoteCacheManager;
	}

}
