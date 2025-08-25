package org.memmcol.portalonboardservice.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
//import org.memmcol.portalonboardservice.util.NodeCacheMapStore;
//import org.memmcol.portalonboardservice.util.OrganizationCacheMapStore;
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
		config.addMapConfig(new MapConfig("portalOtpCache")
				.setTimeToLiveSeconds(60)
				.setMaxIdleSeconds(60)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		config.addMapConfig(new MapConfig("portalOtpExpCache")
//				.setTimeToLiveSeconds(60)
				.setMaxIdleSeconds(60)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		// Configure Verified Users Cache (Cleared After Password Change)
		config.addMapConfig(new MapConfig("portalVerifiedUsers")
				.setTimeToLiveSeconds(60)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		// Configure Verified Users Cache (Cleared After Password Change)
		config.addMapConfig(new MapConfig("portalAuditCache")
//				.setTimeToLiveSeconds(86400)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		// File-based MapStore for portalNodeCache
//		MapStoreConfig portalNodeStoreConfig = new MapStoreConfig()
//				.setImplementation(new NodeCacheMapStore())
//				.setWriteDelaySeconds(0); // write-through persistence
//
//		config.addMapConfig(new MapConfig("portalNodeCache")
//				.setEvictionConfig(new EvictionConfig()
//						.setEvictionPolicy(EvictionPolicy.LRU)
//						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
//						.setSize(1000))
//				.setBackupCount(1)
//				.setMapStoreConfig(portalNodeStoreConfig));

//		config.addMapConfig(new MapConfig("portalNodeCache")
////				.setTimeToLiveSeconds(86400)
//				.setEvictionConfig(new EvictionConfig()
//						.setEvictionPolicy(EvictionPolicy.LRU)
//						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
//						.setSize(1000))
//				.setBackupCount(1));

		config.addMapConfig(new MapConfig("portalAuditCache")
//				.setTimeToLiveSeconds(86400)
				.setEvictionConfig(new EvictionConfig()
						.setEvictionPolicy(EvictionPolicy.LRU)
						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
						.setSize(1000))
				.setBackupCount(1));

		// File-based MapStore for portalNodeCache
//		MapStoreConfig organizationStoreConfig = new MapStoreConfig()
//				.setImplementation(new OrganizationCacheMapStore())
//				.setWriteDelaySeconds(0); // write-through persistence
//
//		config.addMapConfig(new MapConfig("organizationCache")
//				.setEvictionConfig(new EvictionConfig()
//						.setEvictionPolicy(EvictionPolicy.LRU)
//						.setMaxSizePolicy(MaxSizePolicy.PER_NODE)
//						.setSize(1000))
//				.setBackupCount(1)
//				.setMapStoreConfig(organizationStoreConfig));
		// Set up Near Cache
		config.getMapConfig("portalNearCache").setNearCacheConfig(nearCacheConfig);

		return Hazelcast.newHazelcastInstance(config);
	}
}

