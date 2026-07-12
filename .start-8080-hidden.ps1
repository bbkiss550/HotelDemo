$env:DB_URL='jdbc:postgresql://ep-restless-wildflower-aobuu3z2-pooler.c-2.ap-southeast-1.aws.neon.tech/db_hotel?sslmode=require&channelBinding=require'
$env:DB_USER='neondb_owner'
$env:DB_PASSWORD='npg_gHJQ3kbuiKf5'
& java -jar 'D:\project spingboot\00 HotelDemo\HotelSystem\target\hotel-system-0.0.1-SNAPSHOT.jar' *> 'app-8080.log'
