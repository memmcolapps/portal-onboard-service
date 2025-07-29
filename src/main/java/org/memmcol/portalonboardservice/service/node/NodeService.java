package org.memmcol.portalonboardservice.service.node;

import org.memmcol.portalonboardservice.model.node.*;

import java.util.Map;
import java.util.UUID;

public interface NodeService {

    Map<String, Object> singleNode(UUID nodeId, UUID orgId);

    Map<String, Object> getAllNodes(UUID orgId);

    Map<String, Object> createRegionBhubServiceCenterNode(RegionBhubServiceCenter request);

    Map<String, Object> createSubStationFeederLineTransformerNode(SubStationTransformerFeederLine request);

    Map<String, Object> updateRegionBhubServiceCenterNode(RegionBhubServiceCenter request);

    Map<String, Object> updateSubStationFeederLineTransformerNode(SubStationTransformerFeederLine request);
}
