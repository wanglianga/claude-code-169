package com.city.binroom.bootstrap;

import com.city.binroom.domain.entity.*;
import com.city.binroom.domain.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 种子数据：街道 / 小区 / 楼栋 / 垃圾房 / 居民 / 各端账号。
 * README 中列出的测试账号由此创建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataRunner implements ApplicationRunner {

    private final CommunityRepository communityRepo;
    private final BuildingRepository buildingRepo;
    private final BinRoomRepository binRoomRepo;
    private final ResidentRepository residentRepo;
    private final SysUserRepository sysUserRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (communityRepo.count() > 0) {
            return;
        }

        Community yg = community("阳光花园小区", "滨河街道", "宜居物业");
        Community ch = community("翠湖家园小区", "滨河街道", "宜居物业");
        communityRepo.save(yg);
        communityRepo.save(ch);

        BinRoom r1 = room(yg, "BR-001", "阳光花园1号垃圾房", "3号楼北侧");
        BinRoom r2 = room(yg, "BR-002", "阳光花园2号垃圾房", "7号楼南侧");
        BinRoom r3 = room(ch, "BR-003", "翠湖家园1号垃圾房", "东门商业街旁");
        binRoomRepo.save(r1);
        binRoomRepo.save(r2);
        binRoomRepo.save(r3);

        Building b3 = building(yg, "3号楼");
        Building b7 = building(yg, "7号楼");
        Building b1 = building(ch, "1号楼");
        buildingRepo.save(b3);
        buildingRepo.save(b7);
        buildingRepo.save(b1);

        residentRepo.save(resident("R1001", "张伟", b3, "13800000001"));
        residentRepo.save(resident("R1002", "李娜", b3, "13800000002"));
        residentRepo.save(resident("R1003", "王强", b7, "13800000003"));
        residentRepo.save(resident("R2001", "赵敏", b1, "13800000004"));

        sysUserRepo.save(user("property", "property123", "PROPERTY", "物业管家-小陈", yg.getId()));
        sysUserRepo.save(user("sanitation", "sanitation123", "SANITATION", "环卫调度-老刘", null));
        sysUserRepo.save(user("street", "street123", "STREET", "街道城管-王科", null));
        sysUserRepo.save(user("admin", "admin123", "ADMIN", "系统管理员", null));

        log.info("种子数据初始化完成：2 个小区、3 个垃圾房、4 位居民、4 个工作人员账号");
    }

    private Community community(String name, String street, String company) {
        Community c = new Community();
        c.setName(name);
        c.setStreet(street);
        c.setPropertyCompany(company);
        return c;
    }

    private BinRoom room(Community c, String code, String name, String address) {
        BinRoom r = new BinRoom();
        r.setCommunity(c);
        r.setCode(code);
        r.setName(name);
        r.setAddress(address);
        r.setKitchenFillPct(20);
        r.setOtherFillPct(20);
        r.setOdorLevel(10);
        r.setOnline(true);
        r.setLastHeartbeatAt(LocalDateTime.now());
        r.setTruckOnSite(false);
        r.setNightPeak(false);
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private Building building(Community c, String name) {
        Building b = new Building();
        b.setCommunity(c);
        b.setName(name);
        return b;
    }

    private Resident resident(String cardNo, String name, Building b, String phone) {
        Resident r = new Resident();
        r.setCardNo(cardNo);
        r.setName(name);
        r.setBuilding(b);
        r.setPhone(phone);
        r.setPointsBalance(0);
        return r;
    }

    private SysUser user(String username, String pwd, String role, String displayName, Long communityId) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(pwd);
        u.setRole(role);
        u.setDisplayName(displayName);
        u.setCommunityId(communityId);
        return u;
    }
}
