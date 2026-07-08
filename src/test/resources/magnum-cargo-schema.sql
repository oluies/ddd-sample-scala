-- Schema for the Magnum CargoRepository PoC. Used by
-- MagnumCargoRepositoryTest against H2 in-memory.
--
-- A future Flyway/Liquibase migration would manage this for production.
-- For the PoC we ship it as a test resource and load it in @BeforeEach.
--
-- Table names follow Magnum's SqlNameMapper.CamelToSnakeCase mapping of
-- the row case-class names: `CargoRow` → `cargo_row`, `LegRow` → `leg_row`.

create table if not exists cargo_row (
    id                         bigint generated always as identity primary key,
    tracking_id                varchar(64) not null unique,
    origin_un_locode           varchar(5)  not null,
    spec_destination_un_locode varchar(5)  not null,
    spec_arrival_deadline      timestamp with time zone not null
);

create table if not exists leg_row (
    id               bigint generated always as identity primary key,
    cargo_id         bigint      not null,
    leg_index        int         not null,
    voyage_number    varchar(64) not null,
    load_un_locode   varchar(5)  not null,
    unload_un_locode varchar(5)  not null,
    load_time        timestamp with time zone not null,
    unload_time      timestamp with time zone not null,
    foreign key (cargo_id) references cargo_row (id) on delete cascade,
    unique (cargo_id, leg_index)
);
