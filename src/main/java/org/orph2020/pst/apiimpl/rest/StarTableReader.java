package org.orph2020.pst.apiimpl.rest;

import jakarta.ws.rs.WebApplicationException;
import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.Target;
import org.ivoa.dm.stc.coords.Epoch;
import org.ivoa.dm.stc.coords.EquatorialPoint;
import org.ivoa.dm.stc.coords.SpaceSys;
import org.ivoa.vodml.stdtypes.Unit;
import uk.ac.starlink.table.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StarTableReader {
    
    /**
     * Converts a string representation of coordinates in hours/degrees format to decimal degrees.
     * 
     * @param value The string to parse (e.g., "12:34:56.78" or "-12h34m56.78s")
     * @param mode Either "hms" for hours/minutes/seconds or "dms" for degrees/minutes/seconds
     * @return The degree value, or null if parsing fails
     */
    public static Double xmsToDeg(String value, String mode) {
        // match strings like +21:12:34.56, 21 12 34.56 or 12h34m56.78s if mode is "hms"
        Pattern regexp;
        if (mode.equals("hms")) {
            regexp = Pattern.compile("(?<sign>[+-])?(?<hd>\\d{1,2})[: hH](?<m>\\d{1,2})[: mM](?<s>\\d{1,2}(\\.\\d+)?)[sS]?");
        }
        else {
            regexp = Pattern.compile("(?<sign>[+-])?(?<hd>\\d{1,2})[: ](?<m>\\d{1,2})[: ](?<s>\\d{1,2}(\\.\\d+)?)");
        }

        Matcher matcher = regexp.matcher(value);
        if (!matcher.matches()) {
            return null;
        }

        double ret = Integer.parseInt(matcher.group("hd"));
        ret += Integer.parseInt(matcher.group("m")) / 60.0;
        ret += Double.parseDouble(matcher.group("s")) / 3600.0;

        if (mode.equals("hms")) {
            ret *= 15;
        }
        if (matcher.group("sign") != null && matcher.group("sign").equals("-")) {
            ret *= -1;
        }
        return ret;
    }

    public static List<Target> convertToListOfTargets(
            String resource,
            SpaceSys spaceSys,
            List<String> existingNames
    )
            throws WebApplicationException {
        List<Target> targets = new ArrayList<>();

        try (StarTable starTable = loadStarTable(resource)) {

            int nCol = starTable.getColumnCount();

            if (nCol == 0) {
                throw new WebApplicationException("table has zero columns", 400);
            }
            else if (nCol < 3) {
                throw new WebApplicationException("table is required to have at least 3 columns", 400);
            }

            long nRow = getRows(starTable);

            if (nRow == -1) {
                throw new WebApplicationException("unable to determine number of rows", 400);
            }

            if (nRow == 0) {
                throw new WebApplicationException("table has zero rows (no data)", 400);
            }

            // NAME, RA_d, Dec_d, [PMRA, PMDEC, PLX, RV]

            int idIndex = findColumnIndex(starTable, "meta.id", List.of("^ID", "^NAME", "^MAIN_ID"));
            int raIndex = findColumnIndex(starTable, "pos.eq.ra", List.of("^RA"));
            int decIndex = findColumnIndex(starTable, "pos.eq.dec", List.of("^DEC"));

            String errorMessage = "";

            if (idIndex == -1) {
                errorMessage += "-- unable to find 'ID' or 'NAME' column";
            }

            if (raIndex == -1) {
                errorMessage += "-- unable to find 'RA' column";
            }

            if (decIndex == -1) {
                errorMessage += "-- unable to find 'DEC' column";
            }

            if (!errorMessage.isEmpty()) {
                throw new WebApplicationException(errorMessage, 400);
            }

            String raUnit = starTable.getColumnInfo(raIndex).getUnitString();
            String decUnit = starTable.getColumnInfo(decIndex).getUnitString();

            if (raUnit != null && !List.of("d", "deg", "degs", "degrees").contains(raUnit)) {
                throw new WebApplicationException("coordinates must be given in units of degrees", 400);
            }

            if (decUnit != null && !List.of("d", "deg", "degs", "degrees").contains(decUnit)) {
                throw new WebApplicationException("coordinates must be given in units of degrees", 400);
            }

            //these are optional - either they don't exist as columns or they exist but have null data
            int pmRaIndex = findColumnIndex(starTable, "", List.of("^PMRA")); // proper motion has UCD pos.pm as vector
            int pmDecIndex = findColumnIndex(starTable, "", List.of("^PMDEC"));
            int plxIndex = findColumnIndex(starTable, "pos.parallax", List.of("^PLX"));
            int rvIndex = findColumnIndex(starTable, "", List.of("^RV"));

            HashMap<Integer, String> nonUniqueNames = new HashMap<>();

            List<String> tableTargetNames = new ArrayList<>();

            for (int i = 0; i < nRow; i++) {
                String name = (String) starTable.getCell(i, idIndex);

                //check for uniqueness in both the existing names and the names in the table
                if (existingNames.contains(name) || tableTargetNames.contains(name)) {
                    //the name is not unique, collect the offending name and row count to feed back to user
                    int row = i + 1;
                    nonUniqueNames.put(row, name);
                }

                tableTargetNames.add(name);

                double raValue =  ((Number)starTable.getCell(i, raIndex)).doubleValue();
                double decValue =  ((Number)starTable.getCell(i, decIndex)).doubleValue();

                Object pmRaObj = pmRaIndex == -1 ? null : starTable.getCell(i, pmRaIndex);
                Object pmDecObj = pmDecIndex == -1 ? null :starTable.getCell(i, pmDecIndex);
                Object plxObj = plxIndex == -1 ? null : starTable.getCell(i, plxIndex);
                Object rvObj = rvIndex == -1 ? null : starTable.getCell(i, rvIndex);

                double pmRaValue = pmRaIndex == -1 ? 0 : Objects.equals(pmRaObj.toString(), "NaN") ? 0 :
                        ((Number)pmRaObj).doubleValue();
                String pmRaUnit = pmRaIndex == -1 ? "" :
                        starTable.getColumnInfo(pmRaIndex).getUnitString() == null ? "mas.yr-1" :
                                starTable.getColumnInfo(pmRaIndex).getUnitString();

                double pmDecValue = pmDecIndex == -1 ? 0 : Objects.equals(pmDecObj.toString(), "NaN") ? 0 :
                        ((Number)pmDecObj).doubleValue();
                String pmDecUnit = pmDecIndex == -1 ? "" :
                        starTable.getColumnInfo(pmDecIndex).getUnitString() == null ? "mas.yr-1" :
                                starTable.getColumnInfo(pmDecIndex).getUnitString();

                double plxValue = plxIndex == -1 ? 0 : Objects.equals(plxObj.toString(), "NaN") ? 0 :
                        ((Number)plxObj).doubleValue();
                String plxUnit = plxIndex == -1 ? "" :
                        starTable.getColumnInfo(plxIndex).getUnitString() == null ? "mas" :
                                starTable.getColumnInfo(plxIndex).getUnitString();

                double rvValue = rvIndex == -1 ? 0 : Objects.equals(rvObj.toString(), "NaN") ? 0 :
                        ((Number)rvObj).doubleValue();
                String rvUnit = rvIndex == -1 ? "" :
                        starTable.getColumnInfo(rvIndex).getUnitString() == null ? "km.yr-1" :
                                starTable.getColumnInfo(rvIndex).getUnitString();


                targets.add(CelestialTarget.createCelestialTarget(c -> {
                            c.sourceName = name;
                            c.sourceCoordinates = new EquatorialPoint(
                                    new RealQuantity(raValue, new Unit("degrees")),
                                    new RealQuantity(decValue, new Unit("degrees")),
                                    spaceSys
                            );
                            c.positionEpoch = new Epoch("J2000.0");

                            //optional stuff
                            c.pmRA = pmRaIndex == -1 ? null : Objects.equals(pmRaObj.toString(), "NaN") ? null :
                                    new RealQuantity(pmRaValue, new Unit(pmRaUnit));

                            c.pmDec = pmDecIndex == -1 ? null : Objects.equals(pmDecObj.toString(), "NaN") ? null :
                                    new RealQuantity(pmDecValue, new Unit(pmDecUnit));

                            c.parallax = plxIndex == -1 ? null : Objects.equals(plxObj.toString(), "NaN") ? null :
                                    new RealQuantity(plxValue, new Unit(plxUnit));

                            c.sourceVelocity = rvIndex == -1 ? null : Objects.equals(rvObj.toString(), "NaN") ? null :
                                    new RealQuantity(rvValue, new Unit(rvUnit));
                        })
                );
            }

            if (!nonUniqueNames.isEmpty()) {
                StringBuilder nonUniqueNamesBuilder = new StringBuilder();
                nonUniqueNamesBuilder
                        .append("Unable to store target list as there are non-unique names at the following rows:\n");

                for (Map.Entry<Integer, String> entry : nonUniqueNames.entrySet()) {
                    nonUniqueNamesBuilder
                            .append(entry.getKey()).append(": ")
                            .append(entry.getValue()).append("\n");
                }

                throw new WebApplicationException(nonUniqueNamesBuilder.toString(), 400);
            }

        } catch (IOException e) {
            throw new WebApplicationException(e.getMessage(), 500);
        }

        return targets;
    }


    private static StarTable loadStarTable( String resourceLocation ) throws IOException {
        return new StarTableFactory(true).makeStarTable( resourceLocation );
    }

    private static int findColumnIndex(StarTable starTable, String columnNamePattern)  {

        Pattern pattern = Pattern.compile(columnNamePattern, Pattern.CASE_INSENSITIVE);

        int iCol = 0;
        int nCol = starTable.getColumnCount();

         do {
            Matcher matcher = pattern.matcher(starTable.getColumnInfo(iCol).getName());
            if (matcher.find()) break;
        } while (++iCol < nCol);

        if (iCol >= nCol) {
            return -1;
        }

        return iCol;
    }

    private static int findColumnIndex(StarTable starTable, String ucd, List<String> columnNamePatterns) {
        // First try to find by UCD
        int iCol = 0;
        int nCol = starTable.getColumnCount();
        do {
            if (starTable.getColumnInfo(iCol).getUCD() != null && 
                Arrays.asList(starTable.getColumnInfo(iCol).getUCD().split(";")).contains(ucd)) {
                return iCol;
            }
        } while (++iCol < nCol);
        
        // If not found by UCD, try by column name patterns
        for (String pattern : columnNamePatterns) {
            int result = findColumnIndex(starTable, pattern);
            if (result != -1) {
                return result;
            }
        }
        
        return -1;
    }

    private static long getRows(StarTable starTable) throws WebApplicationException, IOException {
        long nRow = starTable.getRowCount(); //can return a -1 if row count is not easily determined

        if (nRow == -1) {
            //determine row count using a 'RowSequence' - initial position is before the first row
            RowSequence rowSequence = starTable.getRowSequence();
            nRow = 0;
            while (rowSequence.next()) {
                nRow++;
            }
            rowSequence.close();
        }

        return nRow;
    }
}
