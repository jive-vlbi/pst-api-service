alter table if exists "pdm"."CalibrationObservation" add column "intents" varchar(255) array;
update  "pdm"."CalibrationObservation" set intents[0] = intent;
alter table "pdm"."CalibrationObservation" alter column "intents" set not null;
alter table "pdm"."CalibrationObservation" drop column "intent";
