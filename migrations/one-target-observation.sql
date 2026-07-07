BEGIN;
 
ALTER TABLE IF EXISTS "pdm"."Observation"
    ADD COLUMN "target" bigint;
 
UPDATE "pdm"."Observation" o
SET "target" = first_target."target_ID"
FROM (
    SELECT DISTINCT ON ("Observation_ID")
        "Observation_ID",
        "target_ID"
    FROM public."Observation_Target"
    ORDER BY "Observation_ID", "target_ID"
) first_target
WHERE o."ID" = first_target."Observation_ID";
 
ALTER TABLE IF EXISTS "pdm"."Observation"
    ALTER COLUMN "target" SET NOT NULL;
 
ALTER TABLE IF EXISTS "pdm"."Observation"
    ADD CONSTRAINT "FKlpj38ddxi25cgkbkn0sbaogtj"
    FOREIGN KEY ("target")
    REFERENCES "pdm"."Target"("ID");

DROP TABLE IF EXISTS public."Observation_Target";

COMMIT;