package com.hotel.config;

import com.hotel.model.AppUser;
import com.hotel.model.Floor;
import com.hotel.model.Gender;
import com.hotel.model.Menu;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.repository.AppUserRepository;
import com.hotel.repository.FloorRepository;
import com.hotel.repository.GenderRepository;
import com.hotel.repository.MenuRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seedData(RoomRepository rooms, AppUserRepository users, GenderRepository genders, FloorRepository floors, RoomTypeRepository roomTypes, MenuRepository menus, PasswordEncoder encoder) {
        return args -> {
            Gender male = genders.findByName("ชาย").orElseGet(() -> {
                Gender gender = new Gender();
                gender.setName("ชาย");
                return genders.save(gender);
            });
            Gender female = genders.findByName("หญิง").orElseGet(() -> {
                Gender gender = new Gender();
                gender.setName("หญิง");
                return genders.save(gender);
            });
            Gender other = genders.findByName("อื่น ๆ").orElseGet(() -> {
                Gender gender = new Gender();
                gender.setName("อื่น ๆ");
                return genders.save(gender);
            });

            if (!users.existsByUsername("admin")) {
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin123"));
                admin.setFullName("ผู้ดูแลระบบ");
                admin.setGender(other);
                admin.setBirthDate(LocalDate.of(1990, 1, 1));
                users.save(admin);
            } else {
                users.findByUsername("admin").ifPresent(admin -> {
                    if (admin.getGender() == null) {
                        admin.setGender(other);
                        users.save(admin);
                    }
                });
            }

            Floor firstFloor = seedFloor(floors, "ชั้น 1", 1, 10);
            Floor secondFloor = seedFloor(floors, "ชั้น 2", 2, 20);
            RoomType standard = seedRoomType(roomTypes, "Standard", new BigDecimal("500"), new BigDecimal("6500"), "ห้องมาตรฐานสำหรับเข้าพักทั่วไป");
            RoomType deluxe = seedRoomType(roomTypes, "Deluxe", new BigDecimal("750"), new BigDecimal("9000"), "ห้องขนาดใหญ่ขึ้น พร้อมสิ่งอำนวยความสะดวกเพิ่มเติม");

            if (rooms.count() == 0) {
                for (int i = 1; i <= 20; i++) {
                    Room room = new Room();
                    room.setRoomNumber(String.format("%03d", i));
                    room.setRoomType(i <= 10 ? standard : deluxe);
                    room.setFloor(i <= 10 ? firstFloor : secondFloor);
                    room.setNightlyPrice(room.getRoomType().getNightlyPrice());
                    room.setMonthlyPrice(room.getRoomType().getMonthlyPrice());
                    rooms.save(room);
                }
            } else {
                rooms.findAll().stream()
                        .filter(room -> room.getFloor() == null)
                        .forEach(room -> {
                            int floorNumber = room.getLegacyFloor() == null ? inferFloorNumber(room.getRoomNumber()) : room.getLegacyFloor();
                            Floor floor = seedFloor(floors, "ชั้น " + floorNumber, floorNumber, floorNumber * 10);
                            room.setFloor(floor);
                            rooms.save(room);
                        });
                rooms.findAll().stream()
                        .filter(room -> room.getRoomType() == null)
                        .forEach(room -> {
                            String typeName = room.getLegacyRoomType() == null || room.getLegacyRoomType().isBlank() ? "Standard" : room.getLegacyRoomType();
                            RoomType roomType = roomTypes.findByNameIgnoreCase(typeName)
                                    .orElseGet(() -> seedRoomType(roomTypes, typeName, room.getNightlyPrice(), room.getMonthlyPrice(), ""));
                            room.setRoomType(roomType);
                            room.setNightlyPrice(roomType.getNightlyPrice());
                            room.setMonthlyPrice(roomType.getMonthlyPrice());
                            rooms.save(room);
                        });
            }

            seedMenu(menus, "bi-grid-fill", "Dashboard", "/", 10);
            seedMenu(menus, "bi-door-open-fill", "จัดการห้องพัก", "/rooms", 20);
            seedMenu(menus, "bi-house-gear-fill", "ประเภทห้อง", "/room-types", 25);
            seedMenu(menus, "bi-clipboard2-pulse-fill", "ข้อมูลสถานะห้องพัก", "/guests", 30);
            seedMenu(menus, "bi-credit-card-fill", "ชำระเงิน", "/payments", 40);
            seedMenu(menus, "bi-person-gear", "ผู้ใช้งาน", "/users", 50);
        };
    }

    private void seedMenu(MenuRepository menus, String icon, String name, String link, Integer sortOrder) {
        Menu menu = menus.findByLink(link).orElseGet(Menu::new);
        menu.setIcon(icon);
        menu.setName(name);
        menu.setLink(link);
        menu.setSortOrder(sortOrder);
        menus.save(menu);
    }

    private Floor seedFloor(FloorRepository floors, String name, Integer number, Integer sortOrder) {
        Floor floor = floors.findByNumber(number).orElseGet(Floor::new);
        floor.setName(name);
        floor.setNumber(number);
        floor.setSortOrder(sortOrder);
        return floors.save(floor);
    }

    private RoomType seedRoomType(RoomTypeRepository roomTypes, String name, BigDecimal nightlyPrice, BigDecimal monthlyPrice, String detail) {
        RoomType roomType = roomTypes.findByNameIgnoreCase(name).orElseGet(RoomType::new);
        roomType.setName(name);
        roomType.setNightlyPrice(nightlyPrice == null ? BigDecimal.ZERO : nightlyPrice);
        roomType.setMonthlyPrice(monthlyPrice == null ? BigDecimal.ZERO : monthlyPrice);
        roomType.setDetail(detail);
        return roomTypes.save(roomType);
    }

    private int inferFloorNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.isBlank()) {
            return 1;
        }
        try {
            int parsed = Integer.parseInt(roomNumber);
            return Math.max(1, parsed / 100);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }
}
