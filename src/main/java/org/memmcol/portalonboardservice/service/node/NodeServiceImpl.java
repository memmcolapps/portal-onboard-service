package org.memmcol.portalonboardservice.service.node;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.portalonboardservice.components.GenericHandler;
import org.memmcol.portalonboardservice.mapper.NodeMapper;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.memmcol.portalonboardservice.model.node.*;
import org.memmcol.portalonboardservice.repository.ExceptionAuditRepository;
import org.memmcol.portalonboardservice.service.auditlog.SafeAuditService;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.memmcol.portalonboardservice.config.ResponseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import static org.memmcol.portalonboardservice.components.handleValidUser.handleUserValidation;

@Service
public class NodeServiceImpl implements NodeService {
    private static final Logger log = LoggerFactory.getLogger(NodeServiceImpl.class);

    @Autowired
    private NodeMapper nodeMapper;

    @Autowired
    private ResponseProperties status;

    @Autowired
    private SafeAuditService safeAuditService;

    @Autowired
    private GenericHandler genericHandler;

    @Autowired
    private ExceptionAuditRepository exceptionAuditRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private final IMap<String, Node> nodeCache;

    private final IMap<String, AuditLog> auditCache;

    public NodeServiceImpl(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.nodeCache = hazelcastInstance.getMap("portalNodeCache");
        this.auditCache = hazelcastInstance.getMap("portalAuditCache");
    }

    @Transactional
    @Override
    public Map<String, Object> createRegionBhubServiceCenterNode(RegionBhubServiceCenter request) {
        RegionBhubServiceCenter regionBhubServiceCenter;
        UUID id;
        try {

            handleRegionPayloadCheck(request);
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            Operator um = handleUserValidation();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(request.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Parent node does not exist");
            }

            RegionBhubServiceCenter duplicate = nodeMapper.verifyNode(request.getRegionId(), request.getOrgId(), request.getType());

            if (duplicate != null && request.getType().equalsIgnoreCase(duplicate.getType())
                    && duplicate.getRegionId().equalsIgnoreCase(request.getRegionId())) {
                throw new GlobalExceptionHandler.NotFoundException(
                        request.getType().substring(0, 1).toUpperCase()
                                + request.getType().substring(1).toLowerCase()
                                + " already exists for region ID (" + request.getRegionId() + ")"
                );
            }

//            if(n != null){
//                if (n.getRegionId().equalsIgnoreCase(request.getRegionId())){
//                    throw new GlobalExceptionHandler.NotFoundException("Region ID ("+ request.getRegionId()+") " + status.getExistDesc());
//                }
//            }

            if (Boolean.TRUE.equals(nodeMapper.existsByRegionEmailExcludingCurrent(request.getEmail()))) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Email (" + request.getEmail() + ") already been used"
                );
            }

            RegionBhubServiceCenter rgBhubService = nodeMapper.getBhubByOrgIdAndName(request.getName(), request.getOrgId());
            if (rgBhubService != null){
                String type = request.getType().toLowerCase();

                switch (type){
                    case "region":
                        throw new GlobalExceptionHandler.NotFoundException("Region Name (" + request.getName()+") " + status.getExistDesc() +" for a "+rgBhubService.getType());
                    case "business hub":
                        throw new GlobalExceptionHandler.NotFoundException("Business Name (" + request.getName()+") " + status.getExistDesc() +" for a "+rgBhubService.getType());
                    case "service center":
                        throw new GlobalExceptionHandler.NotFoundException("Service Name (" + request.getName()+") " + status.getExistDesc() +" for a "+rgBhubService.getType());

                    default:
                        throw new GlobalExceptionHandler.NotFoundException("Node Name (" + request.getName()+") " + status.getExistDesc() +" for a "+rgBhubService.getType());
                }
            }

//            RegionBhubServiceCenter duplicate = nodeMapper.verifyNode(request.getRegionId(), request.getOrgId());
//            if (duplicate != null && request.getType().equalsIgnoreCase(duplicate.getType())) {
//                throw new GlobalExceptionHandler.NotFoundException(
//                        request.getType().substring(0, 1).toUpperCase()
//                                + request.getType().substring(1).toLowerCase()
//                                + " already exists for Region ID (" + request.getRegionId() + ")"
//                );
//            }

//            if (Boolean.TRUE.equals(nodeMapper.existsByRegionEmail(request.getEmail(), request.getOrgId()))) {
//                throw new GlobalExceptionHandler.NotFoundException(
//                        "Email (" + request.getEmail() + ") already been used"
//                );
//            }

            nodeMapper.createNode(node);

