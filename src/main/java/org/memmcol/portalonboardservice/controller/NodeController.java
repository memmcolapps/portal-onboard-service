package org.memmcol.portalonboardservice.controller;

import org.memmcol.portalonboardservice.model.node.*;
import org.memmcol.portalonboardservice.service.node.NodeService;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler.SQLServerException;
import org.memmcol.portalonboardservice.util.ResponseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/gfPortal/node/service")
public class NodeController {

    @Autowired private NodeService nodeService;
    @Autowired private ResponseProperties status;
    @Autowired private GlobalExceptionHandler exception;


    @PostMapping("/create/node/region-bhub-service-center")
    public ResponseEntity<Map<String, Object>> createRegionBhubServiceCenterNode(@RequestBody RegionBhubServiceCenter request) {
        try {
            Map<String, Object> result = nodeService.createRegionBhubServiceCenterNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }


    @PutMapping("/update/node/region-bhub-service-center")
    public ResponseEntity<Map<String, Object>> updateRegionBhubServiceCenterNode(@RequestBody RegionBhubServiceCenter request) {
        try {
            Map<String, Object> result = nodeService.updateRegionBhubServiceCenterNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }


    @PostMapping("/create/node/substation-transformer-feeder-line")
    public ResponseEntity<Map<String, Object>> createSubStationFeederLineTransformerNode(@RequestBody SubStationTransformerFeederLine request) {
        try {
            Map<String, Object> result = nodeService.createSubStationFeederLineTransformerNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }


    @PutMapping("/update/node/substation-transformer-feeder-line")
    public ResponseEntity<Map<String, Object>> updateSubStationFeederLineTransformerNode(@RequestBody SubStationTransformerFeederLine request) {
        try {
            Map<String, Object> result = nodeService.updateSubStationFeederLineTransformerNode(request);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<Map<String, Object>> singleNode(@RequestParam UUID nodeId, @RequestParam UUID orgId) {

        try {
            Map<String, Object> result =  nodeService.singleNode(nodeId, orgId);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }


    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> fetchAllNodes(UUID orgId) {

        try {
            Map<String, Object> result =  nodeService.getAllNodes(orgId);

            return ResponseEntity.ok(result);
        } catch (SQLServerException e) {
            return handleException(e);
        }
    }




    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
