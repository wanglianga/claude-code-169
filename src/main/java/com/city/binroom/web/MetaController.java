package com.city.binroom.web;

import com.city.binroom.domain.repo.BinRoomRepository;
import com.city.binroom.domain.repo.BuildingRepository;
import com.city.binroom.domain.repo.CommunityRepository;
import com.city.binroom.web.dto.ViewMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 基础元数据（供登录页与下拉选择，免鉴权） */
@Tag(name = "元数据")
@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    private final CommunityRepository communityRepo;
    private final BinRoomRepository roomRepo;
    private final BuildingRepository buildingRepo;
    private final ViewMapper viewMapper;

    @Operation(summary = "小区列表")
    @GetMapping("/communities")
    public List<?> communities() {
        return communityRepo.findAll().stream().map(viewMapper::community).toList();
    }

    @Operation(summary = "全部垃圾房及实时状态")
    @GetMapping("/rooms")
    public List<?> rooms() {
        return roomRepo.findAll().stream().map(viewMapper::room).toList();
    }

    @Operation(summary = "楼栋列表")
    @GetMapping("/buildings")
    public List<?> buildings() {
        return buildingRepo.findAll().stream()
                .map(b -> java.util.Map.of(
                        "id", b.getId(),
                        "name", b.getName(),
                        "communityId", b.getCommunity().getId()))
                .toList();
    }
}