            UUID nodeId = node.getId();
            UUID parentNodeId = node.getParentId();

            request.setNodeId(nodeId);
            request.setParentId(parentNodeId);

            if(request.getType().equalsIgnoreCase("region") ||
                    request.getType().equalsIgnoreCase("business hub") ||
                    request.getType().equalsIgnoreCase("service center")){

//                boolean emailExists = nodeMapper.existByEmail(request.getEmail());
//                if(Boolean.TRUE.equals(emailExists)){
//                    throw new GlobalExceptionHandler.NotFoundException("Email " + status.getExistDesc() + " For a Customer");
//                }
                request.setEmail(
                        StringUtils.isBlank(request.getEmail()) ? null : request.getEmail()
                );
                nodeMapper.createRegionBhubServiceCenter(request);
                id = request.getNodeId();
                regionBhubServiceCenter = nodeMapper.getRegionBhubServiceCenter(id);
                desc = regionBhubServiceCenter.getName() + "newly created";
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }
//            RegionBhubServiceCenter regionBhubServiceCenter1 = nodeMapper.getRegionBhubServiceCenter(request.getId());
//            handleClearCache(node);

            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equals("region") ? "region" : request.getType().equals("service center") ? "service center" : "business hub", regionBhubServiceCenter, metadata);
//            auditRepository.save(auditLog);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(),
                    "Node "+ status.getRegDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "creating region node");
            throw exception;
        }
    }

    @Transactional
    @Override
    public Map<String, Object> createSubStationFeederLineTransformerNode(SubStationTransformerFeederLine request) {

        SubStationTransformerFeederLine subStationTransformerFeederLine;
        UUID id;
        try {

            handlePayloadCheck(request);
            String desc;
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            Operator um = handleUserValidation();

            Node node = new Node();
            node.setName(request.getName());
            node.setOrgId(request.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getParentId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Parent node does not exist");
            }

            SubStationTransformerFeederLine sub = nodeMapper.verifySubNode(
                    request.getAssetId(), request.getOrgId(),  request.getType());
            if(sub != null && sub.getType().equalsIgnoreCase(request.getType())
                    && sub.getAssetId().equalsIgnoreCase(request.getAssetId())){
                throw new GlobalExceptionHandler.NotFoundException(
                        request.getType().substring(0, 1).toUpperCase()
                                + request.getType().substring(1).toLowerCase()
                                + " already exists for asset ID (" + request.getAssetId() + ")");
//                throw new GlobalExceptionHandler.NotFoundException("Asset ID ("+ request.getAssetId()+") " + status.getExistDesc() +" for a "+ request.getType());
            }

            if (Boolean.TRUE.equals(nodeMapper.existsBySerial(request.getSerialNo(), request.getOrgId(), request.getType().toLowerCase()))) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Serial No (" + request.getSerialNo() + ") " + status.getExistDesc());
            }

            if (Boolean.TRUE.equals(nodeMapper.existsByEmail(request.getEmail()))) {
                throw new GlobalExceptionHandler.NotFoundException(
                        "Email (" + request.getEmail() + ") already been used"
                );
            }

            SubStationTransformerFeederLine subTransFeeder = nodeMapper.getSubTransformerFeederLineByOrgIdAndName(request.getOrgId(), request.getName());
            if (subTransFeeder != null){

                String type = request.getType().toLowerCase();
                switch (type){
                    case "dss":
                        throw new GlobalExceptionHandler.NotFoundException("DSS Name ("+ request.getName()+") " + status.getExistDesc() +" for a "+subTransFeeder.getType());
                    case "feeder line":
                        throw new GlobalExceptionHandler.NotFoundException("Feeder line Name ("+ request.getName()+") " + status.getExistDesc() +" for a "+subTransFeeder.getType());
                    case "substation":
                        throw new GlobalExceptionHandler.NotFoundException("Substation Name (" + request.getName()+") " + status.getExistDesc() +" for a "+subTransFeeder.getType());

                    default:
                        throw new GlobalExceptionHandler.NotFoundException("Node Name (" + request.getName()+") " + status.getExistDesc() +" for a "+subTransFeeder.getType());
                }

            }


            nodeMapper.createNode(node);

            UUID nodeId = node.getId();
            UUID parentNodeId = node.getParentId();

            request.setNodeId(nodeId);
            request.setParentId(parentNodeId);

            if(request.getType().equalsIgnoreCase("dss") ||
                    request.getType().equalsIgnoreCase("feeder line") ||
                    request.getType().equalsIgnoreCase("substation")){
                request.setEmail(
                        StringUtils.isBlank(request.getEmail()) ? null : request.getEmail()
                );
                nodeMapper.createSubStationTransformerFeederLine(request);
                id = request.getNodeId();
                subStationTransformerFeederLine = nodeMapper.getSubStationTransformerFeederLine(id);
                desc = subStationTransformerFeederLine.getName() + "newly created";
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }
//            handleClearCache(node);

            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equals("dss") ? "dss" : request.getType().equals("feeder line") ? "feeder line" : "substation", subStationTransformerFeederLine, metadata);
