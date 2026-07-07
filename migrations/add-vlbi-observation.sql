create table "pdm"."VlbiObservation" ("ID" bigint not null, primary key ("ID"));
create table "pdm"."VlbiObservation_CheckSource" ("VlbiObservation_ID" bigint not null, "checkSource_ID" bigint not null);
create table "pdm"."VlbiObservation_PhaseReference" ("VlbiObservation_ID" bigint not null, "phaseReference_ID" bigint not null);
alter table if exists "pdm"."VlbiObservation" add constraint "FK77t7yeh5hma39gte8jj1rcg6y" foreign key ("ID") references "pdm"."Observation";
alter table if exists "pdm"."VlbiObservation_CheckSource" add constraint "FKt5rqpn9gtdpiospg3p83u6n1p" foreign key ("checkSource_ID") references "pdm"."Target";
alter table if exists "pdm"."VlbiObservation_CheckSource" add constraint "FKt1hj3i0f74l0dwy6bj4rq083l" foreign key ("VlbiObservation_ID") references "pdm"."VlbiObservation";
alter table if exists "pdm"."VlbiObservation_PhaseReference" add constraint "FKqfn8cfx6yvbn77k4j69w3r0ak" foreign key ("phaseReference_ID") references "pdm"."Target";
alter table if exists "pdm"."VlbiObservation_PhaseReference" add constraint "FK169pyoi0fqs8a7rpu4pffxumc" foreign key ("VlbiObservation_ID") references "pdm"."VlbiObservation";
