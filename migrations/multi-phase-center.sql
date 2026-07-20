alter table if exists "pdm"."Target" add column "VLBIOBSERVATION_ID" bigint;
alter table if exists "pdm"."Target" add constraint "FKhi9jotqsd0ci5qudyn41gdtxf" foreign key ("VLBIOBSERVATION_ID") references "pdm"."VlbiObservation";