//            auditRepository.save(auditLog);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+ status.getRegDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "creating substation, feeder line & dss");
            throw exception;
        }
    }

    private void handlePayloadCheck(SubStationTransformerFeederLine request) {
        if (request.getAssetId() == null || request.getAssetId().trim().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Asset ID is required");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Name is required");
        }

        if (request.getSerialNo() == null || request.getSerialNo().trim().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Serial No is required");
        }

        if (request.getStatus() == null) {
            throw new GlobalExceptionHandler.NotFoundException("Status is required");
        }

        if (request.getVoltage() == null || request.getVoltage().trim().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Voltage is required");
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateRegionBhubServiceCenterNode(RegionBhubServiceCenter request) {
        RegionBhubServiceCenter regionBhubServiceCenter;
        try {
            handleRegionPayloadCheck(request);
            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            Operator um = handleUserValidation();

            Node node = new Node();
            node.setId(request.getNodeId());
            node.setName(request.getName());
            node.setOrgId(request.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getNodeId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Node does not exist");
            }

            RegionBhubServiceCenter existingRecord = nodeMapper.getRegionBhubServiceCenter(request.getNodeId());

            if (existingRecord == null) {
                throw new GlobalExceptionHandler.NotFoundException("Region/Business Hub/Service Center record not found");
            }

            // Validate Region ID + Type combination (only if regionId changed)
            if (request.getRegionId() != null && !request.getRegionId().isEmpty() &&
                    !request.getRegionId().equals(existingRecord.getRegionId())) {

                if (Boolean.TRUE.equals(nodeMapper.existsByRegionIdAndTypeExcludingCurrent(
                        request.getRegionId(), request.getOrgId(), request.getType(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            request.getType().substring(0, 1).toUpperCase()
                                    + request.getType().substring(1).toLowerCase()
                                    + " already exists for Region ID (" + request.getRegionId() + ")"
                    );
                }
            }

            // Validate Email (only if email changed)
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {

                Boolean emailTaken =
                        nodeMapper.existsByEmailExcludingNode(
                                request.getEmail(),
                                request.getNodeId()
                        );

                if (Boolean.TRUE.equals(emailTaken)) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Email (" + request.getEmail() + ") already been used"
                    );
                }
            }

            // Validate Name (only if name changed)

            if (request.getName() != null && !request.getName().isEmpty() &&
                    !request.getName().equals(existingRecord.getName())) {

                if (Boolean.TRUE.equals(nodeMapper.existsByNameExcludingCurrent(
                        request.getName(), request.getOrgId(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Node Name (" + request.getName() + ") " + status.getExistDesc()
                    );
                }
            }

//            // Validate Phone Number (only if phone number changed)
//            if (request.getPhoneNo() != null && !request.getPhoneNo().isEmpty() &&
//                    !request.getPhoneNo().equals(existingRecord.getPhoneNo())) {
//
//                if (Boolean.TRUE.equals(nodeMapper.existsByPhoneNumberExcludingCurrent(
//                        request.getPhoneNo(), request.getOrgId(), request.getNodeId()))) {
//                    throw new GlobalExceptionHandler.NotFoundException(
//                            "Phone Number (" + request.getPhoneNo() + ") already been used"
//                    );
//                }
//            }

            nodeMapper.updateNode(node);

            UUID nodeId = node.getId();

            request.setNodeId(nodeId);

            if(request.getType().equalsIgnoreCase("region") ||
                    request.getType().equalsIgnoreCase("business hub") ||
                    request.getType().equalsIgnoreCase("service center") ||
                    request.getType().equalsIgnoreCase("root")) {
                request.setEmail(
                        StringUtils.isBlank(request.getEmail()) ? null : request.getEmail()
                );
                nodeMapper.updateRegionBhubServiceCenter(request);
                regionBhubServiceCenter = nodeMapper.getRegionBhubServiceCenter(request.getNodeId());
                desc = regionBhubServiceCenter.getName()  + " edited";
            }  else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }

//            handleClearCache(node);
            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equals("region") ? "Region" : request.getType().equals("service center") ? "Service center" : "Business hub", regionBhubServiceCenter, metadata);
//            auditRepository.save(auditLog);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+ status.getUpdateDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "editing region, business hub line or service center");
            throw exception;
        }
    }

    private void handleRegionPayloadCheck(RegionBhubServiceCenter request) {
        if (request.getRegionId() == null || request.getRegionId().trim().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Region ID is required");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new GlobalExceptionHandler.NotFoundException("Name is required");
        }
    }

    @Transactional
    @Override
    public Map<String, Object> updateSubStationFeederLineTransformerNode(SubStationTransformerFeederLine request) {
        SubStationTransformerFeederLine subStationTransformerFeederLine;

        try {
            handlePayloadCheck(request);

            Map<String, String> metadata = genericHandler.extractRequestMetadata(httpServletRequest);
            String desc;
            Operator um = handleUserValidation();


            Node node = new Node();
            node.setId(request.getNodeId());
            node.setName(request.getName());
            node.setOrgId(request.getOrgId());
            node.setParentId(request.getParentId());

            Node nd = nodeMapper.isNodeExist(request.getNodeId());

            if(nd == null) {
                throw new GlobalExceptionHandler.NotFoundException("Parent node does not exist");
            }

            SubStationTransformerFeederLine existingRecord = nodeMapper.getSubStationTransformerFeederLine(request.getNodeId());

            if (existingRecord == null) {
                throw new GlobalExceptionHandler.NotFoundException("Substation/Feeder/DSS record not found for the given node ID");
            }

            // Serial Number Validation - only check if value changed
            if (!request.getSerialNo().equals(existingRecord.getSerialNo())) {

                // Check if another record of same type has this serial (excluding current)
                if (Boolean.TRUE.equals(nodeMapper.existsBySerialForSameTypeExcludingCurrent(
                        request.getSerialNo(), request.getOrgId(), request.getType().toLowerCase(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Serial No (" + request.getSerialNo() + ") " + status.getExistDesc()
                                    + " for a " + request.getType());
                }
            }

            // Validate Email (only if email changed)
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {

                Boolean emailTaken =
                        nodeMapper.existsByEmailForDifferentNode(
                                request.getEmail(),
                                request.getNodeId()
                        );

                if (Boolean.TRUE.equals(emailTaken)) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Email (" + request.getEmail() + ") already been used"
                    );
                }
            }

            // Asset ID Validation - only check if value changed
            if (!request.getAssetId().equals(existingRecord.getAssetId())) {

                // Check if another record of same type has this assetId (excluding current)
                if (Boolean.TRUE.equals(nodeMapper.existsByAssetIdForSameTypeExcludingCurrent(
                        request.getAssetId(), request.getOrgId(), request.getType().toLowerCase(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Asset ID (" + request.getAssetId() + ") " + status.getExistDesc()
                                    + " for a " + request.getType());
                }
            }


            if (!request.getName().equalsIgnoreCase(existingRecord.getName())) {

                if (Boolean.TRUE.equals(nodeMapper.existsByNameExcludingCurrentName(
                        request.getName(), request.getOrgId(), request.getNodeId()))) {
                    throw new GlobalExceptionHandler.NotFoundException(
                            "Node Name (" + request.getName() + ") " + status.getExistDesc()
                    );
                }
            }

            nodeMapper.updateNode(node);

            UUID nodeId = node.getId();

            request.setUpdatedAt(LocalDateTime.now());
            request.setNodeId(nodeId);

            if(request.getType().equalsIgnoreCase("dss") ||
                    request.getType().equalsIgnoreCase("feeder line") ||
                    request.getType().equalsIgnoreCase("substation")){
                request.setEmail(
                        StringUtils.isBlank(request.getEmail()) ? null : request.getEmail()
                );
                nodeMapper.updateSubStationTransformerFeederLine(request);
//                id = request.getId();
                subStationTransformerFeederLine = nodeMapper.getSubStationTransformerFeederLine(request.getNodeId());
                desc = subStationTransformerFeederLine.getName()  + "edited";
            } else {
                throw new GlobalExceptionHandler.NotFoundException("Request type " +" ("+ request.getType()+" )"+ " not found");
            }

//            handleClearCache(node);

            AuditLog auditLog = buildAuditLog(um, desc, request.getType().equalsIgnoreCase("dss") ? "dss" : request.getType().equalsIgnoreCase("feeder line") ? "Feeder line" : "Substation", subStationTransformerFeederLine, metadata);
//            auditRepository.save(auditLog);
            safeAuditService.saveAudit(auditLog);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+ status.getUpdateDesc(), "");

        } catch (Exception exception) {
            log.error("Error occurred while creating node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "editing substation, feeder line or dss");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> singleNode(UUID nodeId, UUID orgId) {
        try {
            Operator um = handleUserValidation();
//
//            Object cachedUser = nodeCache.get(nodeId.toString() + "_" + um.getOrgId());
//            if (cachedUser != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached Node " + status.getDesc(), cachedUser);
//            }

            List<Node> flatList = nodeMapper.getNodeWithChildren(nodeId, orgId);
            if (flatList == null || flatList.isEmpty()) {
                return ResponseMap.response(status.getSuccessCode(), "No nodes found", "");
            }

            Map<UUID, Node> nodeMap = new HashMap<>();
            Node root = null;

            for (Node node : flatList) {
                node.setNodesTree(new ArrayList<>());
                nodeMap.put(node.getId(), node);
            }

            for (Node node : flatList) {
                System.out.println("node id: " + nodeId);
                if (node.getId().equals(nodeId)) {
                    root = node; // this is the node we're querying for
                }
                if (node.getParentId() != null && nodeMap.containsKey(node.getParentId())) {
                    Node parent = nodeMap.get(node.getParentId());
                    parent.getNodesTree().add(node);
                }
            }

            assert root != null;
//            handleAddCache(root);
            return ResponseMap.response(status.getSuccessCode(), "Node " + status.getDesc(), root);

        } catch (Exception exception) {
            log.error("Error occurred while fetching node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "fetching single node");
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAllNodes(UUID orgId) {
        try {

            Operator um = handleUserValidation();

//            StringBuilder cacheKeyBuilder = new StringBuilder("nodes_"+um.getOrgId());
//            String cacheKey = cacheKeyBuilder.toString();
//
////             Return from cache if available
//            Object cachedNode = nodeCache.get(cacheKey);
//            if (cachedNode != null) {
//                return ResponseMap.response(status.getSuccessCode(), "Cached Nodes " + status.getDesc(), cachedNode);
//            }

            List<Node> flatList =  nodeMapper.getAllNode(orgId);
            if(flatList == null || flatList.isEmpty()){
                return ResponseMap.response(status.getSuccessCode(), status.getDesc(), flatList);
            }
            Map<UUID, Node> nodeMap = new HashMap<>();
            List<Node> roots = new ArrayList<>();

            // Map nodes by ID
            for (Node node : flatList) {
                nodeMap.put(node.getId(), node);
                node.setNodesTree(new ArrayList<>()); // Initialize children list
            }

            // Reconstruct the tree
            for (Node node : flatList) {
                if (node.getParentId() == null) {
                    roots.add(node); // Add root nodes to the list
                } else {
                    Node parent = nodeMap.get(node.getParentId());
                    if (parent != null) {
                        parent.getNodesTree().add(node); // Add as a child to the parent
                    }
                }
            }

//            nodeCache.put(cacheKey, roots);

            return ResponseMap.response(status.getSuccessCode(),  "Node "+status.getDesc(), roots);
        } catch (Exception exception) {
            log.error("Error occurred while updated node [ACTION]: {}", exception.getMessage().trim(), exception);
            genericHandler.logAndSaveException(exception, "fetching nodes");
            throw exception;
        }
    }


    private void handleAddCache(Node node) {
        nodeCache.remove(node.getId().toString()+"_"+node.getOrgId());
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : nodeCache.keySet()) {
            if (key.startsWith("nodes_"+node.getOrgId())) {
                nodeCache.remove(key);
            }
        }
        nodeCache.put(node.getId().toString(), node);  // Cache updated or deleted entity
    }

    private void handleClearCache(Node node) {
        for (String key : auditCache.keySet()) {
            if (key.startsWith("grid_flex_audit_log_page_")) {
                auditCache.remove(key);
            }
        }
        for (String key : nodeCache.keySet()) {
            if (key.startsWith("nodes_"+node.getOrgId())) {
                nodeCache.remove(key);
            }
        }
    }

    private AuditLog buildAuditLog(Operator creator, String description, String type, Object createdEntity, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setCreator(creator);
        log.setDescription(description);
        log.setType(type);
        log.setRegionBhubServiceCenter(createdEntity instanceof RegionBhubServiceCenter ? (RegionBhubServiceCenter) createdEntity : null);
        log.setSubStationTransformerFeederLine(createdEntity instanceof SubStationTransformerFeederLine ? (SubStationTransformerFeederLine) createdEntity : null);
        log.setIpAddress(metadata.get("ipAddress"));
        log.setUserAgent(metadata.get("userAgent"));
        log.setEndPoint(metadata.get("endpoint"));
        log.setHttpMethod(metadata.get("httpMethod"));
        return log;
    }

}
