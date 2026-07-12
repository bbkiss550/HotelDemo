package com.hotel.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoomTransferSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public RoomTransferSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_room_transfer (
                    "ID_room_transfer" BIGSERIAL PRIMARY KEY,
                    "ID_guest" BIGINT NOT NULL REFERENCES t_guest("ID_guest"),
                    "ID_from_room" BIGINT NOT NULL REFERENCES t_room("ID_room"),
                    "ID_to_room" BIGINT NOT NULL REFERENCES t_room("ID_room"),
                    transfer_date DATE NOT NULL DEFAULT CURRENT_DATE,
                    stay_type VARCHAR(50),
                    old_price NUMERIC(12,2) NOT NULL DEFAULT 0,
                    new_price NUMERIC(12,2) NOT NULL DEFAULT 0,
                    remark VARCHAR(1000),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_room_transfer_guest ON t_room_transfer(\"ID_guest\")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_room_transfer_from_room ON t_room_transfer(\"ID_from_room\")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_room_transfer_to_room ON t_room_transfer(\"ID_to_room\")");
    }
}
