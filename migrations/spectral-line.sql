create table "pdm"."EVNSpectralLine" ("ID" bigint not null, "range_value" float(53) not null, "range_unit" varchar(255) not null, "resolution_value" float(53) not null, "resolution_unit" varchar(255) not null, "restFrequency_value" float(53) not null, "restFrequency_unit" varchar(255) not null, "shift_value" float(53) not null, "shift_unit" varchar(255) not null, "TECHNICALGOAL_ID" bigint, "spectralLine_ORDER" integer, primary key ("ID"));
create sequence "EVNSpectralLine_SEQ" start with 1 increment by 50;
alter table if exists "pdm"."EVNSpectralLine" add constraint "FKtcb3m4rxsy90p1rfw1amp0x32" foreign key ("TECHNICALGOAL_ID") references "pdm"."TechnicalGoal";

drop table "pdm"."ExpectedSpectralLine";
drop sequence public."ExpectedSpectralLine_SEQ";
drop table "pdm"."ScienceSpectralWindow";
drop sequence public."ScienceSpectralWindow_SEQ";
