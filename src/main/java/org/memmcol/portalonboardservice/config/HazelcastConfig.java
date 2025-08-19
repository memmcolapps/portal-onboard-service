package org.memmcol.portalonboardservice.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {
	@Value("${hazelcast.cluster-name}")
	private String clusterName;

	@Value("${hazelcast.network.port}")
	private int networkPort;

	@Value("${hazelcast.instance-name}")
	private String instanceName;

	@Value("${hazelcast.network.join.multicast.enabled}")
	private boolean multicastEnabled;

	@Autowired
	private NearCacheConfig nearCacheConfig;

	@Bean
	public HazelcastInstance hazelcastInstance() {
		Config config = new Config();
		config.setInstanceName(instanceName); // Or use a property for this as well
		config.setClusterName(clusterName);
		config.getNetworkConfig().setPort(networkPort);
		config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(multicastEnabled);

		// Configure OTP Cache (Expires in 60 Seconds)
		config.addMapConfig(new MapConfig("otpCache")
				.setTimeToLiveSeconds(60)
				.setMaxIdleSeconds(60)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		// Configure Verified Users Cache (Cleared After Password Change)
		config.addMapConfig(new MapConfig("verifiedUsers")
				.setTimeToLiveSeconds(60)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		// Configure Verified Users Cache (Cleared After Password Change)
		config.addMapConfig(new MapConfig("auditCache")
//				.setTimeToLiveSeconds(86400)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		// Configure Verified Users Cache (Cleared After Password Change)
		config.addMapConfig(new MapConfig("sbcCache")
//				.setTimeToLiveSeconds(86400)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));
		// Set up Near Cache
		config.getMapConfig("near-cache").setNearCacheConfig(nearCacheConfig);

		return Hazelcast.newHazelcastInstance(config);
	}
}

