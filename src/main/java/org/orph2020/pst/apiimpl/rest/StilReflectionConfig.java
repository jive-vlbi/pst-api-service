package org.orph2020.pst.apiimpl.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Registers the STIL (starlink) table builder classes for reflection so they are
 * available in the native executable built for production.
 *
 * STIL's {@link uk.ac.starlink.table.StarTableFactory} instantiates these builders
 * at runtime via {@code Class.forName(...)}, which GraalVM's closed-world analysis
 * cannot detect. Without this registration the native image logs
 * "... not found - can't register" for every builder and target list uploads fail
 * with "no table handlers available".
 *
 * The class names below are a subset of the default and known builder lists of
 * {@code StarTableFactory} in STIL 4.1.4. The Parquet, PDS4, Feather and GBIN
 * builders are deliberately omitted: they depend on optional libraries
 * (parquet-mr, NASA pds4-jparser, Gaia tools) that are not on the classpath,
 * which makes the native image build fail with "Discovered unresolved method
 * during parsing" if they are registered. StarTableFactory logs them as
 * "not found - can't register" and carries on, matching JVM behaviour for
 * absent optional formats.
 */
@RegisterForReflection(classNames = {
        // default (auto-detected) builders
        "uk.ac.starlink.votable.FitsPlusTableBuilder",
        "uk.ac.starlink.votable.ColFitsPlusTableBuilder",
        "uk.ac.starlink.fits.ColFitsTableBuilder",
        "uk.ac.starlink.fits.FitsTableBuilder",
        "uk.ac.starlink.votable.VOTableBuilder",
        "uk.ac.starlink.cdf.CdfTableBuilder",
        "uk.ac.starlink.ecsv.EcsvTableBuilder",
        "uk.ac.starlink.table.formats.MrtTableBuilder",
        // known (named format) builders
        "uk.ac.starlink.table.formats.AsciiTableBuilder",
        "uk.ac.starlink.table.formats.CsvTableBuilder",
        "uk.ac.starlink.table.formats.TstTableBuilder",
        "uk.ac.starlink.table.formats.IpacTableBuilder",
        "uk.ac.starlink.table.formats.WDCTableBuilder"
})
public class StilReflectionConfig {
}
