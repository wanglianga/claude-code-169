package com.city.binroom.web;

import com.city.binroom.service.LinkageSweepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "管理运维")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LinkageSweepService sweepService;

    @Operation(summary = "手动触发联动巡检（断网/积分补发/超时升级等）")
    @PostMapping("/sweep")
    public Map<String, String> sweep() {
        return Map.of("result", sweepService.runSweep());
    }
}
