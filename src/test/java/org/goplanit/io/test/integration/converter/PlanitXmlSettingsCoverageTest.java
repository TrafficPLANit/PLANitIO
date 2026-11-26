package org.goplanit.io.test.integration.converter;

import org.goplanit.io.converter.demands.PlanitDemandsReaderSettings;
import org.goplanit.io.converter.demands.PlanitDemandsWriterSettings;
import org.goplanit.io.converter.demands.PlanitDiscreteDemandsReaderSettings;
import org.goplanit.io.converter.demands.PlanitDiscreteDemandsWriterSettings;
import org.goplanit.io.converter.intermodal.PlanitIntermodalReaderSettings;
import org.goplanit.io.converter.intermodal.PlanitIntermodalWriterSettings;
import org.goplanit.utils.settings.SettingsCoverage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the annotations of every PLANit reader and writer settings class cover those settings: every method
 * changing settings is a key, is covered by one, or is marked as no key; every getter of nested settings is marked;
 * every key has a read; and every key a convenience is covered by exists
 * <p>
 * The intermodal reader and writer settings reach the network, zoning, service network and routed services settings
 * through the files they name; the demands and discrete demands settings are reached on their own.
 * </p>
 *
 * @author markr
 */
public class PlanitXmlSettingsCoverageTest {

  /** the coverage of every settings class the PLANit readers and writers reach */
  private static final SettingsCoverage COVERAGE = SettingsCoverage.of(PlanitIntermodalReaderSettings.class,
      PlanitIntermodalWriterSettings.class, PlanitDemandsReaderSettings.class, PlanitDiscreteDemandsReaderSettings.class,
      PlanitDemandsWriterSettings.class, PlanitDiscreteDemandsWriterSettings.class);

  /** Every method changing settings has exactly one marker */
  @Test
  public void everyMethodChangingSettingsHasExactlyOneMarker() {
    assertEquals(List.of(), COVERAGE.findUnmarkedMethodsChangingSettings());
  }

  /** Every getter of nested settings is a section, a file reference, the read of a key, or marked as no key */
  @Test
  public void everyGetterOfNestedSettingsIsMarked() {
    assertEquals(List.of(), COVERAGE.findUnmarkedGettersOfNestedSettings());
  }

  /** Every key has a read of the same key, so settings can be written out */
  @Test
  public void everyKeyHasARead() {
    assertEquals(List.of(), COVERAGE.findKeysWithoutRead());
  }

  /** Every key a convenience is covered by exists, a key of a part found through the key naming the part's file */
  @Test
  public void everyCoveringKeyExists() {
    assertEquals(List.of(), COVERAGE.findUnknownCoveringKeys());
  }
}
