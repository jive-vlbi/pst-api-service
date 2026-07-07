create table "pdm"."CorrelatorParameters" ("ID" bigint not null, "doCrossHands" boolean not null, "fieldOfView_value" float(53) not null, "fieldOfView_unit" varchar(255) not null, primary key ("ID"));
create table "pdm"."PulsarGate" ("ID" bigint not null, "numberOfBins" integer not null, "phaseEnd_value" float(53) not null, "phaseEnd_unit" varchar(255) not null, "phaseStart_value" float(53) not null, "phaseStart_unit" varchar(255) not null, "CORRELATORPARAMETERS_ID" bigint, "pulsarGates_ORDER" integer, primary key ("ID"));
alter table if exists "pdm"."TechnicalGoal" add column "correlatorParameters_ID" bigint;
alter table if exists "pdm"."TechnicalGoal" drop constraint if exists "UKn5mp9f45w8kwdyf36xutkhq0k";
alter table if exists "pdm"."TechnicalGoal" add constraint "UKn5mp9f45w8kwdyf36xutkhq0k" unique ("correlatorParameters_ID");
create sequence "CorrelatorParameters_SEQ" start with 1 increment by 50;
create sequence "PulsarGate_SEQ" start with 1 increment by 50;
alter table if exists "pdm"."PulsarGate" add constraint "FKqwj03vyc0kth0k4j3sdk34fx6" foreign key ("CORRELATORPARAMETERS_ID") references "pdm"."CorrelatorParameters";
alter table if exists "pdm"."TechnicalGoal" add constraint "FKqfwb2ak4y1unmhsj2jjqxscx4" foreign key ("correlatorParameters_ID") references "pdm"."CorrelatorParameters";

-- Populate correlator parameters for existing technical goals with 1 arcsec FoV and cross-hands enabled
DO $$
DECLARE
    tg RECORD;
    new_id bigint;
BEGIN
    FOR tg IN SELECT "ID" FROM pdm."TechnicalGoal" WHERE "correlatorParameters_ID" IS NULL LOOP
        new_id := nextval('public."CorrelatorParameters_SEQ"');
        INSERT INTO pdm."CorrelatorParameters" ("ID", "doCrossHands", "fieldOfView_value", "fieldOfView_unit")
        VALUES (new_id, true, 1.0, 'arcsec');
        UPDATE pdm."TechnicalGoal"
        SET "correlatorParameters_ID" = new_id
        WHERE "ID" = tg."ID";
    END LOOP;
END $$;